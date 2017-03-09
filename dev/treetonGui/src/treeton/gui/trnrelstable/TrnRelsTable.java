/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnrelstable;


import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import treeton.core.Treenotation;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnRelationType;
import treeton.core.util.ObjectPair;
import treeton.core.util.trnconvert.TreenotationConverter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.NavigableMap;

public class TrnRelsTable {
    JXTable table = new JXTable();
    JScrollPane pane = new JScrollPane(table);
    private TrnRelsTableModel<TrnRelationType> model;

    public TrnRelsTable() {
        table.setColumnControlVisible(true);

        table.getTableHeader().setDefaultRenderer(new RotatedTableCellHeaderRenderer());
        table.setDefaultRenderer(String.class, new DefaultTableCellHeaderRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setToolTipText(value == null ? "" : value.toString());
                return this;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                TrnRelationType rel = (TrnRelationType) value;
                try {
                    if (rel == null)
                        setText("");
                    else if (rel.isRoot())
                        setText("ROOT");
                    else
                        setText(rel.getName());
                } catch (TreetonModelException e) {
                    e.printStackTrace();
                    setText("!");
                }
            }
        });
        table.setSortable(false);
//        table.getColumnModel().setColumnSelectionAllowed(true);
        table.addHighlighter(new ColorHighlighter(new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                return adapter.column == adapter.row + 1;
            }
        }, Color.GRAY, Color.WHITE, Color.GRAY, Color.WHITE));
        table.addHighlighter(new ColorHighlighter(new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                return adapter.column == adapter.row + 1 && model.isRoot(adapter.row);
            }
        }, Color.RED, Color.WHITE, Color.RED, Color.WHITE));

    }

    public TrnRelsTable(Treenotation trn) throws Exception {
        this();
        setTreenotation(trn);
    }

    public Component getComponent() {
        return pane;
    }

    public void setTreenotation(Treenotation trn) throws Exception {
        NavigableMap<Treenotation, ObjectPair<Treenotation, TrnRelationType>> map = TreenotationConverter.getTrnParentMap(trn, false);
        table.setModel(model = new TrnRelsTableModel<TrnRelationType>(map, trn));
    }
}
