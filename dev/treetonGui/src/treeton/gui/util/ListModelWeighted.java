/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;

public interface ListModelWeighted extends ListModel {
    double getWeight(Object node);
}
