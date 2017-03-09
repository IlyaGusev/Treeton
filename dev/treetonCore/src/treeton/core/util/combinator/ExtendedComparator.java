/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import java.util.Comparator;

public interface ExtendedComparator<T> extends Comparator<T> {
    double getPriority(T object);
}
