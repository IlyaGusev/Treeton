/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.fsm;

import treeton.prosody.mdlcompiler.api.custom.Action;
import treeton.prosody.mdlcompiler.api.custom.Filter;

import java.util.Set;
import java.util.List;

public interface State<T> {
    List<TermStatePair<T>> getTransitions();
    Set<TermStatePair<T>> getTransitionsSet();
    Set<Filter<T>> getFilters();
    List<Action<T>> getActions();

    boolean isFinal();

    void visit(StateVisitor<T> visitor, boolean childrenFirst, Set<State<T>> passedAlready);
    <V>void visitExtended(ExtendedTermStatePairVisitor<T,V> statePairVisitor, Set<State<T>> passedAlready, V forwardRecursionObject);
}
