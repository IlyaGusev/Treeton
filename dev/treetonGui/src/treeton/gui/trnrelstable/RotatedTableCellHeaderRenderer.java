/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnrelstable;

import org.jdesktop.swingx.JXLabel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class RotatedTableCellHeaderRenderer extends JXLabel implements TableCellRenderer {
    private boolean horizontalTextPositionSet;

    public RotatedTableCellHeaderRenderer() {
        setTextRotation(JXLabel.VERTICAL_LEFT);
        setHorizontalAlignment(JLabel.CENTER);
    }

    public static SortOrder getColumnSortOrder(JTable table, int column) {
        SortOrder rv = null;
        if (table.getRowSorter() == null) {
            return rv;
        }
        java.util.List<? extends RowSorter.SortKey> sortKeys =
                table.getRowSorter().getSortKeys();
        if (sortKeys.size() > 0 && sortKeys.get(0).getColumn() ==
                table.convertColumnIndexToModel(column)) {
            rv = sortKeys.get(0).getSortOrder();
        }
        return rv;
    }

    public void setHorizontalTextPosition(int textPosition) {
        horizontalTextPositionSet = true;
        super.setHorizontalTextPosition(textPosition);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        Icon sortIcon = null;

        boolean isPaintingForPrint = false;

        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                Color fgColor = null;
                Color bgColor = null;
                if (hasFocus) {
                    fgColor = UIManager.getColor("TableHeader.focusCellForeground");
                    bgColor = UIManager.getColor("TableHeader.focusCellBackground");
                }
                if (fgColor == null) {
                    fgColor = header.getForeground();
                }
                if (bgColor == null) {
                    bgColor = header.getBackground();
                }
                setForeground(fgColor);
                setBackground(bgColor);

                setFont(header.getFont());

                isPaintingForPrint = header.isPaintingForPrint();
            }

            if (!isPaintingForPrint && table.getRowSorter() != null) {
                if (!horizontalTextPositionSet) {
                    // There is a row sorter, and the developer hasn't
                    // set a text position, change to leading.
                    setHorizontalTextPosition(JLabel.LEADING);
                }
                SortOrder sortOrder = getColumnSortOrder(table, column);
                if (sortOrder != null) {
                    switch (sortOrder) {
                        case ASCENDING:
                            sortIcon = UIManager.getIcon(
                                    "Table.ascendingSortIcon");
                            break;
                        case DESCENDING:
                            sortIcon = UIManager.getIcon(
                                    "Table.descendingSortIcon");
                            break;
                        case UNSORTED:
                            sortIcon = UIManager.getIcon(
                                    "Table.naturalSortIcon");
                            break;
                    }
                }
            }
        }

        String s = value == null ? "" : value.toString();
        setText(s);
        setToolTipText(s);
        setIcon(sortIcon);

        Border border = null;
        if (hasFocus) {
            border = UIManager.getBorder("TableHeader.focusCellBorder");
        }
        if (border == null) {
            border = UIManager.getBorder("TableHeader.cellBorder");
        }
        border = BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY);
        setBorder(border);

        return this;
    }
}

