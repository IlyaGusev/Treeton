/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

public class ScapeFSMPair extends ScapeBindedPair {
    boolean reverse;

    ScapeFSMPair(ScapeTreenotationTerm _t, ScapeFSMState _s) {
        super(_t, _s, null);
        reverse = false;
    }
}
