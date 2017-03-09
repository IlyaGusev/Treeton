/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.Treenotation;

public abstract class Suppositor {
    public abstract boolean next();

    public abstract void reset();

    public abstract Treenotation getTreenotation();

    public abstract Treenotation getSourceTrn();

    public abstract void finish();
}
