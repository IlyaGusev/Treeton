/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.AnnotationSet;
import gate.Document;
import treeton.core.TreenotationStorage;

public interface ScapeRHSActionResult {
    void applyTo(AnnotationSet set, Document doc);

    void applyTo(TreenotationStorage storage);
}
