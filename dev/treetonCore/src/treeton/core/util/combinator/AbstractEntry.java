/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import treeton.core.util.AbsoluteTime;

public abstract class AbstractEntry<T> implements Entry<T> {
    private int id;
    private T object;
    private AbstractEntry<T> left;
    private AbstractEntry<T> right;
    private AbstractEntry<T> parent;
    private boolean color;
    private int creationTime;
    private int index;
    private Entry<T> successor;
    private Entry<T> predecessor;

    public AbstractEntry(T object, int id, Entry<T> parent) {
        this.object = object;
        this.id = id;
        this.parent = (AbstractEntry<T>) parent;

        creationTime = AbsoluteTime.tick();
    }

    public int getId() {
        return id;
    }

    private void appendIndent(StringBuffer buf, int indent) {
        while (indent-- > 0) {
            buf.append(" ");
        }
    }

    public void append(StringBuffer buf, int indent) {
        appendIndent(buf, indent);
        buf.append(id);
        buf.append(": ");
        buf.append(object);
        buf.append("\n");
        appendIndent(buf, indent);
        buf.append("left ");
        if (left != null) {
            buf.append("(\n");
            left.append(buf, indent + 1);
            appendIndent(buf, indent);
            buf.append(")\n");
        } else {
            buf.append("()\n");
        }
        appendIndent(buf, indent);
        buf.append("right ");
        if (right != null) {
            buf.append("(\n");
            right.append(buf, indent + 1);
            appendIndent(buf, indent);
            buf.append(")\n");
        } else {
            buf.append("()\n");
        }
    }

    public int getCreationTime() {
        return creationTime;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public T getObject() {
        return object;
    }

    public String toString() {
        return id + " : " + object;
    }

    public Entry<T> getLeft() {
        return left;
    }

    public void setLeft(Entry<T> left) {
        this.left = (AbstractEntry<T>) left;
    }

    public Entry<T> getRight() {
        return right;
    }

    public void setRight(Entry<T> right) {
        this.right = (AbstractEntry<T>) right;
    }

    public Entry<T> getParent() {
        return parent;
    }

    public void setParent(Entry<T> parent) {
        this.parent = (AbstractEntry<T>) parent;
    }

    public Entry<T> getSuccessor() {
        return successor;
    }

    public void setSuccessor(Entry<T> successor) {
        this.successor = successor;
    }

    public Entry<T> getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Entry<T> predecessor) {
        this.predecessor = predecessor;
    }

    public boolean isColor() {
        return color;
    }

    public void setColor(boolean color) {
        this.color = color;
    }
}