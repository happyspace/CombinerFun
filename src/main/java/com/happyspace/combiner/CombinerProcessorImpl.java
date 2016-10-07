package com.happyspace.combiner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class CombinerProcessorImpl<T> implements CombinerProcessor<T> {
    // a thread to handle hand off.
    private final ExecutorService service;
    // a guard so this can only be started once.
    private AtomicBoolean hasStarted = new AtomicBoolean(false);

    /**
     * Create a new thread to handle hand off.
     */
    public CombinerProcessorImpl() {
        service = Executors.newSingleThreadExecutor();
    }

    /**
     * Start a thread to handle the hand off.
     * @param input A blocking queue of queues
     * @param output The hand off point.
     * @throws Combiner.CombinerException
     */
    @Override
    public void process(BlockingQueue<QueueWrapper<T>> input, SynchronousQueue<T> output) throws Combiner.CombinerException {
                if(!hasStarted.get()) {
                    hasStarted.set(true);
                    Runnable r = () -> {
                        while(!service.isShutdown()) {
                            try {
                                QueueWrapper<T> take = input.take();
                                BlockingQueue<T> queue = take.getQueue();

                                if(!queue.isEmpty()) {
                                    // make this as atomic as possible.
                                    output.put(queue.take());
                                    take.resetEmptyTimeOut();
                                }
                                else {
                                    take.addToEmptyTimeOut();
                                }
                            } catch (InterruptedException e) {
                                service.shutdown();
                            }
                        }
                    };
                    service.execute(r);
                }
            }
}
