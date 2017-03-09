/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import java.util.Comparator;

public class EntryWithStandardComparator<T> extends AbstractEntry<T> {
    private Comparator<T> comparator;
    private double priority;

    public EntryWithStandardComparator(T object, int id, Entry<T> parent, Comparator<T> comparator) {
        super(object, id, parent);
        this.comparator = comparator;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double p) {
        this.priority = p;
    }

    public int compareTo(Entry<T> o) {
        int c = comparator.compare(getObject(), o.getObject());
        return c < 0 ? -1 : c > 0 ? 1 : getId() - o.getId();
    }

    public int compareToT(T o) {
        int c = comparator.compare(getObject(), o);
        return c < 0 ? -1 : c == 0 ? 0 : 1;
    }
}