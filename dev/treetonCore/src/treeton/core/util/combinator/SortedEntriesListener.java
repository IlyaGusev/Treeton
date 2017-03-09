/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

public interface SortedEntriesListener<T> {
    void entryAdded(Entry<T> e, SortedEntries<T> entries);
}
