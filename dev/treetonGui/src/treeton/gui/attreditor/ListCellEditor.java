/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import javax.swing.*;
import java.awt.*;

public class ListCellEditor extends DefaultCellEditor {
    public ListCellEditor(AttrTable tab) {
        super(new JFormattedTextField());
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value, boolean isSelected,
                                                 int row, int column) {

        JFormattedTextField ftf =
                (JFormattedTextField) super.getTableCellEditorComponent(
                        table, value, isSelected, row, column);
        ftf.setValue(value);
        return ftf;
    }
}

