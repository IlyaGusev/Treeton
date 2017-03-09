/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.Factory;
import gate.FeatureMap;
import gate.Node;
import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.model.TrnTypeUtils;
import treeton.core.scape.*;
import treeton.core.util.MutableInteger;
import treeton.core.util.NumeratedObject;
import treeton.core.util.nu;
import treeton.core.util.sut;

import java.util.HashMap;
import java.util.Iterator;

public class ScapeRHSCreateAction extends ScapeRHSAction {
    public static boolean transferFeatureGateHack = true;
    private static char[] keyword = "create".toCharArray();
    TrnType tp;
    ScapeBinding start;
    int startModif;
    ScapeBinding end;
    int endModif;
    AssignmentVector assignments;
    boolean createOpened = false;

    protected ScapeRHSCreateAction(ScapeRule rule) {
        super(rule);
    }

    public static void assignHack(AssignmentVector assignments, FeatureMap fm, TrnType tp) {
        Iterator<NumeratedObject> it = assignments.numeratedObjectIterator();
        while (it.hasNext()) {
            NumeratedObject no = it.next();
            if (no.n == -2) {
                if (no.o instanceof Object[]) {
                    Object[] arr = (Object[]) no.o;
                    Annotation src = ((ScapeBinding) arr[0]).getAnn((Integer) arr[1]);
                    fm.putAll(src.getFeatures());
                } else {
                    Annotation src = ((ScapeBinding) no.o).getAnn(0);
                    fm.putAll(src.getFeatures());
                }
            } else {
                ScapeExpression ex = (ScapeExpression) no.o;
                Object r = ex.evaluate();
                try {
                    if (r == nu.ll) {
                        fm.remove(tp.getFeatureNameByIndex(no.n));
                    } else if (r != null) {
                        fm.put(tp.getFeatureNameByIndex(no.n), TrnTypeUtils.treatFeatureValueForGate(tp, no.n, r));
                    }
                } catch (TreetonModelException e) {
                    //do nothing
                }
            }
        }
    }

    public ScapeRHSActionResult buildResult() {
        if (start == null || end == null) {
            return null;
        }

        if (start.getSize() == 0 || end.getSize() == 0) {
            return null;
        }

        if (start.getStartToken() != null) { //Treeton
            Token ss;
            Token se;
            Token es;
            Token ee;
            if (startModif < 0) {
                ss = start.getStartToken();
                se = start.getEndToken();
            } else {
                Treenotation trn = start.getTrn(startModif);
                if (trn == null)
                    return null;
                ss = trn.getStartToken();
                se = trn.getEndToken();
            }

            if (endModif < 0) {
                es = end.getStartToken();
                ee = end.getEndToken();
            } else {
                Treenotation trn = end.getTrn(endModif);
                if (trn == null)
                    return null;
                es = trn.getStartToken();
                ee = trn.getEndToken();
            }

            Token min = ss.compareTo(es) < 0 ? ss : es;
            Token max = se.compareTo(ee) > 0 ? se : ee;
            assignments.assign(rule.localBoard);
            if (createOpened) {
                Treenotation trn = TreetonFactory.newSyntaxTreenotation(min.getStorage(), min, max, tp, rule.localBoard);
                return new ScapeRHSCreateActionResult(trn);
            } else {
                Treenotation trn = TreetonFactory.newTreenotation(min, max, tp, rule.localBoard);
                return new ScapeRHSCreateActionResult(trn);
            }
        } else { //Gate
            Node ss;
            Node se;
            Node es;
            Node ee;
            if (startModif < 0) {
                ss = start.getStartNode();
                se = start.getEndNode();
            } else {
                Annotation ann = start.getAnn(startModif);
                if (ann == null)
                    return null;
                ss = ann.getStartNode();
                se = ann.getEndNode();
            }

            if (endModif < 0) {
                es = end.getStartNode();
                ee = end.getEndNode();
            } else {
                Annotation ann = end.getAnn(endModif);
                if (ann == null)
                    return null;
                es = ann.getStartNode();
                ee = ann.getEndNode();
            }

            Node min = ss.getOffset().compareTo(es.getOffset()) < 0 ? ss : es;
            Node max = se.getOffset().compareTo(ee.getOffset()) > 0 ? se : ee;

            FeatureMap fm = Factory.newFeatureMap();
            if (!transferFeatureGateHack) {
                assignments.assign(rule.localBoard);
            } else {
                assignHack(assignments, fm, tp);
            }
            try {
                return new ScapeRHSCreateActionResult(min, max, fm, tp.getName());
            } catch (TreetonModelException e) {
                throw new RuntimeException("Problem with model!!!");
            }
        }
    }

    public int readIn(TrnTypeStorage types, char[] s, int pl, int endpl, HashMap<String, ? extends ScapeVariable> bindings, HashMap<String, RegexpVariable> regexbindings) throws ParseException {
        startModif = -1;
        endModif = -1;

        pl = sut.skipSpacesEndls(s, pl, endpl);
        pl = sut.readInString(s, pl, endpl, keyword);
        pl = sut.skipSpacesEndls(s, pl, endpl);

        sut.checkEndOfStream(s, pl, endpl);

        createOpened = false;

        if (s[pl] == '-') {
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            int i = pl;
            pl = sut.skipVarName(s, pl, endpl);
            if (pl == i) {
                throw new ParseException("missing option", null, s, pl, endpl);
            }
            String op = new String(s, i, pl - i);

            if (op.equals("o")) {
                createOpened = true;
            }

            pl = sut.skipSpacesEndls(s, pl, endpl);

        }

        int i = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == i) {
            throw new ParseException("missing type name", null, s, pl, endpl);
        }
        String t = new String(s, i, pl - i);
        try {
            if ((tp = types.get(t)) == null) {
                throw new ParseException("unregistered type " + t, null, s, pl, endpl);
            }
        } catch (TreetonModelException e) {
            throw new ParseException("unregistered type " + t, null, s, pl, endpl);
        }

        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);

        if (s[pl] != '[') {
            throw new ParseException("missing '['", null, s, pl, endpl);
        }
        pl++;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing binding name", null, s, pl, endpl);
        }
        t = new String(s, beg, pl - beg);
        start = (ScapeBinding) bindings.get(t);
        if (start == null) {
            throw new ParseException("Cannot resolve symbol " + t, null, s, pl, endpl);
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != ',' && s[pl] != '[') {
            throw new ParseException("missing ',' or '['", null, s, pl, endpl);
        }
        MutableInteger mi = new MutableInteger();
        if (s[pl] == '[') {
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            pl = sut.readInInteger(s, pl, endpl, mi);
            startModif = mi.value;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] != ']') {
                throw new ParseException("missing ']'", null, s, pl, endpl);
            }
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] != ',') {
                throw new ParseException("missing ','", null, s, pl, endpl);
            }
        }
        pl++;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing binding name", null, s, pl, endpl);
        }
        t = new String(s, beg, pl - beg);
        end = (ScapeBinding) bindings.get(t);
        if (end == null) {
            throw new ParseException("Cannot resolve symbol " + t, null, s, pl, endpl);
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != ']' && s[pl] != '[') {
            throw new ParseException("missing ']' or '['", null, s, pl, endpl);
        }
        if (s[pl] == '[') {
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            pl = sut.readInInteger(s, pl, endpl, mi);
            endModif = mi.value;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] != ']') {
                throw new ParseException("missing ']'", null, s, pl, endpl);
            }
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] != ']') {
                throw new ParseException("missing ']'", null, s, pl, endpl);
            }
        }
        pl++;
        pl = sut.skipSpacesEndls(s, pl, endpl);

        assignments = new AssignmentVector(tp);
        return assignments.readIn(s, pl, endpl, bindings, regexbindings);
    }

}
