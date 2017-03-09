/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AttrEdit extends JFrame implements ActionListener {
    JButton badd;
    JButton bremove;
    JButton bplus;
    JButton baddvalue;
    AttrTable table;
    AttrStorage dat;
    private boolean DEBUG = false;

    public AttrEdit() {
        super("AttrEdit");

        AttrTableModel myModel = new AttrTableModel();
        table = new AttrTable(myModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setPreferredScrollableViewportSize(new Dimension(700, 100));
        //Create the scroll pane and add the table to it. 
        JScrollPane scrollPane = new JScrollPane(table);
        //initcolumns
        initColumnSizes(table, myModel);
        //set up type column
        setUpTypeColumn(table.getColumnModel().getColumn(2));

        getContentPane().add(scrollPane, BorderLayout.CENTER);

        //buttons
        baddvalue = new JButton("Add value");
        baddvalue.setVerticalTextPosition(AbstractButton.CENTER);
        baddvalue.setHorizontalTextPosition(AbstractButton.LEFT);
        baddvalue.setActionCommand("addValue");


        badd = new JButton("Add attribute");
        badd.setVerticalTextPosition(AbstractButton.CENTER);
        badd.setHorizontalTextPosition(AbstractButton.LEFT);
        badd.setActionCommand("add");

        bremove = new JButton("Remove");
        bremove.setVerticalTextPosition(AbstractButton.CENTER);
        bremove.setHorizontalTextPosition(AbstractButton.CENTER);
        bremove.setActionCommand("remove");

        //Listen for actions on buttons
        badd.addActionListener(this);
        baddvalue.addActionListener(this);
        bremove.addActionListener(this);


        badd.setToolTipText("Click to add attribute.");
        bremove.setToolTipText("Click to remove attribute");

        //Add Components to this container, using the default FlowLayout. 

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(baddvalue);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(badd);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(bremove);

        //       buttonPane.add(baddvalue);
        getContentPane().add(buttonPane, BorderLayout.PAGE_END);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        AttrEdit frame = new AttrEdit();
        frame.makeFrame();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("add")) {
            table.model.addRow();
        } else if (e.getActionCommand().equals("remove")) {
            int i = table.getSelectedRow();
            if (i >= 0) {
                table.model.removeRow(i);
                updateList();
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
        table.tableChanged(new TableModelEvent(table.getModel()));
        try {
            table.setRowSelectionInterval(row, row);
            table.setColumnSelectionInterval(col, col);
        } catch (IllegalArgumentException e) {
            try {
                if (table.getModel().getRowCount() > 0) {
                    table.setRowSelectionInterval(0, 0);
                    table.setColumnSelectionInterval(1, 1);
                }
            } catch (IllegalArgumentException e1) {
                //do nothing
            }
        }
        table.requestFocus();
    }

    private void initColumnSizes(JTable table, AttrTableModel model) {
        TableColumn column = null;
        Component comp = null;
        int headerWidth = 0;
        int cellWidth = 0;

        for (int i = 0; i < 4; i++) {
            column = table.getColumnModel().getColumn(i);
            //System.out.print(column.getHeaderValue());
            comp = table.getDefaultRenderer(model.getColumnClass(i)).
                    getTableCellRendererComponent(
                            table, model.data.get(i),
                            false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;

            if (DEBUG) {
                System.out.println("Initializing width of column "
                        + i + ". "
                        + "headerWidth = " + headerWidth
                        + "; cellWidth = " + cellWidth);
            }

            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }

    public void setUpTypeColumn(TableColumn typeColumn) {
        //Set up the editor for the sport cells.
        JComboBox comboBox = new JComboBox();
        comboBox.addItem("String");
        comboBox.addItem("Integer");

        //       comboBox.addItem("Boolean");
        typeColumn.setCellEditor(new TypeCellEditor(comboBox));

        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        typeColumn.setCellRenderer(renderer);

        //Set up tool tip for the sport column header.
        TableCellRenderer headerRenderer = typeColumn.getHeaderRenderer();
        if (headerRenderer instanceof DefaultTableCellRenderer) {
            ((DefaultTableCellRenderer) headerRenderer).setToolTipText(
                    "Click type to see a list of choices");
        }
    }

    public void makeFrame() {
        this.pack();
        this.setLocation(200, 300);
        this.setVisible(true);
    }
}


