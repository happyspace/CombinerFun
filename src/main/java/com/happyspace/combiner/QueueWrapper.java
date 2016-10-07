package com.happyspace.combiner;

import org.threeten.extra.Temporals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper class for BlockingQueue to record metadata for that queue like empty time out.
 *
 * This class is only intended for use as a helper class to StochasticCombiner.
 * Thus package-private.
 */
final class QueueWrapper<T> implements Comparable<QueueWrapper> {

    // how long this queue has been empty.
    private Duration emptyPeriod;
    // when did the emptiness start
    private LocalDateTime emptyStartTime;
    // priority
    private final Double priority;
    // the queue it's self.
    private final BlockingQueue<T> queue;
    // how long this queue can remain empty.
    private final Duration isEmptyTimeout;

    public QueueWrapper(BlockingQueue<T> queue,
                        Double priority,
                        long isEmptyTimeout,
                        TimeUnit timeUnit) {
        this.emptyPeriod = Duration.ZERO;
        this.priority = priority;
        this.queue = queue;

        if(queue.isEmpty()) {
            this.emptyStartTime = LocalDateTime.now();
        }
        else {
            emptyStartTime = null;
        }
        // For Java 8 convert timeUnit to ChronoUnit.
        // Java 9 has a utility to do this conversion.
        ChronoUnit chronoUnit = Temporals.chronoUnit(timeUnit);
        this.isEmptyTimeout = Duration.of(isEmptyTimeout, chronoUnit);
    }

    // A
    protected QueueWrapper(BlockingQueue<T> queue){
        this(queue, 0.0, 0, TimeUnit.SECONDS);
    }

    /**
     * Intentionally violates the consistency with equals (see below).
     * @param o Object to compare.
     * @return if
     */
    @Override
    public int compareTo(QueueWrapper o) {
        return o.priority.compareTo(this.priority);
    }

    /**
     * Only check that the queue is equal so the removal of a queue will be 0(1).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueueWrapper that = (QueueWrapper) o;
        return queue.equals(that.queue);

    }
    
    /**
     * Only the queue contributes to the hash.
     */
    @Override
    public int hashCode() {
        return queue.hashCode();
    }

    /**
     * @return  Returns if this queue has timed out.
     */
    protected boolean isTimedOut() {
        return emptyPeriod.compareTo(isEmptyTimeout) > 0;
    }

    /**
     * Add time to a queue's empty time out if the queue is empty.
     * If there is no empty start time this will set one up.
     */
    protected void addToEmptyTimeOut() {
        if(!queue.isEmpty()){
            resetEmptyTimeOut();
            return;
        }
        if(emptyStartTime == null){
            this.emptyPeriod = Duration.ZERO;
            emptyStartTime = LocalDateTime.now();
        }
        else {
            Duration duration = Duration.between(emptyStartTime, LocalDateTime.now());
            emptyPeriod = emptyPeriod.plus(duration);
        }

    }

    /**
     * Reset the empty time out for this queue.
     */
    protected void resetEmptyTimeOut() {
        this.emptyPeriod = Duration.ZERO;
        this.emptyStartTime = null;
    }

    /**
     * @return if the queue is empty.
     */
    protected boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * @return the queue.
     */
    protected BlockingQueue<T> getQueue(){
        return this.queue;
    }


    /**
     * @return priority.
     */
    public Double getPriority() {
        return priority;
    }
}
