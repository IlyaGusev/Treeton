/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.fsm.TermStatePair;

public class ScapeDFSMPair extends ScapeBindedPair implements TermStatePair {
    ScapeDFSMPair(ScapeTerm _t, ScapeState _s, ScapeBindingSet _bindings) {
        super(_t, _s, _bindings);
    }
}
