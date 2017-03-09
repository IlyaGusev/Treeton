/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public class NumeratedObjectEx extends NumeratedObject {
    public NumeratedObjectEx next;

    public NumeratedObjectEx(int n, Object o) {
        super(n, o);
        next = null;
    }
}
