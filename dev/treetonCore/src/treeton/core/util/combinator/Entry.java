/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import treeton.core.util.IdProvider;

public interface Entry<T> extends IdProvider, Comparable<Entry<T>> {
    double getPriority();

    int compareToT(T o);

    void append(StringBuffer buf, int indent);

    int getCreationTime();

    int getIndex();

    void setIndex(int index);

    T getObject();

    Entry<T> getLeft();

    void setLeft(Entry<T> left);

    Entry<T> getRight();

    void setRight(Entry<T> right);

    Entry<T> getParent();

    void setParent(Entry<T> parent);

    Entry<T> getSuccessor();

    public void setSuccessor(Entry<T> successor);

    Entry<T> getPredecessor();

    public void setPredecessor(Entry<T> predecessor);

    boolean isColor();

    void setColor(boolean color);
}
