/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnType;
import treeton.core.util.NumeratedObject;

import java.util.Map;
import java.util.Set;

public interface BlackBoard {
    public void put(NumeratedObject[] arr);

    public void put(int n, Object o);

    public void put(String s, TrnType type, Object o);

    public void put(int n, TrnType type, Object o);

    public void put(NumeratedObject[] arr, BlackBoard filter);

    public void put(NumeratedObject[] arr, Set<Integer> filter);

    public void put(int n, Object o, BlackBoard filter);

    public void put(String s, TrnType type, Object o, BlackBoard filter);

    public void put(int n, TrnType type, Object o, BlackBoard filter);

    public Object get(int n);

    public boolean contains(int n);

    public Object erase(int n);

    public boolean containsErase(int n);

    public void clean();

    public int getDepth();

    public int getFirstNumber();

    public int getNumberOfObjects();

    void fill(TrnType tp, Map map);

    public void appendTrnStringView(StringBuffer buf, TrnType type);

    void treatObjects(TrnType tp);

    void put(BlackBoard board);
}
