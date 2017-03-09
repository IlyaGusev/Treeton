/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import java.util.Iterator;

public interface CharFeatureMap {
    public Object get(String key);

    public Object get(TString key);

    public void put(String key, Object value);

    public void put(TString key, Object value);

    public int size();

    public Iterator valueIterator();

    public void pack();

    public String getFullString();

    public int readInFullString();
}
