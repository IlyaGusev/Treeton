/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.collector;

public interface MutableCollectable<T> extends Collectable<T> {
    public void readIn(Collector col, T o) throws CollectorException, ClassCastException;

    public T newInstance(Collector col, Class<? extends T> c) throws IllegalAccessException, InstantiationException, CollectorException;
}
