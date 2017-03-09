/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.fsmdrawer;

import treeton.core.fsm.State;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public interface StateDrawer {
    public void drawSelf(Graphics g, Rectangle2D rect);

    void setState(State state);
}
