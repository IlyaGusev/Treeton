/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.fsmdrawer;

import treeton.core.fsm.Term;
import treeton.gui.trnview.TreenotationLabel;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class DefaultTermDrawer implements TermDrawer {
    public static Font font = new Font("Courier", 0, 12);
    TreenotationLabel label = new TreenotationLabel(500);
    Term term;

    public void setTerm(Term term) {
        this.term = term;
        label.fillIn(term.getString());
    }

    public void drawSelf(Graphics g, Rectangle2D rect) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        label.draw(g, (int) rect.getMinX(), (int) rect.getMaxX(), (int) rect.getMaxY() - fm.getDescent());
    }

    public Rectangle2D getPreferredBounds(Graphics g, Rectangle2D externalRect) {
        Rectangle2D prefRect = label.getPreferredBounds(g, font);
        Rectangle2D internalRect = new Rectangle((int) externalRect.getCenterX() - (int) (prefRect.getWidth() / 2), (int) (externalRect.getMaxY() - prefRect.getHeight()), (int) prefRect.getWidth(), (int) prefRect.getHeight());
        return internalRect.createIntersection(externalRect);
    }
}
