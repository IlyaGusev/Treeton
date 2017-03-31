/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api;

import java.util.List;

public interface AllBindingsProvider<T> {
    List<T> getBindedObjects();
}
