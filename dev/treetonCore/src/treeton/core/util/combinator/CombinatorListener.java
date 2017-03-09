/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

public interface CombinatorListener<T> {
    void combinationChanged(Combinator<T> source);
}
