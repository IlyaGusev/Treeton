/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.Iterator;

public class NullIterator implements Iterator {
    int size;
    private int i;

    public NullIterator(int size) {
        this.size = size;
        i = 0;
    }

    public boolean hasNext() {
        return i < size;
    }

    public Object next() {
        i++;
        return null;
    }

    public void remove() {
    }
}
