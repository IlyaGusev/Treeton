/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

import java.util.ArrayList;
import java.util.HashSet;

public class RegExpParseTree implements Parsable {
    public static final char OP_AND = '&';
    public static final char OP_OR = '|';
    public static final char OP_SEQUENCE = '*';
    public static final char OP_OPTIONAL = '?';
    static int statesCount;
    static int markCount;
    private char operator;
    private IsLogicSet container;
    private RegExpParseTree left = null;
    private RegExpParseTree right = null;
    private HashSet<Integer> firstpos;
    private HashSet<Integer> lastpos;
    private boolean nullable;
    private ArrayList<Node> terms;
    private Integer mark = null;

    RegExpParseTree() {
    }

    public static RegExpParseTree createAnd(RegExpParseTree a, RegExpParseTree b) {
        if (a == null) return null;
        if (b == null) return null;
        if (a.container == LogicTerm.EPS) return b;
        if (b.container == LogicTerm.EPS) return a;
        RegExpParseTree res = new RegExpParseTree();
        res.makeAnd(a, b);
        return res;
    }

    public static RegExpParseTree createOr(RegExpParseTree a, RegExpParseTree b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.equals(b)) return a;
        if (a.container == LogicTerm.EPS) return createOptional(b);
        if (b.container == LogicTerm.EPS) return createOptional(a);
        RegExpParseTree res = new RegExpParseTree();
        res.makeOr(a, b);
        return res;
    }

    public static RegExpParseTree createSeq(RegExpParseTree a) {
        if (a == null) return null;
        if (a.container == LogicTerm.EPS) return a;  //e*=e
        RegExpParseTree res = new RegExpParseTree();
        res.makeSeq(a);
        return res;
    }

    public static RegExpParseTree createOptional(RegExpParseTree a) {
        if (a == null) return null;
        if (a.container == LogicTerm.EPS) return a;  //e?=e
        RegExpParseTree res = new RegExpParseTree();
        res.makeOptional(a);
        return res;
    }

    public static RegExpParseTree createTerm(IsLogicSet term) {
        RegExpParseTree res = new RegExpParseTree();
        res.makeTerm(term);
        return res;
    }

    private static Node getSetFromHashSetOfSets(Node s, HashSet<Node> hs) {
        if (hs != null)
            for (Node subset : hs)
                if (subset.equals(s)) return subset;
        return null;
    }

    public static IsLogicSet buildComplementForUnion(ArrayList<IsLogicSet> union) {
        if (union == null || union.size() == 0) {
            return null;
        }
        IsLogicSet res = union.get(0).not();
        for (int i = 1; i < union.size(); i++) {
            IsLogicSet set = union.get(i);
            res = res.and(set.not());
        }
        return res;
    }

    public static ArrayList<IsLogicSet> breakSetUnionToUnionOfNotIntersectingSet(ArrayList<IsLogicSet> union) {
        ArrayList<IsLogicSet> res = new ArrayList<IsLogicSet>();
        IsLogicSet emptySet = null;
        for (IsLogicSet subset : union)
            if (!subset.isEmpty()) {
                if (res.size() == 0) {
                    res.add(subset);
                } else {
                    ArrayList<IsLogicSet> newres = new ArrayList<IsLogicSet>();
                    IsLogicSet aMinusAllB = subset;
                    for (IsLogicSet broken : res)
                        if (!broken.isEmpty()) {
                            IsLogicSet aAndB = subset.and(broken);
                            if (aAndB.isEmpty())
                                newres.add(broken);
                            else {

                                if (!aMinusAllB.isEmpty())
                                    aMinusAllB = aMinusAllB.and(aAndB.not());
                                newres.add(aAndB);
                                IsLogicSet bMinusA = broken.and(subset.not());
                                if (!bMinusA.isEmpty()) newres.add(bMinusA);
                            }
                        } else newres.add(subset);
                    if (!aMinusAllB.isEmpty()) newres.add(aMinusAllB);
                    res = newres;
                }

            } else emptySet = subset;
        if (emptySet != null) res.add(emptySet);
        return res;
    }

    public static RegExpParseTree parseRegExp(String exp) throws Exception {
        return parseRegExp(exp, true);
    }

    public static RegExpParseTree parseRegExp(String exp, boolean caseSensitive) throws Exception {
        if (exp.length() == 0) {
            return createTerm(SingleCharLogicSet.createEmpty());
        }
        RegExpParseTree res = new RegExpParseTree();
        markCount = 1;
        int i = res.parse(exp, caseSensitive);
        //res.mark = 0;  // = $0
        // $0 must be implemented otherway
        markCount = 1;
        res.correctBindings();

    /* add # to end
     *          .
     *   T =  /  \
     *       T    #
     */
//    res.makeAnd(res.clone(),
//      RegExpParseTree.createTerm(SingleCharLogicSet.createEmpty()));

        return res;
    }

    public static String convertOrthmToRegex(String orthm) {
        StringBuffer res = new StringBuffer();
        boolean inBrackets = false;
        for (int i = 0; i < orthm.length(); i++) {
            char c = orthm.charAt(i);
            switch (c) {
                case '[':
                    inBrackets = true;
                    res.append(c);
                    break;
                case ']':
                    inBrackets = false;
                    res.append(c);
                    break;
                case '*':
                    if (!inBrackets) res.append('(');
                    res.append(c);
                    if (!inBrackets) res.append(')');
                    break;
                default:
                    res.append(c);
            }
        }
        res.append('*');
        return res.toString();
    }

    public RegExpParseTree clone() {
        RegExpParseTree res = new RegExpParseTree();
        res.left = left;
        res.right = right;
        res.operator = operator;
        res.container = container;
        res.mark = mark;
        res.firstpos = firstpos;
        res.lastpos = lastpos;
        res.nullable = nullable;
        return res;
    }

    void makeOr(RegExpParseTree a, RegExpParseTree b) {
        left = a;
        right = b;
        operator = OP_OR;
        container = null;
        mark = null;
    }

    private void makeAnd(RegExpParseTree a, RegExpParseTree b) {
        left = a;
        right = b;
        operator = OP_AND;
        container = null;
        mark = null;
    }

    private void makeSeq(RegExpParseTree a) {
        left = a;
        right = null;
        operator = OP_SEQUENCE;
        container = null;
        mark = null;
    }

    private void makeOptional(RegExpParseTree a) {
        left = a;
        right = null;
        operator = OP_OPTIONAL;
        container = null;
        mark = null;
    }

    private void makeTerm(IsLogicSet o) {
        left = null;
        right = null;
        container = o;
    }

    public LogicFSM createNonDeterminedLogicFSM() {
        LogicFSM res = new LogicFSM();
        createNonDeterminedLogicFSM(res);
        return res;
    }

    public void createNonDeterminedLogicFSM(LogicFSM res) {
        statesCount = 0;
        LogicState startState = new LogicState(statesCount++, Integer.toString(statesCount));
        LogicState finState = getNonDeterminedLogicFSMFinishState(res, startState);
        finState.setFinal(true);
        res.finals.add(finState);
        res.states.add(startState);
        res.startState = startState;
    }

    private LogicState getNonDeterminedLogicFSMFinishState(LogicFSM fsm, LogicState start) {
        LogicState fin1, fin2;
        LogicState res = null;

        if (container == null) {
            if (operator == OP_AND) {
//      -LEFT--RIGHT-
                fin1 = left.getNonDeterminedLogicFSMFinishState(fsm, start);
                fin2 = right.getNonDeterminedLogicFSMFinishState(fsm, fin1);
                res = fin2;
            } else if (operator == OP_OR) {
//                l
//            /-e-LEFT--e-\
//       -start            fin-
//            \-e-RIGHT-e-/
//                r
                statesCount++;
                LogicState l = new LogicState(statesCount, Integer.toString(statesCount));
                statesCount++;
                LogicState r = new LogicState(statesCount, Integer.toString(statesCount));
                statesCount++;
                LogicState f = new LogicState(statesCount, Integer.toString(statesCount));
                fin1 = left.getNonDeterminedLogicFSMFinishState(fsm, l);
                fin2 = right.getNonDeterminedLogicFSMFinishState(fsm, r);
                LogicPair pSL = new LogicPair(start, l, LogicTerm.EPS);
                LogicPair pSR = new LogicPair(start, r, LogicTerm.EPS);
                LogicPair pLF = new LogicPair(fin1, f, LogicTerm.EPS);
                LogicPair pRF = new LogicPair(fin2, f, LogicTerm.EPS);
                start.addPair(pSL);
                start.addPair(pSR);
                fin1.addPair(pLF);
                fin2.addPair(pRF);
                fsm.pairs.add(pSL);
                fsm.pairs.add(pSR);
                fsm.pairs.add(pLF);
                fsm.pairs.add(pRF);
                fsm.states.add(l);
                fsm.states.add(r);
                fsm.states.add(f);
                res = f;
            } else if (operator == OP_SEQUENCE) {
                statesCount++;
                LogicState fin0 = new LogicState(statesCount, Integer.toString(statesCount));
                statesCount++;
                fin2 = new LogicState(statesCount, Integer.toString(statesCount));
                fin1 = left.getNonDeterminedLogicFSMFinishState(fsm, fin0);

                LogicPair cycle = new LogicPair(fin1, fin0, LogicTerm.EPS);
                LogicPair exit = new LogicPair(fin0, fin2, LogicTerm.EPS);
                LogicPair enter = new LogicPair(start, fin0, LogicTerm.EPS);
                fin1.addPair(cycle);
                fin0.addPair(exit);
                start.addPair(enter);
                fsm.pairs.add(cycle);
                fsm.pairs.add(exit);
                fsm.pairs.add(enter);
                fsm.states.add(fin0);
                fsm.states.add(fin2);
                res = fin2;
            } else if (operator == OP_OPTIONAL) {
                statesCount++;
                LogicState fin0 = new LogicState(statesCount, Integer.toString(statesCount));
                statesCount++;
                fin2 = new LogicState(statesCount, Integer.toString(statesCount));
                fin1 = left.getNonDeterminedLogicFSMFinishState(fsm, fin0);

                LogicPair skip = new LogicPair(start, fin2, LogicTerm.EPS);
                LogicPair exit = new LogicPair(fin1, fin2, LogicTerm.EPS);
                LogicPair enter = new LogicPair(start, fin0, LogicTerm.EPS);
                fin1.addPair(exit);
                start.addPair(enter);
                start.addPair(skip);
                fsm.pairs.add(exit);
                fsm.pairs.add(enter);
                fsm.pairs.add(skip);
                fsm.states.add(fin0);
                fsm.states.add(fin2);
                res = fin2;
            }
        } else {
            statesCount++;
            fin1 = new LogicState(statesCount, Integer.toString(statesCount));
            LogicPair pair = new LogicPair(start, fin1, new LogicTerm(container));
            start.addPair(pair);
            fsm.pairs.add(pair);
            fsm.states.add(fin1);
            res = fin1;
        }
        assert res != null;
        if (mark != null) {
            start.startBinding.add(mark);
            res.finishBinding.add(mark);
        }
        return res;
    }

    public LogicFSM createDeterminedLogicFSM() {
        return createNonDeterminedLogicFSM().getDeterminized();
    }

    private void markPos(ArrayList<Node> rootTerms) {
        firstpos = new HashSet<Integer>();
        lastpos = new HashSet<Integer>();
        if (left != null) left.markPos(rootTerms);
        if (right != null) right.markPos(rootTerms);

        if (container == null) {
            if (operator == OP_AND) {
                if (right != null) {
                    lastpos.addAll(right.lastpos);
                    if (right.nullable && (left != null))
                        lastpos.addAll(left.lastpos);
                }
                if (left != null) {
                    firstpos.addAll(left.firstpos);
                    if (left.nullable && (right != null))
                        firstpos.addAll(right.firstpos);
                }
                nullable = ((left == null) || left.nullable) && ((right == null) || right.nullable);
            } else if (operator == OP_OR) {
                if (left != null) {
                    lastpos.addAll(left.lastpos);
                    firstpos.addAll(left.firstpos);
                }
                if (right != null) {
                    lastpos.addAll(right.lastpos);
                    firstpos.addAll(right.firstpos);
                }
                nullable = ((left == null) || left.nullable) || ((right == null) || right.nullable);
            } else if (operator == OP_SEQUENCE) {
                if (left != null) {
                    lastpos.addAll(left.lastpos);
                    firstpos.addAll(left.firstpos);
                }
                nullable = true;
            } else nullable = true;
        } else {
            Integer pos = rootTerms.size();
            rootTerms.add(new Node(pos, container));
            lastpos.add(pos);
            firstpos.add(pos);
            nullable = false;//container.isEmpty();
        }
    }

    private void followpos(ArrayList<Node> rootTerms) {
        if (right != null) right.followpos(rootTerms);
        if (left != null) {
            left.followpos(rootTerms);
            if (operator == OP_AND) {
                if (right != null)
                    for (Integer t : left.lastpos)
                        rootTerms.get(t).nextIds.addAll(right.firstpos);
            } else if (operator == OP_SEQUENCE) {
                for (Integer t : left.lastpos)
                    rootTerms.get(t).nextIds.addAll(left.firstpos);
            }
        }
    }

    public int parse(String s) {
        return parse(s, true);
    }

    public int parse(String s, boolean caseSensitive) {
        int i;
        try {
            i = pE(s, caseSensitive);
        } catch (Exception e) {
            return -1;
        }
        return i;
    }

    private void correctBindings() {
        correctBindings(markCount);
    }

    public int correctBindings(int m) {
        if (mark != null) mark = m++;
        if (left != null) m = left.correctBindings(m);
        if (right != null) m = right.correctBindings(m);
        return m;
    }

    private int pE(String s, boolean caseSensitive) throws Exception {
        int i = this.pM(s, caseSensitive);
        if (i < s.length()) {
            if (s.charAt(i) == '|') {
                i++;
                RegExpParseTree rest = new RegExpParseTree();
                RegExpParseTree clone = this.clone();
                i += rest.pE(s.substring(i), caseSensitive);
                this.makeOr(clone, rest);

                return i;
            }
        }
        return i;
    }

    private int pM(String s, boolean caseSensitive) throws Exception {
        int i = this.pS(s, caseSensitive);
        if ((i < s.length() && (s.charAt(i) != '|') && (s.charAt(i) != ')'))
                ) {
            RegExpParseTree rest = new RegExpParseTree();
            RegExpParseTree clone = this.clone();
            i += rest.pM(s.substring(i), caseSensitive);
            this.makeAnd(clone, rest);
            return i;
        } else return i;
    }

    private int pS(String s, boolean caseSensitive) throws Exception {
        int i = this.pT(s, caseSensitive);
        if (i < s.length()) {
            if (s.charAt(i) == '*') {
                i++;
                RegExpParseTree clone = this.clone();
                this.makeSeq(clone);
            } else if (s.charAt(i) == '?') {
                i++;
                RegExpParseTree clone = this.clone();
                this.makeOptional(clone);
            }
        }
        return i;
    }

    private int pT(String s, boolean caseSensitive) throws Exception {
        //ParsebleTerm.ParseRes term=Parsable.pa

        int i = 0;
        char c = s.charAt(0);
        if (c == '(') {
            i++;
            i += this.pE(s.substring(i), caseSensitive);
            if (s.charAt(i) == ')') i++;
            else throw new Exception();
            this.mark = markCount++; // = $1, $2 ...
        } else //if((c!='|')&&(c!=')'))
        {
            SingleCharLogicSet term = new SingleCharLogicSet();
            i = term.parse(s);
            if (!caseSensitive) term.makeNotCaseSensitive();
            if (i < 0) throw new Exception();
            this.makeTerm(term);
        }
        return i;
    }

    public String toString() {
        String nul = "NULL";
        String res;
        if (container == null) {
            String lefts = (left == null) ? nul : left.toString();
            String rights = (right == null) ? nul : right.toString();
            switch (operator) {
                case OP_AND:
                    res = lefts + rights;
                    break;
                case OP_OR:
                    res = "{" + lefts + "|" + rights + "}";
                    break;
                case OP_SEQUENCE:
                    res = lefts + "*";
                    break;
                case OP_OPTIONAL:
                    res = lefts + "?";
                    break;
                default:
                    res = nul;
            }
        } else res = container.toString();
        if (mark != null && mark != 0) res = "(" + res + ")";
        return res;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof RegExpParseTree)) return false;
        RegExpParseTree r = (RegExpParseTree) obj;
        if (container != null) return container.equals(r.container);
        if (operator != r.operator) return false;

        return (left != null && r.left != null && left.equals(r.left) || left == null && r.left == null)
                && (right != null && r.right != null && right.equals(r.right) || right == null && r.right == null);
    }

    public Integer getMark() {
        return mark;
    }

    public void setMark(Integer mark) {
        this.mark = mark;
    }

    public void setMark() {
        this.mark = 0;
    }

    private class Node {
        public Integer id;
        public boolean isFinal = false;
        public Object container;
        public HashSet<Integer> nextIds;
        public boolean flag = false;

        public Node(int i, Object o) {
            id = i;
            container = o;
            nextIds = new HashSet<Integer>();
        }

        public boolean equals(Object n) {
            if (n == null) return false;
            if (n.getClass() != this.getClass()) return false;
            HashSet<Integer> a = this.nextIds;
            HashSet<Integer> b = ((Node) n).nextIds;
            if ((a == null) && (b == null)) return true;
            if ((a == null) || (b == null)) return false;
            if (a.size() != b.size()) return false;
            else
                for (Integer i : a)
                    if (!b.contains(i)) return false;
            return true;
        }

        public String toString() {
            StringBuffer res = new StringBuffer();
            res.append(id);
            char c = '(';
            for (Integer i : nextIds) {
                res.append(c);
                c = ',';
                res.append(i);
            }
            c = ')';
            res.append(c);
            return res.toString();
        }
    }
}
