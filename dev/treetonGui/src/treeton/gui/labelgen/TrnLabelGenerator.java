/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.config.context.treenotations.TreenotationsContext;


public interface TrnLabelGenerator {
    public static boolean DEBUG = false;

    void init(TreenotationsContext context);

    String generateLabel(Treenotation trn);

    String generateCaption(Treenotation trn);
}
