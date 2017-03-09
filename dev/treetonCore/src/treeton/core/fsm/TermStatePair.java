/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

public interface TermStatePair {
    Term getTerm();

    State getState();

    String getString();
}
