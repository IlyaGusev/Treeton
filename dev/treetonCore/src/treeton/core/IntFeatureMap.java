/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnType;
import treeton.core.util.FeaturesValueCollector;
import treeton.core.util.NumeratedObject;

import java.util.Iterator;

public interface IntFeatureMap {
    public Object get(int key);

    public Object getByIndex(int index);

    public void putByIndex(int idx, Object o);

    public boolean contains(int key);

    public void put(int key, Object value);

    public void put(int key, Object value, TrnType tp);

    public void remove(int key);

    public void removeAll();

    public void put(BlackBoard board);

    public void put(BlackBoard board, TrnType tp);

    public Iterator valueIterator();

    public Iterator<NumeratedObject> numeratedObjectIterator();

    public void fillBlackBoard(BlackBoard board);

    public Treenotation convertToTreenotation(TrnType tp);

    public Treenotation convertToTreenotation(Token start, Token end, TrnType tp);

    public int size();

    String getString(TrnType tp);

    String getHtmlString(TrnType tp);

    void appendSelf(StringBuffer buf, FeaturesValueCollector collector);

    public Object clone();

    void leaveIntersection(IntFeatureMap map);

    void leaveIntersection(Iterable<? extends IntFeatureMap> maps);

    void leaveDifference(IntFeatureMap map);

    int getKey(int n);
}
