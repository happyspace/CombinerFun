package com.happyspace.combiner;


import java.util.concurrent.SynchronousQueue;

public class App {

    public static void main(String[] args) {
        SynchronousQueue<Integer> si = new SynchronousQueue<>();
        StochasticCombiner<Integer> sc = new StochasticCombiner(si);
        sc.process();
        while (true) {}
    }

}
