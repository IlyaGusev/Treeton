/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class ValueCellRenderer extends DefaultTableCellRenderer {
    boolean Value;

    public Component getTableCellRendererComponent
            (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component cell = super.getTableCellRendererComponent
                (table, value, isSelected, hasFocus, row, column);
        if (column != 1) {
            Value = false;
            if (!isSelected) {
                cell.setForeground(Color.white);
            } else {
                cell.setForeground(table.getSelectionBackground());
            }
        } else Value = true;
        return cell;
    }

    public void paint(Graphics _g) {
        Graphics2D g = (Graphics2D) _g;
        super.paint(g);
        if (!Value) {
            Color old = g.getColor();
            g.setColor(Color.lightGray);
            Dimension sz = getSize();
            for (int i = 0; i < sz.height; i += 3) {
                g.drawLine(0, sz.height - i, sz.height - i, 0);
            }
            for (int i = 0; i < sz.width; i += 3) {
                g.drawLine(i, sz.height, sz.height + i, 0);
            }
            g.setColor(old);
        }
    }

}
