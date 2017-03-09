/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz;

public class TypePriority {
    String sType1;
    String sType2;
    Elector cmpr;

    public TypePriority(String type1, String type2,
                        Elector cmpr) {
        this.sType1 = type1;
        this.sType2 = type2;
        this.cmpr = cmpr;
    }
}
