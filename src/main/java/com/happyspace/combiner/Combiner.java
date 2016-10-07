package com.happyspace.combiner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * A Combiner multiplexes items from multiple input queues into a single output
 * queue. Each input queue has a priority, which determines the frequency at
 * which its items are removed and added to the output queue. E.g. if queue A
 * has priority 9.0, and queue B has priority 1.0, then for every 10 items added
 * to the output queue, 9 should come from queue A, and 1 should come from queue
 * B.
 * <p>
 * Input queues are dynamically added to the Combiner. If an input queue is seen
 * as empty for more than a given time, then it is removed from the Combiner.
 * Input queues can be dynamically removed from the Combiner.
 * </p>
 */
public abstract class Combiner<T>
{
    protected final SynchronousQueue<T> outputQueue;
    
    protected Combiner(SynchronousQueue<T> outputQueue)
    {
        this.outputQueue = outputQueue;
    }

    /**
     * Adds the given queue to this Combiner.
     */
    public abstract void addInputQueue(BlockingQueue<T> queue, double priority, long isEmptyTimeout, TimeUnit timeUnit)
        throws CombinerException;

    /**
     * Removes the given queue from this Combiner.
     */
    public abstract void removeInputQueue(BlockingQueue<T> queue) throws CombinerException;
    
    /**
     * Returns true if the given queue is currently an input queue to this Combiner.
     */
    public abstract boolean hasInputQueue(BlockingQueue<T> queue);
    
    public static class CombinerException extends Exception
    {
        public CombinerException()
        {
            super();
        }

        public CombinerException(String message, Throwable cause) {
            super(message, cause);
        }

        public CombinerException(String message)
        {
            super(message);
        }
    }
}
