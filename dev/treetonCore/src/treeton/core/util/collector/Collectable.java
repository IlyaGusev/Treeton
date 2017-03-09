/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.collector;

public interface Collectable<T> {
    void append(Collector col, T o) throws CollectorException, ClassCastException;

    T readIn(Collector col) throws CollectorException;
}