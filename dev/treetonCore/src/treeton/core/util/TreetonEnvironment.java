/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.HashMap;
import java.util.Map;

public class TreetonEnvironment {
    private static TreetonEnvironment ourInstance = new TreetonEnvironment();
    Map<String, Object> properties = new HashMap<String, Object>();

    private TreetonEnvironment() {
    }

    public static TreetonEnvironment getInstance() {
        return ourInstance;
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
}
