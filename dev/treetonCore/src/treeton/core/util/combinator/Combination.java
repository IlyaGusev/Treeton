/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

public interface Combination<T> extends Comparable<Combination<T>> {
    Entry<T> getValue(int i);

    Double getNorm();

    int getSize();
}
