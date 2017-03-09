/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz;

import treeton.core.Treenotation;

public abstract class Elector implements Cloneable {
    boolean inverse = false;

    protected int win() {
        return inverse ? -1 : 1;
    }

    protected int loose() {
        return inverse ? 1 : -1;
    }

    protected int tie() {
        return 0;
    }

    protected void inverse() {
        if (isInversable()) {
            inverse = !inverse;
        }
    }

    protected abstract int vote(Treenotation t1, Treenotation t2);

    public Object clone() {
        try {
            Elector e = (Elector) super.clone();
            return e;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    protected abstract boolean isInversable();
}
