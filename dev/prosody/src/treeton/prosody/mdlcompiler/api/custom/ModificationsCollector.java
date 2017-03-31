/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

public interface ModificationsCollector<T> {
    void add(Modification<T> modification);
    void apply(Output<T> output) throws Exception;

    void clear();
}
