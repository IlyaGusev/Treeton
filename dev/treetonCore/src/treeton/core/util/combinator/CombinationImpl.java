/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import java.util.Arrays;

public class CombinationImpl<T> implements Combination<T> {
    public static boolean nonDeterminancyMode = false;

    private Entry<T>[] entries;
    private int fixedDimension = -1;
    private int depth;
    private int maxdepth = -1;

    public CombinationImpl(Entry<T>[] entries, int depth) {
        this.entries = entries;
        this.depth = depth;
    }

    public Entry<T>[] getEntries() {
        return entries;
    }

    public Double getNorm() {
        double n = 0;
        for (Entry<T> entry : entries) {
            n += entry.getPriority();
        }
        return n;
    }

    public Entry<T> getValue(int i) {
        return entries[i];
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("(");
        for (int i = 0; i < entries.length; i++) {
            Entry<T> entry = entries[i];
            buf.append(entry.getId());
            buf.append(":");
            buf.append(Double.toString(entry.getPriority()));
            if (i < entries.length - 1) {
                buf.append(",");
            }
        }
        buf.append("|n=").append(Double.toString(getNorm()));
        buf.append(")");
        return buf.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CombinationImpl that = (CombinationImpl) o;

        return Arrays.equals(entries, that.entries);
    }

    public int hashCode() {
        return Arrays.hashCode(entries);
    }

    public int getFixedDimension() {
        return fixedDimension;
    }

    public void setFixedDimension(int fixedDimension) {
        this.fixedDimension = fixedDimension;
    }

    public int getDepth() {
        return depth;
    }

    public int getSize() {
        return entries.length;
    }

    public int compareTo(Combination<T> o) {
        double d = getNorm() - o.getNorm();
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        } else {
            int c = nonDeterminancyMode ? getDepth() - ((CombinationImpl<T>) o).getDepth() : 0;

            if (c == 0) {
                if (getSize() < o.getSize()) {
                    return -1;
                } else if (getSize() > o.getSize()) {
                    return 1;
                } else {
                    for (int i = 0; i < getSize(); i++) {
                        Entry<T> e1 = getValue(i);
                        Entry<T> e2 = o.getValue(i);

                        c = e1.compareTo(e2);

                        if (c < 0) {
                            return -1;
                        }

                        if (c > 0) {
                            return 1;
                        }
                    }

                    return 0;
                }
            } else if (c < 0) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public int getMaxdepth() {
        return maxdepth;
    }

    public void setMaxdepth(int maxdepth) {
        this.maxdepth = maxdepth;
    }
}
