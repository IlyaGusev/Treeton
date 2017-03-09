/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import treeton.core.Treenotation;
import treeton.core.scape.ParseException;
import treeton.core.scape.ScapeVariable;
import treeton.core.util.MutableInteger;
import treeton.core.util.sut;

import java.util.HashMap;

public class ScapeRHSRemoveAction extends ScapeRHSAction {
    private static char[] keyword = "remove".toCharArray();
    ScapeBinding binding;
    int modif;

    protected ScapeRHSRemoveAction(ScapeRule rule) {
        super(rule);
    }

    public ScapeRHSActionResult buildResult() {
        if (binding == null || binding.getSize() == 0) {
            return null;
        }

        if (binding.getStartToken() != null) { //Treeton
            if (modif < 0) {
                return new ScapeRHSRemoveActionResult(binding.toTrnArray());
            } else {
                Treenotation trn = binding.getTrn(modif);
                if (trn == null) {
                    return null;
                }

                Treenotation[] arr = new Treenotation[1];
                arr[0] = trn;
                return new ScapeRHSRemoveActionResult(arr);
            }
        } else { //Gate
            if (modif < 0) {
                return new ScapeRHSRemoveActionResult(binding.toAnnArray());
            } else {
                Annotation ann = binding.getAnn(modif);
                if (ann == null) {
                    return null;
                }

                Annotation[] arr = new Annotation[1];
                arr[0] = ann;
                return new ScapeRHSRemoveActionResult(arr);
            }
        }
    }

    public int readIn(char[] s, int pl, int endpl, HashMap<String, ? extends ScapeVariable> bindings) throws ParseException {
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
        binding = (ScapeBinding) bindings.get(t);
        if (binding == null) {
            throw new ParseException("Cannot resolve symbol " + t, null, s, pl, endpl);
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] == '[') {
            MutableInteger mi = new MutableInteger();
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
        }
        return pl;
    }
}
