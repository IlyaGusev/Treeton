/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.ParamDescription;
import treeton.core.config.context.resources.api.ResourceSignature;
import treeton.gui.GuiResources;
import treeton.gui.util.ExceptionDialog;
import treeton.gui.util.ToolBarFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;
import java.util.List;

public class AttrPanel extends JPanel implements ActionListener {
    JButton badd;
    JButton bremove;
    JButton bplus;
    JButton baddvalue;
    AttrTable table;
    AttrStorage dat;
    JToolBar jtb;
    TableModelListener listener;
    Listlistener l;
    LinkedList<String> signature;
    ResourceSignature r;
    private boolean DEBUG = false;


    public AttrPanel() {
        this.dat = new AttrStorage();
    }

    public void init(ResourceSignature res, Map<String, Object> map) throws ContextException {
        clear();
        this.dat = new AttrStorage();
        this.r = res;
        this.dat.res = res;
        this.signature = new LinkedList<String>();
        if (res != null) {
            Iterator<ParamDescription> it = res.iterator();
            while (it.hasNext()) {
                ParamDescription pd = it.next();
                this.signature.add(pd.getName());
//        ParamDescr p = new ParamDescr(pd);
//        p.pEditable=false;
//        p.pOptional=pd.isOptional();
//        dat.put(pd.getName(),p);
            }
        }
        if (map != null) {
            Set<Map.Entry<String, Object>> entries = map.entrySet();
            Iterator i = entries.iterator();
            while (i.hasNext()) {
                Map.Entry<String, Object> entry = (Map.Entry<String, Object>) i.next();
//        if(dat.containsKey(entry.getKey())){
                ParamDescr p;
                if (res.getParamDescription(entry.getKey()) != null) {
                    p = new ParamDescr(res.getParamDescription(entry.getKey()));
                    p.pOptional = res.getParamDescription(entry.getKey()).isOptional();
                    p.pEditable = false;
                } else {
                    String type = "String";
                    if (entry.getValue() instanceof Integer) {
                        type = "Integer";
                    }
                    if (entry.getValue() instanceof Long) {
                        type = "Long";
                    } else if (entry.getValue() instanceof Boolean) {
                        type = "Boolean";
                    } else if (entry.getValue() instanceof URI) {
                        type = "URI";
                    }
                    p = new ParamDescr(entry.getValue(), true, type, true);
                    p.pEditable = true;

                }
                if (entry.getValue() instanceof java.util.LinkedList) {
                    p.pValue = (LinkedList<Object>) entry.getValue();
                    p.pMultiple = true;
                } else if (entry.getValue() instanceof java.util.ArrayList) {
                    ArrayList al = (ArrayList) entry.getValue();
                    p.pValue = new LinkedList<Object>();
                    for (int k = 0; k < al.size(); k++) {
                        p.pValue.add(al.get(k));
                    }
                    p.pMultiple = true;

                } else {
                    p.pValue.set(0, entry.getValue());
                    p.pMultiple = false;
                }
                dat.put(entry.getKey(), p);
//        }
//        else{
//          ParamDescr p = new ParamDescr(entry.getValue(),true, "String",true);
//          p.pOptional=true;
//          dat.put(entry.getKey(),p);
//        }
            }
        }
        AttrTableModel myModel = new AttrTableModel(dat);
        table = new AttrTable(myModel);

        this.table.model.addTableModelListener(listener);
        this.table.model.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                int i = table.getSelectedRow();

                if (i >= 0 && i < table.getRowCount()) {

                    TableElement el = table.model.elemlist.get(i);
                    if (!el.isEditable && el.isParent && !el.isOptional) {
                        bremove.setEnabled(false);

                    } else {
                        table.setColumnSelectionAllowed(false);
                        bremove.setEnabled(true);
                    }
                    if (el.pMultiple) {
                        table.setColumnSelectionAllowed(false);
                        baddvalue.setEnabled(true);
                    } else baddvalue.setEnabled(false);
                }
            }


        });

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);
        table.setPreferredScrollableViewportSize(new Dimension(700, 200));

        //table.setFocusable(true);
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        //initcolumns
//    initColumnSizes(table, myModel);
        //set up type column
        setUpTypeColumn(table.getColumnModel().getColumn(2));
//    setUpNameColumn(table.getColumnModel().getColumn(0));
        //scrollPane.setSize(700,200);
        this.setLayout(new GridBagLayout());

        //buttons
        ToolBarFactory tbf = GuiResources.tbf;
        jtb = new JToolBar(JToolBar.HORIZONTAL);
        jtb.setFloatable(false);
        jtb.setRollover(true);
        jtb.setBorder(BorderFactory.createEmptyBorder());

        //updateList();
        //this.validate();
        //this.setVisible(true);
        try {
            baddvalue = tbf.addToolBarButton(jtb, "addfile.gif", "addValue", "Add new value", this);
            baddvalue.setEnabled(false);
            badd = tbf.addToolBarButton(jtb, "insrow.gif", "add", "Add new parameter", this);
            bremove = tbf.addToolBarButton(jtb, "delfile.gif", "remove", "Remove", this);
        } catch (MalformedURLException e) {
            ExceptionDialog.showExceptionDialog(null, e);
        }


        badd.setToolTipText("Click to add attribute.");
        bremove.setToolTipText("Click to remove attribute");
        this.add(jtb, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        this.add(scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));

//      table.model.addTableModelListener(new TableModelListener() {
//          public void tableChanged(TableModelEvent e) {
//              int mColIndex = e.getColumn();
//              if(mColIndex==3) table.clearSelection();
//          }
//      });
        l = new Listlistener();
//        table.model.addTableModelListener(new TableModelListener() {
//            public void tableChanged(TableModelEvent e) {
//                updateList();
//            }
//        });
        table.getSelectionModel().addListSelectionListener(l);


        this.updateUI();


    }

    public void actionPerformed(ActionEvent e) {

        if (e.getActionCommand().equals("add")) {
            table.model.addRow();
            int row = table.getSelectedRow();
            if (row > 0) {
                table.setRowSelectionInterval(table.getSelectedRow(), table.getSelectedRow());
            }
            //     setUpNameColumn(table.getColumnModel().getColumn(0));
        } else if (e.getActionCommand().equals("remove")) {

            int[] rows = table.getSelectedRows();
            if (rows.length > 0) {
                if (table.getCellEditor() != null) {
                    table.getCellEditor().cancelCellEditing();
                }
                for (int i = 0; i < rows.length; i++) {
                    table.model.removeRow(rows[0]);
                    int j = rows[0];
                    if (j == table.getRowCount()) j = j - 1;
                    if (j > -1) {
                        table.setRowSelectionInterval(j, j);
                    } else table.clearSelection();
                    //                   updateList();
                }
            }
        } else if (e.getActionCommand().equals("addValue")) {
            int i = table.getSelectedRow();
            if (i >= 0) table.model.addValue(i);
            else JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(baddvalue), "Choose row first!");
            updateList();
        }
    }

    private void updateList() {

        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
//    setUpNameColumn(table.getColumnModel().getColumn(0));
//    setSignature();
        table.tableChanged(new TableModelEvent(table.getModel()));
        try {
            table.setRowSelectionInterval(row, row);
            //         table.setColumnSelectionInterval(col, col);
        } catch (IllegalArgumentException e) {
            try {
                if (table.getModel().getRowCount() > 0) {
                    table.setRowSelectionInterval(0, 0);
                    //                   table.setColumnSelectionInterval(1, 1);
                }
            } catch (IllegalArgumentException e1) {
                // do nothing
            }
        }
        table.requestFocus();
    }


    public void setUpTypeColumn(TableColumn typeColumn) {
        //Set up the editor for the sport cells.
        JComboBox comboBox = new JComboBox();
        comboBox.addItem("String");
        comboBox.addItem("Integer");
        comboBox.addItem("Long");
        comboBox.addItem("Boolean");
        comboBox.addItem("URI");
        typeColumn.setCellEditor(new TypeCellEditor(comboBox));

        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        typeColumn.setCellRenderer(renderer);

        TableCellRenderer headerRenderer = typeColumn.getHeaderRenderer();
        if (headerRenderer instanceof DefaultTableCellRenderer) {
            ((DefaultTableCellRenderer) headerRenderer).setToolTipText("Click type to see a list of choices");
        }
    }

    public void clear() {
        this.removeAll();
        this.updateUI();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setChildrenEnabled(this, enabled);
    }

    private void setChildrenEnabled(Container container, boolean enabled) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component comp = container.getComponent(i);
            comp.setEnabled(enabled);
            if (comp instanceof Container) setChildrenEnabled((Container) comp, enabled);
        }
    }
//  public void setSignature(){
//    this.signature = new LinkedList<String>();
//    if (this.r != null) {
//      Iterator<ParamDescription> it = r.iterator();
//      while(it.hasNext()){
//        ParamDescription pd = it.next();
//        if(!this.table.model.data.containsKey(pd.getName())){
//        this.signature.add(pd.getName());
//        }
//      }
//      }

//  }

    public Map<String, Object> getData() {
        Map<String, Object> map = new HashMap<String, Object>();
        Set<Map.Entry<String, ParamDescr>> datas = this.dat.entrySet();
        Iterator it = datas.iterator();
        while (it.hasNext()) {
            Map.Entry<String, ParamDescr> entry = (Map.Entry<String, ParamDescr>) it.next();
            ParamDescr par = entry.getValue();
            Object val;
            if (par.pMultiple) {
                val = (List) par.pValue;
            } else val = par.pValue.getFirst();
            map.put(entry.getKey(), val);
        }

        return map;
    }

    public void addTableModelListener(TableModelListener listener) {
        this.listener = listener;
//      this.table.model.addTableModelListener(listener);
    }

    class Listlistener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                int i = table.getSelectedRow();

                if (i >= 0) {

                    TableElement el = table.model.elemlist.get(i);
                    if (!el.isEditable && el.isParent && !el.isOptional) {
                        bremove.setEnabled(false);

                    } else {
                        table.setColumnSelectionAllowed(false);
                        bremove.setEnabled(true);
                    }
                    if (el.pMultiple) {
                        table.setColumnSelectionAllowed(false);
                        baddvalue.setEnabled(true);
                    } else baddvalue.setEnabled(false);
                }
            }
        }
    }


}


