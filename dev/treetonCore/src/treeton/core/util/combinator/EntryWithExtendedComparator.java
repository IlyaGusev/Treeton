/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

public class EntryWithExtendedComparator<T> extends AbstractEntry<T> {
    private ExtendedComparator<T> extendedComparator;

    public EntryWithExtendedComparator(T object, int id, Entry<T> parent, ExtendedComparator<T> extendedComparator) {
        super(object, id, parent);
        this.extendedComparator = extendedComparator;
    }

    public double getPriority() {
        return extendedComparator.getPriority(getObject());
    }

    public int compareTo(Entry<T> o) {
        double d = getPriority() - o.getPriority();
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        } else {
            int c = extendedComparator.compare(getObject(), o.getObject());
            return c < 0 ? -1 : c > 0 ? 1 : getId() - o.getId();
        }
    }

    public int compareToT(T o) {
        double d = getPriority() - extendedComparator.getPriority(o);
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        } else {
            int c = extendedComparator.compare(getObject(), o);
            return c < 0 ? -1 : c == 0 ? 0 : 1;
        }
    }
}
