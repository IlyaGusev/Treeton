/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnview;

import treeton.core.Treenotation;

import java.util.ArrayList;
import java.util.Iterator;

public class TrnManipulationEvent {
    private ArrayList selection;
    private Iterator it;
    private Treenotation curTrn;
    private int clickCount;
    private TreenotationViewPanelAbstract source;

    TrnManipulationEvent(TreenotationViewPanelAbstract source, ArrayList _selection, int clickCount) {
        this.source = source;
        this.selection = _selection;
        this.clickCount = clickCount;
    }

    public TreenotationViewPanelAbstract getSource() {
        return source;
    }

    public boolean nextSelectionElement() {
        if (it == null) {
            it = selection.iterator();
        }

        if (it.hasNext()) {
            curTrn = (Treenotation) it.next();
            return true;
        } else {
            it = null;
            curTrn = null;
            return false;
        }
    }

    public Treenotation getSelectedTrn() {
        return curTrn;
    }

    public int getClickCount() {
        return clickCount;
    }
}
