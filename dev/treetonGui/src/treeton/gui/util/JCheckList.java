/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Enumeration;

public class JCheckList extends JScrollPane {

    // This listbox holds only the checkboxes
    private JList listCheckBox;
    // This listbox holds the actual descriptions of list items.
    private JList listDescription;
    //
    private CheckListListener checkListListener;

    public JCheckList() {
        super();
        listCheckBox = new JList(new DefaultListModel());
        //This listbox holds the actual descriptions of list items.
        listDescription = new JList(new DefaultListModel());
        checkListListener = null;

        ToolTipManager.sharedInstance().registerComponent(listDescription);

        //Make the listBox with Checkboxes look like a rowheader.
        //This will place the component on the left corner of the scrollpane
        this.setRowHeaderView(listCheckBox);

        //Now, make the listbox with actual descriptions as the main view
        this.setViewportView(listDescription);

        // Align both the checkbox height and widths
        listDescription.setFixedCellHeight(20);
        listCheckBox.setFixedCellHeight(listDescription.getFixedCellHeight());
        listCheckBox.setFixedCellWidth(20);

        listDescription.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() != 2) return;
                int selectedIndex = listDescription.locationToIndex(me.getPoint());
                if (selectedIndex < 0) return;
                CheckBoxItem item = (CheckBoxItem) listCheckBox.getModel().getElementAt(selectedIndex);
                if (item.isEnabled()) {
                    item.setChecked(!item.isChecked());
                    notifyCheckListListener(selectedIndex);
                }
                listCheckBox.repaint();
            }
        });

        listCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                int selectedIndex = listCheckBox.locationToIndex(me.getPoint());
                if (selectedIndex < 0) return;
                CheckBoxItem item = (CheckBoxItem) listCheckBox.getModel().getElementAt(selectedIndex);
                if (item.isEnabled()) {
                    item.setChecked(!item.isChecked());
                    notifyCheckListListener(selectedIndex);
                }
                listDescription.setSelectedIndex(selectedIndex);
                listCheckBox.repaint();
            }
        });

        listDescription.addMouseMotionListener(
                new MouseMotionAdapter() {
                    public void mouseMoved(MouseEvent me) {
                        int i = listDescription.locationToIndex(me.getPoint());
                        if (i < 0) {
                            listDescription.setToolTipText(null);
                        } else {
                            listDescription.setToolTipText(
                                    ((CheckBoxItem) getDescriptionModel().
                                            getElementAt(i)).getToolTip());
                        }
                    }
                });

        listCheckBox.setCellRenderer(new CheckBoxRenderer());
        listCheckBox.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listDescription.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listDescription.setCellRenderer(new LabelRenderer());

        final JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(new AbstractAction("Select") {
            public void actionPerformed(ActionEvent e) {
                for (int i : listDescription.getSelectedIndices())
                    getElementAt(i).setChecked(true);
                listCheckBox.updateUI(); // terrible hack! own list model and fireContentChanged() must be used instead!
            }
        }));
        popup.add(new JMenuItem(new AbstractAction("Deselect") {
            public void actionPerformed(ActionEvent e) {
                for (int i : listDescription.getSelectedIndices())
                    getElementAt(i).setChecked(false);
                listCheckBox.updateUI(); // terrible hack! own list model and fireContentChanged() must be used instead!
            }
        }));
        listDescription.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON3)
                    return;
                popup.show(listDescription, e.getX(), e.getY());
            }
        });
    }

    public JList getListDescription() {
        return listDescription;
    }

    public void addCheckListListener(
            CheckListListener cl) {
        checkListListener = cl;
    }

    public void notifyCheckListListener(int itemIndex) {
        if (checkListListener != null) {
            checkListListener.checkChanged(this, itemIndex);
        }
    }

    public void notifyCheckListListener() {
        notifyCheckListListener(-1);
    }

    public CheckBoxItem addElement(CheckBoxItem item) {
        getDescriptionModel().addElement(item);
        getCheckBoxModel().addElement(item);
        return item;
    }

    public CheckBoxItem addElement(String name, boolean checked,
                                   String toolTip) {
        return addElement(new CheckBoxItem(name, checked, toolTip));
    }

    public CheckBoxItem insertElementAt(CheckBoxItem item,
                                        int index) {
        getDescriptionModel().insertElementAt(item, index);
        getCheckBoxModel().insertElementAt(item, index);
        return item;
    }

    public CheckBoxItem insertElementAt(String name, boolean checked,
                                        String toolTip, int index) {
        return insertElementAt(
                new CheckBoxItem(name, checked, toolTip), index);
    }

    public void remove(int index) {
        getDescriptionModel().remove(index);
        getCheckBoxModel().remove(index);
    }

    public void removeAllElements() {
        getDescriptionModel().removeAllElements();
        getCheckBoxModel().removeAllElements();
    }


    public CheckBoxItem getElementAt(int index) {
        return (CheckBoxItem)
                getDescriptionModel().getElementAt(index);
    }

    public int getElementsCount() {
        return getDescriptionModel().size();
    }

    public Enumeration elements() {
        return getDescriptionModel().elements();
    }

    public DefaultListModel getDescriptionModel() {
        return (DefaultListModel) listDescription.getModel();
    }

    protected DefaultListModel getCheckBoxModel() {
        return (DefaultListModel) listCheckBox.getModel();
    }

    public int getSelectedIndex() {
        return listDescription.getSelectedIndex();
    }

    public void setSelectedIndex(int index) {
        listDescription.setSelectedIndex(index);
    }

    class CheckBoxRenderer extends JCheckBox
            implements ListCellRenderer {

        public CheckBoxRenderer() {
            setBackground(UIManager.getColor("List.textBackground"));
            setForeground(UIManager.getColor("List.textForeground"));
        }

        public Component getListCellRendererComponent(
                JList listBox, Object obj, int currentindex,
                boolean isChecked, boolean hasFocus) {
            setSelected(((CheckBoxItem) obj).isChecked());
            setEnabled(((CheckBoxItem) obj).isEnabled());
            return this;
        }

        public Insets getInsets() {
            return new Insets(0, 2, 0, 2);
        }
    }

    class LabelRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(
                JList listBox, Object obj, int currentindex,
                boolean isChecked, boolean hasFocus) {
            CheckBoxItem c = (CheckBoxItem) obj;
            super.getListCellRendererComponent(listBox, obj, currentindex,
                    isChecked, hasFocus);
            if (c.getFrColor() != null) {
                setForeground(c.getFrColor());
            }
            if (c.getIcon() != null) {
                setIcon(c.getIcon());
            }
            return this;
        }
    }
}
