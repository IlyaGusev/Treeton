/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnedit;

import treeton.core.IntFeatureMap;

public interface IntFeatureMapEditorListener {
    public void imapEdited(IntFeatureMap source, IntFeatureMap attrs, IntFeatureMap inducedAttrs);
}
