/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.fsmdrawer;

import java.util.ArrayList;
import java.util.Iterator;

public class Layer {
    private static long curLabel = 131;
    public double top;
    public double bottom;
    public Layer parent;
    public Layer topLayer;
    public Layer bottomLayer;
    public ArrayList layerGroupsList;
    private long label;

    public Layer() {
        top = 0;
        bottom = 0;
        parent = null;
        topLayer = null;
        bottomLayer = null;
        layerGroupsList = new ArrayList();
    }

    public void shiftTop(double delta) {
        top -= delta;
        if (topLayer != null) {
            topLayer.moveUp(delta, true);
        } else {
            if (parent != null) {
                if (top < parent.top) {
                    parent.shiftTop(parent.top - top);
                }
            }
        }
    }

    public void shiftBottom(double delta) {
        bottom += delta;
        if (bottomLayer != null) {
            bottomLayer.moveDown(delta, true);
        } else {
            if (parent != null) {
                if (bottom > parent.bottom) {
                    parent.shiftBottom(bottom - parent.bottom);
                }
            }
        }
    }

    public void moveUp(double delta, boolean recursive) {
        top -= delta;
        bottom -= delta;
        Iterator it = layerGroupsList.iterator();
        while (it.hasNext()) {
            LayerGroup gr = (LayerGroup) it.next();
            gr.bottom.moveUp(delta, false);
        }
        if (topLayer != null) {
            topLayer.moveUp(delta, recursive);
        } else {
            if (recursive && parent != null) {
                if (top < parent.top) {
                    parent.shiftTop(parent.top - top);
                }
            }
        }
    }

    public void moveDown(double delta, boolean recursive) {
        top += delta;
        bottom += delta;
        Iterator it = layerGroupsList.iterator();
        while (it.hasNext()) {
            LayerGroup gr = (LayerGroup) it.next();
            gr.top.moveDown(delta, false);
        }
        if (bottomLayer != null) {
            bottomLayer.moveDown(delta, recursive);
        } else {
            if (recursive && parent != null) {
                if (bottom > parent.bottom) {
                    parent.shiftBottom(bottom - parent.bottom);
                }
            }
        }
    }

    public LayerGroup createLayerGroup(double _center, double oneLayerHeight, int nLayers, VisibleState s) {
        double center = bottom - (bottom - top) * _center;
    /* if (center < top || center > bottom)
       throw new IllegalArgumentException();*/

        double halfheight = 0;

        Iterator pairs = s.pairsIterator();
        for (int i = 0; i < nLayers; i++) {
            VisibleState.VisiblePair p = (VisibleState.VisiblePair) pairs.next();
            halfheight += oneLayerHeight * (1 + (p.s.depth - s.depth - 1) * 1);
        }
        halfheight /= 2;

        if (center + halfheight > bottom)
            shiftBottom(center + halfheight - bottom);
        if (center - halfheight < top)
            shiftTop(top - center + halfheight);
        LayerGroup res = new LayerGroup();

        double curtop = center - halfheight;
        Layer prev = null;
        pairs = s.pairsIterator();
        for (int i = 0; i < nLayers; i++) {
            Layer n = new Layer();
            VisibleState.VisiblePair p = (VisibleState.VisiblePair) pairs.next();
            n.top = curtop;
            n.bottom = curtop + oneLayerHeight * (1 + (p.s.depth - s.depth - 1) * 1);
            n.parent = this;
            if (prev == null) {
                res.top = n;
            } else {
                n.topLayer = prev;
                prev.bottomLayer = n;
            }
            curtop = n.bottom;
            prev = n;
        }
        res.bottom = prev;
        layerGroupsList.add(res);
        return res;
    }

    public Layer getCommonLayer(Layer otherLayer) {
        Layer a = this, b = otherLayer;
        long label = curLabel++;
        while (a != null) {
            a.label = label;
            a = a.parent;
        }

        while (b != null) {
            if (b.label == label)
                return b;
            b = b.parent;
        }
        return null;
    }

    public Layer getPredParent(Layer parent) {
        Layer a = this;
        while (a.parent != parent) {
            a = a.parent;
        }
        return a;
    }

    public void addLayerGroup(Layer a, Layer b) {
        LayerGroup ng = new LayerGroup();
        ng.top = a;
        ng.bottom = b;
        a.topLayer = null;
        b.bottomLayer = null;
        while (a != null) {
            a.parent = this;
            a = a.bottomLayer;
        }
        layerGroupsList.add(ng);
    }

    public LayerGroup getLayerGroup(Layer first) {
        Iterator it = layerGroupsList.iterator();
        while (it.hasNext()) {
            LayerGroup g = (LayerGroup) it.next();
            if (g.top == first)
                return g;
        }
        return null;
    }

    public class LayerGroup {
        public Layer top;
        public Layer bottom;
    }
}
