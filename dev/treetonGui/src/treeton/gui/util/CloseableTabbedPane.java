/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.gui.GuiResources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.MalformedURLException;

public class CloseableTabbedPane extends JTabbedPane
        implements MouseListener {

    TabCloseListener closeListener;
    private Image imgUp = null;
    private Image imgDn = null;
    private Point imgLoc;
    private boolean isPressed;

    public CloseableTabbedPane() {
        super();
        isPressed = false;
        try {
            init();
        } catch (MalformedURLException e) {
            ExceptionDialog.showExceptionDialog(null, e);
        }
    }

    public CloseableTabbedPane(int tabPlacement) {
        super(tabPlacement);
        try {
            init();
        } catch (MalformedURLException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
    }

    public void setTabCloseListener(TabCloseListener listener) {
        this.closeListener = listener;
    }

    public void clearTabCloseListener() {
        this.closeListener = null;
    }

    private void init() throws MalformedURLException {
        imgUp = GuiResources.getImageIcon("tabcl-up.gif").getImage();
        imgDn = GuiResources.getImageIcon("tabcl-dn.gif").getImage();
        addMouseListener(this);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getTabCount() > 0) {
            Image im = isPressed ? imgDn : imgUp;
            imgLoc = new Point((int) getSize().getWidth(), 1);
            g.drawImage(im, imgLoc.x, imgLoc.y, this);
        } else {
            imgLoc = null;
        }
    }

    public Dimension getSize() {
        if (getTabCount() < 1) {
            return super.getSize();
        }
        Dimension orig = super.getSize();
        return new Dimension((int) orig.getWidth() - imgUp.getWidth(this) - 1,
                (int) orig.getHeight());
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseReleased(MouseEvent me) {
        if (imgLoc != null && isPressed) {
            int iw = imgUp.getWidth(this);
            int ih = imgUp.getHeight(this);
            isPressed = false;
            repaint(imgLoc.x, imgLoc.y, iw, ih);
        }
    }

    public void mousePressed(MouseEvent me) {
        if (imgLoc != null) {
            int x = me.getX();
            int y = me.getY();
            int iw = imgUp.getWidth(this);
            int ih = imgUp.getHeight(this);
            if (x >= imgLoc.x && x <= imgLoc.x + iw
                    && y >= imgLoc.y && y <= imgLoc.y + ih) {
                isPressed = true;
                repaint(imgLoc.x, imgLoc.y, iw, ih);
            }
        }
    }

    public void mouseClicked(MouseEvent me) {
        if (imgLoc != null) {
            int x = me.getX();
            int y = me.getY();
            if (x >= imgLoc.x && x <= imgLoc.x + imgUp.getWidth(this)
                    && y >= imgLoc.y && y <= imgLoc.y + imgUp.getHeight(this)) {
                if (closeListener != null) {
                    closeListener.tabCloseRequest(getSelectedComponent());
                } else {
                    remove(getSelectedIndex());
                }
            }
        }
    }
}
