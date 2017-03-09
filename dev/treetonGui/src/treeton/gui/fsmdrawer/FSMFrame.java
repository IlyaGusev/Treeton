/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.fsmdrawer;

import treeton.core.fsm.FSM;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowListener;
import java.util.Map;

public class FSMFrame extends JFrame {
    public String menuTitle;
    public String windowTitle;
    private FSMFrame instance;
    private FSMViewPanel panel;

    protected FSMFrame(FSM fsm) {
        super();

        instance = this;
        menuTitle = "FSM Viewer";
        windowTitle = "View FSM";
        panel = new FSMViewPanel(fsm);
        init();
    }

    public static FSMFrame getInstance(FSM fsm) {
        return new FSMFrame(fsm);
    }

    public static void showFSMFrame(FSM fsm, WindowListener listener, Map<Class, StateDrawer> stateDrawers) {
        FSMFrame userFrame = getInstance(fsm);
        if (stateDrawers != null)
            userFrame.panel.setStateDrawers(stateDrawers);
        int state = userFrame.getExtendedState();
        if ((state & Frame.ICONIFIED) != 0) {
            userFrame.setExtendedState(state & ~Frame.ICONIFIED);
        }
        if (listener != null)
            userFrame.addWindowListener(listener);
        userFrame.setVisible(true);
        userFrame.requestFocus();
    }

    public static void showFSMFrame(FSM fsm) {
        showFSMFrame(fsm, null, null);
    }

    protected void init() {
        setTitle(windowTitle);

        this.setDefaultCloseOperation(HIDE_ON_CLOSE);

        this.setResizable(true);

        setContentPane(panel);

        Dimension dim = panel.getPreferredSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        instance.setBounds(((int) screenSize.getWidth() - (int) Math.min(screenSize.getWidth() * 0.7, dim.getWidth() + 8)) / 2, (int) (screenSize.getHeight() * 0.15), (int) Math.min(screenSize.getWidth() * 0.7, dim.getWidth() + 8), (int) Math.min(screenSize.getHeight() * 0.7, dim.getHeight() + 34));
        this.validate();
    }

    public void actionPerformed(ActionEvent e) {

    }
}
