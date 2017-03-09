/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.TString;
import treeton.core.Treenotation;
import treeton.core.TreenotationImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;

public class VerseLabelGenerator
        implements TrnLabelGenerator {


    public String generateCaption(Treenotation trn) {
        if (TrnLabelGenerator.DEBUG) {
            return _generateCaption(trn) + "[nView: " + ((TreenotationImpl) trn).getNView() + " id: " + trn.getId() + "]";
        } else {
            return _generateCaption(trn);
        }
    }

    public String _generateCaption(Treenotation trn) {
        TString meter = (TString) trn.get("meter");
        if (meter == null)
            return "unknown meter";
        return meter.toString();
    }

    public void init(TreenotationsContext context) {
    }

    public String generateLabel(Treenotation trn) {
        return "";
    }
}