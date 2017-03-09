/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz.elector;

import treeton.core.Treenotation;
import treeton.res.minimz.Elector;

/**
 * ElectorFirstBoundary
 * <p/>
 * Более приоритетной считается первая аннотация, но только
 * в том случае если вес второй меньше safeBoundary. Если
 * вес второй аннотации >= safeBoundary, то приоритетной
 * считается вторая.
 * <p/>
 * см. ElectorSecondBoundary
 */
public class ElectorFirstBoundary extends Elector {

    public int safeBoundary;

    public ElectorFirstBoundary(int safeBoundary) {
        this.safeBoundary = safeBoundary;

    }

    protected int vote(Treenotation t1, Treenotation t2) {
        int w = -1;
        Object ow = t2.get("WEIGHT");
        if (ow != null) {
            w = (Integer) ow;
        }

        if (w < safeBoundary) {
            return win();
        } else {
            return loose();
        }
    }

    protected boolean isInversable() {
        return true;
    }
}
