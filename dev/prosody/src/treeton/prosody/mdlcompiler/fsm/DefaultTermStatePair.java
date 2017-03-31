/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.prosody.mdlcompiler.api.fsm.TermStatePair;
import treeton.prosody.mdlcompiler.api.custom.Term;
import treeton.prosody.mdlcompiler.api.fsm.State;

public class DefaultTermStatePair<T> extends DefaultBinded implements TermStatePair<T> {
    private Term term;
    private State<T> state;

    private boolean hashable = true;

    public DefaultTermStatePair(Term term, State<T> state) {
        this.term = term;
        this.state = state;
    }

    public DefaultTermStatePair() {
    }

    public Term getTerm() {
        return term;
    }

    public State<T> getState() {
        return state;
    }

    public void setTerm(Term term) {
        this.term = term;
    }

    public void setState(State<T> state) {
        this.state = state;
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    @Override
    public boolean equals(Object o) {
        if (hashable) {
            if (!super.equals(o))
                return false;
            DefaultTermStatePair that = (DefaultTermStatePair) o;

            return state.equals(that.state) && !(term != null ? !term.equals(that.term) : that.term != null);
        } else {
            return this == o;
        }
    }

    private int hash = 0;

    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }

        if (hashable) {
            int result = super.hashCode();
            result = 31 * result + (term != null ? term.hashCode() : 0);
            result = 31 * result + state.hashCode();
            hash = result;
        } else {
            hash = System.identityHashCode(this);
        }
        return hash;
    }

    public void setHashable(boolean hashable) {
        this.hashable = hashable;
    }
}
