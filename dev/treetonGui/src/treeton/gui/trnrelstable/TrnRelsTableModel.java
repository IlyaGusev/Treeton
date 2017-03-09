/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnrelstable;


import treeton.core.Treenotation;
import treeton.core.util.ObjectPair;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public class TrnRelsTableModel<T> extends AbstractTableModel {
    private static final int lineLength = 15;

    private final NavigableMap<Treenotation, ObjectPair<Treenotation, T>> map;
    private final Treenotation rootTrn;

    private final List<Treenotation> trns;

    public TrnRelsTableModel(NavigableMap<Treenotation, ObjectPair<Treenotation, T>> map, Treenotation trn) {
        this.map = map;
        rootTrn = trn;
        trns = new ArrayList<Treenotation>();
        for (Map.Entry<Treenotation, ObjectPair<Treenotation, T>> entry : map.entrySet()) {
            if (entry.getValue().getFirst().equals(rootTrn)) // не показываем самую корневую тринотацию типа Syntax
                continue;
            trns.add(entry.getKey());
        }
    }

    public static String wrap(String s, int lineLength) {
        if (s.length() <= lineLength)
            return s;

        StringBuffer buf = new StringBuffer(s);

        int i = lineLength;

        while (i < buf.length()) {
            i = buf.indexOf(" ", i);
            if (i == -1)
                break;
            buf.replace(i, i + 1, "<br>");
            i += lineLength;
        }

        return buf.toString();
    }


    @Override
    public String getColumnName(int column) {
        if (column == 0)
            return "slave\\parent";

        String s = getTrnLabel(trns.get(column - 1));
        return "<html>" + wrap(s, 15) + "</html>";
//        return "<html>"+s+"</html>";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? String.class : Object.class;
    }

    private String getTrnLabel(Treenotation trn) {
        return trn.getText();
    }

    public int getColumnCount() {
        return trns.size() + 1;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0)
            return getTrnLabel(trns.get(rowIndex));
        ObjectPair<Treenotation, T> parent = map.get(trns.get(rowIndex));
        return trns.get(columnIndex - 1).equals(parent.getFirst()) ? parent.getSecond() : null;
    }

    public int getRowCount() {
        return trns.size();
    }

    public boolean isRoot(int trnIndex) {
        Treenotation parent = map.get(trns.get(trnIndex)).getFirst();
        return map.get(parent).getFirst().equals(rootTrn);
    }
}
