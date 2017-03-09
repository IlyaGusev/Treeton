/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnedit;

import treeton.core.BlackBoard;
import treeton.core.IntFeatureMap;
import treeton.core.IntFeatureMapImpl;
import treeton.core.TreetonFactory;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeUtils;
import treeton.core.model.dclimpl.ValidationTree;
import treeton.core.util.NumeratedObject;
import treeton.core.util.TString;
import treeton.core.util.nu;
import treeton.gui.GuiResources;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;

public class IntFeatureMapEditorPanel extends JPanel implements MouseListener, ActionListener {
    public static final String CMD_ATTRNEW = "TNW";
    public static final String BTN_ATTRNEW = "Создать атрибут";
    public static final String IMG_ATTRNEW = "insrow.gif";
    public static final String CMD_ATTRDEL = "TDL";
    public static final String BTN_ATTRDEL = "Удалить атрибут";
    public static final String IMG_ATTRDEL = "delrow.gif";
    protected static Color inducedColor = new Color(0, 200, 0, 50);
    TrnType tp;
    IntFeatureMapEditorListener listener;
    IntFeatureMap source;
    IntFeatureMap attrs;
    IntFeatureMap inducedAttrs;
    IntFeatureMap sourcePotential;
    HashSet featuresPotential;
    HashSet valuesPotential;
    Object currentPotentialFeature;
    Object currentPotentialValue;
    ValidationTree tree;
    HashSet editHash;
    CellComboEditor featureCmb;
    CellComboEditor valueCmb;
    CellButtonRenderer btnRnd;
    Model model;
    JTable table;
    JButton jbtnAttrNew;
    JButton jbtnAttrDel;
    ArrayList sorter = new ArrayList();
    UComp ucomp = new UComp();


    public IntFeatureMapEditorPanel() {
        init();
    }

    public void setEditHash(HashSet editHash) {
        this.editHash = editHash;
    }

    public void setTree(ValidationTree tree) {
        this.tree = tree;
    }

    private void init() {
        putClientProperty("JInternalFrame.isPalette", Boolean.FALSE);
        setLayout(new BorderLayout());


        table = new JTable(model = new Model()) {
            public boolean isCellEditable(int row, int col) {
                if (col == 0) {
                    int key = attrs.getKey(row);
                    if (key != -1) {
                        HashSet h = (HashSet) sourcePotential.get(key);
                        if (h.isEmpty()) {
                            return false;
                        }
                        return true;
                    } else {
                        if (currentPotentialFeature != null && currentPotentialValue != null) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                if (col == 2) {
                    if (!featuresPotential.isEmpty() && row == model.getRowCount() - 1 && currentPotentialFeature != null) {
                        return true;
                    }

                    int key = attrs.getKey(row);
                    if (key != -1) {
                        HashSet h = (HashSet) sourcePotential.get(key);
                        if (h.isEmpty()) {
                            return false;
                        }
                    }
                    return true;
                }

                if (col == 1) {
                    if (!featuresPotential.isEmpty() && row == model.getRowCount() - 1) {
                        return true;
                    }
                    return false;
                }
                return false;
            }
        };
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);

        featureCmb = new CellComboEditor();
        valueCmb = new CellComboEditor();
        //valueCmb.get
        btnRnd = new CellButtonRenderer();

        TableCellRenderer rnd = new CellRenderer();
        TableColumn aColumn;
        aColumn = table.getColumnModel().getColumn(0);
        aColumn.setCellRenderer(rnd);
        CellButtonRenderer brd = new CellButtonRenderer();
        brd.addActionListener(this);
        aColumn.setCellEditor(brd);

        aColumn = table.getColumnModel().getColumn(1);
        aColumn.setCellRenderer(rnd);
        DefaultCellEditor dcr = new DefaultCellEditor(featureCmb) {
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                Component res = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                if (column == 1 && featureCmb.currentRow != row) {
                    featureCmb.removeAllItems();

                    sorter.clear();
                    for (Object i : featuresPotential) {
                        try {
                            sorter.add(tp.getFeatureNameByIndex((Integer) i));
                        } catch (TreetonModelException e) {
                        }
                    }
                    Collections.sort(sorter, ucomp);
                    for (Object o : sorter) {
                        featureCmb.addItem(o);
                    }

                    if (featureCmb.getItemCount() > 0) {
                        featureCmb.setSelectedIndex(0);
                    }
                    featureCmb.currentRow = row;
                }
                return res;
            }
        };
        aColumn.setCellEditor(dcr);

        aColumn = table.getColumnModel().getColumn(2);
        aColumn.setCellRenderer(rnd);
        dcr = new DefaultCellEditor(valueCmb) {
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                Component res = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                if (column == 2 && valueCmb.currentRow != row) {
                    int key = attrs.getKey(row);
                    valueCmb.removeAllItems();
                    if (key != -1) {
                        HashSet h = (HashSet) sourcePotential.get(key);

                        sorter.clear();
                        for (Object o : h) {
                            if (o != nu.other) {
                                sorter.add(o);
                            }
                        }
                        Collections.sort(sorter, ucomp);

                        for (Object o : sorter) {
                            valueCmb.addItem(o);
                        }

                        if (h.contains(nu.other)) {
                            valueCmb.setEditable(true);
                        } else {
                            valueCmb.setEditable(false);
                        }
                        valueCmb.setSelectedItem(attrs.get(key));
                    } else if (valuesPotential != null) {
                        sorter.clear();
                        for (Object o : valuesPotential) {
                            if (o != nu.other) {
                                sorter.add(o);
                            }
                        }
                        Collections.sort(sorter, ucomp);

                        for (Object o : sorter) {
                            valueCmb.addItem(o);
                        }


                        if (valuesPotential.contains(nu.other)) {
                            valueCmb.setEditable(true);
                        } else {
                            valueCmb.setEditable(false);
                        }
                        if (currentPotentialValue != null) {
                            valueCmb.setSelectedItem(currentPotentialValue);
                        } else if (valueCmb.getItemCount() > 0 && !valueCmb.isEditable()) {
                            valueCmb.setSelectedIndex(0);
                        } else {
                            valueCmb.setSelectedItem(null);
                        }
                    }
                    valueCmb.currentRow = row;
                }
                return res;
            }
        };
        aColumn.setCellEditor(dcr);

        add(table, BorderLayout.CENTER);


        FontMetrics fm = getFontMetrics((Font) Toolkit.getDefaultToolkit().getDesktopProperty("win.frame.captionFont"));

        Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((scrSize.width) / 4,
                (scrSize.height) / 2);

    }

    public void detach() {
        source = null;
        sourcePotential = null;
        featuresPotential = null;
        valuesPotential = null;
        currentPotentialFeature = null;
        currentPotentialValue = null;
        attrs = null;
        inducedAttrs = null;
        tp = null;
    }

    public void refresh() {
        if (attrs == null) {
            table.setPreferredSize(new Dimension(0, 0));
            setPreferredSize(new Dimension(0, 0));
        } else {
            Iterator it = attrs.numeratedObjectIterator();
            int maxWidth1 = 0, maxWidth2 = 0;
            FontMetrics fm = table.getFontMetrics(table.getFont());
            FontMetrics vfm = valueCmb.getFontMetrics(valueCmb.getFont());
            while (it.hasNext()) {
                NumeratedObject no = (NumeratedObject) it.next();
                int w;
                try {
                    w = fm.stringWidth(tp.getFeatureNameByIndex(no.n));
                    if (w > maxWidth1) {
                        maxWidth1 = w;
                    }
                } catch (TreetonModelException e) {
                }
                w = fm.stringWidth(no.o.toString());
                if (w > maxWidth2) {
                    maxWidth2 = w;
                }

                Iterator it1 = ((HashSet) sourcePotential.get(no.n)).iterator();
                while (it1.hasNext()) {
                    Object o = it1.next();
                    w = vfm.stringWidth(o.toString());
                    if (w + 20 > maxWidth2) {
                        maxWidth2 = w + 20;
                    }
                }
            }

            if (!featuresPotential.isEmpty()) {
                fm = featureCmb.getFontMetrics(featureCmb.getFont());
                it = featuresPotential.iterator();
                while (it.hasNext()) {
                    Integer i = (Integer) it.next();
                    int w;
                    try {
                        w = fm.stringWidth(tp.getFeatureNameByIndex(i));
                        if (w + 20 > maxWidth1) {
                            maxWidth1 = w + 20;
                        }
                    } catch (TreetonModelException e) {
                    }
                }
            }

            if (valuesPotential != null) {
                fm = valueCmb.getFontMetrics(valueCmb.getFont());
                it = valuesPotential.iterator();
                while (it.hasNext()) {
                    Object o = it.next();
                    int w;
                    w = fm.stringWidth(o.toString());
                    if (w + 20 > maxWidth2) {
                        maxWidth2 = w + 20;
                    }
                }
            }


            maxWidth1 += 4;
            maxWidth2 += 4;
            TableColumn aColumn = table.getColumnModel().getColumn(0);
            aColumn.setMaxWidth(18);
            aColumn.setWidth(18);

            maxWidth1 = Math.min(maxWidth1, 100);
            maxWidth2 = Math.min(maxWidth2, 400);

            aColumn = table.getColumnModel().getColumn(1);
            aColumn.setPreferredWidth(maxWidth1);
            aColumn.setWidth(maxWidth1);
            aColumn = table.getColumnModel().getColumn(2);
            aColumn.setPreferredWidth(maxWidth2);
            aColumn.setWidth(maxWidth2);
            table.setPreferredSize(null);
            setPreferredSize(null);

            int row = table.getSelectedRow();
            int col = table.getSelectedColumn();
            table.tableChanged(new TableModelEvent(table.getModel()));
            table.changeSelection(row, col, true, false);
        }
    }

    public void attach(TrnType tp, IntFeatureMap source, IntFeatureMap inducedAttrs) {
        this.source = source;
        BlackBoard board = TreetonFactory.newBlackBoard(100, false);
        source.fillBlackBoard(board);
        this.attrs = new IntFeatureMapImpl(board);
        tree.filterTemplate(attrs, editHash);
        this.tp = tp;
        sourcePotential = new IntFeatureMapImpl();
        refreshCombos();
        this.inducedAttrs = inducedAttrs;
        featureCmb.currentRow = -1;
        valueCmb.currentRow = -1;
    }

    void refreshCombos() {
        int n = attrs.size();
        for (int i = 0; i < n; i++) {
            int key = attrs.getKey(i);
            sourcePotential.put(key, tree.getPossibleValuesForFeature(attrs, key, editHash));
        }
        featuresPotential = tree.getPossibleFeatures(attrs, editHash);
        currentPotentialFeature = null;
        currentPotentialValue = null;
        valuesPotential = null;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void setListener(IntFeatureMapEditorListener listener) {
        this.listener = listener;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("BTN")) {
            CellButtonRenderer btn = (CellButtonRenderer) e.getSource();
            if (btn.currentRow != -1 && btn.getIcon() != GuiResources.iconExpand) {
                int key = attrs.getKey(btn.currentRow);
                if (key != -1) {
                    attrs.remove(key);
                }
            } else if (btn.currentRow != -1) {
                int feature = 0;
                try {
                    feature = tp.getFeatureIndex((String) currentPotentialFeature);
                } catch (TreetonModelException e1) {
                }
                try {
                    Object o = TrnTypeUtils.treatFeatureValue(tp, feature, currentPotentialValue);
                    attrs.put(feature, o);
                } catch (NumberFormatException ex) {
                    return;
                }
            }
            featureCmb.currentRow = -1;
            valueCmb.currentRow = -1;
            refreshCombos();
            listener.imapEdited(source, attrs, inducedAttrs);
            refresh();
            btn.stopCellEditing();
        }
    }

    class Model extends AbstractTableModel {
        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return attrs == null ? 0 : attrs.size() + (featuresPotential != null && !featuresPotential.isEmpty() ? 1 : 0);
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (attrs != null) {
                if (columnIndex == 0) {
                    if (featuresPotential.isEmpty() || rowIndex < getRowCount() - 1) {
                        return GuiResources.iconDeleteSyntaxRuleUsage;
                    } else {
                        return GuiResources.iconExpand;
                    }
                } else if (columnIndex == 1) {
                    if (!featuresPotential.isEmpty() && rowIndex == getRowCount() - 1) {
                        return currentPotentialFeature;
                    } else {
                        int key = attrs.getKey(rowIndex);
                        try {
                            return tp.getFeatureNameByIndex(key);
                        } catch (TreetonModelException e) {
                        }
                    }
                } else if (columnIndex == 2) {
                    if (!featuresPotential.isEmpty() && rowIndex == getRowCount() - 1) {
                        return currentPotentialValue;
                    } else {
                        int key = attrs.getKey(rowIndex);
                        return attrs.get(key);
                    }
                }
            }
            return null;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (attrs != null && aValue != null) {
                if (columnIndex == 0) {
                } else if (columnIndex == 1) {
                    if (!featuresPotential.isEmpty() && rowIndex == getRowCount() - 1) {
                        currentPotentialFeature = aValue;
                        currentPotentialValue = null;
                        try {
                            valuesPotential = tree.getPossibleValuesForFeature(attrs, tp.getFeatureIndex((String) currentPotentialFeature), editHash);
                        } catch (TreetonModelException e) {
                        }
                        valueCmb.currentRow = -1;
                    }
                } else {
                    if (!featuresPotential.isEmpty() && rowIndex == getRowCount() - 1) {
                        currentPotentialValue = aValue;
                    } else {
                        int key = attrs.getKey(rowIndex);
                        attrs.put(key, aValue);
                        if (inducedAttrs != null && inducedAttrs.contains(key)) {
                            inducedAttrs.remove(key);
                        }
                        featureCmb.currentRow = -1;
                        valueCmb.currentRow = -1;
                        refreshCombos();
                        listener.imapEdited(source, attrs, inducedAttrs);
                    }
                }
                refresh();
            }
        }
    }

    public class CellRenderer extends DefaultTableCellRenderer {
        public CellRenderer() {
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setIcon(null);
            if (column == 0) {
                Component res;
                if (isSelected) {
                    res = btnRnd.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                } else {
                    setIcon((Icon) value);
                    res = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                    setBgr(-1, res);
                }
                int key = attrs.getKey(row);
                if (key != -1) {
                    HashSet h = (HashSet) sourcePotential.get(key);
                    if (h.isEmpty()) {
                        res.setEnabled(false);
                    } else {
                        res.setEnabled(true);
                    }
                } else {
                    if (currentPotentialFeature != null && currentPotentialValue != null) {
                        res.setEnabled(true);
                    } else {
                        res.setEnabled(false);
                    }
                }
                return res;
            }

            if (!featuresPotential.isEmpty() && row == model.getRowCount() - 1) if (column == 1) {
                if (featureCmb.currentRow != row) {
                    featureCmb.removeAllItems();
                    sorter.clear();
                    for (Object i : featuresPotential) {
                        try {
                            sorter.add(tp.getFeatureNameByIndex((Integer) i));
                        } catch (TreetonModelException e) {
                        }
                    }
                    Collections.sort(sorter, ucomp);
                    for (Object o : sorter) {
                        featureCmb.addItem(o);
                    }

                    featureCmb.setSelectedIndex(0);
                    featureCmb.currentRow = row;
                }
                return featureCmb.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (column == 2) {
                if (valuesPotential != null && isSelected) {
                    if (valueCmb.currentRow != row) {
                        valueCmb.removeAllItems();
                        sorter.clear();
                        for (Object o : valuesPotential) {
                            if (o != nu.other) {
                                sorter.add(o);
                            }
                        }
                        Collections.sort(sorter, ucomp);

                        for (Object o : sorter) {
                            valueCmb.addItem(o);
                        }

                        if (valuesPotential.contains(nu.other)) {
                            valueCmb.setEditable(true);
                        } else {
                            valueCmb.setEditable(false);
                        }
                        if (currentPotentialValue != null) {
                            valueCmb.setSelectedItem(currentPotentialValue);
                        } else if (valueCmb.getItemCount() > 0 && !valueCmb.isEditable()) {
                            valueCmb.setSelectedIndex(0);
                        } else {
                            valueCmb.setSelectedItem(null);
                        }
                        valueCmb.currentRow = row;
                    }
                    return valueCmb.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                } else {
                    Component res = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    res.setEnabled(valuesPotential != null);
                    return res;
                }
            } else {
                if (column == 2 && isSelected) {
                    int key = attrs.getKey(row);
                    HashSet h = (HashSet) sourcePotential.get(key);
                    if (h.isEmpty()) {
                        Component res = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        res.setEnabled(false);
                        setBgr(-1, res);
                        return res;
                    }
                    if (valueCmb.currentRow != row) {
                        valueCmb.removeAllItems();

                        sorter.clear();
                        for (Object o : h) {
                            if (o != nu.other) {
                                sorter.add(o);
                            }
                        }
                        Collections.sort(sorter, ucomp);

                        for (Object o : sorter) {
                            valueCmb.addItem(o);
                        }

                        if (h.contains(nu.other)) {
                            valueCmb.setEditable(true);
                        } else {
                            valueCmb.setEditable(false);
                        }
                        valueCmb.setSelectedItem(attrs.get(key));
                        valueCmb.currentRow = row;
                    }
                    return valueCmb.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                } else {
                    Component res = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    int key = attrs.getKey(row);
                    if (key != -1) {
                        HashSet h = (HashSet) sourcePotential.get(key);
                        if (h.isEmpty()) {
                            res.setEnabled(false);
                        } else {
                            res.setEnabled(true);
                        }
                    } else {
                        res.setEnabled(false);
                    }
                    setBgr(key, res);
                    return res;
                }
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        private void setBgr(int key, Component res) {
            if (key != -1 && inducedAttrs != null && inducedAttrs.contains(key)) {
                res.setBackground(inducedColor);
            } else {
                res.setBackground(table.getBackground());
            }
        }
    }

    public class CellComboEditor extends JComboBox implements TableCellRenderer {

        int currentRow = -1;

        public CellComboEditor() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            return this;
        }

    }

    public class CellButtonRenderer extends JButton implements TableCellRenderer, TableCellEditor {
        int currentRow;

        public CellButtonRenderer() {
            setActionCommand("BTN");
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setIcon((Icon) value);
            return this;
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            setIcon((Icon) value);
            setVisible(true);
            return this;
        }

        public void cancelCellEditing() {
            currentRow = -1;
        }

        public boolean stopCellEditing() {
            currentRow = -1;
            fireEditingStopped();
            return true;
        }

        public Object getCellEditorValue() {
            return "";
        }

        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        public void addCellEditorListener(CellEditorListener l) {
            listenerList.add(CellEditorListener.class, l);
        }

        public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(CellEditorListener.class, l);
        }

        protected void fireEditingStopped() {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == CellEditorListener.class) {
                    if (changeEvent == null)
                        changeEvent = new ChangeEvent(this);
                    ((CellEditorListener) listeners[i + 1]).editingStopped(changeEvent);
                }
            }
        }

    }

    class UComp implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 instanceof String) {
                if (o2 instanceof Integer || o2 == nu.ll) {
                    return 1;
                } else if (o2 instanceof String) {
                    return ((String) o1).compareTo((String) o2);
                } else if (o2 instanceof TString) {
                    return ((String) o1).compareTo(o2.toString());
                }
            } else if (o1 instanceof TString) {
                if (o2 instanceof Integer || o2 == nu.ll) {
                    return 1;
                } else if (o2 instanceof String) {
                    return (o1.toString()).compareTo((String) o2);
                } else if (o2 instanceof TString) {
                    return ((TString) o1).compareTo(o2);
                }
            } else if (o1 instanceof Integer) {
                if (o2 instanceof Integer) {
                    return ((Integer) o1).compareTo((Integer) o2);
                } else if (o2 == nu.ll) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (o1 == nu.ll) {
                if (o2 == nu.ll) {
                    return 0;
                } else {
                    return -1;
                }
            }
            return 0;
        }
    }
}
