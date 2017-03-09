/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnrelstable;

import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

public class RotatedHeaderTest extends AbstractTableModel {

    public static void main(String[] args) {
        JXTable table = new JXTable(new RotatedHeaderTest());
        table.getTableHeader().setDefaultRenderer(new RotatedTableCellHeaderRenderer());

        JFrame f = new JFrame();
        f.setContentPane(new JScrollPane(table));

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);
    }

    @Override
    public String getColumnName(int column) {
        return "<html>Long column name<br> for column " + column + "</html>";
    }

    public int getRowCount() {
        return 10;
    }

    public int getColumnCount() {
        return 15;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return rowIndex + columnIndex;
    }
}
