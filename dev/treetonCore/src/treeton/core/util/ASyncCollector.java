/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.concurrent.atomic.AtomicInteger;

public class ASyncCollector<E> implements Collector<E> {
    E[] data;
    AtomicInteger i = new AtomicInteger(0);

    public ASyncCollector(int capacity) {
        data = (E[]) new Object[capacity];
    }

    public int size() {
        return i.intValue();
    }

    public E get(int index) {
        return data[index];
    }

    public void add(E object) {
        data[i.getAndIncrement()] = object;
    }
}
