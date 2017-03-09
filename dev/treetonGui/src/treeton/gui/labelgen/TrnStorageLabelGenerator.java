/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.TreenotationStorage;
import treeton.core.config.context.treenotations.TreenotationsContext;

public interface TrnStorageLabelGenerator {
    void init(TreenotationsContext context);

    String generateLabel(TreenotationStorage storage);
}
