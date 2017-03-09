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

import java.util.Arrays;
import java.util.Iterator;

public class ScapeNFSM implements FSM {
    ScapeDFSMState start;
    RBTreeMap statesIndex;
    BlockStack stack = new BlockStack(100);
    BlockStack ruleStack = new BlockStack(100);
    ScapeFSMState[] eclosure = new ScapeFSMState[100];
    int eclosureLen = 0;
    ScapeDFSMState stateForSearch = new ScapeDFSMState();
    ScapeFSMState[] t = new ScapeFSMState[100];
    ScapeFSMBinding[] bindingsArr = new ScapeFSMBinding[100];
    BlackBoard localBoard = TreetonFactory.newBlackBoard(100, false);
    BlockStack entryStack = new BlockStack(100);
    private ScapePhase phase;
    public ScapeNFSM(ScapePhase _phase) {
        phase = _phase;
        start = null;
        statesIndex = new RBTreeMap();
    }

    public State getStartState() {
        return start;
    }

    public ScapeDFSMState addState(ScapeFSMState[] _oldStates, int _osLen, ScapeRuleSet _ruleSet) {
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
        statesIndex.put(s, s);
        return s;
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

    public void slurp(Iterator fsms) {
        ScapeDFSMPair u;

        eclosureIterator eclosureIt = new eclosureIterator(null);
        int nFsm = 0;
        while (fsms.hasNext()) {
            if (nFsm >= t.length) {
                ScapeFSMState[] tarr = new ScapeFSMState[(int) (t.length * 1.5)];
                System.arraycopy(t, 0, tarr, 0, t.length);
                t = tarr;
            }
            t[nFsm++] = (ScapeFSMState) ((ScapeFSM) fsms.next()).getStartState();
        }
        buildEClosure(t, nFsm);
        Arrays.sort(eclosure, 0, eclosureLen);
        ScapeFSMState[] oldStates = new ScapeFSMState[eclosureLen];
        for (int i = 0; i < eclosureLen; i++) {
            ScapeFSMState s = eclosure[i];
            oldStates[i] = s;
            if (s.isFinal()) {
                ruleStack.push(s.rule);
            }
        }
        ScapeRuleSet ruleSet = phase.newRuleSet(ruleStack);
        start = addState(oldStates, eclosureLen, ruleSet);
        ruleStack.clean();

        stack.push(start);
        while (true) {
            if (stack.isEmpty()) {
                break;
            } else {
                ScapeDFSMState s = (ScapeDFSMState) stack.pop();

                eclosureIt.reset((ScapeFSMState[]) s.oldStates);
                while (eclosureIt.hasNext()) {
                    ScapeFSMPair curPair = (ScapeFSMPair) eclosureIt.next();

                    t[0] = (ScapeFSMState) curPair.getState();
                    int k = 1;

                    buildEClosure(t, k);

                    Arrays.sort(eclosure, 0, eclosureLen);

                    stateForSearch.oldStates = eclosure;
                    stateForSearch.osLen = eclosureLen;

                    ScapeDFSMState newState = (ScapeDFSMState) statesIndex.get(stateForSearch);

                    if (newState == null) {
                        oldStates = new ScapeFSMState[eclosureLen];
                        for (int j = 0; j < eclosureLen; j++) {
                            ScapeFSMState os = eclosure[j];
                            oldStates[j] = os;
                            if (os.isFinal()) {
                                ruleStack.push(os.rule);
                            }
                        }
                        ruleSet = phase.newRuleSet(ruleStack);
                        newState = addState(oldStates, eclosureLen, ruleSet);
                        ruleStack.clean();
                        stack.push(newState);
                    }
                    Treenotation trn = ((ScapeTreenotationTerm) curPair.getTerm()).trn;
                    u = s.addPair((ScapeTreenotationTerm) curPair.getTerm(), newState, curPair.bindingSet);

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
                            e.pairs[e.nPairs++] = u;
                        }
                    }
                }

                if (localBoard.getNumberOfObjects() == 0) {
                    s.transitions = null;
                } else {
                    for (int i = 0; i <= localBoard.getDepth(); i++) {
                        Entry e = (Entry) localBoard.get(i);
                        if (e == null)
                            continue;
                        ScapeDFSMPair[] tarr = new ScapeDFSMPair[e.nPairs];
                        s.mSize += 4 + e.nPairs * 4;
                        System.arraycopy(e.pairs, 0, tarr, 0, e.nPairs);
                        localBoard.put(i, tarr);
                        freeEntry(e);
                    }
                    s.transitions = new IntFeatureMapStaticStraight(localBoard);
                    s.buildTrnTypeSet(phase);
                }
            }
        }
    }

    private void buildEClosure(ScapeFSMState[] states, int len) {
        ScapeBindedPair nullPair;
        ScapeFSMState s;
        long curLabel = ScapeUniLabel.get();
        eclosureLen = 0;
        long startStackPos = stack.getPosition();


        for (int i = 0; i < len; i++) {
            s = states[i];
            if (s.label == curLabel)
                continue;
            if (eclosureLen >= eclosure.length) {
                ScapeFSMState[] t = new ScapeFSMState[(int) (eclosure.length * 1.5)];
                System.arraycopy(eclosure, 0, t, 0, eclosure.length);
                eclosure = t;
            }
            eclosure[eclosureLen++] = s;

            s.label = curLabel;
            nullPair = s.firstNullPair;

            while (true) {
                if (nullPair == null) {
                    if (startStackPos == stack.getPosition()) {
                        break;
                    } else {
                        nullPair = (ScapeFSMPair) stack.pop();
                    }
                    nullPair = nullPair.next;
                } else {
                    ScapeFSMState nextState = (ScapeFSMState) nullPair.getState();
                    if (nextState.label == curLabel) {
                        nullPair = nullPair.next;
                    } else {
                        stack.push(nullPair);
                        nextState.label = curLabel;
                        if (eclosureLen >= eclosure.length) {
                            ScapeFSMState[] t = new ScapeFSMState[(int) (eclosure.length * 1.5)];
                            System.arraycopy(eclosure, 0, t, 0, eclosure.length);
                            eclosure = t;
                        }
                        eclosure[eclosureLen++] = nextState;
                        nullPair = nextState.firstNullPair;
                    }
                }
            }
        }
    }

    private class Entry {
        ScapeDFSMPair[] pairs;
        int nPairs;

        Entry() {
            pairs = new ScapeDFSMPair[10];
            nPairs = 0;
        }
    }

    private class eclosureIterator implements Iterator {
        ScapeBindedPair curPair;
        ScapeFSMState[] arr;
        int i;

        eclosureIterator(ScapeFSMState[] _arr) {
            arr = _arr;

            i = 0;
            curPair = null;
            if (arr == null)
                return;
            while (curPair == null && i < arr.length) {
                curPair = arr[i++].firstPair;
            }
        }

        void reset(ScapeFSMState[] _arr) {
            arr = _arr;

            i = 0;
            while (i < arr.length) {
                curPair = arr[i].firstPair;
                if (curPair != null)
                    break;
                i++;
            }
        }

        public void remove() {
        }

        public boolean hasNext() {
            return i < arr.length;
        }

        public Object next() {
            Object o = curPair;
            curPair = curPair.next;
            if (curPair == null) {
                i++;
                while (i < arr.length) {
                    curPair = arr[i].firstPair;
                    if (curPair != null)
                        break;
                    i++;
                }
            }
            return o;
        }
    }
}
