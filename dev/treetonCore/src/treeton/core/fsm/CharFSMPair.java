/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

public class CharFSMPair implements TermStatePair {
    private CharTerm t;
    private CharFSMState s;

    public CharFSMPair(CharTerm _t, CharFSMState _s) {
        t = _t;
        s = _s;
    }

    public Term getTerm() {
        return t;
    }

    public State getState() {
        return s;
    }

    public String getString() {
        return "";
    }
}
