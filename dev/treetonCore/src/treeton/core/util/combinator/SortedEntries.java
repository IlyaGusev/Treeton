/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import java.util.Comparator;

public interface SortedEntries<T> {
    Entry<T> getFirst();

    Entry<T> add(T object);

    void addListener(SortedEntriesListener<T> listener);

    void refreshStraightIndexes();

    Comparator<T> getComparator();

    int size();

    Entry<T> getPreceiding(T object);
}
