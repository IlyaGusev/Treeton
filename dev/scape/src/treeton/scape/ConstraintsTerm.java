/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import java.awt.*;

public class ConstraintsTerm extends ScapeTerm {
    public static Font font = new Font("Courier", 0, 12);
    String expr;

    public ConstraintsTerm(String _expr) {
        expr = _expr;
    }

    public String getString() {
        StringBuffer b = new StringBuffer("");

        b.append(expr);
        return b.toString();
    }
}
