/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.BlackBoard;
import treeton.core.IntFeatureMapStaticStraight;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.fsm.FSM;
import treeton.core.fsm.ScapeTreenotationClassTree;
import treeton.core.fsm.State;
import treeton.core.util.BlockStack;
import treeton.core.util.RBTreeMap;

import java.util.*;

public class ScapeDFSM implements FSM {
    ArrayList<ScapeDFSMState> states;
    RBTreeMap statesIndex;
    BlockStack ruleStack = new BlockStack(100);
    BlockStack entryStack = new BlockStack(100);
    ScapeDFSMState stateForSearch = new ScapeDFSMState();
    ScapeDFSMState[] t = new ScapeDFSMState[100];
    eclosureMixIterator mixIt = new eclosureMixIterator(null);
    ScapeBinding[] bindingsArr = new ScapeBinding[100];
    BlockStack bindingsStack = new BlockStack(100);
    BlockStack stack = new BlockStack(100);
    private ScapeDFSMState start;
    private ScapePhase phase;

    public ScapeDFSM(ScapePhase _phase) {
        start = null;
        states = new ArrayList<ScapeDFSMState>();
        statesIndex = new RBTreeMap();
        phase = _phase;
    }

    private Entry newEntry() {
        if (entryStack.isEmpty()) {
            return new Entry();
        } else {
            Entry e = (Entry) entryStack.pop();
            e.nPairs = 0;
            return e;
        }
    }

    private void freeEntry(Entry e) {
        entryStack.push(e);
    }

    public State getStartState() {
        return start;
    }

    public Iterator<ScapeDFSMState> statesIterator() {
        return states.iterator();
    }

    public int getNumberOfStates() {
        return states.size();
    }

    public ScapeDFSMState addState(ScapeDFSMState[] _oldStates, int _osLen, ScapeRuleSet _ruleSet) {
        ScapeDFSMState s;
        if (_ruleSet != null && _ruleSet.size != 0) {
            s = new ScapeDFSMStateFinal();
            s.oldStates = _oldStates;
            s.osLen = _osLen;
            ((ScapeDFSMStateFinal) s).ruleSet = _ruleSet;
        } else {
            s = new ScapeDFSMState();
            s.oldStates = _oldStates;
            s.osLen = _osLen;
        }
        states.add(s);
        statesIndex.put(s, s);

        return s;
    }

    public void slurp(ScapeNFSM nfsm) {
        BlackBoard localBoard = TreetonFactory.newBlackBoard(100, false);

        ScapeDFSMState[] oldStates = new ScapeDFSMState[1];
        oldStates[0] = nfsm.start;
        if (nfsm.start.isFinal()) {
            ScapeDFSMStateFinal fs = (ScapeDFSMStateFinal) nfsm.start;
            start = addState(oldStates, 1, fs.ruleSet);
        } else {
            start = addState(oldStates, 1, null);
        }

        stack.push(start);

        while (true) {
            if (stack.isEmpty()) {
                break;
            } else {
                ScapeDFSMState s = (ScapeDFSMState) stack.pop();

                mixIt.reset((ScapeDFSMState[]) s.oldStates);

                while (mixIt.hasNext()) {
                    Treenotation trn = (Treenotation) mixIt.next();
                    ScapeDFSMPair pair = (ScapeDFSMPair) mixIt.next();
                    Iterator<ScapeTreenotationClassTree.FinalNode> it = phase.allClassesTree.finalStatesIterator();
                    while (it.hasNext()) {
                        ScapeTreenotationClassTree.FinalNode node = it.next();
                        if (node.contains(trn)) {
                            int id = node.safeGetId();
                            Entry e = (Entry) localBoard.get(id);
                            if (e == null) {
                                e = newEntry();
                                localBoard.put(id, e);
                            }
                            if (e.nPairs >= e.pairs.length) {
                                ScapeDFSMPair[] tarr = new ScapeDFSMPair[(int) (e.pairs.length * 1.5)];
                                System.arraycopy(e.pairs, 0, tarr, 0, e.pairs.length);
                                e.pairs = tarr;
                            }
                            e.pairs[e.nPairs++] = pair;
                        }
                    }
                }

                HashMap<HashSet<ScapeDFSMPair>, ScapeDFSMPair[]> hash = new HashMap<HashSet<ScapeDFSMPair>, ScapeDFSMPair[]>();

                for (int i = 0; i <= localBoard.getDepth(); i++) {
                    Entry e = (Entry) localBoard.get(i);
                    if (e == null) {
                        continue;
                    }
                    HashSet<ScapeDFSMPair> tHash = new HashSet<ScapeDFSMPair>();
                    e.fillSet(tHash);

                    ScapeDFSMPair[] narr = hash.get(tHash);
                    if (narr == null) {
                        Entry ne = newEntry();
                        for (int j = 0; j < e.nPairs; j++) {
                            ScapeDFSMPair curPair = e.pairs[j];
                            if (curPair == null)
                                continue;
                            long label = ScapeUniLabel.get();
                            ScapeTreenotationTerm trm = (ScapeTreenotationTerm) curPair.getTerm();
                            trm.rule.label = label;
                            trm.rule.sampleBindingSet = curPair.bindingSet;

                            t[0] = (ScapeDFSMState) curPair.getState();
                            t[0].label = label;
                            int k = 1;
                            bindingsStack.push(trm.rule.sampleBindingSet);
                            int newBindingsLen = trm.rule.sampleBindingSet.size;
                            for (int q = j + 1; q < e.nPairs; q++) {
                                curPair = e.pairs[q];
                                if (curPair == null)
                                    continue;
                                ScapeDFSMState state = (ScapeDFSMState) curPair.getState();
                                trm = (ScapeTreenotationTerm) curPair.getTerm();

                                if (trm.rule.label != label) {
                                    trm.rule.label = label;
                                    trm.rule.sampleBindingSet = curPair.bindingSet;

                                    if (k >= t.length) {
                                        ScapeDFSMState[] tarr = new ScapeDFSMState[(int) (t.length * 1.5)];
                                        System.arraycopy(t, 0, tarr, 0, t.length);
                                        t = tarr;
                                    }
                                    if (state.label != label) {
                                        t[k++] = state;
                                        state.label = label;
                                    }
                                    e.pairs[q] = null;
                                    bindingsStack.push(trm.rule.sampleBindingSet);
                                    newBindingsLen += trm.rule.sampleBindingSet.size;
                                } else if (curPair.bindingSet.equals(trm.rule.sampleBindingSet)) {
                                    if (k >= t.length) {
                                        ScapeDFSMState[] tarr = new ScapeDFSMState[(int) (t.length * 1.5)];
                                        System.arraycopy(t, 0, tarr, 0, t.length);
                                        t = tarr;
                                    }
                                    if (state.label != label) {
                                        t[k++] = state;
                                        state.label = label;
                                    }
                                    e.pairs[q] = null;
                                }
                            }

                            if (newBindingsLen > bindingsArr.length) {
                                ScapeBinding[] tarr = new ScapeBinding[(int) (Math.max(bindingsArr.length * 1.5, newBindingsLen))];
                                System.arraycopy(bindingsArr, 0, tarr, 0, bindingsArr.length);
                                bindingsArr = tarr;
                            }

                            newBindingsLen = 0;
                            while (!bindingsStack.isEmpty()) {
                                ScapeBindingSet bindingSet = (ScapeBindingSet) bindingsStack.pop();
                                for (int q = 0; q < bindingSet.size; q++) {
                                    bindingsArr[newBindingsLen++] = bindingSet.bindings[q];
                                }
                            }
                            Arrays.sort(bindingsArr, 0, newBindingsLen);

                            //Группа выделилась вся

                            Arrays.sort(t, 0, k);

                            stateForSearch.oldStates = t;
                            stateForSearch.osLen = k;

                            //Смотрим, нет ли такого состояния
                            ScapeDFSMState newState = (ScapeDFSMState) statesIndex.get(stateForSearch);


                            if (newState == null) {
                                oldStates = new ScapeDFSMState[k];
                                for (int q = 0; q < k; q++) {
                                    ScapeDFSMState os = t[q];
                                    oldStates[q] = os;
                                    if (os.isFinal()) {
                                        ScapeDFSMStateFinal fs = (ScapeDFSMStateFinal) os;
                                        for (int l = 0; l < fs.ruleSet.size; l++) {
                                            ruleStack.push(fs.ruleSet.rules[l]);
                                        }
                                    }
                                }
                                ScapeRuleSet ruleSet = phase.newRuleSet(ruleStack);
                                newState = addState(oldStates, k, ruleSet);
                                ruleStack.clean();
                                stack.push(newState);
                            }

                            if (ne.nPairs >= ne.pairs.length) {
                                ScapeDFSMPair[] tarr = new ScapeDFSMPair[(int) (ne.pairs.length * 1.5)];
                                System.arraycopy(ne.pairs, 0, tarr, 0, ne.pairs.length);
                                ne.pairs = tarr;
                            }
                            ne.pairs[ne.nPairs++] = s.addPair(new ConstraintsTerm(((Integer) i).toString()), newState, phase.newBindingSet(bindingsArr, newBindingsLen));
                        }

                        narr = new ScapeDFSMPair[ne.nPairs];
                        for (int j = 0; j < ne.nPairs; j++) {
                            for (int k = 0; k < ne.pairs[j].bindingSet.size; k++) {
                                ((ScapeFSMBinding) ne.pairs[j].bindingSet.bindings[k]).nPairs++;
                            }
                            narr[j] = ne.pairs[j];
                        }

                        hash.put(tHash, narr);
                        freeEntry(e);
                        freeEntry(ne);
                    } else {
                        for (ScapeDFSMPair p : narr) {
                            ((ConstraintsTerm) p.getTerm()).expr += ", " + ((Integer) i).toString();
                        }
                    }

                    localBoard.put(i, narr);
                }

                if (localBoard.getNumberOfObjects() == 0) {
                    s.transitions = null;
                } else {
                    s.transitions = new IntFeatureMapStaticStraight(localBoard);
                    s.buildTrnTypeSet(phase);
                }
            }
        }
    }

    private static class Entry {
        ScapeDFSMPair[] pairs;
        int nPairs;

        Entry() {
            pairs = new ScapeDFSMPair[10];
            nPairs = 0;
        }

        public void fillSet(HashSet<ScapeDFSMPair> tHash) {
            for (int i = 0; i < nPairs; i++) {
                tHash.add(pairs[i]);
            }
        }
    }

    private class eclosureMixIterator implements Iterator {
        boolean timeOfKey;
        ScapeBindedPair curPair;
        ScapeDFSMState[] arr;
        int i;

        eclosureMixIterator(ScapeDFSMState[] _arr) {
            arr = _arr;
            timeOfKey = true;
            i = 0;
            if (arr == null)
                return;
            while (i < arr.length) {
                if (arr[i].firstPair != null) {
                    curPair = arr[i].firstPair;
                    break;
                }
                i++;
            }
        }

        void reset(ScapeDFSMState[] _arr) {
            arr = _arr;
            timeOfKey = true;
            i = 0;
            if (arr == null)
                return;
            while (i < arr.length) {
                if (arr[i].firstPair != null) {
                    curPair = arr[i].firstPair;
                    break;
                }
                i++;
            }
        }

        public void remove() {
        }

        public boolean hasNext() {
            return i < arr.length;
        }

        public Object next() {
            if (timeOfKey) {
                timeOfKey = false;
                return ((ScapeTreenotationTerm) curPair.getTerm()).trn;
            } else {
                Object o = curPair;
                curPair = curPair.next;
                if (curPair == null) {
                    i++;
                    while (i < arr.length) {
                        if (arr[i].firstPair != null) {
                            curPair = arr[i].firstPair;
                            break;
                        }
                        i++;
                    }
                }
                timeOfKey = true;
                return o;
            }
        }
    }
}
