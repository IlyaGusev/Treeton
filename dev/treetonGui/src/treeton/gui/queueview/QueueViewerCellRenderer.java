/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.queueview;

import treeton.gui.GuiResources;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class QueueViewerCellRenderer extends DefaultTreeCellRenderer {
    WeightController weightController;
    LabelGenerator labelGen;

    public void setLabelGenerator(LabelGenerator labelGen) {
        this.labelGen = labelGen;
    }

    public void setWeightController(WeightController weightController) {
        this.weightController = weightController;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof QueueTreeModel) {
            renderer.setBackgroundNonSelectionColor(Color.white);
            renderer.setIcon(null);
            return renderer;
        }

        Color c = weightController == null ? Color.white : countColor(weightController.getWeight(((QueueTreeModel.ElementWrapper) value).obj));
        renderer.setBackgroundNonSelectionColor(c);
        renderer.setIcon(GuiResources.iconNone);

        if (labelGen != null)
            renderer.setText(labelGen.getLabel(((QueueTreeModel.ElementWrapper) value).obj));

        return renderer;
    }

    private Color countColor(double weight) {
        weight = weight > weightController.getMaxWeight() ? weightController.getMaxWeight() : weight;
        double k = (weight - weightController.getMinWeight()) / (weightController.getMaxWeight() - weightController.getMinWeight());
        return new Color((int) (255 * k), (int) (255 * (1 - k)), 0, 50);
    }

}
