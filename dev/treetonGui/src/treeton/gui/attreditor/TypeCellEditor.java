/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import javax.swing.*;
import java.awt.*;

public class TypeCellEditor extends DefaultCellEditor {
    JComboBox component = new JComboBox();

    public TypeCellEditor(JComboBox jc) {
        super(jc);
        component = jc;
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int rowIndex, int vColIndex) {
        // 'value' is value contained in the cell located at (rowIndex, vColIndex)

        if (isSelected) {
            // cell (and perhaps other cells) are selected
        }
        // Configure the component with the specified value
        component.setSelectedItem((String) value);
        return component;
    }

}
