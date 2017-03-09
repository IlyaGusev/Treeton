/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnType;

import java.util.Iterator;

public interface IntFeatureMapStatic {
    public Object get(int key);

    public int getRetainedSize();

    public boolean contains(int key);

    public int getMinFeature();

    public int getMaxFeature();

    public Iterator valueIterator();

    public Iterator numeratedObjectIterator();

    public int size();

    String getString(TrnType tp);

    String getHtmlString(TrnType tp);
}
