/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import java.util.Arrays;

public class Combination2D<T> implements Combination<T> {
    private Entry<T>[] entries;
    private Combinator2D<T>.ListNode node;

    public Combination2D(Entry<T> first, Entry<T> second) {
        //noinspection unchecked
        this.entries = new Entry[]{first, second};
    }

    public Entry<T> getValue(int i) {
        return entries[i];
    }

    public Double getNorm() {
        return null;
    }

    public int getSize() {
        return 2;
    }

    public int compareTo(Combination<T> o) {
        return 0;
    }

    public Combinator2D<T>.ListNode getNode() {
        return node;
    }

    public void setNode(Combinator2D<T>.ListNode node) {
        this.node = node;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Combination2D that = (Combination2D) o;

        return Arrays.equals(entries, that.entries);
    }

    public int hashCode() {
        return Arrays.hashCode(entries);
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
        buf.append("|d=").append(dest());
        buf.append(")");
        return buf.toString();
    }

    private double dest() {
        return Math.abs(getValue(0).getPriority() - getValue(1).getPriority());
    }

}
