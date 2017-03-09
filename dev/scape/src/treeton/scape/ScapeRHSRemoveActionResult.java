/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;

public class ScapeRHSRemoveActionResult implements ScapeRHSActionResult {
    Annotation[] anns;
    Treenotation[] trns;

    public ScapeRHSRemoveActionResult(Annotation[] anns) {
        this.anns = anns;
    }

    public ScapeRHSRemoveActionResult(Treenotation[] trns) {
        this.trns = trns;
    }

    public void applyTo(AnnotationSet set, Document doc) {
        for (Annotation ann : anns) {
            set.remove(ann);
        }
    }

    public void applyTo(TreenotationStorage storage) {
        for (Treenotation trn : trns) {
            storage.remove(trn);
        }
    }
}
