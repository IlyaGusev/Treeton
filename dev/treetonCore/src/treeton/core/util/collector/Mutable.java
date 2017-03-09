/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.collector;

public abstract class Mutable<T> implements MutableCollectable<T> {
    public T readIn(Collector col) throws CollectorException {
        throw new CollectorException("Collectable Thing is Mutable. Use for read method readIn(Collector col,Object blankObject)");
    }

    public T newInstance(Collector col, Class<? extends T> c) throws IllegalAccessException, InstantiationException, CollectorException {
        return c.newInstance();
    }
}
