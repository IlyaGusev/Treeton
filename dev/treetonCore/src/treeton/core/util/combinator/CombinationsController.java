/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import treeton.core.util.IdProvider;

public interface CombinationsController {
    boolean combinationIsMarked(boolean markIfNot, IdProvider... ids);

    void unmark(IdProvider... ids);
}
