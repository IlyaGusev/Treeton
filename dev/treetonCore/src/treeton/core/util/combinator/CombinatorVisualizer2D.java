/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collection;
import java.util.LinkedList;

public class CombinatorVisualizer2D extends JPanel implements ComponentListener {
    private static final Color mainBackGroundColor = new Color(210, 210, 210);
    private static final Color borderColor = new Color(21, 21, 21);
    private static final Color mainFrontColor = new Color(0, 210, 0);
    private static final Color currentCombinationColor = new Color(210, 0, 0);
    Combinator combinator;
    private DrawingPane dp;
    private double normScale;
    private LinkedList<Point> points = new LinkedList<Point>();

    public CombinatorVisualizer2D(Combinator combinator, double normScale) {
        super();
        this.combinator = combinator;
        dp = null;
        this.normScale = normScale;

        init();
    }

    void init() {
        dp = new DrawingPane();
        setLayout(null);
        add(dp);
        dp.setBackground(mainBackGroundColor);
        dp.setToolTipText("");
        setOpaque(true);
        addComponentListener(this);
        this.validate();
    }

    public void componentResized(ComponentEvent e) {
        Dimension dim = getSize();
        if (dp.initialized) {
            dp.setBounds(0, 0, dim.width, dim.height);
        } else {
            dp.setBounds(0, 0, dim.width, dim.height);
        }
        repaint();
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    class DrawingPane extends JComponent {
        boolean initialized;

        DrawingPane() {
            super();
            initialized = false;
        }

        private void drawCombinationsFront(Graphics2D g) {
            Dimension sz = getSize();

            /*g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);*/
            int width = ((SortedEntries) combinator.getSortedEntriesList().get(0)).size();
            int height = ((SortedEntries) combinator.getSortedEntriesList().get(1)).size();

            g.setColor(borderColor);
            int shift = sz.height - height - 2;
            g.fillRect(0, shift, 1, height + 2);
            g.fillRect(0, shift, width + 2, 1);
            g.fillRect(0, shift + height + 1, width + 2, 1);
            g.fillRect(width + 1, shift, 1, height + 2);


            for (Object entries : combinator.getSortedEntriesList()) {
                ((SortedEntries) entries).refreshStraightIndexes();
            }


            int x;
            int y;

            Collection<Combination> combs = combinator.getCombinationsFront();
            if (combs != null) {
                for (Object entries : combinator.getSortedEntriesList()) {
                    ((SortedEntries) entries).refreshStraightIndexes();
                }

                for (Combination c : combs) {
                    x = c.getValue(0).getIndex();
                    y = c.getValue(1).getIndex();
                    g.setColor(mainFrontColor);
                    g.fillRect(x + 1, y + 1 + shift, 1, 1);
                }
                Combination c = combinator.getCurrentCombination();
                if (c != null) {
                    g.setColor(currentCombinationColor);
                    x = c.getValue(0).getIndex();
                    y = c.getValue(1).getIndex();

                    g.fillRect(x + 1, y + shift, 1, 3);
                    g.fillRect(x, y + 1 + shift, 3, 1);

                    if (combinator instanceof CombinatorWithFront) {
                        points.addLast(new Point(((CombinatorWithFront) combinator).getNumberOfGeneratedCombinations(), (int) (((CombinationImpl) c).getNorm() * normScale)));
                        while (points.size() > sz.width) {
                            points.removeFirst();
                        }
                        int i = 0;
                        for (Point p : points) {
                            g.fillRect(i++, shift - p.y, 1, 1);
                        }
                    }
                }
            }


        }

        public void paint(Graphics _g) {
            Graphics2D g = (Graphics2D) _g;
            if (!initialized) {
                initialized = true;
                componentResized(null);
                repaint();
            } else {
                drawCombinationsFront(g);
            }
        }
    }
}
