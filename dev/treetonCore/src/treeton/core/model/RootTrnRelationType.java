/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model;

public class RootTrnRelationType implements TrnRelationType {
    public String getName() throws TreetonModelException {
        return TrnRelationTypeStorage.root_RELATION_name;
    }

    public int getIndex() throws TreetonModelException {
        return TrnRelationTypeStorage.root_RELATION;
    }

    public boolean isRoot() throws TreetonModelException {
        return true;
    }
}
