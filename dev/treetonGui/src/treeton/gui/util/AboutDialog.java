/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.gui.GuiResources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AboutDialog extends JWindow
        implements MouseListener, KeyListener, FocusListener {

    private static Image splashImage;

    public AboutDialog(Window owner) {
        super(owner);
        if (splashImage == null) {
            ImageIcon ii;
            ii = GuiResources.getImageIcon("logo01.gif");
            splashImage = ii.getImage();
        }
        setFocusable(true);
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
        addKeyListener(this);
        addMouseListener(this);
        addFocusListener(this);
        super.show();
        requestFocus();
    }

    public void mouseClicked(MouseEvent e) {
        setVisible(false);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
            setVisible(false);
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
            setVisible(false);
        }
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        setVisible(false);
    }
}
