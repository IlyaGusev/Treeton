/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.Token;
import treeton.core.TreenotationStorage;
import treeton.core.UncoveredAreasIterator;

public class UncoveredAreasIteratorPlug implements UncoveredAreasIterator {
    Token start;
    Token end;
    boolean first;

    public UncoveredAreasIteratorPlug(TreenotationStorage storage) {
        start = storage.firstToken();
        end = storage.lastToken();
        first = true;
    }

    public boolean next() {
        if (first) {
            first = false;
            return true;
        }
        return false;
    }

    public Token getStartToken() {
        return start;
    }

    public Token getEndToken() {
        return end;
    }
}
