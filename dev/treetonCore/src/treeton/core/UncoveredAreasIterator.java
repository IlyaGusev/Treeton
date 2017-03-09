/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

public interface UncoveredAreasIterator {
    boolean next();

    Token getStartToken();

    Token getEndToken();
}
