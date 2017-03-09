/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import java.awt.*;

public class CharTerm implements Term {
    public static Font font = new Font("Courier", 0, 12);
    public static Term empty = new CharTerm(' ');
    char[] c;

    public CharTerm(char _c) {
        c = new char[1];
        c[0] = _c;
    }

    public String getString() {
        return new String(c);
    }
}
