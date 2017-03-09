/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.AnnotationSet;
import gate.Document;
import treeton.core.TreenotationStorage;

public class ScapeRHSOrthoActionResult implements ScapeRHSActionResult {
    private String name;

    public ScapeRHSOrthoActionResult(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public void applyTo(AnnotationSet set, Document doc) {
    }

    public void applyTo(TreenotationStorage storage) {
    }
}
