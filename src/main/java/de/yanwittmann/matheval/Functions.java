package de.yanwittmann.matheval;

public abstract class Functions {

    public interface Function2<A, B, R> {
        R apply(A a, B b);
    }

    public interface Function3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    public interface Function4<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }
}
