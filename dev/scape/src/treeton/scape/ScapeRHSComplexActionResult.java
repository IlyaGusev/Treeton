/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.AnnotationSet;
import gate.Document;
import treeton.core.TreenotationStorage;

import java.util.ArrayList;
import java.util.List;

public class ScapeRHSComplexActionResult implements ScapeRHSActionResult {
    List<ScapeRHSActionResult> actions;

    public void applyTo(AnnotationSet set, Document doc) {
        if (actions == null)
            return;
        for (ScapeRHSActionResult result : actions) {
            result.applyTo(set, doc);
        }
    }

    public void applyTo(TreenotationStorage storage) {
        if (actions == null)
            return;
        for (ScapeRHSActionResult result : actions) {
            result.applyTo(storage);
        }
    }

    public void add(ScapeRHSCreateActionResult actionResult) {
        if (actions == null)
            actions = new ArrayList<ScapeRHSActionResult>();
        actions.add(actionResult);
    }
}
