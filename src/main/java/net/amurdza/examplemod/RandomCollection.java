package net.amurdza.examplemod;

import net.minecraft.util.RandomSource;

import java.util.NavigableMap;
import java.util.TreeMap;

public class RandomCollection<E> {
    private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
    private double total = 0;

    public RandomCollection() {

    }

    public RandomCollection<E> add(double weight, E result) {
        if (weight <= 0) {
            return this;
        }
        total = total + weight;
        map.put(total, result);
        return this;
    }

    public E next(RandomSource random) {
        return map.higherEntry(random.nextDouble() * total).getValue();
    }
}