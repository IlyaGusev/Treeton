/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;
import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ScapeRHSModifyActionResult implements ScapeRHSActionResult {
    Treenotation trn;
    Treenotation newTrn;

    Annotation ann;
    String type;
    List<Map.Entry> changes = new ArrayList<Map.Entry>();

    public ScapeRHSModifyActionResult(Treenotation trn, Treenotation newTrn) {
        this.trn = trn;
        this.newTrn = newTrn;
    }

    public ScapeRHSModifyActionResult(Annotation ann, FeatureMap newFeatureMap, String type) {
        this.ann = ann;

        Iterator it = newFeatureMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            Object o = ann.getFeatures().get(e.getKey());
            if (o == null) {
                if (e.getValue() != null) {
                    changes.add(e);
                }
            } else {
                if (!o.equals(e.getKey())) {
                    changes.add(e);
                }
            }
        }

        for (Object k : ann.getFeatures().keySet()) {
            if (!newFeatureMap.containsKey(k)) {
                changes.add(new Remover(k));
            }
        }

        this.type = type;
    }

    public void applyTo(AnnotationSet set, Document doc) {
        if (!ann.getType().equals(type)) {
            throw new UnsupportedOperationException();
            //AnnotationUtil.changeAnnotationType(ann.getId(),type,set);
        }
        for (Map.Entry entry : changes) {
            if (entry instanceof Remover) {
                ann.getFeatures().remove(entry.getKey());
            } else {
                ann.getFeatures().put(entry.getKey(), entry.getValue());
            }
        }

    }

    public void applyTo(TreenotationStorage storage) {
        //TODO
    }

    public String toString() {
        return "modify";
    }

    private class Remover implements Map.Entry {
        Object k;

        public Remover(Object k) {
            this.k = k;
        }

        public Object getKey() {
            return k;
        }

        public Object getValue() {
            return null;
        }

        public Object setValue(Object value) {
            return null;
        }
    }
}
