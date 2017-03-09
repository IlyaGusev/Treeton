/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.applier;

import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;

import java.util.ArrayList;

public abstract class Applier {
    private ApplierData data;
    private ArrayList<ApplierChangeListener> changeListeners = new ArrayList<ApplierChangeListener>();

    public boolean isUpdatable() {
        return data.isUpdatable();
    }

    public final ApplierData getApplierData() {
        return data;
    }

    public final void setApplierData(ApplierData data) {
        this.data = data;
    }

    public abstract void init() throws ResourceInstantiationException;

    public abstract void applyTo(TextMarkingStorage storage) throws ExecutionException;

    public final void dataChanged() {
        for (Object changeListener : changeListeners) {
            ((ApplierChangeListener) changeListener).applierDataChanged();
        }
    }

    public final void addChangeListener(ApplierChangeListener changeListener) {
        if (changeListeners.indexOf(changeListener) < 0) {
            changeListeners.add(changeListener);
        }
    }

    public final void removeChangeListener(ApplierChangeListener changeListener) {
        changeListeners.remove(changeListener);
    }
}
