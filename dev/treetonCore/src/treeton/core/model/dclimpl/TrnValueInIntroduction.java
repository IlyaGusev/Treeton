/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeUtils;
import treeton.core.scape.ParseException;
import treeton.core.util.nu;
import treeton.core.util.sut;

public class TrnValueInIntroduction {

    private static char[][] keywords = {
            "value".toCharArray(),
            "viewname".toCharArray(),
            "description".toCharArray(),
    };
    TrnTypeDclImpl tp;
    Object value;
    int featureIndex;
    String viewname;
    String description;

    int readIn(char s[], int pl, int endpl) throws ParseException {
        int startPl = pl;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        if (n == 0) { // value
            pl += keywords[n].length;
        } else {
            throw new ParseException("Wrong syntax", null, s, pl, endpl);
        }

        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarValue(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing feature value", null, s, pl, endpl);
        }
        Object V;
        if (pl - beg == 4 && s[beg] == 'n' && s[beg + 1] == 'u' && s[beg + 2] == 'l' && s[beg + 3] == 'l') {
            V = nu.ll;
        } else if (pl - beg == 5 && s[beg] == 'o' && s[beg + 1] == 't' && s[beg + 2] == 'h' && s[beg + 3] == 'e' && s[beg + 3] == 'r') {
            V = nu.other;
        } else {
            if (s[beg] == '"') {
                V = sut.extractTString(s, beg + 1, pl - beg - 2);
            } else {
                V = sut.extractTString(s, beg, pl - beg);
            }
        }

        value = TrnTypeUtils.treatFeatureValue(tp, featureIndex, V);

        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);

        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }
        pl++;

        boolean nameFound = false;
        boolean descriptionFound = false;
        while (true) {
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] == '}') {
                break;
            }

            n = sut.checkDelims(s, pl, endpl, keywords);

            if (n == 1) { //viewname
                pl += keywords[n].length;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                int start = pl;
                pl = sut.skipBracesContent(s, pl, endpl);
                viewname = new String(s, start + 1, pl - 1 - start - 1);
                nameFound = true;
            } else if (n == 2) { //description
                pl += keywords[n].length;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                int start = pl;
                pl = sut.skipBracesContent(s, pl, endpl);
                description = new String(s, start + 1, pl - 1 - start - 1);
                descriptionFound = true;
            } else {
                throw new ParseException("Wrong syntax", null, s, pl, endpl);
            }
        }

        if (!descriptionFound) {
            throw new ParseException("missing description block", null, s, startPl, endpl);
        }

        if (!nameFound) {
            throw new ParseException("missing viewname block", null, s, startPl, endpl);
        }

        return pl;
    }

    public TrnType getTrnType() { //getters by pjalybin 17.11.05
        return tp;
    }

    public Object getValue() {
        return value;
    }

    public String toString() {
        return value.toString();
    }

    public int getFeatureIndex() {
        return featureIndex;
    }

    public String getViewname() {
        return viewname;
    }

    public String getDescription() {
        return description;
    }
}
