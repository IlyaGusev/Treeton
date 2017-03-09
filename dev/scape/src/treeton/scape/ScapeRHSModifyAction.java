/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.Factory;
import gate.FeatureMap;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.scape.AssignmentVector;
import treeton.core.scape.ParseException;
import treeton.core.scape.RegexpVariable;
import treeton.core.scape.ScapeVariable;
import treeton.core.util.MutableInteger;
import treeton.core.util.sut;

import java.util.HashMap;

public class ScapeRHSModifyAction extends ScapeRHSAction {
    public static boolean transferFeatureGateHack = true;

    private static char[] keyword = "modify".toCharArray();
    ScapeBinding toModify;
    int modif;

    AssignmentVector assignments;

    TrnType tp;

    protected ScapeRHSModifyAction(ScapeRule rule) {
        super(rule);
    }

    public ScapeRHSActionResult buildResult() {
        if (toModify.getStartToken() != null) { //Treeton
            Treenotation trn = toModify.getTrn(modif < 0 ? 0 : modif);
            trn.fillBlackBoard(rule.localBoard);
            Treenotation ntrn = TreetonFactory.newTreenotation(trn.getStartToken(), trn.getEndToken(), tp, rule.localBoard);
            assignments.assign(rule.localBoard);
            ntrn.put(rule.localBoard);
            return new ScapeRHSModifyActionResult(trn, ntrn);
        } else { //Gate
            Annotation ann = toModify.getAnn(modif < 0 ? 0 : modif);

            FeatureMap fm = Factory.newFeatureMap();
            fm.putAll(ann.getFeatures());
            if (!transferFeatureGateHack) {
                assignments.assign(rule.localBoard);
            } else {
                ScapeRHSCreateAction.assignHack(assignments, fm, tp);
            }
            try {
                return new ScapeRHSModifyActionResult(ann, fm, tp.getName());
            } catch (TreetonModelException e) {
                return null;
            }
        }
    }

    public int readIn(TrnTypeStorage types, char[] s, int pl, int endpl, HashMap<String, ScapeVariable> bindings) throws ParseException {
        pl = sut.skipSpacesEndls(s, pl, endpl);
        pl = sut.readInString(s, pl, endpl, keyword);
        pl = sut.skipSpacesEndls(s, pl, endpl);

        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing binding name", null, s, pl, endpl);
        }
        String t = new String(s, beg, pl - beg);
        toModify = (ScapeBinding) bindings.get(t);
        if (toModify == null) {
            throw new ParseException("Cannot resolve symbol " + t, null, s, pl, endpl);
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        modif = -1;
        MutableInteger mi = new MutableInteger();
        if (s[pl] == '[') {
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            pl = sut.readInInteger(s, pl, endpl, mi);
            modif = mi.value;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] != ']') {
                throw new ParseException("missing ']'", null, s, pl, endpl);
            }
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
        }
        int i = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == i) {
            throw new ParseException("missing type name", null, s, pl, endpl);
        }
        t = new String(s, i, pl - i);
        try {
            if ((tp = types.get(t)) == null) {
                throw new ParseException("unregistered type " + t, null, s, pl, endpl);
            }
        } catch (TreetonModelException e) {
            throw new ParseException("Treeton model", null, s, pl, endpl);
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);

        assignments = new AssignmentVector(tp);
        return assignments.readIn(s, pl, endpl, bindings, new HashMap<String, RegexpVariable>());
    }

}
