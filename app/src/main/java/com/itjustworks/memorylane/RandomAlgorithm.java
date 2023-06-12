package com.itjustworks.memorylane;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

/*
 * RandomAlgorithm.java
 *
 * Class Description: Randomly chooses next question set with bias.
 * Class Invariant: Randomisation depends on the weights of the question
 *                  sets, giving priority to the ones with higher weight
 *
 */

public class RandomAlgorithm<E> {
    private final NavigableMap<Double, E> map = new TreeMap<>();
    private final Random random;
    private double total = 0;

    public RandomAlgorithm() {
        this(new Random());
    }

    public RandomAlgorithm(Random random) {
        this.random = random;
    }

    public RandomAlgorithm<E> add(double weight, E result) {
        if (weight <= 0) return this;
        total += weight;
        map.put(total, result);
        return this;
    }

    public E next() {
        double value = random.nextDouble() * total;
        return Objects.requireNonNull(map.higherEntry(value)).getValue();
    }
}