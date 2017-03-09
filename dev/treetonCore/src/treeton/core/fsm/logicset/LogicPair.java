/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

import treeton.core.fsm.TermStatePair;
import treeton.core.util.collector.Collector;
import treeton.core.util.collector.CollectorException;
import treeton.core.util.collector.Mutable;

public class LogicPair implements /*IsLogicSet,*/ TermStatePair {
    private String _name = "";
    private LogicState _nextState = null;
    private LogicState _parent = null;
    private LogicTerm _term = null;

    public LogicPair(LogicState parent, LogicState nextState, LogicTerm term) {
        _parent = parent;
        _nextState = nextState;
        _term = term;
    }


    public LogicPair() {
    } //do not remove

    public String getString() {
        return _parent.toString() + "--" + _term.toString() + "--" + _nextState.toString();
    }

    public String toString() {
        return getString();
    }

    public LogicState getState() {
        return _nextState;
    }

    public LogicTerm getTerm() {
        return _term;
    }

    public boolean equals(Object o) {
        if (o.getClass() != this.getClass()) return false;
        LogicPair p = (LogicPair) o;
        if (_parent != p._parent) return false; //TODO: add .equals to LogicState
        if (_nextState != p._nextState) return false;
//    return _term.equals(p._term);
        return _term.toString().equals(p._term.toString());
    }

    LogicState getParent() {
        return _parent;
    }

    void setParent(LogicState parent) {
        this._parent = parent;
    }

    void setNextState(LogicState nextState) {
        this._nextState = nextState;
    }

    public static class LogicPairCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicPair t = (LogicPair) o;
            t._nextState = (LogicState) col.get();
            t._parent = (LogicState) col.get();
            t._term = (LogicTerm) col.get();

        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicPair t = (LogicPair) o;
            col.put(t._nextState);
            col.put(t._parent);
            col.put(t._term);
        }
    }
}
