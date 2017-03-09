/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.ParamDescription;
import treeton.gui.util.ExceptionDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Iterator;
import java.util.LinkedList;

public class AttrTable extends JTable {
    AttrTableModel model;

    public AttrTable(AttrTableModel pMyModel) {
        model = pMyModel;
        this.setModel(model);
    }

    public boolean isCellEditable(int rowIndex, int vColIndex) {
        AttrTableModel model = (AttrTableModel) this.getModel();
        TableElement el = model.elemlist.get(rowIndex);
        if (!el.isEditable && el.isOptional && el.isParent) {
            if (vColIndex == 1 || vColIndex == 0) return true;
            else return false;
        } else if (el.isParent && el.isEditable) return true;

        else {
            if (vColIndex == 1) return true;
            else return false;
        }
    }

    public TableCellEditor getCellEditor(int row, int col) {
        TableCellEditor tmpEditor = null;
        String type = (String) this.getValueAt(row, 2);
        //System.out.println("type:" + type);
        JTextField textField = new JTextField();
        textField.setBorder(BorderFactory.createEmptyBorder());
        DefaultCellEditor editor = new DefaultCellEditor(textField);
        editor.setClickCountToStart(1);
        if (col == 0) {
            JComboBox comboBox = new JComboBox();
            LinkedList<String> signature;
            signature = new LinkedList<String>();
            if (model.data.res != null) {
                Iterator<ParamDescription> it = model.data.res.iterator();
                while (it.hasNext()) {
                    ParamDescription pd = it.next();
                    if (!model.data.containsKey(pd.getName())) {
                        signature.add(pd.getName());
                    }
                }
            }
//проверка совпадения типов
            for (String aSignature : signature) {
                try {
                    ParamDescr p = new ParamDescr(model.data.res.getParamDescription(aSignature));
                } catch (ContextException e) {
                    ExceptionDialog.showExceptionDialog(this, e);
                }
                //              if(model.data.getList().get(row).pType.equals(p.pType)){
                comboBox.addItem(aSignature);
//               }
//              else {
//                   if((model.data.getList().get(row).pType.equals("String"))){
//                        if(model.data.getList().get(row).pValue==""){
//                                        comboBox.addItem(aSignature);
//                        }
//              }
//               }
            }
            if (signature.size() == 0) comboBox.addItem("");
            tmpEditor = new NameCellEditor(comboBox);
            return tmpEditor;

        }
        if (col == 1) {
            if (type.equals("Integer")) {
                tmpEditor = new NumberCellEditor();
            } else if (type.equals("Long")) {
                tmpEditor = new LongCellEditor();
            } else if (type.equals("String")) {
                tmpEditor = new DefaultCellEditor(new JTextField());
            } else if (type.equals("URI")) {
                tmpEditor = new URICellEditor();
            } else if (type.equals("Boolean")) {
                tmpEditor = new DefaultCellEditor(new JCheckBox());
            }

            if (tmpEditor != null) {
                return tmpEditor;
            }
        }
        return super.getCellEditor(row, col);
    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        TableCellRenderer tmpRend = null;
        final TableElement el = model.elemlist.get(row);
        if (!el.isEditable && col != 1) tmpRend = new ResourceCellRenderer();
        if (!el.isParent && col != 1) tmpRend = new ValueCellRenderer();
//    if(col==1) tmpRend=new DefaultTableCellRenderer();
        if (col == 1) {
            if (el.pType.equals("Boolean")) {

                tmpRend = new TableCellRenderer() {
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        JCheckBox newCheckbox = new JCheckBox();
                        if (isSelected) {
                            newCheckbox.setForeground(table.getSelectionForeground());
                            newCheckbox.setBackground(table.getSelectionBackground());
                        } else {
                            newCheckbox.setForeground(table.getForeground());
                            newCheckbox.setBackground(table.getBackground());
                        }
                        newCheckbox.setSelected((value != null && (Boolean) value));
                        return newCheckbox;
                    }
                };
            } else tmpRend = new DefaultTableCellRenderer();
        }
        if (tmpRend != null) {
            return tmpRend;
        }

        return super.getCellRenderer(row, col);

    }
}
