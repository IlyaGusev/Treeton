/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import java.util.Iterator;

public class TreenotationIndexValueIterator implements Iterator {
    ScapeTreenotationIndex.FinalNode cur;
    int i;

    public TreenotationIndexValueIterator() {
        cur = null;
        i = 0;
    }

    public TreenotationIndexValueIterator(ScapeTreenotationIndex idx) {
        cur = idx.firstFinal;
        i = 0;
    }

    public void reset(ScapeTreenotationIndex idx) {
        cur = idx.firstFinal;
        i = 0;
    }

    public void remove() {
    }

    public boolean hasNext() {
        return cur != null;
    }

    public Object next() {
        ScapeTreenotationIndex.FinalNode t = cur;
        int ti = i++;
        if (i >= cur.getValues().length) {
            cur = cur.getNextFinal();
            i = 0;
        }
        return t.getValues()[ti];
    }

}
