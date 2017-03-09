/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.fsm.State;
import treeton.core.fsm.Term;
import treeton.core.fsm.TermStatePair;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

public class ScapeFSMState extends ScapeState {
    public static Font font = new Font("Courier", 0, 12);
    ScapeFSMPair firstPair;
    ScapeFSMPair firstNullPair;
    ScapeFSMBinding pushBinding;
    boolean popBinding;
    boolean isFinalState;
    ScapeRule rule;
    int nPairs;
    int nInputs;

    public ScapeFSMState() {
        firstPair = null;
        firstNullPair = null;
        isFinalState = false;
        pushBinding = null;
        popBinding = false;
        nPairs = 0;
        nInputs = 0;
        rule = null;
    }

    TermStatePair put(Term _t, State _s, boolean reverse) {
        if (!(_t instanceof ScapeTreenotationTerm) || !(_s instanceof ScapeFSMState)) {
            throw new IllegalArgumentException();
        }
        ScapeTreenotationTerm t = (ScapeTreenotationTerm) _t;
        ScapeFSMState s = (ScapeFSMState) _s;
        ScapeFSMPair np = new ScapeFSMPair(t, s);
        np.reverse = reverse;
        nPairs++;
        if (t == ScapeTreenotationTerm.nullTerm) {
            if (firstNullPair == null) {
                firstNullPair = np;
            } else {
                np.next = firstNullPair;
                firstNullPair = np;
            }
        } else {
            if (firstPair == null) {
                firstPair = np;
            } else {
                np.next = firstPair;
                firstPair = np;
            }
        }
        return np;
    }

    public TermStatePair put(Term _t, State _s) {
        if (!(_t instanceof ScapeTreenotationTerm) || !(_s instanceof ScapeFSMState)) {
            throw new IllegalArgumentException();
        }
        ScapeTreenotationTerm t = (ScapeTreenotationTerm) _t;
        ScapeFSMState s = (ScapeFSMState) _s;
        ScapeFSMPair np = new ScapeFSMPair(t, s);
        np.reverse = false;
        nPairs++;
        if (t == ScapeTreenotationTerm.nullTerm) {
            if (firstNullPair == null) {
                firstNullPair = np;
            } else {
                np.next = firstNullPair;
                firstNullPair = np;
            }
        } else {
            if (firstPair == null) {
                firstPair = np;
            } else {
                np.next = firstPair;
                firstPair = np;
            }
        }
        return np;
    }

    public Iterator<TermStatePair> pairsIterator() {
        return new PairsIterator();
    }

    public int getNumberOfPairs() {
        return nPairs;
    }

    public boolean isFinal() {
        return isFinalState;
    }

    public void finalizeState() {
        isFinalState = true;
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
        String ruleStr = rule == null ? "null" : rule.name;
        return "<b>[" + ruleStr + ":" + getId() + "]</b>";
    }

    public String toString() {
        String ruleStr = rule == null ? "null" : rule.name;
        return "[" + ruleStr + ":" + getId() + "]";
    }

    private class PairsIterator implements Iterator<TermStatePair> {
        ScapeBindedPair p;
        boolean firstTime;

        PairsIterator() {
            p = firstPair;
            if (p == null) {
                p = firstNullPair;
                firstTime = false;
            } else {
                firstTime = true;
            }
        }

        public void remove() {
        }

        public boolean hasNext() {
            return p != null;
        }

        public TermStatePair next() {
            ScapeBindedPair t = p;
            p = p.next;
            if (p == null && firstTime) {
                p = firstNullPair;
                firstTime = false;
            }
            return t;
        }
    }
}
