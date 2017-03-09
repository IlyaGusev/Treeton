/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.dict;

import treeton.core.IntFeatureMap;

public class DefaultInputEntry implements InputEntry {
    private String uri;
    private IntFeatureMap attrs;

    public DefaultInputEntry(String uri, IntFeatureMap attrs) {
        this.uri = uri;
        this.attrs = attrs;
    }

    public String getUri() {
        return uri;
    }

    public IntFeatureMap getAttrs() {
        return attrs;
    }
}
