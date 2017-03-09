/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Node;
import treeton.core.Token;
import treeton.core.Treenotation;

public abstract class ScapeBinding implements Comparable {
    private static int ID = 0;
    long label;
    private int id;

    ScapeBinding() {
        label = -1;
        id = ID++;
    }

    public static String getText(Document doc, Annotation ann) {
        if (doc == null || ann == null) return null;
        try {
            return doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String getText(Document doc, AnnotationSet annSet) {
        if (doc == null || annSet == null) return null;
        try {
            return doc.getContent().getContent(annSet.firstNode().getOffset(), annSet.lastNode().getOffset()).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public int compareTo(Object o) {
        if (o instanceof ScapeBinding) {
            ScapeBinding anotherBinding = (ScapeBinding) o;
            if (id < anotherBinding.id) {
                return -1;
            } else if (id > anotherBinding.id) {
                return 1;
            }
            return 0;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int getId() {
        return id;
    }

    public abstract Treenotation getTrn(int n);

    public abstract Annotation getAnn(int n);

    public abstract Token getStartToken();

    public abstract Token getEndToken();

    public abstract Node getStartNode();

    public abstract Node getEndNode();

    public abstract int getSize();

    public abstract Treenotation[] toTrnArray();

    public abstract Annotation[] toAnnArray();
}
