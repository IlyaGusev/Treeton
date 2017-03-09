/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.dict;

import treeton.core.model.TrnType;

import java.util.Iterator;

public interface Dictionary {
    public Iterator<String> lemmaIterator();

    public ResultSet findInDict(String s);

    TrnType getType();
}
