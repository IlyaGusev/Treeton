/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz.elector;

import treeton.core.Treenotation;

public class ElectorGramWeight extends ElectorWeight {

    static String[] gramFeatures = new String[]{
            "POS", "REPR", "VOX", "MD", "TNS", "ASP", "TRANS",
            "PRS", "CAS", "NMB", "ANIM", "GEND", "ATTR",
            "AGGROTYPE", "PNT"
    };

    protected int vote(Treenotation t1, Treenotation t2) {
        throw new RuntimeException("Not supported");
    }

    protected boolean isInversable() {
        return false;
    }
}
