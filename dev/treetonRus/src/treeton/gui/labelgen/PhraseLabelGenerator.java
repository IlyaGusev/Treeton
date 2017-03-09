/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.TreenotationImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;

public class PhraseLabelGenerator implements TrnLabelGenerator {
    int TYPE_feature;
    GrammLabelGenerator mlabel;

    public PhraseLabelGenerator() {
    }

    public void init(TreenotationsContext context) {
        TYPE_feature = -1;
        try {
            TYPE_feature = context.getType("Phrase").getFeatureIndex("TYPE");
        } catch (TreetonModelException e) {
            //do nothing
        }
        mlabel = new GrammLabelGenerator(false, false, false, false);
    }

    public String generateLabel(Treenotation trn) {
        if (TrnLabelGenerator.DEBUG) {
            return _generateLabel(trn) + "[nView: " + ((TreenotationImpl) trn).getNView() + " id: " + trn.getId() + "]";
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
        Object o = trn.get(TYPE_feature);
        if (o != null) {
            buf.append(o);
        }
        return buf.toString();
    }
}
