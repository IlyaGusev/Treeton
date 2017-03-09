/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class JButtonFrame extends JFrame {
    public String menuTitle;
    public String windowTitle;

    protected JButtonFrame(String name, ActionListener listener, String cmd) {
        super();

        menuTitle = "Button";
        windowTitle = "Button";
        JButton button = new JButton(name);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(button);

        setTitle(windowTitle);
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.setResizable(true);
        this.validate();

        button.setActionCommand(cmd);
        button.addActionListener(listener);
        setBounds(50, 50, 100, 50);
    }

    public static void showJButton(String name, ActionListener listener, String cmd) {
        JButtonFrame userFrame = new JButtonFrame(name, listener, cmd);
        int state = userFrame.getExtendedState();
        if ((state & Frame.ICONIFIED) != 0) {
            userFrame.setExtendedState(state & ~Frame.ICONIFIED);
        }
        userFrame.setVisible(true);
        userFrame.requestFocus();
    }

}
