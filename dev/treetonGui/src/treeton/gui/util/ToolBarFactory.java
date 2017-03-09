/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.gui.GuiResources;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;

public class ToolBarFactory {
    public void addToolBarSeperator(JToolBar tb, int w, int h) {
        JLabel sepr = new JLabel();
        sepr.setBorder(BorderFactory.createEmptyBorder());
        GUIUtil.setAllSizes(sepr, w, h);
        tb.add(sepr);
    }

    public JButton addToolBarButton(JToolBar tb, String imgFile,
                                    String actionCommand, String hint, ActionListener listener) throws MalformedURLException {
        ImageIcon img = GuiResources.getImageIcon(imgFile);
        JButton btn = new JButton(img);
        btn.setActionCommand(actionCommand);
        btn.setToolTipText(hint);
        btn.addActionListener(listener);
        tb.add(btn);
        return btn;
    }

    public JToggleButton addToolBarToggleButton(JToolBar tb, String imgFile,
                                                String actionCommand, String hint, ActionListener listener) throws MalformedURLException {
        ImageIcon img = GuiResources.getImageIcon(imgFile);
        JToggleButton btn = new JToggleButton(img);
        btn.setActionCommand(actionCommand);
        btn.setToolTipText(hint);
        btn.setFocusable(false);
        btn.addActionListener(listener);
        tb.add(btn);
        return btn;
    }
}
