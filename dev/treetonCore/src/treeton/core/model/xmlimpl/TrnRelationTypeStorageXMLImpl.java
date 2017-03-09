/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.xmlimpl;

import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnRelationType;
import treeton.core.model.TrnRelationTypeStorage;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TrnRelationTypeStorageXMLImpl extends TrnRelationTypeStorage {
    private TreenotationsContext context;
    private boolean autofill = true;
    private int lastIndex = -1;
    private HashMap<String, TrnRelationTypeXMLImpl> types = new HashMap<String, TrnRelationTypeXMLImpl>();
    private ArrayList<TrnRelationTypeXMLImpl> typesByIndex = new ArrayList<TrnRelationTypeXMLImpl>();

    public TrnRelationTypeStorageXMLImpl(TreenotationsContext context, TrnRelationTypeStorageXMLImpl parent) {
        this.context = context;
        if (parent != null) {
            lastIndex = parent.lastIndex;
            types = (HashMap<String, TrnRelationTypeXMLImpl>) parent.types.clone();
            typesByIndex = (ArrayList<TrnRelationTypeXMLImpl>) parent.typesByIndex.clone();
        }
    }

    public TrnRelationTypeStorageXMLImpl(TreenotationsContext tc, TrnRelationType[] t) throws TreetonModelException {
        context = tc;
        for (int i = 0; i < t.length; i++) {
            TrnRelationType type = t[i];
            if (!(type instanceof TrnRelationTypeXMLImpl)) throw new TreetonModelException("Not Xml impl");
            TrnRelationTypeXMLImpl nt = (TrnRelationTypeXMLImpl) type;
            typesByIndex.add(nt);
            types.put(nt.getName(), nt);
        }
        lastIndex = typesByIndex.size() - 1;
    }

    public TrnRelationType _get(String s) {
        TrnRelationType t;
        t = types.get(s);

        if (autofill) {
            if (t == null) {
                register(s, false);
                return types.get(s);
            }
        }

        return t;
    }

    public TrnRelationType _get(int i) {
        TrnRelationType t;
        t = typesByIndex.get(i - numberOfSystemTypes);
        return t;
    }

    public TrnRelationTypeXMLImpl register(String s, boolean isRoot) {
        TrnRelationTypeXMLImpl t;
        t = types.get(s);
        if (t != null) {
            throw new IllegalArgumentException(s);
        }
        float[] hsb = new float[3];
        hsb[0] = (float) Math.random() * 0.8f + 0.1f;
        hsb[1] = (float) Math.random() / 2f + 0.4f;
        hsb[2] = (float) Math.random() / 2f + 0.4f;
        t = new TrnRelationTypeXMLImpl(this, s, (++lastIndex + numberOfSystemTypes), new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])), isRoot);
        types.put(s, t);
        typesByIndex.add(t);
        return t;
    }

    public TrnRelationTypeXMLImpl register(String s, Color c, boolean isRoot) {
        TrnRelationTypeXMLImpl t;
        t = types.get(s);
        if (t != null) {
            throw new IllegalArgumentException(s);
        }
        t = new TrnRelationTypeXMLImpl(this, s, ++lastIndex, c, isRoot);
        types.put(s, t);
        typesByIndex.add(t);
        return t;
    }

    public void _fillInTypes(TrnRelationType[] arr, int offset) throws TreetonModelException {
        for (TrnRelationType rel : types.values()) {
            arr[offset++] = rel;
        }
    }


    public int _size() {
        return lastIndex + 1;
    }

    public int hashCode() {
        return context.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof TrnRelationTypeStorageXMLImpl && context.equals(((TrnRelationTypeStorageXMLImpl) obj).context);
    }
}
