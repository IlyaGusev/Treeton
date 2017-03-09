/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.Iterator;

public interface ClosableIterator extends Iterator {
    public void close();
}
