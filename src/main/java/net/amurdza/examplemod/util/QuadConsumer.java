package net.amurdza.examplemod.util;

@FunctionalInterface
public interface QuadConsumer<A,B,C,D> {
    public abstract void accept(A a, B b, C c, D d);
}
