/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.scape.ParseException;
import treeton.core.util.sut;

public class ScapeDoAction extends ScapeRHSAction {
    private static char[][] keywords = new char[][]{"do".toCharArray()};
    String command;

    protected ScapeDoAction(ScapeRule rule) {
        super(rule);
    }

    public ScapeRHSActionResult buildResult() {
        return null;
    }

    public int readIn(char[] s, int pl, int endpl) throws ParseException {

        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        if (n == -1) {
            throw new ParseException("missing 'do' keyword", null, s, pl, endpl);
        } else if (n == 0) { //true
            pl += keywords[0].length;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            int beg = pl;
            pl = sut.skipVarValue(s, pl, endpl);
            if (pl == beg) {
                throw new ParseException("missing value", null, s, pl, endpl);
            }
            if (s[beg] == '"') {
                command = sut.extractString(s, beg + 1, pl - beg - 2);
            } else {
                command = sut.extractString(s, beg, pl - beg);
            }
        }
        return pl;
    }

    public String getCommand() {
        return command;
    }

}
