/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class ComboCellEditor extends DefaultCellEditor {
    JComboBox jComboBox;
    Object currentValue;

    public ComboCellEditor() {
        this(new JComboBox());
    }

    public ComboCellEditor(JComboBox jComboBox) {
        super(jComboBox);
        this.jComboBox = jComboBox;
    }

    public void setEnumeration(Vector enumerationVector) {
        for (int i = 0; i < enumerationVector.size(); i++) {
            jComboBox.addItem(enumerationVector.elementAt(i));
        }
    }

    public void setValues(String[] values) {
        for (String value : values) {
            jComboBox.addItem(value);
        }
    }

    public void setValues(Integer[] values) {
        for (Integer value : values) {
            jComboBox.addItem(value);
        }
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        jComboBox.setSelectedItem(value.toString());
        return jComboBox;
    }

    public Object getCellEditorValue() {
        return jComboBox.getSelectedItem();
    }
}
