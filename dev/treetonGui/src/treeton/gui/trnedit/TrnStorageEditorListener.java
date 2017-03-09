/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnedit;

import treeton.core.Treenotation;

public interface TrnStorageEditorListener extends TreenotationStorageProvider {
    void storageChanged();

    void attrEditRequest(Treenotation trn);

    int getSelectedIntervalStart();

    int getSelectedIntervalEnd();

    void resetStorageView();
}
