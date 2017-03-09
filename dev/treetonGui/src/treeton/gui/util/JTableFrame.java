/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class JTableFrame extends JFrame implements MouseListener {
    public String menuTitle;
    public String windowTitle;
    boolean resizeByCaptionOnly = true;
    private JTableFrameListener listener;
    private JTableFrame instance;
    private JScrollPane jsc;
    private JTable table;
    private TableCellRenderer tcr;

    protected JTableFrame(TableModel model) {
        super();

        instance = this;
        menuTitle = "Table Viewer";
        windowTitle = "View Table";
        jsc = new JScrollPane();
        jsc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        table = new JTable();
        table.setModel(model);
        //table.setTableHeader(null);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setGridColor(Color.black);
        table.addMouseListener(this);
        jsc.getViewport().add(table);

        tcr = new DefaultTableCellRenderer() {
            Object o;

            // implements javax.swing.table.TableCellRenderer
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                o = value;
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }

            public String getToolTipText() {
                return o == null ? null : o.toString();
            }
        };

        table.setDefaultRenderer(String.class, tcr);

        ToolTipManager.sharedInstance().registerComponent(table);
        init();
    }

    protected JTableFrame(TableModel model, TableCellRenderer tcr) {
        super();

        this.tcr = tcr;
        instance = this;
        menuTitle = "Table Viewer";
        windowTitle = "View Table";
        jsc = new JScrollPane();
        jsc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JTable table = new JTable();
        table.setModel(model);
        //table.setTableHeader(null);

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn aColumn = table.getColumnModel().getColumn(i);
            aColumn.setCellRenderer(tcr);
            resizeColumn(table, i);
        }

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setGridColor(Color.black);

        jsc.getViewport().add(table);

        init();
    }

    public static JTableFrame getInstance(TableModel model) {
        return new JTableFrame(model);
    }

    private static JTableFrame getInstance(TableModel model, TableCellRenderer tcr) {
        return new JTableFrame(model, tcr);
    }

    public static JTableFrame showJTable(TableModel model) {
        JTableFrame userFrame = getInstance(model);
        int state = userFrame.getExtendedState();
        if ((state & Frame.ICONIFIED) != 0) {
            userFrame.setExtendedState(state & ~Frame.ICONIFIED);
        }
        userFrame.setVisible(true);
        userFrame.requestFocus();

        return userFrame;
    }

    public static void showJTable(TableModel model, TableCellRenderer tcr) {
        JTableFrame userFrame = getInstance(model, tcr);
        int state = userFrame.getExtendedState();
        if ((state & Frame.ICONIFIED) != 0) {
            userFrame.setExtendedState(state & ~Frame.ICONIFIED);
        }
        userFrame.setVisible(true);
        userFrame.requestFocus();
    }

    public boolean isResizeByCaptionOnly() {
        return resizeByCaptionOnly;
    }

    public void setResizeByCaptionOnly(boolean resizeByCaptionOnly) {
        this.resizeByCaptionOnly = resizeByCaptionOnly;
    }

    private void resizeColumn(JTable table, int column) {
        TableColumn aColumn;
        aColumn = table.getColumnModel().getColumn(column);
        DefaultTableCellRenderer tcr = (DefaultTableCellRenderer) aColumn.getCellRenderer();
        FontMetrics fm = table.getFontMetrics(table.getFont());
        int j = 0;
        int sz = 0;
        int w = (int) fm.getStringBounds(aColumn.getHeaderValue().toString(), getGraphics()).getWidth();
        Insets sets = tcr.getInsets();
        w += sets.left + sets.right + 2;

        if (w > sz) {
            sz = w;
        }

        if (!resizeByCaptionOnly) {
            while (j < table.getModel().getRowCount()) {
                DefaultTableCellRenderer l = (DefaultTableCellRenderer) tcr.getTableCellRendererComponent(table, table.getModel().getValueAt(j, column), false, false, j, column);
                w = (int) fm.getStringBounds(l.getText(), getGraphics()).getWidth();
                sets = l.getInsets();
                w += sets.left + sets.right + 2;

                if (w > sz) {
                    sz = w;
                }
                j++;
            }
        }
        aColumn.setPreferredWidth(sz);
        aColumn.setWidth(sz);
        table.repaint();
    }

    protected void init() {
        setTitle(windowTitle);

        this.setDefaultCloseOperation(HIDE_ON_CLOSE);

        this.setResizable(true);

        setContentPane(jsc);

        Dimension dim = jsc.getPreferredSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        instance.setBounds(((int) screenSize.getWidth() - (int) Math.min(screenSize.getWidth() * 0.7, dim.getWidth() + 8)) / 2, (int) (screenSize.getHeight() * 0.15), (int) Math.min(screenSize.getWidth() * 0.7, dim.getWidth() + 8), (int) Math.min(screenSize.getHeight() * 0.7, dim.getHeight() + 34));
        this.validate();
    }

    public void actionPerformed(ActionEvent e) {

    }

    public void refresh() {
        jsc.updateUI();

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn aColumn = table.getColumnModel().getColumn(i);
            aColumn.setCellRenderer(tcr);
            resizeColumn(table, i);
        }
    }

    public JTableFrameListener getListener() {
        return listener;
    }

    public void setListener(JTableFrameListener listener) {
        this.listener = listener;
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            int row = table.rowAtPoint(e.getPoint());
            int col = table.columnAtPoint(e.getPoint());
            if (row != -1 && col != -1) {
                if (listener != null) {
                    listener.cellDoubleClicked(row, col);
                }

            }

        }
    }

    public void mousePressed(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseReleased(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
