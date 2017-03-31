/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.fsm;

public interface Binding extends Comparable<Binding> {
    String getName();
    int getIndex();
}
