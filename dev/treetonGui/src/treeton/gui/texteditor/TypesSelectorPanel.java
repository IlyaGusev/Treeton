/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.texteditor;

import treeton.core.TreenotationStorage;
import treeton.core.config.context.ContextException;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.gui.trnedit.TreenotationStorageProvider;
import treeton.gui.util.ExceptionDialog;
import treeton.gui.util.TypesInfoProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

public class TypesSelectorPanel extends JPanel
        implements ItemListener, TypesInfoProvider {

    Set<String> selectedTypes = new HashSet<String>();
    Map<JCheckBox, String> allCheckBoxes = new HashMap<JCheckBox, String>();
    private TreenotationStorageProvider trnStorageProvider;
    private List<TypesSelectorPanelListener> listeners = new ArrayList<TypesSelectorPanelListener>();

    public TypesSelectorPanel(TreenotationStorageProvider trnStorageProvider) {
        this.trnStorageProvider = trnStorageProvider;
        try {
            reFill();
        } catch (ContextException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
    }

    public void reFill() throws ContextException {
        removeAll();
        allCheckBoxes.clear();
        TreenotationStorage s = trnStorageProvider.getStorage();
        if (s == null) {
            updateUI();
            return;
        }

        try {
            List<String> typeList = new ArrayList<String>();

            TrnTypeStorage types = s.getTypes();
            for (TrnType type : types.getAllTypes()) {
                typeList.add(type.getName());
            }

            Collections.sort(typeList);

            boolean containsNull = selectedTypes.contains(null);
            selectedTypes.retainAll(typeList);
            if (containsNull) {
                selectedTypes.add(null);
            }

            JPanel panel = new JPanel();

            panel.setLayout(new GridBagLayout());

            JScrollPane jscr = new JScrollPane();
            jscr.getViewport().add(panel);

            JPanel fake = new JPanel();
            fake.setLayout(new GridBagLayout());
            JCheckBox jCheckBox = new JCheckBox();
            jCheckBox.setSelected(selectedTypes.contains(null));
            allCheckBoxes.put(jCheckBox, null);
            JLabel label = new JLabel("");
            fake.add(jCheckBox,
                    new GridBagConstraints(
                            0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.NONE,
                            new Insets(4, 4, 4, 4), 0, 0));
            fake.add(label,
                    new GridBagConstraints(
                            1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.NONE,
                            new Insets(4, 4, 4, 4), 0, 0));

            int i = 0;

            panel.add(fake,
                    new GridBagConstraints(
                            0, i++, 1, 1, 1.0, 1.0,
                            GridBagConstraints.WEST, GridBagConstraints.NONE,
                            new Insets(0, 0, 0, 0), 0, 0));

            for (String type : typeList) {
                fake = new JPanel();
                fake.setLayout(new GridBagLayout());
                jCheckBox = new JCheckBox();
                jCheckBox.setSelected(selectedTypes.contains(type));
                allCheckBoxes.put(jCheckBox, type);
                label = new JLabel(type);

                fake.add(jCheckBox,
                        new GridBagConstraints(
                                0, 0, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                new Insets(4, 4, 4, 4), 0, 0));
                fake.add(label,
                        new GridBagConstraints(
                                1, 0, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                new Insets(4, 4, 4, 4), 0, 0));

                panel.add(fake,
                        new GridBagConstraints(
                                0, i++, 1, 1, 1.0, 1.0,
                                GridBagConstraints.WEST, GridBagConstraints.NONE,
                                new Insets(0, 0, 0, 0), 0, 0));
            }

            setLayout(new BorderLayout());

            add(jscr, BorderLayout.CENTER);

            switchOnInternalListeners();
        } catch (TreetonModelException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
    }

    private void switchOnInternalListeners() {
        for (JCheckBox checkBox : allCheckBoxes.keySet()) {
            checkBox.addItemListener(this);
        }
    }

    private void switchOffInternalListeners() {
        for (JCheckBox checkBox : allCheckBoxes.keySet()) {
            checkBox.removeItemListener(this);
        }
    }


    public void itemStateChanged(ItemEvent e) {
        try {
            if (e.getSource() instanceof JCheckBox) {
                JCheckBox box = (JCheckBox) e.getSource();

                String type = allCheckBoxes.get(box);

                if (type == null) {
                    switchOffInternalListeners();
                    if (box.isSelected()) {
                        for (Map.Entry<JCheckBox, String> entry : allCheckBoxes.entrySet()) {
                            String tp = entry.getValue();
                            if (tp != null) {
                                selectedTypes.add(tp);
                                entry.getKey().setSelected(true);
                            }
                        }
                        selectedTypes.add(null);
                    } else {
                        selectedTypes.clear();
                        for (Map.Entry<JCheckBox, String> entry : allCheckBoxes.entrySet()) {
                            String tp = entry.getValue();
                            if (tp != null) {
                                entry.getKey().setSelected(false);
                            }
                        }
                    }

                    switchOnInternalListeners();
                } else {
                    if (box.isSelected()) {
                        selectedTypes.add(type);
                    } else {
                        selectedTypes.remove(type);
                    }
                }

                notifyListeners();
                updateUI();
            }
        } catch (Exception ex) {
            ExceptionDialog.showExceptionDialog(this, ex);
        }
    }


    public Collection<String> getSelectedTypes() {
        List<String> res = new ArrayList<String>();
        for (String s : selectedTypes) {
            if (s != null) {
                res.add(s);
            }
        }
        return res;
    }

    public void addListener(TypesSelectorPanelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TypesSelectorPanelListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (TypesSelectorPanelListener listener : listeners) {
            listener.selectionChanged();
        }
    }

    public TrnType[] getTrnTypes() throws TreetonModelException {
        List<String> strings = (List<String>) getSelectedTypes();
        TrnType[] res = new TrnType[strings.size()];

        TrnTypeStorage types = trnStorageProvider.getStorage().getTypes();

        for (int i = 0; i < strings.size(); i++) {
            String s = strings.get(i);
            res[i] = types.get(s);
        }

        return res;
    }

    public void deselectAll() throws ContextException {
        selectedTypes.clear();
        allCheckBoxes.clear();
        reFill();
    }

    public void selectAll() {
        switchOffInternalListeners();
        for (Map.Entry<JCheckBox, String> entry : allCheckBoxes.entrySet()) {
            String tp = entry.getValue();
            selectedTypes.add(tp);
            entry.getKey().setSelected(true);
        }
        switchOnInternalListeners();
        notifyListeners();
    }

    public void select(Set<String> types) {
        switchOffInternalListeners();
        for (Map.Entry<JCheckBox, String> entry : allCheckBoxes.entrySet()) {
            String tp = entry.getValue();
            if (types.contains(tp)) {
                selectedTypes.add(tp);
                entry.getKey().setSelected(true);
            }
        }
        switchOnInternalListeners();
        notifyListeners();
    }
}