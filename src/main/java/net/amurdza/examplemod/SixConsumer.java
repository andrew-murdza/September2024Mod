package net.amurdza.examplemod;

@FunctionalInterface
public interface SixConsumer<A,B,C,D,E,F> {
    public abstract void accept(A a, B b, C c, D d, E e, F f);
}