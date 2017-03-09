/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz.elector;

import treeton.core.Treenotation;
import treeton.res.minimz.Elector;

public class ElectorSecond extends Elector {
    protected int vote(Treenotation t1, Treenotation t2) {
        return loose();
    }

    protected boolean isInversable() {
        return true;
    }
}
