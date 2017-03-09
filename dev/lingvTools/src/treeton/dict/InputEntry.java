/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.dict;

import treeton.core.IntFeatureMap;

public interface InputEntry {
    String getUri();

    IntFeatureMap getAttrs();
}
