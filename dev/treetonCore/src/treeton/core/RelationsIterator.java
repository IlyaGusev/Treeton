/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnRelationType;

public interface RelationsIterator {
    public Treenotation getHost();

    public Treenotation getSlave();

    public TrnRelationType getType();

    boolean next();
}
