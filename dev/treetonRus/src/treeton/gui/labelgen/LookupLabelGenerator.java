/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.TreenotationImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;

public class LookupLabelGenerator implements TrnLabelGenerator {
    int majortype_feature;
    int minortype_feature;
    GrammLabelGenerator mlabel;

    public LookupLabelGenerator() {
    }

    public void init(TreenotationsContext context) {
        majortype_feature = -1;
        minortype_feature = -1;
        try {
            majortype_feature = context.getType("Lookup").getFeatureIndex("majorType");
            minortype_feature = context.getType("Lookup").getFeatureIndex("minorType");
        } catch (TreetonModelException e) {
            //do nothing
        }
        mlabel = new GrammLabelGenerator(false, false, false, false);
    }

    public String generateLabel(Treenotation trn) {
        if (TrnLabelGenerator.DEBUG) {
            return _generateLabel(trn) + "<nView: " + ((TreenotationImpl) trn).getNView() + " id: " + trn.getId() + ">";
        } else {
            return _generateLabel(trn);
        }
    }

    public String _generateLabel(Treenotation trn) {
        return mlabel.generateLabel(trn);
    }

    public String generateCaption(Treenotation trn) {
        if (TrnLabelGenerator.DEBUG) {
            return _generateCaption(trn) + "[nView: " + ((TreenotationImpl) trn).getNView() + " id: " + trn.getId() + "]";
        } else {
            return _generateCaption(trn);
        }
    }

    public String _generateCaption(Treenotation trn) {
        StringBuffer buf = new StringBuffer();
        Object o = trn.get(majortype_feature);
        if (o != null) {
            buf.append(o);
        }
        if (minortype_feature >= 0) {
            o = trn.get(minortype_feature);
            if (o != null) {
                buf.append(" : ");
                buf.append(o);
            }
        }
        return buf.toString();
    }
}
