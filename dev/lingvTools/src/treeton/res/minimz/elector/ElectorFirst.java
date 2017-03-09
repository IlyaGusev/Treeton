/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz.elector;

import treeton.core.Treenotation;
import treeton.res.minimz.Elector;

public class ElectorFirst extends Elector {
    protected int vote(Treenotation t1, Treenotation t2) {
        return win();
    }

    protected boolean isInversable() {
        return true;
    }
}
