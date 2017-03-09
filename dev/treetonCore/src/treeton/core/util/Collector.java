/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public interface Collector<E> {
    int size();

    E get(int index);

    void add(E object);
}
