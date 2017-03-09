/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.util.NumeratedObject;

import java.util.Iterator;

public class ScapeDFSMStateFinal extends ScapeDFSMState {
    ScapeRuleSet ruleSet;

    public ScapeDFSMStateFinal() {
        super();
    }


    public boolean isFinal() {
        return true;
    }

    public String getString() {
        StringBuffer buf = new StringBuffer("Old states are: ");
        for (int i = 0; i < oldStates.length; i++) {
            buf.append(oldStates[i].getId());
            if (i < oldStates.length - 1)
                buf.append(", ");
        }
        buf.append("<br>");
        buf.append("Final state.<br>");
        buf.append(ruleSet.getString());
        if (transitions != null) {
            buf.append("<br>Transitions are:<br><table border=1><tr><td>Classes</td><td align=center>Transitions</td></tr>");
            Iterator it = transitions.numeratedObjectIterator();
            while (it.hasNext()) {
                buf.append("<tr>");
                NumeratedObject no = (NumeratedObject) it.next();
                buf.append("<td align=center>");
                buf.append(no.n);
                buf.append("</td>");
                buf.append("<td>");
                ScapeBindedPair[] pairs = (ScapeDFSMPair[]) no.o;
                for (int i = 0; i < pairs.length; i++) {
                    buf.append(pairs[i].getString());
                    if (i < pairs.length - 1)
                        buf.append(", ");
                }
                buf.append("</td>");
                buf.append("</tr>");
            }
            buf.append("</table>");
        }
        return buf.toString();
    }
}
