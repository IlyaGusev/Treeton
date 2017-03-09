/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.Token;
import treeton.core.Treenotation;

import java.util.Comparator;

public class TrnOffsetLengthComparator implements Comparator<Treenotation> {
    public int compare(Treenotation t1, Treenotation t2) {
        Token s1 = t1.getStartToken();
        Token e1 = t1.getEndToken();
        Token s2 = t2.getStartToken();
        Token e2 = t2.getEndToken();

        if (s1 == null) {
            if (s2 == null) {
                if (e1 == null) {
                    if (e2 == null) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (e2 == null) {
                    return 1;
                } else {
                    return e2.compareTo(e1);
                }
            } else {
                return -1;
            }
        } else if (s2 == null) {
            return 1;
        } else {
            int c = s1.compareTo(s2);
            if (c == 0) {
                if (e1 == null) {
                    if (e2 == null) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (e2 == null) {
                    return 1;
                } else {
                    return e2.compareTo(e1);
                }
            } else {
                return c;
            }
        }
    }
}
