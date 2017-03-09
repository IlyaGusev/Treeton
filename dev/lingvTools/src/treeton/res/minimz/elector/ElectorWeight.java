/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz.elector;

import treeton.core.Treenotation;
import treeton.res.minimz.Elector;

/**
 * ElectorWeight
 * <p/>
 * Сравнивает аннотации по атрибуту WEIGHT. Более
 * предпочтительной считается аннотация с большим весом.
 */
public class ElectorWeight extends Elector {
    protected int vote(Treenotation t1, Treenotation t2) {
        int w1 = -1;
        Object ow = t1.get("WEIGHT");
        if (ow != null) {
            w1 = (Integer) ow;
        }

        int w2 = -1;
        ow = t2.get("WEIGHT");
        if (ow != null) {
            w2 = (Integer) ow;
        }

        if (w1 > w2) {
            return win();
        } else if (w1 < w2) {
            return loose();
        }

        return tie();
    }

    protected boolean isInversable() {
        return false;
    }
}
