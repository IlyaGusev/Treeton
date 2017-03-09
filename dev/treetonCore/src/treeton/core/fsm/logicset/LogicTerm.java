/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

import treeton.core.fsm.Term;
import treeton.core.util.collector.Collector;
import treeton.core.util.collector.CollectorException;
import treeton.core.util.collector.Mutable;

public class LogicTerm implements IsLogicSet, Term {
    public static final LogicTermEPS EPS = new LogicTermEPS();
    private static final IsLogicSet EPS_SET = new IsLogicSet() {
        public IsLogicSet and(IsLogicSet b) {
            return null;
        }

        public IsLogicSet not() {
            return null;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean isMember(Object o) {
            return false;
        }

        public String toString() {
            return "ε";
        }
    };
    private IsLogicSet _container;
    private String _name = "";

    public LogicTerm(IsLogicSet set) {
        _container = set;
    }

    public LogicTerm() {
    } //do not remove

    public LogicTerm not() {
        return (_container != null) ?
                new LogicTerm(_container.not())
                : null; //TODO: should create _container=ANYSET
    }

    public boolean isEmpty() {
        return (_container == null) || _container.isEmpty();
    }

    public LogicTerm and(IsLogicSet a) {
        return ((this._container != null)
                && (this.getClass() == a.getClass())
                && (((LogicTerm) a)._container != null)) ?
                new LogicTerm(_container.and(((LogicTerm) a)._container))
                : null;
    }

    public String getString() {
        return _container.toString();
    }

    public String toString() {
        return getString();
    }

    public boolean isMember(Object o) {
        return _container.isMember(o);
    }

    public final static class LogicTermEPS extends LogicTerm {
        private LogicTermEPS() {
            super(EPS_SET);
        }
    }

    public static class LogicTermCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicTerm t = (LogicTerm) o;
            t._container = (IsLogicSet) col.get();

        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicTerm t = (LogicTerm) o;
            col.put(t._container);
        }
    }

    public static class LogicTermEPSCollectable extends Mutable {
        public Object newInstance(Collector col, Class c) throws IllegalAccessException, InstantiationException, CollectorException {
            return EPS;
        }

        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
        }
    }

}
