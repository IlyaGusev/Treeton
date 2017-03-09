/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnType;
import treeton.core.util.ClosableIterator;

public interface TypeIteratorInterface extends TrnIterator, ClosableIterator {
    public void reset(TrnType[] commonTypes, TrnType[] tokenTypes, Token from, Token to);

    public void reset(Token from, Token to);

    public void skipTillToken(Token until);
}
