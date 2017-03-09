/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.Iterator;

public interface HandlingErrorIterator<T> extends Iterator<T> {
    Exception lastException();
}
