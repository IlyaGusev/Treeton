/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnview;

import treeton.core.IntFeatureMap;
import treeton.core.IntFeatureMapStatic;
import treeton.core.TString;
import treeton.core.Treenotation;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.gui.labelgen.TrnLabelGenerator;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class TreenotationLabel {
    private final static String dots = "..";
    private final static String dot = ".";
    private char[] label;
    private int length;

    public TreenotationLabel(int capacity) {
        label = new char[capacity];
        length = 0;
    }

    public int getLength() {
        return length;
    }

    public void fillIn(Treenotation trn) {
        if (trn == null) {
            if (1 > label.length) {
                label = new char[1];
            }
            label[0] = 'ε';
            length = 1;
            return;
        }
        String label = "";
        fillIn(label);
    }

    public void fillInCaption(Treenotation trn) {
        if (trn == null) {
            if (1 > label.length) {
                label = new char[1];
            }
            label[0] = 'ε';
            length = 1;
            return;
        }
        String label = null;
        try {
            label = trn.getId() + ": " + trn.getType().getName();
        } catch (TreetonModelException e) {
            label = trn.getId() + ": " + "Wrong type";
        }
        fillIn(label);
    }

    public void fillIn(Treenotation trn, TrnLabelGenerator labelGenerator) {
        if (trn == null) {
            if (1 > label.length) {
                label = new char[1];
            }
            label[0] = 'ε';
            length = 1;
            return;
        }
        String label = labelGenerator.generateLabel(trn);
        fillIn(label);
    }

    public void fillInCaption(Treenotation trn, TrnLabelGenerator labelGenerator) {
        if (trn == null) {
            if (1 > label.length) {
                label = new char[1];
            }
            label[0] = 'ε';
            length = 1;
            return;
        }
        String label = labelGenerator.generateCaption(trn);
        fillIn(label);
    }

    public void fillIn(String s) {
        if (s.length() > label.length) {
            label = new char[s.length()];
            s.getChars(0, s.length(), label, 0);
            length = label.length;
        } else {
            s.getChars(0, s.length(), label, 0);
            length = s.length();
        }
    }

    public void fillIn(TString s) {
        if (s.length() > label.length) {
            label = new char[s.length()];
            s.getChars(0, s.length(), label, 0);
            length = label.length;
        } else {
            s.getChars(0, s.length(), label, 0);
            length = s.length();
        }
    }

    public void fillIn(IntFeatureMap fm, TrnType tp) {
        String type = null;
        try {
            type = tp.getName();
        } catch (TreetonModelException e) {
            type = "Problem with model!!!";
        }
        if (type.length() > label.length) {
            label = new char[type.length()];
            type.getChars(0, type.length(), label, 0);
            length = label.length;
        } else {
            type.getChars(0, type.length(), label, 0);
            length = type.length();
        }
    }

    public void fillIn(IntFeatureMapStatic fm, TrnType tp) {
        String type = null;
        try {
            type = tp.getName();
        } catch (TreetonModelException e) {
            type = "Problem with model!!!";
        }
        if (type.length() > label.length) {
            label = new char[type.length()];
            type.getChars(0, type.length(), label, 0);
            length = label.length;
        } else {
            type.getChars(0, type.length(), label, 0);
            length = type.length();
        }
    }

    public void draw(Graphics g, int startx, int endx, int y) {
        FontMetrics fm = g.getFontMetrics();

        Rectangle2D rect = fm.getStringBounds(label, 0, length, g);
        int l = endx - startx;

        if (l < rect.getWidth()) {
            Rectangle2D rectDots = fm.getStringBounds(dots, g);
            if (l < rectDots.getWidth()) {
                rectDots = fm.getStringBounds(dot, g);
                if (l >= rectDots.getWidth()) {
                    g.drawString(dot, startx + (int) ((l - rectDots.getWidth()) / 2), y);
                }
            } else {
                l -= rectDots.getWidth();
                int i = 1;
                for (; i < length; i++) {
                    rect = fm.getStringBounds(label, 0, i, g);
                    if (l <= rect.getWidth()) {
                        if (i != 1) {
                            char c1 = label[i - 1], c2 = label[i];
                            label[i - 1] = '.';
                            label[i] = '.';
                            g.drawChars(label, 0, i + 1, startx, y);
                            label[i - 1] = c1;
                            label[i] = c2;
                        } else {
                            g.drawString(dots, startx + (l / 2), y);
                        }
                        return;
                    }
                }
                g.drawChars(label, 0, i, startx, y);
            }
        } else {
            g.drawChars(label, 0, length, startx + (int) ((l - rect.getWidth()) / 2), y);
        }
    }

    public void drawBackGround(Graphics g, int startx, int endx, int y, Color c, boolean border) {
        FontMetrics fm = g.getFontMetrics();
        int descent = fm.getDescent();

        Rectangle2D rect = fm.getStringBounds(label, 0, length, g);
        int l = endx - startx;

        if (l < rect.getWidth()) {
            Rectangle2D rectDots = fm.getStringBounds(dots, g);
            if (l < rectDots.getWidth()) {
                rectDots = fm.getStringBounds(dot, g);
                if (l >= rectDots.getWidth()) {
                    g.setColor(c);
                    g.fillRect(startx - 1 + (int) ((l - rectDots.getWidth()) / 2), (int) (y + descent - 1 - rectDots.getHeight()), (int) rectDots.getWidth() + 2, (int) rectDots.getHeight() + 3);
                    if (border) {
                        g.setColor(Color.black);
                        g.drawRect(startx - 1 + (int) ((l - rectDots.getWidth()) / 2), (int) (y + descent - 1 - rectDots.getHeight()), (int) rectDots.getWidth() + 2, (int) rectDots.getHeight() + 3);
                    }
                }
            } else {
                l -= rectDots.getWidth();
                int i = 1;
                for (; i < length; i++) {
                    rect = fm.getStringBounds(label, 0, i, g);
                    if (l <= rect.getWidth()) {
                        if (i != 1) {
                            char c1 = label[i - 1], c2 = label[i];
                            label[i - 1] = '.';
                            label[i] = '.';
                            rect = fm.getStringBounds(label, 0, i + 1, g);
                            g.setColor(c);
                            g.fillRect(startx - 1, (int) (y + descent - 1 - rect.getHeight()), (int) rect.getWidth() + 2, (int) rect.getHeight() + 3);
                            if (border) {
                                g.setColor(Color.black);
                                g.drawRect(startx - 1, (int) (y + descent - 1 - rect.getHeight()), (int) rect.getWidth() + 2, (int) rect.getHeight() + 3);
                            }
                            label[i - 1] = c1;
                            label[i] = c2;
                        } else {
                            g.setColor(c);
                            g.fillRect(startx - 1 + (l / 2), (int) (y + descent - 1 - rectDots.getHeight()), (int) rectDots.getWidth() + 2, (int) rectDots.getHeight() + 3);
                            if (border) {
                                g.setColor(Color.black);
                                g.drawRect(startx - 1 + (l / 2), (int) (y + descent - 1 - rectDots.getHeight()), (int) rectDots.getWidth() + 2, (int) rectDots.getHeight() + 3);
                            }
                        }
                        return;
                    }
                }
                rect = fm.getStringBounds(label, 0, i, g);
                g.setColor(c);
                g.fillRect(startx - 1, (int) (y + descent - 1 - rect.getHeight()), (int) rect.getWidth() + 2, (int) rect.getHeight() + 3);
                if (border) {
                    g.setColor(Color.black);
                    g.drawRect(startx - 1, (int) (y + descent - 1 - rect.getHeight()), (int) rect.getWidth() + 2, (int) rect.getHeight() + 3);
                }
            }
        } else {
            rect = fm.getStringBounds(label, 0, length, g);
            g.setColor(c);
            g.fillRect(startx - 2 + (int) ((l - rect.getWidth()) / 2), (int) (y + descent - 1 - rect.getHeight()), (int) rect.getWidth() + 4, (int) rect.getHeight() + 3);
            if (border) {
                g.setColor(Color.black);
                g.drawRect(startx - 2 + (int) ((l - rect.getWidth()) / 2), (int) (y + descent - 1 - rect.getHeight()), (int) rect.getWidth() + 4, (int) rect.getHeight() + 3);
            }
        }
    }


    public Rectangle2D getPreferredBounds(Graphics g, Font f) {
        FontMetrics fm = g.getFontMetrics(f);
        return fm.getStringBounds(label, 0, length, g);
    }
}
