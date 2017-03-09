/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.util.sut;

public class MarkInIntroduction {
    private static char[][] keywords = {
            "mark".toCharArray(),
            "viewname".toCharArray(),
            "description".toCharArray(),
    };
    TrnType tp;
    String name;
    String viewname;
    String description;

    int readIn(char s[], int pl, int endpl) throws ParseException {
        int startPl = pl;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        if (n == 0) { //mark
            pl += keywords[n].length;
        } else {
            throw new ParseException("Wrong syntax", null, s, pl, endpl);
        }

        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarValue(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing mark name", null, s, pl, endpl);
        }
        name = sut.extractString(s, beg, pl - beg);

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

    public String getName() { // by pjalybin 16.11.05
        return name;
    }

    public String toString() { // by pjalybin 18.11.05
        return name;
    }

    public String getViewname() { // by pjalybin 16.11.05
        return viewname;
    }

    public String getDescription() { // by pjalybin 16.11.05
        return description;
    }

    public TrnType getTrnType() {
        return tp;
    }
}
