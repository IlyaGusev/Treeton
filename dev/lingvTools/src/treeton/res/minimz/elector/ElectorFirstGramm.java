/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz.elector;

import treeton.core.Treenotation;
import treeton.res.minimz.Elector;

/**
 * ElectorFirst
 * <p/>
 * Более предпочтительна всегда первая аннотация.
 */
public class ElectorFirstGramm extends Elector {
    public String[] attrs;

    public ElectorFirstGramm(String[] attrs) {
        this.attrs = attrs;
    }

    protected int vote(Treenotation t1, Treenotation t2) {
        throw new RuntimeException("Unsupported");
    }

    protected boolean isInversable() {
        return true;
    }
}
