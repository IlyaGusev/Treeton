/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.IntFeatureMapStatic;
import treeton.core.fsm.TermStatePair;
import treeton.core.model.TrnTypeSet;
import treeton.core.util.NumeratedObject;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

public class ScapeDFSMState extends ScapeState {
    public static Font font = new Font("Courier", 0, 12);

    int mSize = 0;

    IntFeatureMapStatic transitions;
    TrnTypeSet trnTypes;

    ScapeBindedPair firstPair;
    int nPairs;

    //Note: if transitions == null && nPairs > 0 ==> all pairs are null-pairs and
    //we are working with aggregate state (dfsm-nfsm connector).

    ScapeState[] oldStates;
    int osLen;

    public ScapeDFSMState() {
        osLen = -1;
        firstPair = null;
        nPairs = 0;
    }

    public ScapeDFSMPair addPair(ScapeTerm sampleTerm, ScapeState newState, ScapeBindingSet sampleBindings) {
        ScapeDFSMPair p = new ScapeDFSMPair(sampleTerm, newState, sampleBindings);
        p.next = firstPair;
        firstPair = p;
        nPairs++;
        return p;
    }

    public void buildTrnTypeSet(ScapePhase phase) {
        phase.typesIterator.reset(this);
        trnTypes = phase.tsFactory.newTrnTypeSet(phase.typesIterator);
    }

    public Iterator<TermStatePair> pairsIterator() {
        return new PairsIterator();
    }

    public int getNumberOfPairs() {
        return nPairs;
    }

    public boolean isFinal() {
        return false;
    }

    public void drawSelf(Graphics g, Rectangle2D rect) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        String s = Integer.toString(getId());
        Rectangle2D prefRect = fm.getStringBounds(s, g);
        Rectangle2D internalRect = new Rectangle((int) rect.getCenterX() - (int) (prefRect.getWidth() / 2), (int) (rect.getMaxY() - prefRect.getHeight()), (int) prefRect.getWidth(), (int) prefRect.getHeight());
        prefRect = internalRect.createIntersection(rect);
        g.drawString(s, (int) prefRect.getMinX(), (int) prefRect.getMaxY() - fm.getDescent());
    }

    public String getString() {
        StringBuffer buf = new StringBuffer("Old states are: ");
        for (int i = 0; i < oldStates.length; i++) {
            buf.append(oldStates[i].getId());
            if (i < oldStates.length - 1)
                buf.append(", ");
        }
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
/*    System.out.println(buf);
    System.out.println("-----------------------------------------");*/
        return buf.toString();
    }

    public int compareTo(Object o) {
        ScapeDFSMState s = (ScapeDFSMState) o;

        int len1 = osLen;
        int len2 = s.osLen;
        int n = Math.min(len1, len2);
        ScapeState v1[] = oldStates;
        ScapeState v2[] = s.oldStates;
        int i = 0;

        while (i < n) {
            int s1 = v1[i].getId();
            int s2 = v2[i].getId();
            if (s1 != s2) {
                return s1 - s2;
            }
            i++;
        }
        return len1 - len2;
    }

    private class PairsIterator implements Iterator<TermStatePair> {
        ScapeBindedPair p;

        PairsIterator() {
            p = firstPair;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return p != null;
        }

        public TermStatePair next() {
            ScapeBindedPair t = p;
            p = p.next;
            return t;
        }
    }
}
