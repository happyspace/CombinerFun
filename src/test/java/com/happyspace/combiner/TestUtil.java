package com.happyspace.combiner;


import java.util.concurrent.BlockingQueue;

public class TestUtil {

    protected static void fill(final BlockingQueue<Integer> bq, int with, int amount) {
        for (int i = 0; i < amount; i++) {
            bq.add(with);
        }
    }
}
