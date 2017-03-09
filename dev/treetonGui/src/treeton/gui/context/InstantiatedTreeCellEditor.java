/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import treeton.core.config.context.resources.ResourceChain;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class InstantiatedTreeCellEditor extends DefaultTreeCellEditor {
    public InstantiatedTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
        super(tree, renderer);
    }

    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
        return super.getTreeCellEditorComponent(tree, ((ResourceChain) value).getName(), isSelected, expanded, leaf, row);
    }


}
