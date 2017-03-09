/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import java.util.Collection;
import java.util.List;

public interface Combinator<T> {
    void addCombinatorListener(CombinatorListener<T> listener);

    void start();

    Combination<T> getCurrentCombination();

    String getStatistics();

    List<SortedEntries<T>> getSortedEntriesList();

    String getName();

    void setName(String name);

    void combinationUsed(Combination<T> combination);

    boolean next();

    Collection<Combination> getCombinationsFront();
}
