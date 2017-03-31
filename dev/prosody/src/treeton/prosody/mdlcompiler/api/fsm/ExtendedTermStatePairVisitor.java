/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.fsm;

public interface ExtendedTermStatePairVisitor<T,V> {
    V execute(TermStatePair<T> pair, V straightRecursionObject);
}
