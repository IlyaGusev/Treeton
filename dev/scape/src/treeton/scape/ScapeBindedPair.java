/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.fsm.State;
import treeton.core.fsm.Term;
import treeton.core.fsm.TermStatePair;

public abstract class ScapeBindedPair implements TermStatePair {
    ScapeTerm t;
    ScapeState s;
    ScapeBindingSet bindingSet;
    ScapeBindedPair next;
    long label;

    ScapeBindedPair(ScapeTerm _t, ScapeState _s, ScapeBindingSet _bindings) {
        t = _t;
        s = _s;
        bindingSet = _bindings;
        next = null;
        label = -1;
    }

    public Term getTerm() {
        return t;
    }

    public State getState() {
        return s;
    }

    public String getString() {
        StringBuffer b = new StringBuffer();
        b.append("{");
        b.append(t.getString());
        b.append("; ");
        b.append(bindingSet != null ? bindingSet.getString() : "");
        b.append("; ");
        b.append(s.getId());
        b.append("}");
        return b.toString();
    }
}
