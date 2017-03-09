/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.fsmdrawer;

import treeton.core.fsm.State;
import treeton.gui.trnview.TreenotationLabel;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class DefaultStateDrawer implements StateDrawer {
    public static Font font = new Font("Courier", 0, 12);
    public static Font numbersFont = new Font("Times New Roman", Font.BOLD, 20);
    TreenotationLabel label = new TreenotationLabel(500);

    State state;

    public void setState(State state) {
        this.state = state;
    }

    public void drawSelf(Graphics g, Rectangle2D rect) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        label.fillIn(state.getString());
        label.draw(g, (int) rect.getMinX(), (int) rect.getMaxX(), ((int) rect.getMinY() + fm.getHeight() / 2 + (int) rect.getMaxY()) / 2);
    }
}
