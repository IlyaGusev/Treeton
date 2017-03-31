/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.fsm;

public interface StateVisitor<T> {
    void execute(State<T> state);
}
