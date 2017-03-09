/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.TreenotationImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.NumeratedObject;

import java.util.Iterator;

public class DefaultLabelGenerator implements TrnLabelGenerator {
    String[] fNames;
    String captionFeature;
    boolean onlyValues = false;


    public DefaultLabelGenerator() {
        fNames = null;
    }

    public DefaultLabelGenerator(String captionFeature, String[] fNames) {
        this.fNames = fNames;
        this.captionFeature = captionFeature;
    }

    public void init(TreenotationsContext context) {
    }

    public String generateLabel(Treenotation trn) {
        StringBuffer s = new StringBuffer();
        if (fNames == null) {
            TrnType tp = trn.getType();
            Iterator it = trn.numeratedObjectIterator();
            while (it.hasNext()) {
                NumeratedObject no = (NumeratedObject) it.next();
                if (!onlyValues) {
                    try {
                        s.append(tp.getFeatureNameByIndex(no.n));
                    } catch (TreetonModelException e) {
                        s.append("<Problem with model!!!>");
                    }
                    s.append("=");
                }
                s.append(no.o);
                if (it.hasNext()) {
                    s.append("; ");
                }
            }
        } else {
            boolean first = true;
            for (String fName : fNames) {
                Object o = trn.get(fName);
                if (o != null) {
                    if (!first) {
                        s.append("; ");
                    } else {
                        first = false;
                    }

                    if (!onlyValues) {
                        s.append(fName);
                        s.append("=");
                    }
                    s.append(o);
                }
            }
        }
        return s.toString();
    }

    public String generateCaption(Treenotation trn) {
        if (TrnLabelGenerator.DEBUG) {
            return _generateCaption(trn) + "[nView: " + ((TreenotationImpl) trn).getNView() + " id: " + trn.getId() + "]";
        } else {
            return _generateCaption(trn);
        }
    }

    public String _generateCaption(Treenotation trn) {
        if (captionFeature == null) {
            return trn.getType().toString();
        } else {
            Object o = trn.get(captionFeature);
            return o == null ? "" : (onlyValues ? "" : captionFeature + '=') + o;
        }
    }

    public boolean isOnlyValues() {
        return onlyValues;
    }

    public void setOnlyValues(boolean onlyValues) {
        this.onlyValues = onlyValues;
    }
}
