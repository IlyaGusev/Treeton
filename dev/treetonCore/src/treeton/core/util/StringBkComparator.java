/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.Comparator;

public class StringBkComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        return sut.reverse(o1.toString()).
                compareToIgnoreCase(sut.reverse(o2.toString()));
    }
}
