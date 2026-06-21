package net.amurdza.examplemod.util;

@FunctionalInterface
public interface SixConsumer<A,B,C,D,E,F> {
    void accept(A a, B b, C c, D d, E e, F f);
}