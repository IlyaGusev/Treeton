/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnview;

import treeton.core.Token;

public class CoordConverterMaxLabelLengthImpl extends CoordConverterImpl {
    private int cellWidth;

    protected double getLength(Token tok, int maxLabelLength) {
        double len = (double) tok.getEndNumerator() / (double) tok.getEndDenominator() - (double) tok.getStartNumerator() / (double) tok.getStartDenominator();
        return Math.max((double) maxLabelLength / (double) cellWidth, len);
    }

    public int getInitialCellWidth() {
        return cellWidth;
    }

    public void setInitialCellWidth(int cellWidth) {
        this.cellWidth = cellWidth;
    }
}
