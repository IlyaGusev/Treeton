/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.fsm;

import treeton.prosody.mdlcompiler.api.custom.Term;

public interface TermStatePair<T> extends Binded {
    Term getTerm();
    State<T> getState();
}
