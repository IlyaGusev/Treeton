/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public class NumeratedObject implements Numerated {
    public int n;
    public Object o;

    public NumeratedObject() {
        n = -1;
        o = null;
    }

    public NumeratedObject(int _n, Object _o) {
        n = _n;
        o = _o;
    }

    public int getNumber() {
        return n;
    }

    public Object clone() {
        return new NumeratedObject(n, o);
    }
}
