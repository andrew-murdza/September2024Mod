package net.amurdza.examplemod.util;

@FunctionalInterface
public interface QuintConsumer<A,B,C,D,E> {
    public abstract void accept(A a, B b, C c, D d, E e);
}
