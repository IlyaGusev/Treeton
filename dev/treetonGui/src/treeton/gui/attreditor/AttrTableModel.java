/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.resources.api.ParamDescription;

import javax.swing.table.AbstractTableModel;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class AttrTableModel extends AbstractTableModel {
    final String[] columnNames = {BasicConfiguration.localize("GuiBundle", "treeton.gui.AttrEditor.AttrPanel.ParName"),
            BasicConfiguration.localize("GuiBundle", "treeton.gui.AttrEditor.AttrPanel.ParValue"),
            BasicConfiguration.localize("GuiBundle", "treeton.gui.AttrEditor.AttrPanel.ParType"),
            BasicConfiguration.localize("GuiBundle", "treeton.gui.AttrEditor.AttrPanel.ParIsMultiple")};
    AttrStorage data = new AttrStorage();
    LinkedList<TableElement> elemlist = new LinkedList<TableElement>();

    public AttrTableModel() {
        super();
        ParamDescr par = new ParamDescr("str", false, "String", true);
        data.put("1st", par);
        par = new ParamDescr(3, true, "Integer", true);
        data.put("2nd", par);
        elemlist = data.getList();
    }

    public AttrTableModel(AttrStorage storage) {
        super();
        data = storage;
        elemlist = data.getList();
    }

    public AttrTableModel(Map<String, ParamDescr> map) {
        super();

    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {

        if (data.isEmpty()) return 0;
        else return data.getList().size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
        TableElement el = elemlist.get(row);
        if (col == 1) {
            if (!el.pType.equals("Boolean")) {
                return el.pValue;
            } else return (Boolean) el.pValue;
        } else if (col == 0) return el.pName;
        else if (col == 2) return el.pType;
        else return el.pMultiple;
    }

    /*
    * JTable uses this method to determine the default renderer/
    * editor for each cell.  If we didn't implement this method,
    * then the last column would contain text ("true"/"false"),
    * rather than a check box.
    */
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    /*
    * Don't need to implement this method unless your table's
    * editable.
    */
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        return true;
    }

    public void addRow() //Add Row
    {
        Object i = elemlist.size() + 1;
        String name = "parameter" + i;
        while (data.containsKey(name)) {
            name = name + 1;
        }
        ParamDescr par = new ParamDescr("", true, "String", true);
        data.put(name, par);
        TableElement el = new TableElement(name, "", true, "String", true, true, true);
        elemlist.addLast(el);
        Collections.sort(elemlist, new AttrStorage());
        fireTableRowsInserted(data.size(), data.size());

    }

    public void addValue(int row) {
        TableElement el = elemlist.get(row);
        if (el.pMultiple) {
            if (el.pType.equals("Integer")) data.get(el.pName).pValue.addLast(0);
            else if (el.pType.equals("Long")) data.get(el.pName).pValue.addLast(0);
            else if (el.pType.equals("String") || el.pType.equals("URI")) data.get(el.pName).pValue.addLast("");
            else if (el.pType.equals("Boolean")) data.get(el.pName).pValue.addLast((Boolean) false);
        }
        elemlist = data.getList();

    }

    public void removeRow(int i) {
        TableElement el = elemlist.get(i);
        //переставить пэрентов
//    if (!el.isParent){
        if (data.get(el.pName).pValue.size() > 1) {
            removeValue(i);
        } else data.remove(el.pName);
//    }
//    else data.remove(el.pName);
        elemlist = data.getList();
        fireTableDataChanged();
    }

    public void setValueAt(Object value, int row, int col) {
        if (value != null) {
            if (row <= elemlist.size()) {
                String name = elemlist.get(row).pName;
                ParamDescr par = data.get(name);
                if (col == 0) {
                    //переименовать аттрибут
                    if (!data.containsKey(value)) {
                        data.remove(name);
                        par.pEditable = true;
                        par.pOptional = true;
                        Iterator<ParamDescription> ik = data.res.iterator();
                        while (ik.hasNext()) {
                            ParamDescription pd = ik.next();
                            if (pd.getName().equals(value)) {
                                ParamDescr p = new ParamDescr(pd);
                                par.pEditable = false;
                                par.pMultiple = pd.isManyValued();
                                par.pType = p.pType;
                                par.pValue = p.pValue;

                            }
                        }
                        //          par.pValue.set(0,true);
                        data.put((String) value, par);
                        fireTableRowsUpdated(0, this.elemlist.size() - 1);
                    }

                } else if (col == 1) {
                    TableElement el = data.getList().get(row);
                    int firstrow = -1;
                    LinkedList<TableElement> elList = data.getList();
                    for (TableElement e : elList)
                        if (e.pName.equals(el.pName)) {
                            firstrow = elList.indexOf(e);
                            break;
                        }
                    //         int ind = par.pValue.indexOf(elemlist.get(row).pValue);
                    int ind = row - firstrow;
                    if (par.pMultiple) par.pValue.set(ind, value);
                    else {
                        if (!par.pType.equals("Boolean")) {
                            par.pValue.set(0, value);
                        } else par.pValue.set(0, (Boolean) value);
                    }

                } else if (col == 2) {
                    par.pType = (String) value;
                    for (int i = 0; i < par.pValue.size(); i++) {
                        if (par.pType.equals("Integer") && !(par.pValue.get(i) instanceof Integer)) {
//              par.pValue.set(i,0);
                            NumberFormat integerFormat = NumberFormat.getIntegerInstance();
                            try {
                                par.pValue.set(i, integerFormat.parseObject(par.pValue.get(i).toString()));
                            } catch (ParseException exc) {
                                //System.err.println("getCellEditorValue: can't parse o: " + o);
                                par.pValue.set(i, 0);
                            }
                            //            fireTableRowsUpdated(0, row);
                        } else if (par.pType.equals("Long") && !(par.pValue.get(i) instanceof Long)) {
//              par.pValue.set(i,0);
                            NumberFormat numberFormat = NumberFormat.getNumberInstance();
                            try {
                                par.pValue.set(i, numberFormat.parseObject(par.pValue.get(i).toString()));
                            } catch (ParseException exc) {
                                //System.err.println("getCellEditorValue: can't parse o: " + o);
                                par.pValue.set(i, 0);
                            }

                            //             fireTableRowsUpdated(0, row);
                        } else if (par.pType.equals("Boolean")) {
//              if(par.pValue.get(i).toString().equals("true")) par.pValue.set(i,true);
                            par.pValue.set(i, (Boolean) false);
                            //             fireTableRowsUpdated(0, row);
                        }
                    }

                } else if (col == 3) {
                    int removed = 0;
                    par.pMultiple = (Boolean) value;
                    if (!par.pMultiple) {
                        for (int i = par.pValue.size() - 1; i > 0; i--) {
                            data.get(name).pValue.remove(i);
                            removed++;
                        }


                    }
                    fireTableRowsDeleted(row + 1, row + 1 + removed);
                }
            }
        }
        elemlist = data.getList();
        //    fireTableDataChanged();
//    fireTableCellUpdated(row,col);
        fireTableRowsUpdated(0, row);

    }

    public void removeValue(int row) {
        TableElement el = data.getList().get(row);
        int firstrow = -1;
        LinkedList<TableElement> elList = data.getList();
        for (TableElement e : elList)
            if (e.pName.equals(el.pName)) {
                firstrow = elList.indexOf(e);
                break;
            }
        if (firstrow != -1) {
            data.get(el.pName).pValue.remove(row - firstrow);
        }
    }

}



