/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model;

import treeton.core.config.context.treenotations.TreenotationsContext;

import java.util.ArrayList;
import java.util.HashSet;


public class TrnTypeSet implements Comparable {
    TrnType[] typesByIndex;
    TrnType[] types;
    int size;

    public static TrnType[] getTrnTypeArrFromHashSet(TreenotationsContext context, HashSet<String> names) {
        ArrayList<TrnType> arr = new ArrayList<TrnType>();
        for (String s : names) {
            TrnType tp = null;
            try {
                tp = context.getType(s);
            } catch (TreetonModelException e) {
                tp = null;
            }
            if (tp != null) {
                arr.add(tp);
            }
        }
        return arr.toArray(new TrnType[names.size()]);
    }

    public TrnType[] getTypes() {
        return types;
    }

    public boolean contains(TrnType tp) {
        int i;
        try {
            i = tp.getIndex();
        } catch (TreetonModelException e) {
            i = -1;
        }
        return !(i < 0 || i >= typesByIndex.length) && typesByIndex[i] != null;
    }

    public int compareTo(Object o) {
        if (o instanceof TrnTypeSet) {
            TrnTypeSet other = (TrnTypeSet) o;
            int len1 = size;
            int len2 = other.size;
            int n = Math.min(len1, len2);
            TrnType[] v1 = types;
            TrnType[] v2 = other.types;
            int i = 0;

            while (i < n) {
                int s1;
                try {
                    s1 = v1[i].getIndex();
                } catch (TreetonModelException e) {
                    s1 = -1;
                }
                int s2;
                try {
                    s2 = v2[i].getIndex();
                } catch (TreetonModelException e) {
                    s2 = -1;
                }
                if (s1 != s2) {
                    return s1 - s2;
                }
                i++;
            }
            return len1 - len2;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean equals(Object o) {
        if (o instanceof TrnTypeSet) {
            TrnTypeSet other = (TrnTypeSet) o;
            if (size != other.size)
                return false;
            TrnType[] v1 = types;
            TrnType[] v2 = other.types;
            int i = 0;
            while (i < size) {
                if (v1[i] != v2[i]) {
                    return false;
                }
                i++;
            }
            return true;
        }
        return false;
    }

    public TrnType[] getCommonTypes() {
        ArrayList<TrnType> arr = new ArrayList<TrnType>();
        for (int i = 0; i < size; i++) {
            try {
                if (!types[i].isTokenType()) {
                    arr.add(types[i]);
                }
            } catch (TreetonModelException e) {
                //do nothing
            }
        }
        return arr.toArray(new TrnType[arr.size()]);
    }

    public TrnType[] getTokenTypes() {
        ArrayList<TrnType> arr = new ArrayList<TrnType>();
        for (int i = 0; i < size; i++) {
            try {
                if (types[i].isTokenType()) {
                    arr.add(types[i]);
                }
            } catch (TreetonModelException e) {
                //do nothing
            }
        }
        return arr.toArray(new TrnType[arr.size()]);
    }

    public HashSet<String> toStringHashSet() {
        HashSet<String> set = new HashSet<String>();
        for (int i = 0; i < size; i++) {
            try {
                set.add(types[i].getName());
            } catch (TreetonModelException e) {
                //do nothing
            }
        }
        return set;
    }
}
