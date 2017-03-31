/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

public interface Modification<T> {
    void apply(Output<T> output) throws Exception;
}
