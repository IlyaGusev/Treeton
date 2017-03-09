/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;
import gate.Node;
import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;

public class ScapeRHSCreateActionResult implements ScapeRHSActionResult {
    Node startNode;
    Node endNode;
    FeatureMap fm;
    String type;
    Treenotation trn;

    public ScapeRHSCreateActionResult(Node startNode, Node endNode, FeatureMap fm, String type) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.fm = fm;
        this.type = type;
    }

    public ScapeRHSCreateActionResult(Treenotation trn) {
        this.trn = trn;
    }

    public void applyTo(AnnotationSet set, Document doc) {
        set.add(startNode, endNode, type, fm);
    }

    public void applyTo(TreenotationStorage storage) {
        storage.add(trn);
    }
}
