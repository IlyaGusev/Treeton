/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.scape.ParseException;
import treeton.core.util.sut;

public class ScapeOrthoRHSAction extends ScapeRHSAction {
    private static char[][] keywords = new char[][]{"match".toCharArray(), "unmatch".toCharArray()};
    boolean match;

    protected ScapeOrthoRHSAction(ScapeRule rule) {
        super(rule);
    }

    public ScapeRHSActionResult buildResult() {
        return new ScapeRHSOrthoActionResult(rule.getName());
    }

    boolean isMatch() {
        return match;
    }

    public int readIn(char[] s, int pl, int endpl) throws ParseException {

        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        if (n == -1) {
            throw new ParseException("missing 'match' or 'unmatch' keyword", null, s, pl, endpl);
        } else if (n == 0) { //true
            match = true;
            pl += keywords[0].length;
        } else if (n == 1) { //false
            match = false;
            pl += keywords[1].length;
        }
        return pl;
    }
}
