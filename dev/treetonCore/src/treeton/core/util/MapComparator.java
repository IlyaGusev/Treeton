/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.Comparator;
import java.util.Map;

public class MapComparator implements Comparator {
    private Object[] keys;

    public MapComparator(Object[] keys) {
        this.keys = keys;
    }

    /**
     * Этот метод сравнивает Map по указанному списку ключей.
     *
     * @param p1   первый из сравниваемых набор свойств
     * @param p2   второй из сравниваемых набор свойств
     * @param keys список ключей для сравнения
     * @return &lt; 0 - p1 "меньше" p2<br>
     * &nbsp;&nbsp;0 - значения p1 совпадают с p2<br>
     * &gt; 0 - p1 "больше" p2
     */
    public static int compareMaps(Map p1, Map p2,
                                  Object[] keys) {
        int gt = 1, eq = 0, lt = -1;
        int rslt = eq;
        int i = 0, n = keys.length;
        Object curKey, k1, k2;
        for (; i < n && rslt == eq; i++) {
            curKey = keys[i];
            k1 = p1.get(curKey);
            k2 = p2.get(curKey);
            if (k1 == null) {
                if (k2 == null) {
                    // Если оба значения равны null, то считаем их равными.
                    //
                    // Оператор "continue" можно было не ставить, поскольку
                    // после этого каскада условных опрераторов мы и так
                    // переходим к следующей итерации. Просто, так нагляднее.
                    continue;
                } else {
                    // значение, равное null (k1), считается меньше
                    rslt = lt;
                }
            } else {
                if (k2 == null) {
                    // значение, равное null (k2), считается меньше
                    rslt = gt;
                } else {
                    if (k1 instanceof Comparable &&
                            k2 instanceof Comparable) {
                        // Если оба значения не равны null, то сравниваем их.
                        rslt = ((Comparable) k1).compareTo((Comparable) k2);
                    } else {
                        rslt = k1.hashCode() - k2.hashCode();
                        rslt = rslt > 0 ? 1 : rslt < 0 ? -1 : 0;
                    }
                }
            }
        }
        return rslt;
    }

    public int compare(Object o1, Object o2) {
        return compareMaps((Map) o1, (Map) o2, keys);
    }
}
