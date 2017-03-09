/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.ResourceSignature;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.Map;

public class AttrFrame extends JFrame {
    public String parentMenuName;
    public String menuTitle;
    public String windowTitle;
    public boolean ready;
    Map<String, Object> map;
    private AttrFrame instance;
    private AttrPanel panel;

    protected AttrFrame(ResourceSignature storage, Map<String, Object> m) throws ContextException {
        super();

        ready = false;
        instance = this;
        parentMenuName = "Tools";
        menuTitle = "Attributes Editor";
        windowTitle = "Attributes Editor";
        panel = new AttrPanel();
        panel.init(storage, m);
        init();
    }

    public void paint(Graphics g) {
        if (!ready) {
            ready = true;
            repaint();
        } else {
            super.paint(g);
        }
    }

    protected void init() {
        setTitle(windowTitle);

        this.setResizable(true);

        setContentPane(panel);

        this.setSize(800, 300);
        this.validate();
    }

    public void actionPerformed(ActionEvent e) {

    }

//  public static AttrFrame getInstance(AttrStorage storage) {
//    return new AttrFrame(storage);
//  }

//  public static void showAttrFrame(AttrStorage storage) {
//    AttrFrame userFrame = getInstance(storage);
//    int state = userFrame.getExtendedState();
//    if ((state & Frame.ICONIFIED) != 0) {
//      userFrame.setExtendedState(state & ~Frame.ICONIFIED);
//    }
//    userFrame.setVisible(true);
//    userFrame.requestFocus();
//  }

    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            map = ((AttrPanel) panel).getData();
            System.exit(0);

        }
        super.processWindowEvent(e);
    }

}