/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.Comparator;
import java.util.Map;

public class EntryComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        Map.Entry e1 = (Map.Entry) o1;
        Map.Entry e2 = (Map.Entry) o2;
        return ((Comparable) e1.getKey()).compareTo(e2.getKey());
    }
}
