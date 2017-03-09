/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnview;

import treeton.core.Token;

public class CoordConverterNonScaledImpl implements CoordConverter {
    int cellWidth;

    public void setLabelLength(Token trn, int maxLabelLength) {
        // do nothing
    }

    public double getSymbWidth(int i) {
        return 1;
    }

    public double scaledDistToStart(double from, Token tok) {
        return tok.toDouble() - from;
    }

    public double scaledDistToEnd(double from, Token tok) {
        return tok.endToDouble() - from;
    }

    public double scaledDist(double from, double to) {
        return to - from;
    }

    public double increaseStoragePos(double storagePos, double scaledEps) {
        return storagePos + scaledEps;
    }

    public void reset() {
        // do nothing
    }

    public int getInitialCellWidth() {
        return cellWidth;
    }

    public void setInitialCellWidth(int cellWidth) {
        this.cellWidth = cellWidth;
    }
}
