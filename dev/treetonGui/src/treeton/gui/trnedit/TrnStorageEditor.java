/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnedit;

import treeton.core.*;
import treeton.core.config.BasicConfiguration;
import treeton.core.model.TreetonModelException;
import treeton.gui.GuiResources;
import treeton.gui.trnview.TrnManipulationEvent;
import treeton.gui.trnview.TrnManipulationListener;
import treeton.gui.util.ExceptionDialog;
import treeton.gui.util.TypesInfoProvider;
import treeton.gui.util.popupmenu.ScrollableJMenu;
import treeton.gui.util.popupmenu.ScrollablePopupMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class TrnStorageEditor implements TrnManipulationListener {
    //private static final Logger logger = Logger.getLogger(TrnStorageEditor.class);

    protected TrnStorageEditorListener storageEditorListener;
    protected TopologyManager manager;
    protected HashSet<Treenotation> selection = new HashSet<Treenotation>();
    private TypesInfoProvider trnTypesInfoProvider;
    private TypesInfoProvider relTypesInfoProvider;
    private JPopupMenu menu;

    public TrnStorageEditor(TypesInfoProvider trnTypesInfoProvider, TypesInfoProvider relTypesInfoProvider, TrnStorageEditorListener storageEditorListener) {
        this.trnTypesInfoProvider = trnTypesInfoProvider;
        this.relTypesInfoProvider = relTypesInfoProvider;
        manager = new TopologyManager();
        manager.setRemoveEmpty(false);
        this.storageEditorListener = storageEditorListener;
    }


    public void trnClicked(TrnManipulationEvent e) {
        selection.clear();
        while (e.nextSelectionElement()) {
            selection.add(e.getSelectedTrn());
        }

        if (selection.size() == 1 && e.getClickCount() == 2) {
            storageEditorListener.attrEditRequest(selection.iterator().next());
        }
    }

    protected JPopupMenu getActionsMenu(final TreenotationStorage storage, final int intervalStart, final int intervalEnd) {
        ScrollablePopupMenu menu = new ScrollablePopupMenu();

        if (manager.isConnected()) {
            if (selection.size() == 1) {
                final TreenotationImpl trn = (TreenotationImpl) selection.iterator().next();

                if (manager.mayBeAggregated(trn)) {
                    ScrollableJMenu aggregateStrongItem = new ScrollableJMenu(BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.aggregate.strong"));
                    aggregateStrongItem.setIcon(GuiResources.iconAggregateStrong);
                    ScrollableJMenu aggregateWeakItem = new ScrollableJMenu(BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.aggregate.weak"));
                    aggregateWeakItem.setIcon(GuiResources.iconAggregateWeak);
                    menu.add(aggregateStrongItem);
                    menu.add(aggregateWeakItem);

                    List<String> types = new ArrayList<String>(trnTypesInfoProvider.getSelectedTypes());
                    Collections.sort(types);

                    for (final String type : types) {
                        JMenuItem typeItem = new JMenuItem();

                        typeItem.setAction(
                                new AbstractAction(type) {
                                    public void actionPerformed(ActionEvent e) {
                                        try {
                                            manager.aggregate(trn, storage.getTypes().get(type), true);
                                            storageEditorListener.storageChanged();
                                        } catch (Exception e1) {
                                            ExceptionDialog.showExceptionDialog(null, e1);
                                        }
                                    }
                                }
                        );

                        aggregateStrongItem.add(typeItem);

                        typeItem = new JMenuItem();

                        typeItem.setAction(
                                new AbstractAction(type) {
                                    public void actionPerformed(ActionEvent e) {
                                        try {
                                            manager.aggregate(trn, storage.getTypes().get(type), false);
                                            storageEditorListener.storageChanged();
                                        } catch (Exception e1) {
                                            ExceptionDialog.showExceptionDialog(null, e1);
                                        }
                                    }
                                }
                        );

                        aggregateWeakItem.add(typeItem);
                    }
                }

                if (manager.mayBeRemovedIncomingLink(trn)) {
                    JMenuItem removeLinkItem = new JMenuItem();
                    menu.add(removeLinkItem);

                    removeLinkItem.setAction(
                            new AbstractAction(BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.removeLink")) {
                                public void actionPerformed(ActionEvent e) {
                                    try {
                                        manager.removeIncomingLink(trn);
                                        storageEditorListener.storageChanged();
                                    } catch (Exception e1) {
                                        ExceptionDialog.showExceptionDialog(null, e1);
                                    }
                                }
                            }
                    );
                    removeLinkItem.setIcon(GuiResources.iconRemoveLink);
                }

                if (manager.mayBeRemovedTreenotation(trn)) {
                    JMenuItem removeTrnItem = new JMenuItem();
                    menu.add(removeTrnItem);

                    removeTrnItem.setAction(
                            new AbstractAction(BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.removeFromContext")) {
                                public void actionPerformed(ActionEvent e) {
                                    try {
                                        manager.removeTreenotation(trn);
                                        storageEditorListener.storageChanged();
                                    } catch (Exception e1) {
                                        ExceptionDialog.showExceptionDialog(null, e1);
                                    }
                                }
                            }
                    );
                    removeTrnItem.setIcon(GuiResources.iconRemoveAggregate);
                }
            } else if (selection.size() == 2) {
                Iterator<Treenotation> it = selection.iterator();
                final TreenotationImpl trn1 = (TreenotationImpl) it.next();
                final TreenotationImpl trn2 = (TreenotationImpl) it.next();

                if (manager.mayBeLinked(trn1, trn2)) {
                    ScrollableJMenu linkItem = new ScrollableJMenu(trn1.getId() + " -" + BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.link") + "-> " + trn2.getId());
                    linkItem.setIcon(GuiResources.iconLink);
                    menu.add(linkItem);

                    List<String> types = new ArrayList<String>(relTypesInfoProvider.getSelectedTypes());
                    Collections.sort(types);

                    for (final String type : types) {
                        JMenuItem typeItem = new JMenuItem();

                        typeItem.setAction(
                                new AbstractAction(type) {
                                    public void actionPerformed(ActionEvent e) {
                                        try {
                                            manager.link(trn1, trn2, storage.getRelations().get(type));
                                            storageEditorListener.storageChanged();
                                        } catch (Exception e1) {
                                            ExceptionDialog.showExceptionDialog(null, e1);
                                        }
                                    }
                                }
                        );

                        linkItem.add(typeItem);
                    }
                }

                if (manager.mayBeLinked(trn2, trn1)) {
                    ScrollableJMenu linkItem = new ScrollableJMenu(trn2.getId() + " -" + BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.link") + "-> " + trn1.getId());
                    linkItem.setIcon(GuiResources.iconLink);
                    menu.add(linkItem);

                    List<String> types = new ArrayList<String>(relTypesInfoProvider.getSelectedTypes());
                    Collections.sort(types);

                    for (final String type : types) {
                        JMenuItem typeItem = new JMenuItem();

                        typeItem.setAction(
                                new AbstractAction(type) {
                                    public void actionPerformed(ActionEvent e) {
                                        try {
                                            manager.link(trn2, trn1, storage.getRelations().get(type));
                                            storageEditorListener.storageChanged();
                                        } catch (Exception e1) {
                                            ExceptionDialog.showExceptionDialog(null, e1);
                                        }
                                    }
                                }
                        );

                        linkItem.add(typeItem);
                    }
                }

                if (manager.mayBeAddedStrong(trn1, trn2)) {
                    JMenuItem addStrongItem = new JMenuItem();
                    menu.add(addStrongItem);

                    addStrongItem.setAction(
                            new AbstractAction(trn1.getId() + " -" + BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.absorbStrong") + "-> " + trn2.getId()) {
                                public void actionPerformed(ActionEvent e) {
                                    try {
                                        manager.addMemberStrong(trn1, trn2);
                                        storageEditorListener.storageChanged();
                                    } catch (Exception e1) {
                                        ExceptionDialog.showExceptionDialog(null, e1);
                                    }
                                }
                            }
                    );
                    addStrongItem.setIcon(GuiResources.iconAddMemberStrong);
                }

                if (manager.mayBeAddedStrong(trn2, trn1)) {
                    JMenuItem addStrongItem = new JMenuItem();
                    menu.add(addStrongItem);

                    addStrongItem.setAction(
                            new AbstractAction(trn2.getId() + " -" + BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.absorbStrong") + "-> " + trn1.getId()) {
                                public void actionPerformed(ActionEvent e) {
                                    try {
                                        manager.addMemberStrong(trn2, trn1);
                                        storageEditorListener.storageChanged();
                                    } catch (Exception e1) {
                                        ExceptionDialog.showExceptionDialog(null, e1);
                                    }
                                }
                            }
                    );
                    addStrongItem.setIcon(GuiResources.iconAddMemberStrong);
                }

                if (manager.mayBeAddedWeak(trn1, trn2)) {
                    JMenuItem addWeakItem = new JMenuItem();
                    menu.add(addWeakItem);

                    addWeakItem.setAction(
                            new AbstractAction(trn1.getId() + " -" + BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.absorbWeak") + "-> " + trn2.getId()) {
                                public void actionPerformed(ActionEvent e) {
                                    try {
                                        manager.addMemberWeak(trn1, trn2);
                                        storageEditorListener.storageChanged();
                                    } catch (Exception e1) {
                                        ExceptionDialog.showExceptionDialog(null, e1);
                                    }
                                }
                            }
                    );
                    addWeakItem.setIcon(GuiResources.iconAddMemberWeak);
                }

                if (manager.mayBeAddedWeak(trn2, trn1)) {
                    JMenuItem addWeakItem = new JMenuItem();
                    menu.add(addWeakItem);

                    addWeakItem.setAction(
                            new AbstractAction(trn2.getId() + " -" + BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.absorbWeak") + "-> " + trn1.getId()) {
                                public void actionPerformed(ActionEvent e) {
                                    try {
                                        manager.addMemberWeak(trn2, trn1);
                                        storageEditorListener.storageChanged();
                                    } catch (Exception e1) {
                                        ExceptionDialog.showExceptionDialog(null, e1);
                                    }
                                }
                            }
                    );
                    addWeakItem.setIcon(GuiResources.iconAddMemberWeak);
                }
            }
        }

        if (selection.size() == 1) {
            Iterator<Treenotation> it = selection.iterator();
            final TreenotationImpl trn = (TreenotationImpl) it.next();
            if (!trn.isLocked() && trn.getContext() == null) {
                JMenuItem editTrnItem = new JMenuItem();
                menu.add(editTrnItem);

                editTrnItem.setAction(
                        new AbstractAction(BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.editTrn")) {
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    if (manager.isConnected())
                                        manager.disconnect();
                                    manager.connect(trn);
                                    storageEditorListener.storageChanged();
                                } catch (Exception e1) {
                                    ExceptionDialog.showExceptionDialog(null, e1);
                                }
                            }
                        }
                );
                editTrnItem.setIcon(GuiResources.iconEditFile);
            }
        }

        if (selection.size() > 0) {
            JMenuItem completelyRemoveTrnItem = new JMenuItem();
            menu.add(completelyRemoveTrnItem);

            completelyRemoveTrnItem.setAction(
                    new AbstractAction(BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.removeCompletely")) {
                        public void actionPerformed(ActionEvent e) {
                            try {
                                for (Treenotation trn : selection) {
                                    if (!trn.getType().isTokenType()) {
                                        if (trn == manager.getTarget()) {
                                            manager.disconnect();
                                        }

                                        storage.remove(trn);
                                    }
                                }
                                storageEditorListener.storageChanged();
                            } catch (Exception e1) {
                                ExceptionDialog.showExceptionDialog(null, e1);
                            }
                        }
                    }
            );

            completelyRemoveTrnItem.setIcon(GuiResources.iconDeleteSyntaxRuleUsage);
        }

        if (intervalStart >= 0) {
            ScrollableJMenu addOpenedTrnItem = new ScrollableJMenu(BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.addOpenedTrn"));
            addOpenedTrnItem.setIcon(GuiResources.iconPlus);
            ScrollableJMenu addLockedTrnItem = new ScrollableJMenu(BasicConfiguration.localize("GuiBundle", "TrnStorageEditor.addLockedTrn"));
            addLockedTrnItem.setIcon(GuiResources.iconPlus);
            menu.add(addOpenedTrnItem);
            menu.add(addLockedTrnItem);

            List<String> types = new ArrayList<String>(trnTypesInfoProvider.getSelectedTypes());
            Collections.sort(types);

            for (final String type : types) {
                JMenuItem typeItem = new JMenuItem();

                try {
                    if (storage.getTypes().get(type).isTokenType())
                        continue;
                } catch (TreetonModelException e) {
                    ExceptionDialog.showExceptionDialog(null, e);
                }

                typeItem.setAction(
                        new AbstractAction(type) {
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    Token min = defineTokenBySelectionStart(storage, intervalStart);
                                    Token max = defineTokenBySelectionEnd(storage, intervalEnd);


                                    storage.add(TreetonFactory.newSyntaxTreenotation(storage, min, max, storage.getTypes().get(type)));
                                    storageEditorListener.storageChanged();
                                } catch (Exception e1) {
                                    ExceptionDialog.showExceptionDialog(null, e1);
                                }
                            }
                        }
                );

                addOpenedTrnItem.add(typeItem);

                typeItem = new JMenuItem();

                typeItem.setAction(
                        new AbstractAction(type) {
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    Token min = defineTokenBySelectionStart(storage, intervalStart);
                                    Token max = defineTokenBySelectionEnd(storage, intervalEnd);

                                    storage.add(TreetonFactory.newTreenotation(min, max, storage.getTypes().get(type)));
                                    storageEditorListener.storageChanged();
                                } catch (Exception e1) {
                                    ExceptionDialog.showExceptionDialog(null, e1);
                                }
                            }
                        }
                );

                addLockedTrnItem.add(typeItem);
            }
        }

        return menu;
    }

    protected Token defineTokenBySelectionStart(TreenotationStorage storage, int offs) {
        Token tok = storage.firstToken();
        while (true) {
            int endoffs = tok.getEndNumerator() / tok.getEndDenominator();
            if (offs < endoffs) {
                break;
            }
            tok = tok.getNextToken();
        }
        int start = tok.getStartNumerator() / tok.getStartDenominator();
        Token afterSplit;
        if (start == offs) {
            afterSplit = tok;
        } else {
            afterSplit = storage.splitToken(tok, offs, 1, tok.getText().substring(0, offs - start), tok.getText().substring(offs - start));
            storageEditorListener.resetStorageView();
        }
        return afterSplit;
    }

    protected Token defineTokenBySelectionEnd(TreenotationStorage storage, int offs) {
        Token tok = storage.firstToken();
        while (true) {
            int endoffs = tok.getEndNumerator() / tok.getEndDenominator();
            if (offs < endoffs) {
                break;
            }
            tok = tok.getNextToken();
        }
        int start = tok.getStartNumerator() / tok.getStartDenominator();
        int end = tok.getEndNumerator() / tok.getEndDenominator();
        if (offs != end - 1) {
            storage.splitToken(tok, offs + 1, 1, tok.getText().substring(0, offs - start), tok.getText().substring(offs - start));
            storageEditorListener.resetStorageView();
        }
        return tok;
    }

    public void setCheckCoverageMode(boolean checkCoverage) {
        manager.setCheckCoverage(checkCoverage);
    }

    public MouseAdapter createMouseListener(final Component invoker) {
        return new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON3)
                    return;
                menu = getActionsMenu(storageEditorListener.getStorage(), storageEditorListener.getSelectedIntervalStart(), storageEditorListener.getSelectedIntervalEnd());
                if (menu.getComponentCount() > 0) {
                    menu.show(invoker, e.getX(), e.getY());
                }
            }
        };
    }

    public void close() {
        manager.disconnect();
    }

    public boolean isMenuShowing() {
        return menu != null && menu.isVisible();
    }
}

