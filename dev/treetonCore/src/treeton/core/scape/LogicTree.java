/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

import java.util.HashMap;

public class LogicTree extends ScapeExpression {
    public void validateExpression(char[] s, int endpl,
                                   HashMap<String, ? extends ScapeVariable> vars,
                                   HashMap<String, RegexpVariable> regexpVars
    ) throws ParseException {
        Class result = validateExpression(s, endpl, root, vars, regexpVars, true);
        if (result != Boolean.class) {
            throw new ParseException("Condition must return boolean", null, s, root.pl, endpl);
        }
    }
}
