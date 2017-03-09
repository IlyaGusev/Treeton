/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.config.context.treenotations.TreenotationsContext;

public class AbbyyLabelGenerator implements TrnLabelGenerator {
    public void init(TreenotationsContext context) {
    }

    public String generateLabel(Treenotation trn) {
        return "";
    }

    public String generateCaption(Treenotation trn) {
        Object o = trn.get("name");
        return o == null ? "" : o.toString();
    }
}
