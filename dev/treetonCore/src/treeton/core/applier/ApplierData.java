/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.applier;

import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.util.ProgressListener;

public abstract class ApplierData {
    private TreenotationsContext context;

    public TreenotationsContext getContext() {
        return context;
    }

    public void setContext(TreenotationsContext context) {
        this.context = context;
    }

    public abstract byte[] getByteRepresentation() throws ApplierException;

    public abstract int readInFromBytes(byte[] arr, int from, ProgressListener plistener) throws ApplierException;

    public abstract boolean isUpdatable();
}
