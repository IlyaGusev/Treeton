/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

import treeton.core.fsm.State;
import treeton.core.util.collector.Collector;
import treeton.core.util.collector.CollectorException;
import treeton.core.util.collector.Mutable;

import java.util.*;

public class LogicState implements State {
    ArrayList<LogicPair> pairs = new ArrayList<LogicPair>();
    Set<LogicState> madeFrom = null;
    ArrayList<Integer> startBinding = new ArrayList<Integer>();
    ArrayList<Integer> finishBinding = new ArrayList<Integer>();
    Object data = null;
    private String _name = "";
    private int _id = 0;
    private boolean _final = false;

    public LogicState() {
    }


    public LogicState(int id, String name) {
        assert id >= 0;
        _id = id;
        _name = name;
    }

    public int getNumberOfPairs() {
        return (pairs == null) ? 0 : pairs.size();
    }

    public String getString() {
        return _name
                + (startBinding == null || startBinding.size() == 0 ? "" : " s" + startBinding.toString())
                + (finishBinding == null || finishBinding.size() == 0 ? "" : " f" + finishBinding.toString())
                + (madeFrom == null ? "" : " made" + madeFrom.toString())
                + (isFinal() ? " Data:" + retrieveData() : "");
    }

    public String toString() {
        return getString();
    }

    public int getId() {
        return _id;
    }

    void setId(int id) {
        assert id >= 0;
        this._id = id;
    }

    public Iterator<LogicPair> pairsIterator() {
        return (pairs == null) ? null : pairs.iterator();
    }

    public boolean addPair(LogicPair pair) {
        if (pairs != null) {
            for (LogicPair p : pairs)
                if (p.equals(pair))
                    return false;
            pairs.add(pair);
            return true;
        }
        return false;
    }

    public boolean removePair(LogicPair pair) {
        if (pairs != null) return pairs.remove(pair);
        else return false;
    }

    public int match(String s, int i) {
        if (isFinal()) return i;
        if (i >= s.length()) return -1;
        for (LogicPair lp : pairs) {
            LogicTerm term = lp.getTerm();
            if (term.isMember(s.charAt(i))) return lp.getState().match(s, i + 1);
        }
        return -1;
    }

    public void matchBindings(ArrayList<String[]> vars, HashMap<Integer, ArrayList<Binding>> hash, String s, int i) {

        for (Integer bindId : finishBinding) {
            for (Binding b : hash.get(bindId)) {
                if (b.finish == null && b.start != null) b.Finish(i);
            }
        }
        for (Integer bindId : startBinding) {

            if (!hash.containsKey(bindId)) {
                Binding b = new Binding(bindId);
                b.Start(i);
                ArrayList<Binding> newA = new ArrayList<Binding>(1);
                newA.add(b);
                hash.put(bindId, newA);
            } else {
                Binding b = new Binding(bindId);
                b.Start(i);
                ArrayList<Binding> newA = hash.get(bindId);
                newA.add(b);
            }
        }

        if (isFinal()) {
            if (i == s.length()) {
                String[] sarr = new String[hash.values().size() + 1];
                for (ArrayList<Binding> al : hash.values())
                    for (Binding b : al) {
                        if (b.finish != null && b.start != null && b.finish >= b.start) {
                            if (sarr[b.id] == null)
                                sarr[b.id] = s.substring(b.start, b.finish);
                            else sarr[b.id] += "," + s.substring(b.start, b.finish);
                        }
                    }
                vars.add(sarr);
            }
        }

        for (LogicPair lp : pairs) {
            LogicTerm term = lp.getTerm();
            if (term == LogicTerm.EPS
                    || term.isEmpty() && i == s.length()) {
                lp.getState().matchBindings(vars, hash, s, i);
            } else if (i < s.length() && term.isMember(s.charAt(i))) {
                lp.getState().matchBindings(vars, hash, s, i + 1);
            }


        }
//    for(ArrayList<Binding> al : hash.values())
//      for(Binding b : al)
//      {
//

//        if(b.start!=null && b.start>i)b.start=null;
//        if(b.finish!=null && b.finish>i)b.finish=null;
//      }
        for (Integer bindId : finishBinding) {
            for (Binding b : hash.get(bindId)) {
                if (b.finish != null && b.finish == i) b.finish = null;
            }
        }
        for (Integer bindId : startBinding) {
            for (Binding b : hash.get(bindId)) {
                if (b.start != null && b.start == i) b.start = null;
            }
        }

    }

    public boolean isFinal() {
        return _final;
    }

    public void setFinal(boolean fin) {
        this._final = fin;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Set<Object> retrieveData() {
        HashSet<Object> set = new HashSet<Object>();
        addData(set);
        return set;
    }

    public void addData(Set target) {
        if (data != null) {
            target.add(data);
        } else {
            if (madeFrom != null) {
                for (LogicState s : madeFrom) {
                    s.addData(target);
                }
            }
        }
    }

    public Object getFirstMadeFromData() {
        if (data != null) return data;
        if (madeFrom == null) return null;
        for (LogicState s : madeFrom) {
            Object res = s.getFirstMadeFromData();
            if (res != null) return res;
        }
        return null;
    }

    public String getName() {
        return _name;
    }

    public void setName(String s) {
        _name = s;
    }

    public static class LogicStateCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicState t = (LogicState) o;

            t.pairs = (ArrayList<LogicPair>) col.get();
            t._name = (String) col.get();
            t._final = (Boolean) col.get();
            t.madeFrom = (HashSet<LogicState>) col.get();
            t.startBinding = (ArrayList<Integer>) col.get();
            t.finishBinding = (ArrayList<Integer>) col.get();
            t.data = col.get();
        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicState t = (LogicState) o;
            col.put(t.pairs);
            col.put(t._name);
            col.put(t._final);
            col.put(t.madeFrom);
            col.put(t.startBinding);
            col.put(t.finishBinding);
            col.put(t.data);
        }
    }
}
