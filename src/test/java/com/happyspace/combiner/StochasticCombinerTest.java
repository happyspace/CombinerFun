package com.happyspace.combiner;

import org.junit.Test;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

// @RunWith(PowerMockRunner.class)
// @PrepareForTest(GenerationCombiner.class)
public class StochasticCombinerTest {


    @Test
    public void setProcessor() throws Exception {

    }

    @Test
    public void addInputQueue() throws Exception {

    }

    @Test
    public void removeInputQueue() throws Exception {

    }

    @Test
    public void removeQueues() throws Exception {

    }

    @Test
    public void addQueues() throws Exception {

    }

    /**
     * Integration test to confirm that timed out queues will be removed.
     */
    @Test
    public void queueTimeOut() throws Exception {
        SynchronousQueue<Integer> si = new SynchronousQueue<>();
        StochasticCombiner<Integer> gi = new StochasticCombiner<>(si);
        // create blocking queues
        BlockingQueue<Integer> q1 = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> q2 = new LinkedBlockingQueue<>();

        gi.addInputQueue(q1, 9.0, 10, TimeUnit.MICROSECONDS);
        gi.addInputQueue(q2, 1.0, 10, TimeUnit.MICROSECONDS);
        gi.process();
        TimeUnit.SECONDS.sleep(2);
        Set<QueueWrapper<Integer>> queues = gi.getQueues();
        assertTrue(queues.isEmpty());

    }

    @Test
    public void process() throws Exception {
        // create a combiner
        SynchronousQueue<Integer> si = new SynchronousQueue<>();
        StochasticCombiner<Integer> gi = new StochasticCombiner<>(si);
        // create blocking queues
        BlockingQueue<Integer> q1 = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> q2 = new LinkedBlockingQueue<>();
        TestUtil.fill(q1, 9, 1000);
        TestUtil.fill(q2, 1, 1000);

        gi.addInputQueue(q1, 9.0, 10, TimeUnit.SECONDS);
        gi.addInputQueue(q2, 1.0, 10, TimeUnit.SECONDS);
        // gi.addQueues();
        gi.process();

        int[] counts = new int[2];
        for (int i = 0; i < 1000; i++) {
            Integer take = si.take();
            // System.out.println(take);
            if(take == 9) {
                counts[0] += 1;
            }
            if(take == 1) {
                counts[1] += 1;
            }
        }
        System.out.println(counts[0]);
        System.out.println(counts[1]);

        double ex = (counts[0] / 1000.0);
        double predicted = 9.0 / 10.0;
        double diff = Math.abs(ex - predicted);
        System.out.println(ex);
        System.out.println(predicted);
        System.out.println(diff);

        assertTrue(diff < 1e-1);
    }

    @Test
    public void hasInputQueue() throws Exception {

    }

}