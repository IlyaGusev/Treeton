/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.TreeSet;

public class SmartIDGenerator implements SmartUniqueIdGenerator {
    TreeSet<Integer> free = null;
    private int start;
    private int next;

    public SmartIDGenerator(int start) {
        this.start = start;
        next = start;
    }

    public int getNextID() {
        if (free != null && free.size() > 0) {
            Integer i = free.first();
            free.remove(i);
            return i;
        } else {
            return next++;
        }
    }

    public void reset() {
        next = start;
        free = null;
    }

    public void freeID(int id) {
        if (id < 0)
            return;
        if (free == null) {
            free = new TreeSet<Integer>();
        }
        free.add(id);
    }
}
