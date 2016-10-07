package com.happyspace.combiner;

import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Test QueueWrapper
 */
public class QueueWrapperTest {
    @Test
    public void compareTo() throws Exception {

    }

    /**
     * Test an empty queue for time out.
     * @throws Exception
     */
    @Test
    public void isTimedEmptyQueueOut() throws Exception {
        BlockingQueue<Integer> bqi = new LinkedBlockingQueue<>();
        QueueWrapper qw = new QueueWrapper(bqi, 9.0, 1, TimeUnit.MICROSECONDS);
        TimeUnit.SECONDS.sleep(1);
        qw.addToEmptyTimeOut();
        assertTrue(qw.isTimedOut());
    }

    /**
     * Test an non-empty queue for time out.
     *
     */
    @Test
    public void isTimedOut() throws Exception {
        BlockingQueue<Integer> bqi = new LinkedBlockingQueue<>();
        bqi.add(1);
        QueueWrapper qw = new QueueWrapper(bqi, 9.0, 1, TimeUnit.MICROSECONDS);
        bqi.take();
        qw.addToEmptyTimeOut();
        TimeUnit.SECONDS.sleep(1);
        qw.addToEmptyTimeOut();
        assertTrue(qw.isTimedOut());
    }

    @Test
    public void addToEmptyTimeOut() throws Exception {

    }

    @Test
    public void resetEmptyTimeOut() throws Exception {

    }

}