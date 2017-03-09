/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.BlackBoard;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.fsm.FSM;
import treeton.core.fsm.State;
import treeton.core.fsm.logicset.LogicFSM;
import treeton.core.scape.ParseException;
import treeton.core.scape.ScapeRegexBinding;
import treeton.core.scape.TrnTemplate;
import treeton.core.util.BlockStack;
import treeton.core.util.NumeratedObject;
import treeton.core.util.sut;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class ScapeFSM implements FSM {
    static final char[][] roundBracketDelim = new char[][]{{')'}};
    ScapeRule rule;
    ArrayList<ScapeFSMState> states;
    private ScapeFSMState start;
    private BlockStack bindingsStack = new BlockStack();
    private BlockStack statesStack = new BlockStack();

    public ScapeFSM() {
        start = null;
        states = new ArrayList<ScapeFSMState>();
    }

    public State getStartState() {
        return start;
    }

    public ScapeFSMState addState() {
        ScapeFSMState s = new ScapeFSMState();
        states.add(s);
        return s;
    }

    public ScapeFSMState addStartState() {
        ScapeFSMState s = new ScapeFSMState();
        states.add(s);
        start = s;
        return s;
    }

    private void assignBindings() {
        for (ScapeFSMState st : states) {
            Iterator it = st.pairsIterator();
            while (it.hasNext()) {
                ScapeFSMPair p = (ScapeFSMPair) it.next();
                if (p.reverse)
                    continue;
                ((ScapeFSMState) p.getState()).nInputs++;
            }
            st.label = 0;
        }
        statesStack.push(start);
        while (!statesStack.isEmpty()) {
            ScapeFSMState st = (ScapeFSMState) statesStack.pop();
            if (st.popBinding)
                bindingsStack.pop();
            if (st.pushBinding != null) {
                bindingsStack.push(st.pushBinding);
            }
            Iterator it = st.pairsIterator();
            while (it.hasNext()) {
                ScapeFSMPair p = (ScapeFSMPair) it.next();
                if (p.reverse)
                    continue;
                if (p.getTerm() != ScapeTreenotationTerm.nullTerm) {
                    p.bindingSet = rule.phase.newBindingSet(bindingsStack);
                }
                ScapeFSMState pst = (ScapeFSMState) p.getState();
                pst.label++;
                if ((int) pst.label == pst.nInputs)
                    statesStack.push(pst);
            }
        }
        for (Object state : states) {
            ScapeFSMState st = (ScapeFSMState) state;
            st.label = -1;
        }
    }

    public int readIn(char s[], int pl, int endpl, char[][] delims, ScapeRule rule) throws ParseException {
        if (start != null) {
            throw new RuntimeException("FSM is already filled with something");
        }
        this.rule = rule;
        start = addState();
        ScapeFSMState end = addState();
        end.finalizeState();
        end.rule = rule;
        pl = _readIn(s, pl, endpl, delims, start, end);
        assignBindings();
        return pl;
    }

    private int _readIn(char s[], int pl, int endpl, char[][] delims, ScapeFSMState start, ScapeFSMState end) throws ParseException {
        ScapeFSMState cur = start;
        BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);

        int delim;
        pl = sut.skipSpacesEndls(s, pl, endpl);

        if (sut.checkDelims(s, pl, endpl, delims) != -1)
            throw new ParseException("empty expression", null, s, pl, endpl);

        while (true) {
            if (s[pl] == '(') {
                ScapeFSMState s1 = addState();
                s1.rule = rule;
                ScapeFSMState s2 = addState();
                s2.rule = rule;
                pl++;
                pl = _readIn(s, pl, endpl, roundBracketDelim, s1, s2);
                pl++;
                cur.put(ScapeTreenotationTerm.nullTerm, s1, false);
                cur = s2;

                pl = sut.skipSpacesEndls(s, pl, endpl);
                if (pl > endpl)
                    break;
                if ((delim = sut.checkDelims(s, pl, endpl, delims)) == -1) {
                    if (s[pl] == ':') {
                        pl++;
                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        int beg = pl;
                        pl = sut.skipVarName(s, pl, endpl);
                        if (pl == beg) {
                            throw new ParseException("missing binding name", null, s, pl, endpl);
                        }
                        String t = new String(s, beg, pl - beg);
                        if (rule.potentialSysName(t)) {
                            throw new ParseException(t + " cannot be a binding name", null, s, pl, endpl);
                        }
                        ScapeFSMBinding b;
                        if ((b = (ScapeFSMBinding) rule.bindings.get(t)) == null) {
                            b = new ScapeFSMBinding(t, rule);
                            rule.bindings.put(t, b);
                        }
                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        if (pl > endpl)
                            break;
                        delim = sut.checkDelims(s, pl, endpl, delims);
                        s1.pushBinding = b;
                        s2.popBinding = true;
                    }
                    if (delim == -1) {
                        if (s[pl] == '*') {
                            s1.put(ScapeTreenotationTerm.nullTerm, s2, false);
                            s2.put(ScapeTreenotationTerm.nullTerm, s1, true);
                            pl++;
                        } else if (s[pl] == '+') {
                            s2.put(ScapeTreenotationTerm.nullTerm, s1, true);
                            pl++;
                        } else if (s[pl] == '?') {
                            s1.put(ScapeTreenotationTerm.nullTerm, s2, false);
                            pl++;
                        }
                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        if (pl > endpl)
                            break;
                        delim = sut.checkDelims(s, pl, endpl, delims);
                    }
                }
                if (delim != -1)
                    break;
            } else if (s[pl] == '|') {
                cur.put(ScapeTreenotationTerm.nullTerm, end, false);
                if (cur == start)
                    throw new ParseException("empty expression before '|'", null, s, pl, endpl);
                cur = start;
                pl++;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                sut.checkDelims(s, pl, endpl, delims);
            } else {
                TrnTemplate tmpl = new TrnTemplate();
                ScapeFSMBinding b = null;
                pl = tmpl.readIn(rule.phase.types, s, pl, endpl, rule.phase.caseInsensitiveFeatures);

                ScapeFSMState s2 = addState();
                s2.rule = rule;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                if (pl > endpl)
                    break;
                if ((delim = sut.checkDelims(s, pl, endpl, delims)) == -1) {
                    if (s[pl] == ':') {
                        pl++;
                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        int beg = pl;
                        pl = sut.skipVarName(s, pl, endpl);
                        if (pl == beg) {
                            throw new ParseException("missing binding name", null, s, pl, endpl);
                        }
                        String t = new String(s, beg, pl - beg);
                        if (rule.potentialSysName(t)) {
                            throw new ParseException(t + " cannot be a binding name", null, s, pl, endpl);
                        }

                        if ((b = (ScapeFSMBinding) rule.bindings.get(t)) == null) {
                            b = new ScapeFSMBinding(t, rule);
                            rule.bindings.put(t, b);
                        }
                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        if (pl > endpl)
                            break;
                        delim = sut.checkDelims(s, pl, endpl, delims);
                    }

                    if (delim == -1) {
                        if (s[pl] == '*') {
                            ScapeFSMState s1 = addState();
                            s1.rule = rule;
                            cur.put(ScapeTreenotationTerm.nullTerm, s1, false);
                            s1.put(ScapeTreenotationTerm.nullTerm, s2, false);
                            s2.put(ScapeTreenotationTerm.nullTerm, s1, true);
                            cur = s1;
                            pl++;
                        } else if (s[pl] == '+') {
                            ScapeFSMState s1 = addState();
                            s1.rule = rule;
                            cur.put(ScapeTreenotationTerm.nullTerm, s1, false);
                            s2.put(ScapeTreenotationTerm.nullTerm, s1, true);
                            cur = s1;
                            pl++;
                        } else if (s[pl] == '?') {
                            if (b != null) {
                                ScapeFSMState s1 = addState();
                                s1.rule = rule;
                                cur.put(ScapeTreenotationTerm.nullTerm, s1, false);
                                cur = s1;
                            }
                            cur.put(ScapeTreenotationTerm.nullTerm, s2, false);
                            pl++;
                        } else if (b != null) {
                            ScapeFSMState s1 = addState();
                            s1.rule = rule;
                            cur.put(ScapeTreenotationTerm.nullTerm, s1, false);
                            cur = s1;
                        }

                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        if (pl > endpl)
                            break;
                        delim = sut.checkDelims(s, pl, endpl, delims);
                    }
                }

                if (b != null) {
                    cur.pushBinding = b;
                    s2.popBinding = true;
                }

                Iterator<Treenotation> it = tmpl.iterator();
                HashMap<RegexBindingNameAndLogicFSM, ScapeFSMBinding> hmap = new HashMap<RegexBindingNameAndLogicFSM, ScapeFSMBinding>();
                HashSet<ScapeFSMBinding> bindings = new HashSet<ScapeFSMBinding>();

                while (it.hasNext()) {
                    Treenotation trn = it.next();
                    Iterator<NumeratedObject> noit = trn.numeratedObjectIterator();
                    bindings.clear();
                    while (noit.hasNext()) {
                        NumeratedObject no = noit.next();
                        Object val = no.o;
                        if (val instanceof Object[]) {
                            Object[] arr = (Object[]) val;
                            if (arr[0] instanceof Integer) {
                                localBoard.put(no.n, arr[0]);
                            } else {
                                for (Object o : arr) {
                                    if (o instanceof LogicFSM) {
                                        String name = tmpl.fsmNames.get((LogicFSM) o);
                                        if (name != null) {
                                            ScapeRegexBinding srb = (ScapeRegexBinding) rule.regexBindings.get(name);
                                            if (srb == null) {
                                                srb = new ScapeRegexBinding(name);
                                                rule.regexBindings.put(name, srb);
                                            }

                                            RegexBindingNameAndLogicFSM rb = new RegexBindingNameAndLogicFSM(name, (LogicFSM) o, no.n);
                                            ScapeFSMBinding bnd = hmap.get(rb);
                                            if (bnd == null) {
                                                String t = rule.nextSysName();
                                                bnd = new ScapeFSMBinding(t, rule);
                                                bnd.regexBinding = srb;
                                                bnd.regexFSM = (LogicFSM) o;
                                                bnd.regexFeature = no.n;

                                                rule.bindings.put(t, bnd);
                                                hmap.put(rb, bnd);
                                            }
                                            bindings.add(bnd);
                                        }
                                    }
                                }
                                localBoard.put(no.n, LogicFSM.multipleAnd(arr));
                            }
                        } else if (val instanceof LogicFSM) {
                            String name = tmpl.fsmNames.get((LogicFSM) val);
                            if (name != null) {
                                ScapeRegexBinding srb = (ScapeRegexBinding) rule.regexBindings.get(name);
                                if (srb == null) {
                                    srb = new ScapeRegexBinding(name);
                                    rule.regexBindings.put(name, srb);
                                }

                                RegexBindingNameAndLogicFSM rb = new RegexBindingNameAndLogicFSM(name, (LogicFSM) val, no.n);
                                ScapeFSMBinding bnd = hmap.get(rb);
                                if (bnd == null) {
                                    String t = rule.nextSysName();
                                    bnd = new ScapeFSMBinding(t, rule);
                                    bnd.regexBinding = srb;
                                    bnd.regexFSM = (LogicFSM) val;
                                    bnd.regexFeature = no.n;
                                    rule.bindings.put(t, bnd);
                                    hmap.put(rb, bnd);
                                }
                                bindings.add(bnd);
                            }
                            localBoard.put(no.n, ((LogicFSM) val).getDeterminized());
                        } else {
                            localBoard.put(no.n, no.o);
                        }
                    }
                    trn = TreetonFactory.newTreenotation(null, null, trn.getType());
                    trn.put(localBoard);
                    rule.phase.registerTrn(trn);

                    ScapeFSMState first = cur;
                    ScapeFSMState last = s2;

                    for (ScapeFSMBinding curb : bindings) {
                        ScapeFSMState s1 = addState();
                        s1.rule = rule;
                        first.put(ScapeTreenotationTerm.nullTerm, s1, false);
                        s1.pushBinding = curb;
                        first = s1;
                        s1 = addState();
                        s1.rule = rule;
                        s1.put(ScapeTreenotationTerm.nullTerm, last, false);
                        s1.popBinding = true;
                        last = s1;
                    }

                    first.put(new ScapeTreenotationTerm(trn, rule), last, false);
                }
                cur = s2;
                if (delim != -1)
                    break;
            }
        }
        sut.checkEndOfStream(s, pl, endpl);
        if (cur == start)
            throw new ParseException("empty expression", null, s, pl, endpl);
        cur.put(ScapeTreenotationTerm.nullTerm, end, false);
        return pl;
    }

    private class RegexBindingNameAndLogicFSM {
        String bn;
        LogicFSM fsm;
        int feature;

        public RegexBindingNameAndLogicFSM(String bn, LogicFSM fsm, int feature) {
            this.bn = bn;
            this.fsm = fsm;
            this.feature = feature;
        }

        public int hashCode() {
            return bn.hashCode() + fsm.hashCode() + feature;
        }

        public boolean equals(Object _other) {
            RegexBindingNameAndLogicFSM other = (RegexBindingNameAndLogicFSM) _other;
            return bn.equals(other.bn) && fsm == other.fsm && feature == other.feature;
        }
    }
}
