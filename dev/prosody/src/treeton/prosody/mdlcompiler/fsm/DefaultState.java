/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.prosody.mdlcompiler.api.custom.Action;
import treeton.prosody.mdlcompiler.api.custom.Filter;
import treeton.prosody.mdlcompiler.api.fsm.*;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class DefaultState<T> implements State<T> {
    protected List<TermStatePair<T>> transitions;
    protected Set<TermStatePair<T>> transitionsSet;

    private List<Action<T>> actions = new ArrayList<Action<T>>();
    private Set<Action<T>> actionsSet = new HashSet<Action<T>>();
    private Set<Filter<T>> filters = new HashSet<Filter<T>>();
    private boolean isFinal;

    public DefaultState() {
        init();
    }

    protected void init() {
        transitions = new ArrayList<TermStatePair<T>>();
        transitionsSet = new HashSet<TermStatePair<T>>();
    }

    public List<TermStatePair<T>> getTransitions() {
        return transitions;
    }

    public Set<TermStatePair<T>> getTransitionsSet() {
        return transitionsSet;
    }

    public Set<Filter<T>> getFilters() {
        return filters;
    }

    public List<Action<T>> getActions() {
        return actions;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void visit(StateVisitor<T> visitor, boolean childrenFirst, Set<State<T>> passedAlready) {
        if (passedAlready.contains(this))
            return;

        passedAlready.add(this);

        if (childrenFirst) {
            for (TermStatePair<T> pair : getTransitions()) {
                pair.getState().visit(visitor,childrenFirst,passedAlready);
            }
        }

        visitor.execute(this);

        if (!childrenFirst) {
            for (TermStatePair<T> pair : getTransitions()) {
                pair.getState().visit(visitor,childrenFirst,passedAlready);
            }
        }
    }

    public <V>void visitExtended(ExtendedTermStatePairVisitor<T,V> statePairVisitor, Set<State<T>> passedAlready, V straightRecursionObject) {
        if (passedAlready.contains(this))
            return;

        passedAlready.add(this);

        for (TermStatePair<T> pair : getTransitions()) {
            V v = statePairVisitor.execute(pair, straightRecursionObject);
            pair.getState().visitExtended(statePairVisitor,passedAlready,v);
        }
    }

    public void addFilter(Filter<T> filter) {
        filters.add(filter);
    }

    public void addTransition(TermStatePair<T> pair) {
        if (transitionsSet.add(pair))
            getTransitions().add(pair);
    }

    public void addAction(Action<T> action) {
        if (actionsSet.add(action))
            actions.add(action);
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }
}
