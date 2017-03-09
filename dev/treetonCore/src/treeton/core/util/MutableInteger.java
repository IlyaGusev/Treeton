/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;


public class MutableInteger implements java.io.Serializable, Comparable {
    public int value;

    public MutableInteger(int _value) {
        value = _value;
    }

    public MutableInteger() {
        value = 0;
    }

    public int compareTo(Object o) {
        if (o instanceof MutableInteger) {
            return value - ((MutableInteger) o).value;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public String toString() {
        return Integer.toString(value);
    }
}

