/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.AnnotationSet;
import gate.Document;
import treeton.core.TreenotationStorage;

public class ScapeRHSObject extends ScapeRHSAction implements ScapeRHSActionResult {
    Object o;

    public ScapeRHSObject(ScapeRule rule, Object o) {
        super(rule);
        this.o = o;
    }

    public ScapeRHSActionResult buildResult() {
        return this;
    }

    public Object getObject() {
        return o;
    }

    public void applyTo(AnnotationSet set, Document doc) {
    }

    public void applyTo(TreenotationStorage storage) {
    }
}
