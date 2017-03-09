/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class JTreeFrame extends JFrame {
    public String menuTitle;
    public String windowTitle;
    private JTreeFrame instance;
    private JScrollPane jsc;

    protected JTreeFrame(TreeModel model, TreeCellRenderer renderer) {
        super();

        instance = this;
        menuTitle = "Tree Viewer";
        windowTitle = "View Tree";
        jsc = new JScrollPane();
        jsc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JTree tree = new JTree();
        tree.setModel(model);
        tree.setEditable(false);
        tree.setCellRenderer(renderer);
        jsc.getViewport().add(tree);

        init();
    }

    public static JTreeFrame getInstance(TreeModel model, TreeCellRenderer renderer) {
        return new JTreeFrame(model, renderer);
    }

    public static void showJTreeFrame(TreeModel model, TreeCellRenderer renderer) {
        JTreeFrame userFrame = getInstance(model, renderer);
        int state = userFrame.getExtendedState();
        if ((state & Frame.ICONIFIED) != 0) {
            userFrame.setExtendedState(state & ~Frame.ICONIFIED);
        }
        userFrame.setVisible(true);
        userFrame.requestFocus();
    }

    protected void init() {
        setTitle(windowTitle);

        this.setDefaultCloseOperation(HIDE_ON_CLOSE);

        this.setResizable(true);

        setContentPane(jsc);

        Dimension dim = jsc.getPreferredSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        instance.setBounds(((int) screenSize.getWidth() - (int) Math.min(screenSize.getWidth() * 0.7, dim.getWidth() + 8)) / 2, (int) (screenSize.getHeight() * 0.15), (int) Math.min(screenSize.getWidth() * 0.7, dim.getWidth() + 8), (int) Math.min(screenSize.getHeight() * 0.7, dim.getHeight() + 34));
        this.validate();
    }

    public void actionPerformed(ActionEvent e) {

    }


}
