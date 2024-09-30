package net.amurdza.examplemod;

@FunctionalInterface
public interface QuadFunction<A,B,C,D,E> {
    public abstract E apply(A a, B b, C c, D d);
}
