/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

import java.util.Iterator;

public interface ResetableObjectIterator<T> extends Iterator<T> {
    public void reset();

    public int getCurrentClassId();
}
