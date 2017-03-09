/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnview;

import java.util.EventListener;

public interface TrnManipulationListener extends EventListener {
    public void trnClicked(TrnManipulationEvent e);
}
