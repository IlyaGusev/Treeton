/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.TreenotationImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;

public class SyllableLabelGenerator
        implements TrnLabelGenerator {


    public String generateCaption(Treenotation trn) {
        if (TrnLabelGenerator.DEBUG) {
            return _generateCaption(trn) + "[nView: " + ((TreenotationImpl) trn).getNView() + " id: " + trn.getId() + "]";
        } else {
            return _generateCaption(trn);
        }
    }

    public String _generateCaption(Treenotation trn) {
        Integer i = (Integer) trn.get("accent");
        if (i == null)
            return trn.getText();
        return i == 1 ? "/" : i == 2 ? "//" : "";
    }

    public void init(TreenotationsContext context) {
    }

    public String generateLabel(Treenotation trn) {
        //return trn.getText();
        return "";
    }
}
