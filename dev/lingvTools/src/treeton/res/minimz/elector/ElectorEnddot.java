/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz.elector;

import treeton.core.Treenotation;
import treeton.res.minimz.Elector;

public class ElectorEnddot extends Elector {
    protected int vote(Treenotation t1, Treenotation t2) {
        if (t1.get("ENDDOT") == null) {
            return win();
        } else {
            Integer dictId1 = (Integer) t1.get("DICTID");
            Integer dictId2 = (Integer) t2.get("DICTID");
            Integer id1 = (Integer) t1.get("ID");
            Integer id2 = (Integer) t2.get("ID");
            if (id1 != null && id2 != null && id1.equals(id2) && dictId1 != null && dictId2 != null && dictId1.equals(dictId2)) {
                return win();
            } else {
                return tie();
            }
        }
    }

    protected boolean isInversable() {
        return false;
    }
}
