/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

import treeton.core.TString;
import treeton.core.fsm.FSM;
import treeton.core.util.collector.Collector;
import treeton.core.util.collector.CollectorException;
import treeton.core.util.collector.Mutable;

import java.util.*;

public class LogicFSM implements IsLogicSet, FSM {
    protected static int newStatesCounter;
    HashSet<LogicState> states; //вершины
    HashSet<LogicState> finals; //конечные состояния
    HashSet<LogicPair> pairs; //ребра
    LogicState startState = null;
    //TODO сделать поточную модель для итератора по биндингам (чтобы не считать все заранее)
    ArrayList<String[]> bindingsVariants = null;
    String[] currentVariant = null;
    private Iterator<String[]> varIterator = null;

    public LogicFSM() //constructor
    {
        states = new HashSet<LogicState>();
        finals = new HashSet<LogicState>();
        pairs = new HashSet<LogicPair>();
    }


    public LogicFSM(LogicState startState) {
        this();
        this.startState = startState;
    }

    public LogicFSM(String s) {
        this(s, true);
    }

    public LogicFSM(String s, boolean caseSensitive) {
        this();
        newStatesCounter = 0;
        startState = new LogicState(newStatesCounter, Integer.toString(newStatesCounter));
        newStatesCounter++;
        states.add(startState);
        LogicState curState = startState;
        int i = 0;
        while (i < s.length()) {
            SingleCharLogicSet term = new SingleCharLogicSet();
            i += term.parse(s.substring(i));
            if (i < 0) break;
            if (!caseSensitive) term.makeNotCaseSensitive();
            LogicState state = new LogicState(newStatesCounter, Integer.toString(newStatesCounter));
            newStatesCounter++;
            states.add(state);
            LogicPair p = new LogicPair(curState, state, new LogicTerm(term));
            pairs.add(p);
            curState.addPair(p);
            curState = state;
        }
        curState.setFinal(true);
        finals.add(curState);
    }

    private static String getBinding(int i, String[] currentVariant) {
        if (currentVariant != null) {
            try {
                return currentVariant[i];
            } catch (Exception e) {
            }
        }
        return null;
    }

    private static String substituteBindings(String s, String[] currentVariant) throws Exception {
        if (s == null) return null;
        if (currentVariant == null) return null;
        boolean slashFlag = false;
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < s.length(); ) {
            Character c = s.charAt(i);
            if (c == '\\') {
                slashFlag = true;
                i++;
            } else if (c == '$' && !slashFlag && i + 1 < s.length() && (Character.isDigit(c = s.charAt(i + 1)))) {
                int n = 0;
                i++;
                while (i < s.length()) {
                    c = s.charAt(i);
                    if (!Character.isDigit(c)) break;
                    n *= 10;
                    n += Character.getNumericValue(c);
                    i++;
                }
                String bind = getBinding(n, currentVariant);
                if (bind == null) throw new Exception("No such binding or binding is null $" + Integer.toString(n));
                res.append(bind);
            } else {
                res.append(c);
                slashFlag = false;
                i++;
            }
        }
        return res.toString();
    }

    private static void markNonFake(LogicState state, HashSet<LogicState> hash, HashSet<LogicState> markHash) {
        if (state.isFinal()) {
            markHash.addAll(hash);
            markHash.add(state);
        }
        Iterator<LogicPair> it = state.pairsIterator();
        while (it.hasNext()) {
            LogicPair p = it.next();
            LogicState s = p.getState();
            if (!hash.contains(state)) {
                hash.add(state);
                markNonFake(s, hash, markHash);
                hash.remove(state);
            }
        }
    }
  /*public boolean match(String s)
  {
    return match(s,0)>=0;
  }
  private int matchDet(String s,int i)
  {
    assert startState!=null;
    return startState.match(s,i);
  }
  */

    public static LogicFSM multipleAnd(Object[] src) {
        if (src == null || src.length == 0) {
            return null;
        }
        Object f = src[0];
        LogicFSM first;
        if (f instanceof TString || f instanceof String) {
            first = new LogicFSM(f.toString());
        } else {
            first = ((LogicFSM) f).getDeterminized();
        }
        for (int i = 1; i < src.length; i++) {
            Object o = src[i];
            LogicFSM cur;
            if (o instanceof TString || o instanceof String) {
                cur = new LogicFSM(o.toString());
            } else {
                cur = (LogicFSM) o;
            }
            first = first.and(cur);
        }
        return first;
    }

    public static LogicFSM multipleUnion(Iterator<LogicFSM> fsms) {
        int statesCount = 0;
        if (fsms == null) return null;
        LogicFSM res = new LogicFSM();
        LogicState newStarting = new LogicState(statesCount, Integer.toString(statesCount));
        res.states.add(newStarting);
        res.startState = newStarting;
        statesCount++;
        while (fsms.hasNext()) {
            LogicFSM fsm = fsms.next();
            if (fsm == null)
                continue;
            int statesNumIncrement = fsm.getMaxStateId();
            for (LogicState state : fsm.states) {
                int id = state.getId() + statesCount;
                state.setId(id);
                state.setName(Integer.toString(id));
            }
            statesCount += statesNumIncrement;
            LogicPair pair = new LogicPair(newStarting, fsm.startState, LogicTerm.EPS);
            res.pairs.add(pair);
            newStarting.addPair(pair);
            res.states.addAll(fsm.states);
            res.pairs.addAll(fsm.pairs);
            res.finals.addAll(fsm.finals);
        }
        return res.getDeterminized();
    }

    public LogicState getStartState() {
        return startState;
    }

    protected void setStartState(LogicState startState) {
        this.startState = startState;
    }

    public LogicState match(String s) {
        return _match(s, startState, 0);
    }

    private LogicState _match(String s, LogicState current, int i) {
        if (i == s.length() && current.isFinal()) {
            return current;
        }
        for (LogicPair lp : current.pairs) {
            LogicTerm term = lp.getTerm();
            if (term == LogicTerm.EPS) {
                LogicState res = _match(s, lp.getState(), i);
                if (res != null) {
                    return res;
                }
            } else if (i < s.length() && term.isMember(s.charAt(i))) {
                LogicState res = _match(s, lp.getState(), i + 1);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    public LogicFSM and(IsLogicSet iB)//(LogicFSM b)
    {
        //TODO: implement it
        if (!(iB instanceof LogicFSM)) return null;
        LogicFSM A = getDeterminizedDenial();
        LogicFSM B = ((LogicFSM) iB).getDeterminizedDenial();
        A.unionToNotDetermined(B);
        return A.getDeterminizedDenial();

//    LogicFSM A = getDeterminized(); //TODO add determinized flag to avoid vain determinizations
//    LogicFSM B = ((LogicFSM)iB).getDeterminized();
//    return A.andDetermined(B);
//    return andDetermined((LogicFSM)iB);

    }

    public void matchBindings(String s) {
        bindingsVariants = new ArrayList<String[]>();
//    FSMFrame.showFSMFrame(this);
        if (s != null)
            startState.matchBindings(bindingsVariants, new HashMap<Integer, ArrayList<Binding>>(), s, 0);
        varIterator = null;
        currentVariant = null;
    }

    public void resetMatch() {
        varIterator = null;
        currentVariant = null;
    }

    public void forgetMatch() {
        bindingsVariants = null;
        varIterator = null;
        currentVariant = null;
    }

    public boolean next() {
        if (varIterator == null) varIterator = bindingsVariants.iterator();
        boolean res = varIterator.hasNext();
        if (res) {
            currentVariant = varIterator.next();
        }
        return res;
    }

    public String getBinding(int i) {
        return getBinding(i, currentVariant);
    }

    public void constraintEquals(String a, String b) throws Exception {
        ArrayList toRemove = new ArrayList();

        for (String[] var : bindingsVariants) {
            if (!substituteBindings(a, var).equals(substituteBindings(b, var)))
                toRemove.add(var);
        }
        bindingsVariants.removeAll(toRemove);
    }

    public String substituteBindings(String s) throws Exception {
        return substituteBindings(s, currentVariant);
    }

    public void epsClosure(Set<LogicState> states, Set<LogicState> res) {
        if (res == null || states == null) return;
        for (LogicState state : states) {
            epsClosure(state, res);
        }
    }

    public void epsClosure(LogicState state, Set<LogicState> res) {
        if (res == null || state == null) return;
        res.add(state);
        Iterator<LogicPair> i = state.pairsIterator();
        while (i.hasNext()) {
            LogicPair lp = i.next();
            if (lp.getTerm() == LogicTerm.EPS) {
                LogicState next = lp.getState();
                if (!res.contains(next)) {
                    res.add(next);
                    epsClosure(next, res);
                }
            }
        }
    }

    public void moveByTerm(Set<LogicState> states, LogicTerm term, Set<LogicState> res) {
        if (res == null || states == null || term == null || term == LogicTerm.EPS) return;
        for (LogicState state : states) {
            Iterator<LogicPair> i = state.pairsIterator();
            while (i.hasNext()) {
                LogicPair lp = i.next();
                LogicTerm term2 = lp.getTerm();
                if (term2 != LogicTerm.EPS
                        && (term.isEmpty() && term2.isEmpty()
                        || !term2.and(term).isEmpty())) {
                    res.add(lp.getState());
                }
            }
        }
    }

    public LogicFSM getDeterminized() {
        newStatesCounter = 0;
        LogicFSM res = new LogicFSM();
        HashSet<LogicState> statesT = new HashSet<LogicState>();
        epsClosure(startState, statesT);
        res.startState = addDstate(statesT, res);
        return res;
    }

    public LogicFSM getDeterminizedDenial() {
        newStatesCounter = 0;
        LogicFSM res = new LogicFSM();
        HashSet<LogicState> statesT = new HashSet<LogicState>();
        epsClosure(startState, statesT);
        res.startState = addDstateDenial(statesT, res);
        HashSet<LogicState> hash = new HashSet<LogicState>();
        HashSet<LogicState> markHash = new HashSet<LogicState>();
        markNonFake(res.startState, hash, markHash);
        res.removeFake(markHash);
        return res;
    }

    private void removeFake(HashSet<LogicState> nonFakeHash) {
        HashSet<LogicState> toRemoveStates = new HashSet<LogicState>();
        HashSet<LogicPair> toRemovePairs = new HashSet<LogicPair>();

        for (LogicState s : states) {
            if (!nonFakeHash.contains(s)) {
                toRemoveStates.add(s);
            }
            Iterator<LogicPair> it = s.pairsIterator();
            while (it.hasNext()) {
                LogicPair p = it.next();
                if (!nonFakeHash.contains(p.getState())) {
                    toRemovePairs.add(p);
                    it.remove();
                }
            }
        }

        states.removeAll(toRemoveStates);
        pairs.removeAll(toRemovePairs);
    }

    public LogicFSM not() {
/*    finals = new HashSet<LogicState>();
    for(LogicState state : states){
      state.setFinal(!state.isFinal());
      if (state.isFinal())
        finals.add(state);
    }*/
        return getDeterminizedDenial();
    }

    public void unionToNotDetermined(LogicFSM fsm) {
        int max = fsm.getMaxStateId() + 1;
        int newmax = 0;
        for (LogicState state : states) {
            int id = max + state.getId();
            if (newmax < id) newmax = id;
            state.setId(id);
            state.setName(Integer.toString(id));
        }
        newmax++;
        LogicState newStart = new LogicState(newmax, Integer.toString(newmax));
        LogicPair p1 = new LogicPair(newStart, startState, LogicTerm.EPS);
        LogicPair p2 = new LogicPair(newStart, fsm.startState, LogicTerm.EPS);
        newStart.addPair(p1);
        newStart.addPair(p2);
        pairs.add(p1);
        pairs.add(p2);
        states.add(newStart);
        states.addAll(fsm.states);
        finals.addAll(fsm.finals);
        pairs.addAll(fsm.pairs);
        startState = newStart;
    }

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();    //TODO : implement
    }

    public int getMaxStateId() {
        int res = 0;
        for (LogicState state : states) {
            int id = state.getId();
            if (res < id) res = id;
        }
        return res;
    }

    private LogicState addDstate(Set<LogicState> statesT, LogicFSM fsm) {
        for (LogicState state : fsm.states) {
            if (state.madeFrom.equals(statesT))
                return state;
        }
        LogicState res = new LogicState(newStatesCounter, Integer.toString(newStatesCounter));
        newStatesCounter++;
        fsm.states.add(res);
        res.madeFrom = statesT;
        ArrayList<IsLogicSet> terms = new ArrayList<IsLogicSet>();
        for (LogicState state : statesT) {
            if (state.isFinal()) {
                res.setFinal(true);
                fsm.finals.add(res);
            }
            Iterator<LogicPair> i = state.pairsIterator();
            while (i.hasNext()) {
                LogicPair lp = i.next();
                LogicTerm term = lp.getTerm();
                if (term != LogicTerm.EPS)
                    terms.add(term);
            }
        }
        ArrayList<IsLogicSet> breakedTerms = RegExpParseTree.breakSetUnionToUnionOfNotIntersectingSet(terms);
        for (IsLogicSet ls : breakedTerms) {
            LogicTerm a = (LogicTerm) ls;
            HashSet<LogicState> statesU = new HashSet<LogicState>();
            HashSet<LogicState> moves = new HashSet<LogicState>();
            moveByTerm(statesT, a, moves);
            epsClosure(moves, statesU);
            LogicState newStateU = addDstate(statesU, fsm);
            LogicPair pair = new LogicPair(res, newStateU, a);
            res.addPair(pair);
            fsm.pairs.add(pair);
        }
        return res;
    }

    private LogicState addDstateDenial(Set<LogicState> statesT, LogicFSM fsm) {
        for (LogicState state : fsm.states) {
            if (state.madeFrom != null && state.madeFrom.equals(statesT))
                return state;
        }
        LogicState res = new LogicState(newStatesCounter, Integer.toString(newStatesCounter));
        newStatesCounter++;
        fsm.states.add(res);
        res.madeFrom = statesT;
        ArrayList<IsLogicSet> terms = new ArrayList<IsLogicSet>();
        boolean fin = true;
        for (LogicState state : statesT) {
            if (state.isFinal()) {
                fin = false;
            }
            Iterator<LogicPair> i = state.pairsIterator();
            while (i.hasNext()) {
                LogicPair lp = i.next();
                LogicTerm term = lp.getTerm();
                if (term != LogicTerm.EPS)
                    terms.add(term);
            }
        }
        if (fin) {
            res.setFinal(true);
            fsm.finals.add(res);
        }
        ArrayList<IsLogicSet> breakedTerms = RegExpParseTree.breakSetUnionToUnionOfNotIntersectingSet(terms);
        for (IsLogicSet ls : breakedTerms) {
            LogicTerm a = (LogicTerm) ls;
            HashSet<LogicState> statesU = new HashSet<LogicState>();
            HashSet<LogicState> moves = new HashSet<LogicState>();
            moveByTerm(statesT, a, moves);
            epsClosure(moves, statesU);
            LogicState newStateU = addDstateDenial(statesU, fsm);
            LogicPair pair = new LogicPair(res, newStateU, a);
            res.addPair(pair);
            fsm.pairs.add(pair);
        }
        IsLogicSet complement = RegExpParseTree.buildComplementForUnion(terms);
        if (complement == null) {
            complement = new LogicTerm(new SingleCharLogicSet("."));
        }
        if (!complement.isEmpty()) {
            LogicState loopState = new LogicState(newStatesCounter, Integer.toString(newStatesCounter));
            newStatesCounter++;
            fsm.states.add(loopState);
            loopState.setFinal(true);
            fsm.finals.add(loopState);
            LogicPair p = new LogicPair(res, loopState, (LogicTerm) complement);
            res.addPair(p);
            fsm.pairs.add(p);
            p = new LogicPair(loopState, loopState, new LogicTerm(new SingleCharLogicSet(".")));
            fsm.pairs.add(p);
            loopState.addPair(p);
        }
        return res;
    }

    //static IsLogicSet createEmpty();   // 0&x=0
    public boolean isEmpty() {
        return finals.size() == 0;
    }

    public boolean isMember(Object o) {
        return false;  //Todo change body of implemented methods use File | Settings | File Templates.
    }

    public void assignData(Object data) {
        for (LogicState s : finals) {
            s.setData(data);
        }
    }

    public void forgetMadeFrom() {
        for (LogicState state : states) {
            state.madeFrom = null;
        }
    }

    public void readIn(String regexp) throws Exception {
        readIn(regexp, true);
    }

    public void readIn(String regexp, boolean caseSensitive) throws Exception {
        RegExpParseTree.parseRegExp(regexp, caseSensitive).createNonDeterminedLogicFSM(this);
        LogicFSM det = this.getDeterminized();
        this.states = det.states;
        this.pairs = det.pairs;
        this.startState = det.startState;
        this.bindingsVariants = null; //just in case
        this.currentVariant = null;
        this.finals = det.finals;
    }

    public Iterator<LogicState> finalStatesIterator() {
        return finals.iterator();
    }

    public int nFinals() {
        return finals.size();
    }

    public LogicFSM andDetermined(LogicFSM b) { // TODO
        LogicFSM res = new LogicFSM();
        newStatesCounter = 0;
        res.startState = addAndStates(res, startState, b.startState);

        return res;
    }

    private LogicState addAndStates(LogicFSM fsm, LogicState curStateA, LogicState curStateB) {
        for (LogicState state : fsm.states) {
            if (state.madeFrom != null
                    && state.madeFrom.size() <= 2
                    && state.madeFrom.contains(curStateA)
                    && state.madeFrom.contains(curStateB)
                    ) return state;
        }
        LogicState newState = new LogicState(newStatesCounter, Integer.toString(newStatesCounter));
        newStatesCounter++;
        newState.madeFrom = new HashSet<LogicState>();
        newState.madeFrom.add(curStateA);
        newState.madeFrom.add(curStateB);
        if (curStateA.isFinal() && curStateB.isFinal()) {
            newState.setFinal(true);
            fsm.finals.add(newState);
        }
        fsm.states.add(newState);

        Iterator<LogicPair> pairsA = curStateA.pairsIterator();
        while (pairsA.hasNext()) {
            Iterator<LogicPair> pairsB = curStateB.pairsIterator();
            LogicPair pairA = pairsA.next();
            LogicTerm termA = pairA.getTerm();
            LogicState stateA = pairA.getState();
            while (pairsB.hasNext()) {
                LogicPair pairB = pairsB.next();
                LogicTerm termB = pairB.getTerm();
                LogicState stateB = pairB.getState();
                LogicTerm newTerm = termA.and(termB);
                if (!newTerm.isEmpty()) {
                    LogicState newNext = addAndStates(fsm, stateA, stateB);
                    LogicPair newPair = new LogicPair(newState, newNext, newTerm);
                    newState.addPair(newPair);
                    //terms.add(newTerm);
                    fsm.pairs.add(newPair);
                }
            }
        }
        return newState;
    }

    //TODO написать еще один and используя <объединение автоматов>.getSubFSMbySetOfFinals(конечные состояния,madeFrom (предки) которых содержит конечные состояния всех исходных автоматов )
    //TODO так же можно сделать быстрое разбиение на подавтоматы: for(f : finals)res.add( getSubFSMbySetOfFinals( {f} ) )
    public LogicFSM getSubFSMbySetOfFinals(HashSet<LogicState> finals) {
        LogicFSM res = new LogicFSM();
        HashMap<LogicState, LogicState> oldToNewMap = new HashMap<LogicState, LogicState>();
        for (LogicState fin : finals)
            if (this.finals.contains(fin)) {
                if (oldToNewMap.keySet().contains(fin)) continue;
                LogicState newFin = new LogicState(fin.getId(), fin.getName());
                newFin.setFinal(true);
                newFin.madeFrom = new HashSet<LogicState>(1);
                newFin.madeFrom.add(fin);
                oldToNewMap.put(fin, newFin);
                res.finals.add(newFin);
                addPrecedingStates(res, fin, newFin, oldToNewMap);
            }
        if (oldToNewMap.keySet().contains(startState))
            res.startState = oldToNewMap.get(startState);
        else return null;

        return res;
    }

    private void addPrecedingStates(LogicFSM fsm, LogicState state, LogicState newState, HashMap<LogicState, LogicState> oldToNewMap) {
        for (LogicPair pair : pairs) {
            if (pair.getState() == state) {
                LogicState preceding = pair.getParent();
                LogicState newPreceding;
                if (oldToNewMap.keySet().contains(preceding)) {
                    newPreceding = oldToNewMap.get(preceding);
                } else {
                    newPreceding = new LogicState(preceding.getId(), preceding.getName());
                    oldToNewMap.put(preceding, newPreceding);
                    fsm.states.add(newPreceding);
                    addPrecedingStates(fsm, preceding, newPreceding, oldToNewMap);
                }
                LogicPair newPair = new LogicPair(newPreceding, newState, pair.getTerm());
                newPreceding.addPair(newPair);
            }
        }


    }

    public LogicState getStateById(int id) {
        for (LogicState s : states) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    private RegExpParseTree RegExpBetweenStatesIJThroughStateNotHigherThanK(LogicState i, LogicState j, int k) {
        RegExpParseTree res = null;
        if (k < 0) {
            HashSet<LogicTerm> al = new HashSet<LogicTerm>();
            IsLogicSet a;
            if (i == j) {
                al.add(LogicTerm.EPS);
            }
            Iterator<LogicPair> pairs = i.pairsIterator();
            while (pairs.hasNext()) {
                LogicPair pair = pairs.next();
                if (pair.getState() == j)
                    al.add(pair.getTerm());
            }
            for (LogicTerm t : al) {
                if (res == null) res = RegExpParseTree.createTerm(t);
                else res = RegExpParseTree.createOr(res, RegExpParseTree.createTerm(t));
            }
            return res;
        } else {
            LogicState ka = getStateById(k);
            RegExpParseTree rij = RegExpBetweenStatesIJThroughStateNotHigherThanK(i, j, k - 1);
            if (ka == null) return rij;
            RegExpParseTree rik = RegExpBetweenStatesIJThroughStateNotHigherThanK(i, ka, k - 1);
            RegExpParseTree rkk = RegExpBetweenStatesIJThroughStateNotHigherThanK(ka, ka, k - 1);
            RegExpParseTree rkj = RegExpBetweenStatesIJThroughStateNotHigherThanK(ka, j, k - 1);
            if (rik != null && rkk != null && rkj != null) {
                // rij | rik rkk* rkj


                res = RegExpParseTree.createAnd(rik,
                        RegExpParseTree.createAnd(
                                RegExpParseTree.createSeq(rkk),
                                rkj
                        )
                );
                if (rij == null) return res;
                else return RegExpParseTree.createOr(rij, res);
            } else return rij;
        }
    }

    public RegExpParseTree getRegExp() {
        int n = getMaxStateId();
        RegExpParseTree res = null;
        for (LogicState fin : finals) {
            RegExpParseTree re = RegExpBetweenStatesIJThroughStateNotHigherThanK(startState, fin, n);
            if (re != null) {
                if (res == null) res = re;
                else res = RegExpParseTree.createOr(res, re);
            }
        }
        return res;
    }

    public static class LogicFSMCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicFSM t = (LogicFSM) o;
            t.startState = (LogicState) col.get();
            t.states = (HashSet<LogicState>) col.get();
            t.finals = (HashSet<LogicState>) col.get();
            t.pairs = (HashSet<LogicPair>) col.get();

        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicFSM t = (LogicFSM) o;
            col.put(t.startState);
            col.put(t.states);
            col.put(t.finals);
            col.put(t.pairs);
        }
    }

}
