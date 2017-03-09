/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SortedEntriesRBTreeImpl<T> implements SortedEntries<T> {
    private static final boolean RED = false;
    private static final boolean BLACK = true;
    List<SortedEntriesListener<T>> listeners = new ArrayList<SortedEntriesListener<T>>();
    private Comparator<T> comparator;
    private double EPS = 0.0001f;
    private transient Entry<T> root = null;
    private transient Entry<T> first = null;
    private transient int size = 0;
    private int id = 0;

    public SortedEntriesRBTreeImpl(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    public SortedEntriesRBTreeImpl(ExtendedComparator<T> comparator) {
        this.comparator = comparator;
    }

    private void incrementSize() {
        size++;
    }

    public int size() {
        return size;
    }

    public Entry<T> getPreceiding(T object) {
        Entry<T> t = root;

        if (t == null) {
            return null;
        }

        while (true) {
            int cmp = -t.compareToT(object);
            if (cmp < 0) {
                if (t.getLeft() != null) {
                    t = t.getLeft();
                } else {
                    return t.getPredecessor();
                }
            } else if (cmp == 0) {
                return t;
            } else { //cmp > 0
                if (t.getRight() != null) {
                    t = t.getRight();
                } else {
                    return t;
                }
            }
        }
    }

    public Entry<T> getFirst() {
        return first;
    }

    @SuppressWarnings({"ConstantConditions"})
    public Entry<T> add(T object) {
        Entry<T> t = root;

        if (t == null) {
            incrementSize();
            if (comparator instanceof ExtendedComparator) {
                root = new EntryWithExtendedComparator<T>(object, id++, null, (ExtendedComparator<T>) comparator);
            } else {
                root = new EntryWithStandardComparator<T>(object, id++, null, comparator);
                ((EntryWithStandardComparator<T>) root).setPriority(0);
            }
            first = root;

            notifyAdd(root);
            return root;
        }

        while (true) {
            int cmp = -t.compareToT(object);
            if (cmp == 0) {
                cmp = 1;
            }
            if (cmp < 0) {
                if (t.getLeft() != null) {
                    t = t.getLeft();
                } else {
                    incrementSize();
                    Entry<T> e;
                    if (comparator instanceof ExtendedComparator) {
                        e = new EntryWithExtendedComparator<T>(object, id++, t, (ExtendedComparator<T>) comparator);
                    } else {
                        e = new EntryWithStandardComparator<T>(object, id++, t, comparator);
                    }
                    t.setLeft(e);
                    e.setSuccessor(t);
                    t.setPredecessor(e);
                    Entry<T> pred = predecessor(e);
                    e.setPredecessor(pred);
                    if (pred != null) {
                        pred.setSuccessor(e);
                        if (!(comparator instanceof ExtendedComparator)) {
                            double d = t.getPriority() - pred.getPriority();
                            if (d < EPS) {
                                refreshStraightIndexes();
                            } else {
                                ((EntryWithStandardComparator<T>) e).setPriority(t.getPriority() - d / 2);
                            }
                        }
                    } else {
                        if (!(comparator instanceof ExtendedComparator)) {
                            ((EntryWithStandardComparator<T>) e).setPriority(t.getPriority() - 1);
                        }
                    }

                    if (t == first) {
                        first = e;
                    }

                    fixAfterInsertion(e);
                    notifyAdd(e);
                    return e;
                }
            } else { //cmp > 0,  function compare never returns zero
                if (t.getRight() != null) {
                    t = t.getRight();
                } else {
                    incrementSize();
                    Entry<T> e;
                    if (comparator instanceof ExtendedComparator) {
                        e = new EntryWithExtendedComparator<T>(object, id++, t, (ExtendedComparator<T>) comparator);
                    } else {
                        e = new EntryWithStandardComparator<T>(object, id++, t, comparator);
                    }
                    t.setRight(e);
                    t.setRight(e);
                    e.setPredecessor(t);
                    t.setSuccessor(e);
                    Entry<T> succ = successor(e);
                    e.setSuccessor(succ);
                    if (succ != null) {
                        succ.setPredecessor(e);
                        if (!(comparator instanceof ExtendedComparator)) {
                            double d = succ.getPriority() - t.getPriority();
                            if (d < EPS) {
                                refreshStraightIndexes();
                            } else {
                                ((EntryWithStandardComparator<T>) e).setPriority(t.getPriority() + d / 2);
                            }
                        }
                    } else {
                        if (!(comparator instanceof ExtendedComparator)) {
                            ((EntryWithStandardComparator<T>) e).setPriority(t.getPriority() + 1);
                        }
                    }

                    fixAfterInsertion(e);
                    notifyAdd(e);
                    return e;
                }
            }
        }
    }

    private void notifyAdd(Entry<T> e) {
        for (SortedEntriesListener<T> listener : listeners) {
            listener.entryAdded(e, this);
        }
    }

    public void addListener(SortedEntriesListener<T> listener) {
        listeners.add(listener);
    }

    public void refreshStraightIndexes() {
        Entry<T> e = first;
        int i = 0;
        while (e != null) {
            int idx = i++;
            e.setIndex(idx);
            if (e instanceof EntryWithStandardComparator) {
                ((EntryWithStandardComparator) e).setPriority(idx);
            }
            e = e.getSuccessor();
        }
    }

    public Comparator<T> getComparator() {
        return comparator;
    }

    public void clear() {
        size = 0;
        root = null;
    }

    private Entry<T> successor(Entry<T> t) {
        if (t == null)
            return null;
        else if (t.getRight() != null) {
            Entry<T> p = t.getRight();
            while (p.getLeft() != null)
                p = p.getLeft();
            return p;
        } else {
            Entry<T> p = t.getParent();
            Entry<T> ch = t;
            while (p != null && ch == p.getRight()) {
                ch = p;
                p = p.getParent();
            }
            return p;
        }
    }

    private Entry<T> predecessor(Entry<T> t) {
        if (t == null)
            return null;
        else if (t.getLeft() != null) {
            Entry<T> p = t.getLeft();
            while (p.getRight() != null)
                p = p.getRight();
            return p;
        } else {
            Entry<T> p = t.getParent();
            Entry<T> ch = t;
            while (p != null && ch == p.getLeft()) {
                ch = p;
                p = p.getParent();
            }
            return p;
        }
    }


    private boolean colorOf(Entry<T> p) {
        return (p == null ? BLACK : p.isColor());
    }

    private Entry<T> parentOf(Entry<T> p) {
        return (p == null ? null : p.getParent());
    }

    private void setColor(Entry<T> p, boolean c) {
        if (p != null) p.setColor(c);
    }

    private Entry<T> leftOf(Entry<T> p) {
        return (p == null) ? null : p.getLeft();
    }

    private Entry<T> rightOf(Entry<T> p) {
        return (p == null) ? null : p.getRight();
    }

    private void rotateLeft(Entry<T> p) {
        Entry<T> r = p.getRight();
        p.setRight(r.getLeft());

        if (r.getLeft() != null)
            r.getLeft().setParent(p);
        r.setParent(p.getParent());

        if (p.getParent() == null)
            root = r;
        else if (p.getParent().getLeft() == p)
            p.getParent().setLeft(r);
        else
            p.getParent().setRight(r);
        r.setLeft(p);
        p.setParent(r);
    }

    private void rotateRight(Entry<T> p) {
        Entry<T> l = p.getLeft();
        p.setLeft(l.getRight());

        if (l.getRight() != null) l.getRight().setParent(p);
        l.setParent(p.getParent());

        if (p.getParent() == null)
            root = l;
        else if (p.getParent().getRight() == p)
            p.getParent().setRight(l);
        else
            p.getParent().setLeft(l);

        l.setRight(p);
        p.setParent(l);
    }

    @SuppressWarnings({"PointlessBooleanExpression"})
    private void fixAfterInsertion(Entry<T> x) {
        x.setColor(RED);

        while (x != null && x != root && x.getParent().isColor() == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Entry<T> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    if (parentOf(parentOf(x)) != null)
                        rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Entry<T> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    if (parentOf(parentOf(x)) != null)
                        rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.setColor(BLACK);
    }

    public String toString() {
        if (root == null) {
            return "()";
        }

        StringBuffer buffer = new StringBuffer();
        root.append(buffer, 0);

        return buffer.toString();
    }
}
