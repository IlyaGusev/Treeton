/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import java.util.Iterator;

public interface State {
    Iterator<? extends TermStatePair> pairsIterator();

    int getNumberOfPairs();

    int getId();

    boolean isFinal();

    public String getString();
}
