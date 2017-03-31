/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

public interface Output<T> {
    void flush();

    void notifyAdd(T object);
    void notifyRemove(T object);
}
