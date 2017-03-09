/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import java.awt.*;

public class Divider extends JComponent {

    public static final int HORZ = 0;
    public static final int VERT = 1;

    public static final int DEFAULT_WIDTH = 1;

    private int type;
    private int width;
    private Color color;

    public Divider() {
        this(HORZ, DEFAULT_WIDTH);
    }

    public Divider(int type) {
        this(type, DEFAULT_WIDTH);
    }

    public Divider(int type, int width) {
        this.type = type;
        this.width = width;
        UIDefaults ud = UIManager.getLookAndFeel().getDefaults();
        this.color = ud.getColor("Button.darkShadow");
        if (color == null) {
            color = Color.BLACK;
        }
        init();
    }

    public Divider(int type, int width, Color c) {
        this.type = type;
        this.width = width;
        this.color = c;
        init();
    }

    private void init() {
        setBackground(color);
        if (type == HORZ) {
            setMaximumSize(new Dimension(1000, width));
            setPreferredSize(new Dimension(1, width));
        } else {
            setMaximumSize(new Dimension(width, 1000));
            setPreferredSize(new Dimension(width, 1));
        }
        setMinimumSize(new Dimension(1, width));
    }

    public void paint(Graphics g) {
        Dimension d = getSize();
        g.setColor(color);
        g.fillRect(0, 0, d.width, d.height);
    }

}
