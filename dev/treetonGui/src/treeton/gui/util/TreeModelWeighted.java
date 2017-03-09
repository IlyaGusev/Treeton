/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.tree.TreeModel;

public interface TreeModelWeighted extends TreeModel {
    double getWeight(Object node);
}
