/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnTypeSet;
import treeton.core.util.ClosableIterator;

public interface FollowIteratorInterface extends ClosableIterator {
    void reset(TrnTypeSet input, TrnTypeSet followTypes, Token after);

    void reset(TrnTypeSet followTypes, Token after);
}
