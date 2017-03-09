/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.dict;

import treeton.core.IntMapper;
import treeton.core.model.TrnType;

import java.net.URI;

public interface ResultSet {
    public boolean next();

    public URI getRecordUri();

    public Object getValueOf(String keyName);

    public Object getValueOf(int key);

    public TrnType getType();

    public IntMapper getMapper();
}
