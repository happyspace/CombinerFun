package com.happyspace.combiner;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Implements Combiner to create a rendezvous point to hand off work.
 *
 * Work will be handed off according to a relative priority. This
 * @param <T>
 */

public class StochasticCombiner<T> extends Combiner<T> {
    // how many queues to select for hand off.
    private static final int PROCESS_BATCH_SIZE = 10;
    // the maximum priority used below to randomly select a queue
    private AtomicReference<Double> maxWeight = new AtomicReference<>(0.0);
    // queues held in priority order.
    // use a set for identity and a tree for a balance between add, remove and find.
    private final Set<QueueWrapper<T>> queues = new TreeSet<>(Collections.reverseOrder());
    // holding area for queues to remove.
    private final Set<QueueWrapper<T>> remove = Collections.synchronizedSet(new TreeSet<>());
    // holding area for queues to add.
    private final Set<QueueWrapper<T>> add = Collections.synchronizedSet(new TreeSet<>());
    // a thread to hand off work
    private CombinerProcessor<T> processor;
    // a thread to do the accounting for this data structure.
    private final ExecutorService service;
    // a channel to send queues to hand off.
    private final BlockingQueue<QueueWrapper<T>> channel;
    // a guard so processes are not started more than once.
    private AtomicBoolean hasStarted = new AtomicBoolean(false);
    // the main lock for this data structure.
    // support three phases of processing add, remove and identify queues for hand off.
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * Create a channel for hand off. Create a service to do the hand off of work.
     * Create a service to handle the accounting for this datastructure.
     * @param outputQueue the hand off point.
     */
    public StochasticCombiner(SynchronousQueue<T> outputQueue) {
        super(outputQueue);
        channel = new LinkedBlockingQueue<>();
        service = Executors.newSingleThreadExecutor();
        processor = new CombinerProcessorImpl<>();
    }

    /**
     * Adds a queue to a holding area which will be added to the main data structure
     * at an appropriate time.
     *
     * @param queue          a queue to be processed
     * @param priority       the priority of the queue
     * @param isEmptyTimeout how long the queue can be empty
     * @param timeUnit       the unit of time to measure isEmptyTimeout
     * @throws CombinerException an exception if the queue can not be added
     */
    @Override
    public void addInputQueue(BlockingQueue<T> queue,
                              double priority, long isEmptyTimeout,
                              TimeUnit timeUnit) throws CombinerException {
        if (!hasInputQueue(queue)) {
            QueueWrapper<T> qw = new QueueWrapper<>(queue, priority, isEmptyTimeout, timeUnit);
            add.add(qw);
        }
    }

    /**
     * Adds a queue to a holding area which will be removed from the main data structure
     * at an appropriate time.
     *
     * @param queue a queue to be removed
     * @throws CombinerException an exception if the queue can not be removed.
     */
    @Override
    public void removeInputQueue(BlockingQueue<T> queue) throws CombinerException {
        // internal detail.
        QueueWrapper<T> qw = new QueueWrapper<>(queue);
        remove.add(qw);
    }

    /**
     * Acquire a lock to start a removal phase.
     * Remove queues that have been requested to be removed and those that have timed out.
     * Update the max priority.
     */

    protected void removeQueues() throws CombinerException {
        mainLock.lock();
        try {
            boolean shouldRecalculate = false;

            for (QueueWrapper<T> queue : queues) {
                if(queue.isTimedOut()) {
                    remove.add(queue);
                }
            }

            for (QueueWrapper<T> r : remove) {
                queues.remove(r);
                if (r.getPriority().equals(maxWeight.get())) {
                    shouldRecalculate = true;
                }
            }
            remove.clear();

            if (shouldRecalculate) {
                maxWeight.getAndSet(calculateMaxWeight());
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Acquire a lock to start an add phase.
     * Add queues that have been requested to be added.
     * Update the max priority.
     */
    protected void addQueues() throws CombinerException {
        if(add.isEmpty()) {
            return;
        }
        mainLock.lock();
        try {
            for (QueueWrapper<T> a : add) {
                queues.add(a);
                if (a.getPriority() > maxWeight.get()) {
                    maxWeight.set(a.getPriority());
                }
            }
            add.clear();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Create a thread that will do the accounting for this data structure.
     * The thread will also select items for hand off based on priority.
     * <p>
     * Create another thread that will simply do the hand off of items T.
     */
    protected void process() {
        // gard start
        if (!hasStarted.get()) {
            Runnable r = () -> {
                try {
                    processor.process(channel, outputQueue);
                } catch (CombinerException | ExecutionException | InterruptedException e) {
                    service.shutdown();
                }
                while (!service.isShutdown()) {
                    try {
                        addQueues();
                        processOutput(PROCESS_BATCH_SIZE);
                        removeQueues();
                        Thread.sleep(5);
                    } catch (CombinerException | InterruptedException e) {
                        service.shutdown();
                    }
                }
            };
            service.execute(r);
        }
    }

    /**
     * Lock to create output. Note that queues could be removed
     * from the main data structure
     * while they are being processed by the processor.
     * Eventually these will be consistent.
     */
    private void processOutput(int amount) {
        mainLock.lock();
        try {
            if (!queues.isEmpty()) {
                if(channel.size() < PROCESS_BATCH_SIZE / 2) {
                    channel.addAll(generateOutput(amount));
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Use 'stochastic acceptance' to select a random weighted channel.
     * this code is based on.
     * https://en.wikipedia.org/wiki/Fitness_proportionate_selection#Java_-_stochastic_acceptance_version
     */
    private List<QueueWrapper<T>> generateOutput(int amount) {
        List<QueueWrapper<T>> results = new ArrayList<>();
        // if there is just one
        if (queues.size() == 1) {
            results.addAll(queues);
        } else {
            List<QueueWrapper<T>> list = queues.stream().collect(Collectors.toCollection(ArrayList::new));
            double mm = this.maxWeight.get();
            boolean notAccepted;
            int index = 0;
            Random rnd = new Random();
            for (int i = 0; i < amount; i++) {
                notAccepted = true;
                while (notAccepted) {
                    index = rnd.nextInt(list.size());
                    double pp = list.get(index).getPriority();
                    if (Math.random() < pp / (double) mm) {
                        notAccepted = false;
                    }
                }
                results.add(list.get(index));
            }
        }
        return results;
    }

    /**
     * Returns whether the queue is registered to this combiner.
     *
     * @param queue a queue to check
     * @return if the queue is registered.
     */
    @Override
    public boolean hasInputQueue(BlockingQueue<T> queue) {
        // private detail
        QueueWrapper<T> qw = new QueueWrapper<>(queue);
        return queues.contains(qw);
    }

    /**
     * On delete max priority may need to be re-calculated.
     * @return Max priority of the current set of queues.
     */
    private double calculateMaxWeight() {
        double max = 0.0;
        for (QueueWrapper<T> queue : queues) {
            if (queue.getPriority() > max) {
                max = queue.getPriority();
            }
        }
        return max;
    }

    /**
     * For test. Powermockito lost its mind...
     * Refactor when java valid byte code errors are not being thrown.
     */
    protected Set<QueueWrapper<T>> getQueues() {
        return queues;
    }
}
