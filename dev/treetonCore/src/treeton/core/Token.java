/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnType;

public interface Token extends Treenotation, Comparable {
    public int compareTo(Token anotherToken);

    public int compareTo(Fraction f);

    public int compareTo(double f);

    public boolean corrupted(TrnType type);

    public Token getNextToken();

    public Token getPreviousToken();

    public boolean hasParentOfType(TrnType tp);
}
