/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.core.Fraction;
import treeton.core.TreenotationImpl;
import treeton.core.TreenotationStorage;
import treeton.core.model.TrnType;
import treeton.gui.labelgen.TrnLabelGenerator;
import treeton.gui.trnview.TreenotationViewPanelAbstract;
import treeton.gui.trnview.TrnManipulationEvent;
import treeton.gui.trnview.TrnManipulationListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;

public class TreenotationFrame extends JFrame implements TrnManipulationListener {
    public String parentMenuName;
    public String menuTitle;
    public String windowTitle;
    public TreenotationViewPanelAbstract panel;
    public boolean ready;
    private TreenotationFrame instance;
    private JSplitPane split;
    private JTextPane textPane;

    protected TreenotationFrame(TrnType[] tp, Fraction _from, Fraction _to, Fraction _focus, TreenotationStorage trns, String doc, int _curPos, HashMap<String, TrnLabelGenerator> labelGenerators) {
        super();

        ready = false;
        instance = this;
        parentMenuName = "Tools";
        menuTitle = "Viewer";
        windowTitle = "View";
        panel = TreenotationViewPanelAbstract.createTreenotationViewPanel(tp, _from, _to, _focus, trns, doc, _curPos, labelGenerators);
        init();
    }

    public static TreenotationFrame getInstance(TrnType[] type, Fraction from, Fraction to, Fraction focus, TreenotationStorage trns, String doc, int curPos, HashMap<String, TrnLabelGenerator> labelGenerators) {
        return new TreenotationFrame(type, from, to, focus, trns, doc, curPos, labelGenerators);
    }

    public static TreenotationFrame showTreenotationFrame(TrnType[] type, Fraction from, Fraction to, Fraction focus, TreenotationStorage trns, String doc, int curPos, HashMap<String, TrnLabelGenerator> labelGenerators) {
        TreenotationFrame userFrame = getInstance(type, from, to, focus, trns, doc, curPos, labelGenerators);
        int state = userFrame.getExtendedState();
        if ((state & Frame.ICONIFIED) != 0) {
            userFrame.setExtendedState(state & ~Frame.ICONIFIED);
        }
        userFrame.setVisible(true);
        userFrame.requestFocus();
        return userFrame;
    }

    public void paint(Graphics g) {
        if (!ready) {
            ready = true;
            split.setDividerLocation(0.8);
            repaint();
        } else {
            super.paint(g);
        }
    }

    public void reset(TrnType[] tp, Fraction _from, Fraction _to, Fraction _focus, TreenotationStorage trns, String doc, int _curPos) {
        panel.reset(tp, _from, _to, _focus, trns, doc, _curPos, null);
    }

    protected void init() {
        setTitle(windowTitle);

        this.setDefaultCloseOperation(HIDE_ON_CLOSE);

        this.setResizable(true);

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        split.add(panel, JSplitPane.TOP);
        split.add(textPane, JSplitPane.BOTTOM);
        split.setResizeWeight(1);

        setContentPane(split);
        panel.addTrnManipulationListener(this);

        instance.setBounds(100, 100, 300, 200);
        this.validate();
    }

    public void actionPerformed(ActionEvent e) {

    }

    public void trnClicked(TrnManipulationEvent e) {
        StringBuffer buf = new StringBuffer();
        while (e.nextSelectionElement()) {
            buf.append(e.getSelectedTrn().getHtmlString());
            buf.append("<br>Context: ");
            buf.append(e.getSelectedTrn().getContext());
            buf.append(", nView: ");
            buf.append(((TreenotationImpl) e.getSelectedTrn()).getNView());
            buf.append(", id: ");
            buf.append(e.getSelectedTrn().getId());
            buf.append(", uri: ");
            buf.append(e.getSelectedTrn().getUri());
        }

        textPane.setText(buf.toString());
    }

    public void validate() {
        panel.componentResized(null);
        super.validate();
    }

}
