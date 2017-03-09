/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import com.sun.glf.goodies.WaveStroke;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class ValidatableTextField extends JTextField implements Validatable {
    Stroke waveStroke =
            new WaveStroke(1, 5, 3);
    boolean invalid = false;


    public ValidatableTextField() {
        super();
    }

    public void paint(Graphics _g) {
        Graphics2D g = (Graphics2D) _g;
        super.paint(g);
        if (invalid) {
            FontMetrics fm = getFontMetrics(getFont());
            Rectangle2D rect = fm.getStringBounds(getText(), g);
            Color old = g.getColor();
            g.setColor(Color.red);
            Stroke oldStroke = g.getStroke();
            g.setStroke(waveStroke);
            Dimension sz = getSize();
            Insets sets = getInsets();
            if (rect.getWidth() < 16) {
                rect.setRect(rect.getMinX(), rect.getMinY(), rect.getHeight(), 16);
            }
            g.drawLine((int) rect.getMinX() + sets.left, (int) sz.getHeight() - sets.bottom - 2, sets.left + (int) rect.getMaxX(), (int) sz.getHeight() - sets.bottom - 2);
            g.setStroke(oldStroke);
            g.setColor(old);
        }
    }

    public void makeInvalid() {
        invalid = true;
    }

    public void makeValid() {
        invalid = false;
    }
}

