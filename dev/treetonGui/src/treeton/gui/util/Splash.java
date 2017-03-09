/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.gui.GuiResources;

import javax.swing.*;
import java.awt.*;

public class Splash extends JWindow {

    private static Image splashImage;

    public Splash(Window owner) {
        super(owner);
        if (splashImage == null) {
            ImageIcon ii;
            ii = GuiResources.getImageIcon("logo01.gif");
            splashImage = ii.getImage();
        }
    }

    public void paint(Graphics g) {
        g.drawImage(splashImage, 0, 0, this);
    }

    public void show() {
        int w = splashImage.getWidth(this);
        int h = splashImage.getHeight(this);
        setSize(w, h);

        Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();

        //Center the window
        setLocation((scrSize.width - w) / 2,
                (scrSize.height - h) / 2);
        super.show();
    }
}
