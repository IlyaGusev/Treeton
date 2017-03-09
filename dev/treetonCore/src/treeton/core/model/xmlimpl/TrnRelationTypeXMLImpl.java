/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.xmlimpl;

import treeton.core.model.TrnRelationType;
import treeton.core.model.TrnRelationTypeStorage;

import java.awt.*;

public class TrnRelationTypeXMLImpl implements TrnRelationType {
    private String name;
    private int index;
    private Color clr;
    private boolean isRoot;
    private TrnRelationTypeStorage storage;

    public TrnRelationTypeXMLImpl(TrnRelationTypeStorage storage, String s, int i, Color c, boolean isRoot) {
        this.storage = storage;
        name = s;
        index = i;
        clr = c;
        this.isRoot = isRoot;
    }

    public TrnRelationTypeStorage getStorage() {
        return storage;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public Color getColor() {
        return clr;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public String toString() {
        return name;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrnRelationTypeXMLImpl that = (TrnRelationTypeXMLImpl) o;

        return name.equals(that.name) && storage.equals(that.storage);

    }

    public int hashCode() {
        int result;
        result = name.hashCode();
        result = 31 * result + storage.hashCode();
        return result;
    }
}
