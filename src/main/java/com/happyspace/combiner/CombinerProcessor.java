package com.happyspace.combiner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;

public interface CombinerProcessor<T> {
    public void process(BlockingQueue<QueueWrapper<T>> input, SynchronousQueue<T> output) throws Combiner.CombinerException, ExecutionException, InterruptedException;
}
