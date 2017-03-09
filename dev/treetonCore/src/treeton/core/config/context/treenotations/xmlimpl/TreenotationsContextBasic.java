/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.treenotations.xmlimpl;

import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.*;

public abstract class TreenotationsContextBasic implements TreenotationsContext {
    protected TrnTypeStorage types;
    protected TrnRelationTypeStorage relations;

    public TrnTypeStorage getTypes() throws TreetonModelException {
        if (types == null)
            activate();
        return types;
    }

    public TrnType getType(String s) throws TreetonModelException {
        if (types == null) {
            activate();
        }
        return types.get(s);
    }

    public TrnType getType(int i) throws TreetonModelException {
        if (types == null)
            activate();
        return types.get(i);
    }

    public TrnRelationType getRelType(String s) throws TreetonModelException {
        if (relations == null)
            activate();
        return relations.get(s);
    }

    public TrnRelationType getRelType(int i) throws TreetonModelException {
        if (relations == null)
            activate();
        return relations.get(i);
    }

    public TrnRelationTypeStorage getRelations() throws TreetonModelException {
        if (relations == null)
            activate();
        return relations;
    }

    protected abstract void activate() throws TreetonModelException;
}
