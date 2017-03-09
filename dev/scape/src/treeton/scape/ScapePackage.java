/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ScapePackage {
    String name;
    HashMap<String, URL> phasesLocations = new HashMap<String, URL>();
    HashMap<String, URL> programLocations = new HashMap<String, URL>();
    Map<String, ScapePackage> descendants = new HashMap<String, ScapePackage>();

    public ScapePackage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
