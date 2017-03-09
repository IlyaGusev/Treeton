/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import treeton.gui.GuiResources;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class ResourceCellRenderer extends DefaultTableCellRenderer {
    public Component getTableCellRendererComponent
            (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component cell = super.getTableCellRendererComponent
                (table, value, isSelected, hasFocus, row, column);
        if (column != 1) {
            AttrTable tab = (AttrTable) table;
            if (!tab.isCellEditable(row, column)) {
                Icon ic = GuiResources.iconLock12;
                setIcon(ic);
            }
        }
        return cell;
    }
}
