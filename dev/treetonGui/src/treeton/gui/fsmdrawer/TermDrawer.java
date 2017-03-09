/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.fsmdrawer;

import treeton.core.fsm.Term;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public interface TermDrawer {
    public void drawSelf(Graphics g, Rectangle2D rect);

    public Rectangle2D getPreferredBounds(Graphics g, Rectangle2D externalRect);

    void setTerm(Term term);
}
