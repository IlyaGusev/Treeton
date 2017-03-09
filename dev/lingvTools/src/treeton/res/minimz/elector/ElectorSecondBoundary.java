/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz.elector;

import treeton.core.Treenotation;
import treeton.res.minimz.Elector;

/**
 * ElectorSecondBoundary
 * <p/>
 * Более приоритетной считается вторая аннотация, но только
 * в том случае если вес первой меньше safeBoundary. Если
 * вес первой аннотации >= safeBoundary, то приоритетной
 * считается первая.
 * <p/>
 * см. ElectorFirstBoundary
 */
public class ElectorSecondBoundary extends Elector {

    public int safeBoundary;

    public ElectorSecondBoundary(int safeBoundary) {
        this.safeBoundary = safeBoundary;

    }

    protected int vote(Treenotation t1, Treenotation t2) {
        int w = -1;
        Object ow = t1.get("WEIGHT");
        if (ow != null) {
            w = (Integer) ow;
        }

        if (w < safeBoundary) {
            return loose();
        } else {
            return win();
        }
    }

    protected boolean isInversable() {
        return true;
    }
}
