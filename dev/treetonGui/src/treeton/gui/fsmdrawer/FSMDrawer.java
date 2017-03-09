/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.fsmdrawer;

import treeton.core.fsm.FSM;
import treeton.core.fsm.State;
import treeton.core.fsm.Term;
import treeton.core.fsm.TermStatePair;
import treeton.core.util.BlockStack;
import treeton.core.util.MutableInteger;
import treeton.core.util.RBTreeMap;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class FSMDrawer {
    public ArrayList states;
    public RBTreeMap statesIndex;
    public int prefWidth, prefHeight;
    public double stateRadius = 16;
    public double deltaX = 80;
    public double deltaY = 50;
    public double arrowLength = 10;
    public double arrowWidth = 6;
    public VisibleState.VisiblePair lastTermSelected;
    public VisibleState.VisiblePair lastSelected;
    Map<Class, StateDrawer> stateDrawers;
    Map<Class, TermDrawer> termDrawers;
    private VisibleState firstState;
    private Color finalStateColor = null;
    private Color firstStateColor = null;
    private Color stateInternalColor = null;
    private Color stateExternalColor = null;
    private Color backGroundColor = null;
    private Color linkColor = null;
    private Color selectedLinkColor = null;
    private int[] xArray;
    private int[][] grid;
    private int maxDepth;
    private int maxHeight;
    private long curLabel;
    private TermDrawer defaultTermDrawer = new DefaultTermDrawer();
    private StateDrawer defaultStateDrawer = new DefaultStateDrawer();
    //private double anglePercent = 2;
    private BlockStack stack1 = new BlockStack();
    private BlockStack stack2 = new BlockStack();


    public FSMDrawer(FSM fsm) {
        statesIndex = new RBTreeMap();
        firstState = new VisibleState(fsm.getStartState());
        statesIndex.put(new MutableInteger(firstState.state.getId()), firstState);

        stack1.push(firstState);

        MutableInteger searchInteger = new MutableInteger();
        while (!stack1.isEmpty()) {
            VisibleState s = (VisibleState) stack1.pop();
            Iterator it = s.state.pairsIterator();
            while (it.hasNext()) {
                TermStatePair p = (TermStatePair) it.next();
                State ns = p.getState();
                searchInteger.value = ns.getId();
                VisibleState nvs = null;
                if ((nvs = (VisibleState) statesIndex.get(searchInteger)) == null) {
                    nvs = new VisibleState(ns);
                    statesIndex.put(new MutableInteger(searchInteger.value), nvs);
                    stack1.push(nvs);
                }
                s.addPair(p, nvs);
            }
        }


        curLabel = 0;
        states = new ArrayList(statesIndex.size() * 2);
        Iterator it = statesIndex.values().iterator();

        while (it.hasNext()) {
            states.add(it.next());
        }

        prefWidth = 0;
        prefHeight = 0;


        firstStateColor = Color.green;
        finalStateColor = Color.red;
        stateExternalColor = Color.black;
        stateInternalColor = Color.white;
        backGroundColor = Color.lightGray;
        linkColor = Color.black;
        selectedLinkColor = Color.blue;
        lastTermSelected = null;
        processSelfLoops();
        long label = curLabel;
        markCyclePairs(curLabel++, firstState);
        int sz = states.size();
        int i;
        for (i = 0; i < sz; i++) {
            VisibleState s = (VisibleState) states.get(i);
            it = ((VisibleState) states.get(i)).pairsIterator();
            while (it.hasNext()) {
                VisibleState.VisiblePair p = (VisibleState.VisiblePair) it.next();
                if (p.label == label) {
                    if (p.next != null) {
                        p.next.previous = p.previous;
                    }
                    if (p.previous != null) {
                        p.previous.next = p.next;
                    } else {
                        s.firstPair = p.next;
                    }
                    s.nPairs--;

                    p.next = p.s.firstPair;
                    if (p.s.firstPair != null)
                        p.s.firstPair.previous = p;
                    p.s.firstPair = p;
                    p.s.nPairs++;
                    p.inverse = -1;
                    p.label = -1;
                    p.s = new VisibleState(null);

                    states.add(p.s);
                    p.s.loopState = true;
                    p.s.realPair = p.p;
                    p.s.realVisiblePair = p;
                    p.s.addPair(null, s);
                    p.s.firstPair.inverse = -1;
                }
            }
        }
        processMultiLinks();
        countX();
        xArray = new int[maxDepth + 1];
        maxHeight = 0;
        for (i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            sortPairsByProximity(s);
            xArray[s.depth]++;
            if (xArray[s.depth] > maxHeight)
                maxHeight = xArray[s.depth];
        }
        grid = new int[maxDepth + 1][maxHeight * 2];
        //randomizeY();
        Layer root = new Layer();
        root.top = -stateRadius;
        root.bottom = stateRadius;
        firstState.layer = root;
        firstState.y = 0.5;
        for (i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            s.counter = 0;
        }
        countY(firstState);

        adjustY();
    }

    private void fillTriangle(Graphics g, double x1, double y1, double x2, double y2, double width) {
        int xPoints[] = new int[3];
        int yPoints[] = new int[3];
        double d = width / 2;

        double dx = x2 - x1, dy = y2 - y1, k;
        k = d / (Math.sqrt(dx * dx + dy * dy));
        xPoints[1] = (int) Math.round((-dy * k + x1));
        yPoints[1] = (int) Math.round((dx * k + y1));
        xPoints[0] = (int) Math.round(x2);
        yPoints[0] = (int) Math.round(y2);
        xPoints[2] = (int) Math.round((dy * k + x1));
        yPoints[2] = (int) Math.round((-dx * k + y1));

        g.setColor(backGroundColor);
        g.fillPolygon(xPoints, yPoints, 3);
        g.setColor(linkColor);
        g.drawPolygon(xPoints, yPoints, 3);
    }

    private void drawArrowOnLine(Graphics g, double x1, double y1, double x2, double y2, int inverse) {
        if (inverse == 0) {
            double dx = x2 - x1, dy = y2 - y1, k;
            k = arrowLength / (Math.sqrt(dx * dx + dy * dy));
            double nx = x1 + dx / 2 + (dx * k) / 4, ny = y1 + dy / 2 + (dy * k) / 4;
            fillTriangle(g, nx, ny, nx + dx * k, ny + dy * k, arrowWidth);
            nx = x1 + dx / 2 - (dx * k) * 1.25;
            ny = y1 + dy / 2 - (dy * k) * 1.25;
            fillTriangle(g, nx + dx * k, ny + dy * k, nx, ny, arrowWidth);
        } else {
            double dx = x2 - x1, dy = y2 - y1, k;
            k = arrowLength / (Math.sqrt(dx * dx + dy * dy));
            double nx = x1 + dx / 2 - (dx * k) / 2, ny = y1 + dy / 2 - (dy * k) / 2;

            if (inverse == 1)
                fillTriangle(g, nx, ny, nx + dx * k, ny + dy * k, arrowWidth);
            else
                fillTriangle(g, nx + dx * k, ny + dy * k, nx, ny, arrowWidth);
        }
    }

    public void draw(double x, double y, Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(backGroundColor);
        g.fillRect((int) x, (int) y, prefWidth, prefHeight);
        g.setColor(linkColor);
        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            Iterator it = ((VisibleState) states.get(i)).pairsIterator();
            Shape sh = g.getClip();
            while (it.hasNext()) {
                VisibleState.VisiblePair p = (VisibleState.VisiblePair) it.next();


                Rectangle2D extRect;
                if (p.s.nInputs <= 1 || p.s.selfLoopState) {
                    extRect = new Rectangle(
                            (int) Math.round(x + Math.max(Math.min(s.x + stateRadius, p.s.x - stateRadius), 0)),
                            (int) Math.round(y + p.s.y - 1 - deltaY / 2),
                            (int) Math.round(Math.min(Math.max(s.x + stateRadius, p.s.x - stateRadius), p.s.x) - Math.max(Math.min(s.x + stateRadius, p.s.x - stateRadius), 0)),
                            (int) Math.round(deltaY / 2)
                    );
                } else {
                    extRect = new Rectangle(
                            (int) Math.round(x + Math.max(Math.min(s.x + stateRadius, p.s.x - stateRadius), 0)),
                            (int) Math.round(y + s.y - 1 - deltaY / 2),
                            (int) Math.round(Math.min(Math.max(s.x + stateRadius, p.s.x - stateRadius), p.s.x) - Math.max(Math.min(s.x + stateRadius, p.s.x - stateRadius), 0)),
                            (int) Math.round(deltaY / 2)
                    );
                }

                if (p.s.nInputs <= 1 || p.s.selfLoopState) {
                    if (p.p != null) {
                        if (p.termSelected) {
                            g.setColor(selectedLinkColor);
                            Rectangle2D rect = getTermDrawer(p).getPreferredBounds(g, extRect);
                            g.drawRect((int) Math.round(rect.getMinX()), (int) Math.round(rect.getMinY()), (int) Math.round(rect.getWidth()), (int) Math.round(rect.getHeight()));
                        }
                        getTermDrawer(p).drawSelf(g, extRect);
                        if (p.termSelected)
                            g.setColor(linkColor);
                    }
                    Color t = null;
                    if (p.selected || s.realVisiblePair != null && s.realVisiblePair.selected) {
                        t = linkColor;
                        linkColor = selectedLinkColor;
                        g.setColor(linkColor);
                    }
                    g.drawLine((int) Math.round(x + s.x), (int) Math.round(y + s.y), (int) Math.round(x + Math.min(s.x + stateRadius, p.s.x)), (int) Math.round(y + p.s.y));
                    if (!p.s.selfLoopState)
                        drawArrowOnLine(g, x + s.x, y + s.y, x + Math.min(s.x + stateRadius, p.s.x), y + p.s.y, p.inverse);

                    g.drawLine((int) Math.round(x + Math.min(s.x + stateRadius, p.s.x)), (int) Math.round(y + p.s.y), (int) Math.round(x + p.s.x), (int) Math.round(y + p.s.y));
                    drawArrowOnLine(g, x + Math.min(s.x + stateRadius, p.s.x), y + p.s.y, x + p.s.x, y + p.s.y, p.inverse);
                    if (t != null) {
                        linkColor = t;
                        g.setColor(linkColor);
                    }
                } else {
                    if (p.p != null) {
                        if (p.termSelected) {
                            g.setColor(selectedLinkColor);
                            Rectangle2D rect = getTermDrawer(p).getPreferredBounds(g, extRect);
                            g.drawRect((int) Math.round(rect.getMinX()), (int) Math.round(rect.getMinY()), (int) Math.round(rect.getWidth()), (int) Math.round(rect.getHeight()));
                        }
                        getTermDrawer(p).drawSelf(g, extRect);
                        if (p.termSelected)
                            g.setColor(linkColor);
                    }
                    Color t = null;
                    if (p.selected || s.realVisiblePair != null && s.realVisiblePair.selected) {
                        t = linkColor;
                        linkColor = selectedLinkColor;
                        g.setColor(linkColor);
                    }
                    g.drawLine((int) Math.round(x + s.x), (int) Math.round(y + s.y), (int) Math.round(x + Math.max(s.x, p.s.x - stateRadius)), (int) Math.round(y + s.y));
                    drawArrowOnLine(g, x + s.x, y + s.y, x + Math.max(s.x, p.s.x - stateRadius), y + s.y, p.inverse);
                    g.drawLine((int) Math.round(x + Math.max(s.x, p.s.x - stateRadius)), (int) Math.round(y + s.y), (int) Math.round(x + p.s.x), (int) Math.round(y + p.s.y));
                    drawArrowOnLine(g, x + Math.max(s.x, p.s.x - stateRadius), y + s.y, x + p.s.x, y + p.s.y, p.inverse);
                    if (t != null) {
                        linkColor = t;
                        g.setColor(linkColor);
                    }
                }
                //g.drawLine((int)Math.round(x+s.x),(int)Math.round(y+s.y),(int)Math.round(x+p.s.x),(int)Math.round(y+p.s.y));


        /*g.setClip(sh);
        if (s.y <= p.s.y) {
          int w = (int)(p.s.x - s.x);
          int h = (int)(p.s.y - s.y);
          g.clipRect((int)(x + s.x),(int)(y + s.y), w+1, h+1);
          int a = Math.min((int)(w*anglePercent),(int)(h*anglePercent));

          if(s.nPairs > 1) {
            //g.drawRoundRect((int)(x + s.x),(int)(y + s.y - h),w*2,h*2,a,a);
            g.drawOval((int)(x + s.x),(int)(y + s.y - h),w*2,h*2);
          } else {
            //g.drawRoundRect((int)(x + s.x - w),(int)(y + s.y),w*2,h*2,a,a);
            g.drawOval((int)(x + s.x - w),(int)(y + s.y),w*2,h*2);
          }
        } else {
          int w = (int)(p.s.x - s.x);
          int h = (int)(s.y - p.s.y);
          g.clipRect((int)(x + s.x),(int)(y+p.s.y),w+1,h+1);
          int a = Math.min((int)(w*anglePercent),(int)(h*anglePercent));
          if(s.nPairs > 1) {
            //g.drawRoundRect((int)(x + s.x),(int)(y+p.s.y),w*2,h*2,a,a);
            g.drawOval((int)(x + s.x),(int)(y+p.s.y),w*2,h*2);
          } else {
            //g.drawRoundRect((int)(x + s.x - w),(int)(y+p.s.y-h),w*2,h*2,a,a);
            g.drawOval((int)(x + s.x - w),(int)(y+p.s.y-h),w*2,h*2);
          }
        }*/
            }
            g.setClip(sh);
        }

        //ArrayList a = new ArrayList();

        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            if (!s.selfLoopState && !s.loopState && !s.multiLinkState) {
                if (s.isFinal) {
                    g.setColor(finalStateColor);
                } else if (s == firstState) {
                    g.setColor(firstStateColor);
                } else {
                    g.setColor(stateInternalColor);
                }
                g.fillOval((int) Math.round(x + s.x - stateRadius), (int) Math.round(y + s.y - stateRadius), (int) Math.round(stateRadius * 2), (int) Math.round(stateRadius * 2));
                g.setColor(stateExternalColor);
                g.drawOval((int) Math.round(x + s.x - stateRadius), (int) Math.round(y + s.y - stateRadius), (int) Math.round(stateRadius * 2), (int) Math.round(stateRadius * 2));
                if (s.isFinal)
                    g.drawOval((int) Math.round(x + s.x - Math.round(stateRadius * 0.7)), (int) Math.round(y + s.y - Math.round(stateRadius * 0.7)), (int) Math.round(stateRadius * 1.4), (int) Math.round(stateRadius * 1.4));
                g.setColor(Color.black);
                Rectangle2D rect = new Rectangle((int) Math.round(x + s.x - stateRadius), (int) Math.round(y + s.y - stateRadius), (int) Math.round(stateRadius * 2), (int) Math.round(stateRadius * 2));
                if (s.state != null)
                    getStateDrawer(s).drawSelf(g, rect);
            } else if (!s.loopState && !s.multiLinkState) {
                Color t = null;
                g.setColor(backGroundColor);
                g.fillOval((int) Math.round(x + s.x - stateRadius * 0.5), (int) Math.round(y + s.y - stateRadius * 0.5), (int) Math.round(stateRadius), (int) Math.round(stateRadius));
                if (s.realVisiblePair != null && s.realVisiblePair.selected) {
                    t = linkColor;
                    linkColor = selectedLinkColor;
                    g.setColor(linkColor);
                } else {
                    g.setColor(stateExternalColor);
                }
                g.drawOval((int) Math.round(x + s.x - stateRadius * 0.5), (int) Math.round(y + s.y - stateRadius * 0.5), (int) Math.round(stateRadius), (int) Math.round(stateRadius));
                fillTriangle(g, x + s.x + stateRadius * 0.5, y + s.y - stateRadius * 0.2, x + s.x + stateRadius * 0.5, y + s.y + stateRadius * 0.2, stateRadius * 0.4);
                if (t != null) {
                    linkColor = t;
                    g.setColor(linkColor);
                }
            } else {
                Color t = null;
                if (s.realVisiblePair != null && s.realVisiblePair.selected) {
                    t = linkColor;
                    linkColor = selectedLinkColor;
                }
                fillTriangle(g, x + s.x, y + s.y - stateRadius * 0.1, x + s.x, y + s.y - stateRadius * 0.3, stateRadius * 0.3);
                fillTriangle(g, x + s.x, y + s.y + stateRadius * 0.1, x + s.x, y + s.y + stateRadius * 0.3, stateRadius * 0.3);
                if (t != null) {
                    linkColor = t;
                }
            }


/*      g.setColor(Color.black);
      a.clear();
      Iterator it = s.pairsIterator();
      while(it.hasNext()) {
        VisibleState.VisiblePair p = (VisibleState.VisiblePair) it.next();
        a.add(p.s);
      }

      FontMetrics fm  = g.getFontMetrics();
      Rectangle2D rect = fm.getMaxCharBounds(g);
      double addx=-stateRadius/2,addy=0;
      for(int p=0;p<a.size();p++) {
        g.drawString(Integer.toString(((VisibleState)a.get(p)).state.getIndex()),(int)(x + s.x + rect.getWidth()*p),(int)(x + s.y - stateRadius / 2));
        for(int q=p+1;q<a.size();q++) {
          g.drawString(Integer.toString(getProximity((VisibleState)a.get(p),(VisibleState)a.get(q))),(int)(x + s.x + addx),(int)(x + s.y + addy));
          addx+=rect.getWidth();
        }
        addy+=rect.getHeight();
        addx=-stateRadius/2;
      }*/

        }
    }

    private StateDrawer getStateDrawer(VisibleState s) {
        StateDrawer drawer = stateDrawers == null ? null : stateDrawers.get(s.getState().getClass());
        if (drawer == null) {
            drawer = defaultStateDrawer;
        }
        drawer.setState(s.getState());
        return drawer;
    }

    private TermDrawer getTermDrawer(VisibleState.VisiblePair p) {
        TermDrawer drawer = termDrawers == null ? null : termDrawers.get(p.p.getTerm().getClass());
        if (drawer == null) {
            drawer = defaultTermDrawer;
        }
        drawer.setTerm(p.p.getTerm());
        return drawer;
    }

    public Dimension getPrefferedSize() {
        if (prefWidth != 0)
            return new Dimension(prefWidth, prefHeight);
        int maxx = 0, maxy = 0;
        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            if (s.x > maxx)
                maxx = (int) Math.round(s.x);
            if (s.y > maxy)
                maxy = (int) Math.round(s.y);
        }
        prefWidth = (int) Math.round(maxx + stateRadius * 2);
        prefHeight = (int) Math.round(maxy + stateRadius * 2);
        return new Dimension(prefWidth, prefHeight);
    }

    private void countDepth(int d, VisibleState s) {
        if (d > s.depth) {
            s.depth = d;
        }
        if (s.counter == s.nInputs) {
            Iterator it = s.pairsIterator();
            while (it.hasNext()) {
                VisibleState.VisiblePair p = (VisibleState.VisiblePair) it.next();
                p.s.counter++;
                countDepth(s.depth + 1, p.s);
            }
        }
    }

    private boolean pathExists(VisibleState from, VisibleState to, long oldLabel) {
        long label = curLabel++;
        from.label2 = label;

        if (from == to)
            return true;


        long pos = stack1.getPosition();
        stack1.push(from.pairsIterator());

        while (pos != stack1.getPosition()) {
            Iterator it = (Iterator) stack1.pop();
            while (it.hasNext()) {
                VisibleState.VisiblePair p = (VisibleState.VisiblePair) it.next();
                if (p.label == oldLabel)
                    continue;
                if (p.s == to) {
                    while (pos != stack1.getPosition()) {
                        stack1.pop();
                    }
                    return true;
                }
                if (p.s.label2 != label) {
                    p.s.label2 = label;
                    stack1.push(p.s.pairsIterator());
                }
            }
        }
        return false;
    }

    private void markCyclePairs(long label, VisibleState s) {
        BlockStack curStack = null;
        BlockStack otherStack = null;

        s.label = label;
        stack1.push(s);

        curStack = stack1;
        otherStack = stack2;


        while (!curStack.isEmpty()) {
            while (!curStack.isEmpty()) {
                s = (VisibleState) curStack.pop();

                Iterator it = s.pairsIterator();
                while (it.hasNext()) {
                    VisibleState.VisiblePair p = (VisibleState.VisiblePair) it.next();
                    if (p.s.label == label && pathExists(p.s, s, label)) {
                        p.label = label;
                    } else {
                        otherStack.push(p.s);
                    }
                }
            }
            while (!otherStack.isEmpty()) {
                s = (VisibleState) otherStack.pop();
                if (s.label != label) {
                    s.label = label;
                    curStack.push(s);
                }
            }
        }
    }


    private void countX() {
        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            Iterator it = s.pairsIterator();
            while (it.hasNext()) {
                VisibleState.VisiblePair p = (VisibleState.VisiblePair) it.next();
                p.s.nInputs++;
            }
            s.counter = 0;
        }
        countDepth(0, firstState);
        maxDepth = 0;
        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            if (s.depth > maxDepth)
                maxDepth = s.depth;
            s.x = 3 * stateRadius + s.depth * deltaX;
        }
    }

    public void randomizeY() {
        for (int i = 0; i < maxDepth + 1; i++) {
            for (int j = 0; j < maxHeight * 2; j++) {
                grid[i][j] = 0;
            }
        }
        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            sortPairsByProximity(s);
            int nFree = 0;
            for (int j = 0; j < maxHeight * 2; j++) {
                if (grid[s.depth][j] == 0)
                    nFree++;
            }
            int r = (int) Math.round(Math.random() * (nFree - 1));
            int j;
            for (j = 0; j < maxHeight * 2; j++) {
                if (grid[s.depth][j] == 0)
                    r--;
                if (r < 0)
                    break;
            }
            grid[s.depth][j] = 1;

            s.y = -(maxHeight * deltaY) + deltaY * j;
        }
    }

    public void adjustY() {
        double miny = 0;
        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            s.y = s.layer.bottom - (s.layer.bottom - s.layer.top) * s.y;
            s.layer = null;
            if (s.y < miny)
                miny = s.y;
        }

        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            s.y -= miny;
            s.y += 3 * stateRadius;
        }
    }

    private int labelDescendants(VisibleState s, long label, long matchWith) {
        int n;
        if (s.label == matchWith)
            n = 1;
        else
            n = 0;
        s.label = label;

        Iterator it = s.pairsIterator();
        while (it.hasNext()) {
            VisibleState.VisiblePair p = (VisibleState.VisiblePair) it.next();
            if (p.s.label != label) {
                n += labelDescendants(p.s, label, matchWith);
            }
        }
        return n;
    }

    private void countY(VisibleState s) {

        if (s.nPairs == 0)
            return;

        if (s.nPairs == 1) {
            s.firstPair.s.assignY(s.layer, s.y);
            if (s.firstPair.s.counter == s.firstPair.s.nInputs)
                countY(s.firstPair.s);
        } else {
            Layer.LayerGroup group = s.layer.createLayerGroup(s.y, deltaY, s.nPairs, s);
            Layer curLayer = group.top;
            VisibleState.VisiblePair p = s.firstPair;
            while (p != null) {
                p.s.assignY(curLayer, 0.5);
                curLayer = curLayer.bottomLayer;
                p = p.next;
            }
            p = s.firstPair;
            while (p != null) {
                if (p.s.counter == p.s.nInputs)
                    countY(p.s);
                p = p.next;
            }
        }
    }


    private int getProximity(VisibleState s1, VisibleState s2) {
        labelDescendants(s1, curLabel++, 0);
        int res = labelDescendants(s2, curLabel, curLabel - 1);
        curLabel++;
        return res;
    }

    private void sortPairsByProximity(VisibleState s) {
        int n = s.nPairs;
        if (n <= 1)
            return;
        int[][] matrix = new int[n][n];
        VisibleState.VisiblePair[] vparr = new VisibleState.VisiblePair[n];
        VisibleState.VisiblePair p = s.firstPair;
        VisibleState.VisiblePair q;
        for (int i = 0; i < n; i++) {
            matrix[i][i] = -1;
            vparr[i] = p;
            q = p.next;
            for (int j = i + 1; j < n; j++) {
                matrix[i][j] = getProximity(p.s, q.s);
                matrix[j][i] = getProximity(p.s, q.s);
                q = q.next;
            }
            p = p.next;
        }
        int max = -1, imax = -1, jmax = -1;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (matrix[i][j] > max) {
                    max = matrix[i][j];
                    imax = i;
                    jmax = j;
                }
            }
        }
        p = vparr[imax];
        q = vparr[jmax];
        p.next = q;
        p.previous = null;
        q.previous = p;
        q.next = null;
        vparr[imax] = null;
        vparr[jmax] = null;
        int c = 2;
        while (c < n) {
            max = -1;
            int nimax = -1;
            for (int i = 0; i < n; i++) {
                if (i != imax && vparr[i] != null) {
                    if (matrix[imax][i] > max) {
                        max = matrix[imax][i];
                        nimax = i;
                    }
                }
            }
            if (max != -1) {
                vparr[nimax].next = p;
                p.previous = vparr[nimax];
                p = p.previous;
                p.previous = null;
                s.firstPair = p;
                imax = nimax;
                vparr[imax] = null;
                c++;
            }

            max = -1;
            int njmax = -1;
            for (int j = 0; j < n; j++) {
                if (j != jmax && vparr[j] != null) {
                    if (matrix[jmax][j] > max) {
                        max = matrix[jmax][j];
                        njmax = j;
                    }
                }
            }
            if (max != -1) {
                vparr[njmax].previous = q;
                q.next = vparr[njmax];
                q = q.next;
                q.next = null;
                jmax = njmax;
                vparr[jmax] = null;
                c++;
            }
        }
    }

    private void processSelfLoops() {
        int sz = states.size();
        for (int i = 0; i < sz; i++) {
            VisibleState s = (VisibleState) states.get(i);
            Iterator it = s.pairsIterator();
            while (it.hasNext()) {
                VisibleState.VisiblePair p = (VisibleState.VisiblePair) it.next();
                if (p.s == s) {
                    VisibleState ns = new VisibleState(null);
                    states.add(ns);
                    ns.selfLoopState = true;
                    ns.realVisiblePair = p;
                    ns.realPair = p.p;
                    p.s = ns;
                    p.inverse = 0;
                }
            }
        }
    }

    private void processMultiLinks() {
        int sz = states.size();
        for (int i = 0; i < sz; i++) {
            VisibleState s = (VisibleState) states.get(i);
            if (s.selfLoopState || s.loopState)
                continue;
            VisibleState.VisiblePair p = s.firstPair;
            while (p != null) {
                VisibleState ns;
                if (!p.s.selfLoopState && !p.s.loopState) {
                    ns = new VisibleState(null);
                    states.add(ns);
                    ns.multiLinkState = true;
                    ns.realPair = p.p;
                    ns.realVisiblePair = p;
                    ns.addPair(null, p.s);
                    p.s = ns;
                }

/*        VisibleState.VisiblePair q = p.next;
        boolean located=false;

        while(q!=null) {
          if (q.s == p.s) {
            located=true;
            ns = new VisibleState();
            states.add(ns);
            ns.multiLinkState = true;
            ns.addPair(null,q.s);
            q.s = ns;
          }
          q=q.next;
        }
        if(located) {
          ns = new VisibleState();
          states.add(ns);
          ns.multiLinkState = true;
          ns.addPair(null,p.s);
          p.s = ns;
        }*/
                p = p.next;
            }
        }
    }

    public VisibleState getState(double x, double y, Point pressed) {
        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            if (x + pressed.x > s.x - stateRadius && x + pressed.x < s.x + stateRadius &&
                    y + pressed.y > s.y - stateRadius && y + pressed.y < s.y + stateRadius) {
                if (s.realPair != null) {
                    if (lastSelected != null)
                        lastSelected.selected = false;
                    if (lastTermSelected != null)
                        lastTermSelected.termSelected = false;
                    lastTermSelected = null;
                    s.realVisiblePair.selected = true;
                    lastSelected = s.realVisiblePair;
                }
                return s;
            }
        }
        if (lastTermSelected != null)
            lastTermSelected.termSelected = false;
        lastTermSelected = null;
        if (lastSelected != null)
            lastSelected.selected = false;
        lastSelected = null;

        return null;
    }

    public Term getTerm(Graphics g, double x, double y, Point p) {
        for (int i = 0; i < states.size(); i++) {
            VisibleState s = (VisibleState) states.get(i);
            Iterator it = s.pairsIterator();
            while (it.hasNext()) {
                VisibleState.VisiblePair pair = (VisibleState.VisiblePair) it.next();
                if (pair.p == null)
                    continue;
                Rectangle2D extRect;
                if (pair.s.nInputs <= 1 || pair.s.selfLoopState) {
                    extRect = new Rectangle(
                            (int) Math.round(x + Math.max(Math.min(s.x + stateRadius, pair.s.x - stateRadius), 0)),
                            (int) Math.round(y + pair.s.y - 1 - deltaY / 2),
                            (int) Math.round(Math.min(Math.max(s.x + stateRadius, pair.s.x - stateRadius), pair.s.x) - Math.max(Math.min(s.x + stateRadius, pair.s.x - stateRadius), 0)),
                            (int) Math.round(deltaY / 2)
                    );
                } else {
                    extRect = new Rectangle(
                            (int) Math.round(x + Math.max(Math.min(s.x + stateRadius, pair.s.x - stateRadius), 0)),
                            (int) Math.round(y + s.y - 1 - deltaY / 2),
                            (int) Math.round(Math.min(Math.max(s.x + stateRadius, pair.s.x - stateRadius), pair.s.x) - Math.max(Math.min(s.x + stateRadius, pair.s.x - stateRadius), 0)),
                            (int) Math.round(deltaY / 2)
                    );
                }
                Rectangle2D rect = getTermDrawer(pair).getPreferredBounds(g, extRect);

                if (rect.contains(x + p.x, y + p.y)) {
                    if (lastSelected != null)
                        lastSelected.selected = false;
                    lastSelected = null;
                    if (lastTermSelected != null)
                        lastTermSelected.termSelected = false;
                    pair.termSelected = true;
                    lastTermSelected = pair;
                    return pair.p.getTerm();
                }
            }
        }
        if (lastTermSelected != null)
            lastTermSelected.termSelected = false;
        lastTermSelected = null;
        if (lastSelected != null)
            lastSelected.selected = false;
        lastSelected = null;
        return null;
    }
}

