/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnview;

import treeton.core.Token;

public interface CoordConverter {
    void setLabelLength(Token trn, int maxLabelLength);

    double getSymbWidth(int i);

    double scaledDistToStart(double from, Token tok);

    double scaledDistToEnd(double from, Token tok);

    double scaledDist(double from, double to);

    double increaseStoragePos(double storagePos, double scaledEps);

    void reset();

    int getInitialCellWidth();

    void setInitialCellWidth(int cellWidth);
}
