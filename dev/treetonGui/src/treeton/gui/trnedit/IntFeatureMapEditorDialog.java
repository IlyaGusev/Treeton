/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnedit;

import treeton.core.IntFeatureMapImpl;
import treeton.core.Treenotation;
import treeton.core.model.dclimpl.TrnTypeDclImpl;
import treeton.gui.util.SwingUtils;

import javax.swing.*;
import java.awt.*;

public class IntFeatureMapEditorDialog {
    private IntFeatureMapEditorPanel intFeatureMapEditorPanel;

    private String titleText;


    public IntFeatureMapEditorDialog(String titleText,
                                     final Treenotation trn, IntFeatureMapEditorListener listener) {
        this.titleText = titleText;
        intFeatureMapEditorPanel = new IntFeatureMapEditorPanel();
        intFeatureMapEditorPanel.setListener(listener);
        TrnTypeDclImpl tp = (TrnTypeDclImpl) trn.getType();
        intFeatureMapEditorPanel.setTree(tp.getHierarchy());
        intFeatureMapEditorPanel.attach(trn.getType(), trn, new IntFeatureMapImpl());
    }

    public void showDialog(Frame parentFrame) {
        JDialog dialog = new JDialog(parentFrame, titleText, true);
        dialog.setContentPane(intFeatureMapEditorPanel);
        dialog.pack();
        dialog.setLocation(SwingUtils.getWindowPositionFromScreenSize(dialog.getSize()));
        dialog.setVisible(true);
    }
}
