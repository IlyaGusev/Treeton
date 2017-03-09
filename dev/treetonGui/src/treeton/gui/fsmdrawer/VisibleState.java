/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.fsmdrawer;

import treeton.core.fsm.State;
import treeton.core.fsm.Term;
import treeton.core.fsm.TermStatePair;

import java.util.Iterator;

public class VisibleState {
    public double x;
    public double y;
    protected VisiblePair firstPair;
    protected State state;
    protected int nPairs;
    protected boolean isFinal;
    protected boolean selfLoopState;
    protected boolean loopState;
    protected boolean multiLinkState;
    protected TermStatePair realPair; //Only for loopState and multiLinkState
    protected VisiblePair realVisiblePair; //Only for loopState and multiLinkState
    protected int depth;
    protected int minDepth;
    protected int nInputs;
    protected int counter;
    protected long label;
    protected long label2;
    protected Layer layer;

    public VisibleState(State _state) {
        if (_state != null) {
            isFinal = _state.isFinal();
            this.state = _state;
        } else {
            isFinal = false;
            this.state = null;
        }

        nPairs = 0;
        selfLoopState = false;
        loopState = false;
        multiLinkState = false;
        firstPair = null;
        x = 0;
        y = 0;
        depth = -1;
        minDepth = -1;
        nInputs = 0;
        label = -1;
        label2 = -1;
        layer = null;
    }

    public State get(Term t) {
        return null;
    }

    public Iterator pairsIterator() {
        return new PairsIterator();
    }

    public void assignY(Layer l, double _y) {
        if (layer == null) {
            layer = l;
            y = _y;
        } else {
            Layer common = layer.getCommonLayer(l);
            Layer newl = null;
            if (common != layer && common != l) {
                Layer a = layer.getPredParent(common);
                Layer b = l.getPredParent(common);

                Layer c = a;
                Layer first = null;

                while (c != null) {
                    if (c == b)
                        break;
                    c = c.topLayer;
                }

                if (c != null) {
                    first = c;
                    c = c.topLayer;
                    while (c != null) {
                        first = c;
                        c = c.topLayer;
                    }
                    c = a;
                    a = b;
                    b = a;
                } else {
                    c = b;
                    while (c != null) {
                        if (c == a)
                            break;
                        c = c.topLayer;
                    }
                    if (c != null) {
                        first = c;
                        c = c.topLayer;
                        while (c != null) {
                            first = c;
                            c = c.topLayer;
                        }
                    } else {
                        newl = common;
                    }
                }

                if (newl == null) {
                    newl = new Layer();
                    newl.parent = common;
                    newl.topLayer = a.topLayer;
                    newl.bottomLayer = b.bottomLayer;
                    newl.top = a.top;
                    newl.bottom = b.bottom;
                    Layer.LayerGroup oldg = common.getLayerGroup(first);
                    if (a == first) {
                        oldg.top = newl;
                        newl.bottomLayer = b.bottomLayer;
                        if (b.bottomLayer != null)
                            b.bottomLayer.topLayer = newl;
                        else
                            oldg.bottom = newl;
                    } else {
                        a.topLayer.bottomLayer = newl;
                        newl.topLayer = a.topLayer;
                        if (b.bottomLayer != null)
                            b.bottomLayer.topLayer = newl;
                        else
                            oldg.bottom = newl;
                        newl.bottomLayer = b.bottomLayer;
                    }
                    newl.addLayerGroup(a, b);
                }
            } else {
                newl = common;
            }

            double absythis = layer.bottom - (layer.bottom - layer.top) * y;
            double absy = l.bottom - (l.bottom - l.top) * _y;

            absy = (absythis * counter + absy) / (counter + 1);

            y = (newl.bottom - absy) / (newl.bottom - newl.top);
            layer = newl;
        }
        counter++;
    }

    public VisiblePair addPair(TermStatePair p, VisibleState s) {
        if (firstPair == null) {
            firstPair = new VisiblePair(p, s);
        } else {
            VisiblePair res;
            res = new VisiblePair(p, s);
            firstPair.previous = res;
            res.next = firstPair;
            firstPair = res;
        }
        nPairs++;
        return firstPair;
    }

    public State getState() {
        return state;
    }

    public String getString() {
        if (state != null)
            return state.getString();
        if (realPair != null)
            return realPair.getString();
        return "";
    }

    class VisiblePair {
        TermStatePair p;
        long label;
        VisibleState s;
        VisiblePair next;
        VisiblePair previous;
        int inverse;
        boolean termSelected;
        boolean selected;

        VisiblePair(TermStatePair _p, VisibleState _s) {
            p = _p;
            s = _s;
            next = null;
            previous = null;
            label = -1;
            inverse = 1;
            termSelected = false;
            selected = false;
        }
    }

    private class PairsIterator implements Iterator {
        VisiblePair p;

        PairsIterator() {
            p = nPairs == 0 ? null : firstPair;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return p != null;
        }

        public Object next() {
            VisiblePair t = p;
            p = p.next;
            return t;
        }
    }

}
