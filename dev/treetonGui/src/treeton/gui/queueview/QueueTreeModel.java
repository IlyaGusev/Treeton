/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.queueview;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

@SuppressWarnings({"unchecked"})
public class QueueTreeModel implements TreeModel {
    List data = new ArrayList<Object>();
    ElementWrapperComparator comparator = new ElementWrapperComparator();
    private String name;

    public void importQueue(Queue queue) {
        data.clear();
        if (queue != null) {
            try {
                for (Object elem : queue) {
                    data.add(new ElementWrapper(elem));
                }
            } catch (ConcurrentModificationException e) {
                //do nothinf
            }
            if (queue instanceof PriorityQueue) {
                PriorityQueue prq = (PriorityQueue) queue;
                Comparator c = prq.comparator();
                if (c == null) {
                    Collections.sort(data);
                } else {
                    comparator.setInternalComparator(c);
                    Collections.sort(data, comparator);
                }
            } else if (queue instanceof PriorityBlockingQueue) {
                PriorityBlockingQueue prq = (PriorityBlockingQueue) queue;
                Comparator c = prq.comparator();
                if (c == null) {
                    Collections.sort(data);
                } else {
                    comparator.setInternalComparator(c);
                    Collections.sort(data, comparator);
                }
            }
        }
    }

    public Object getRoot() {
        return this;
    }

    public Object getChild(Object parent, int index) {
        if (parent == this) {
            return data.get(index);
        }
        return null;
    }

    public int getChildCount(Object parent) {
        if (parent == this) {
            return data.size();
        }
        return 0;
    }

    public boolean isLeaf(Object node) {
        return node != this;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent == this) {
            //noinspection SuspiciousMethodCalls
            return data.indexOf(child);
        }
        return -1;
    }

    public void addTreeModelListener(TreeModelListener l) {
    }

    public void removeTreeModelListener(TreeModelListener l) {
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return name + " (" + data.size() + ")";
    }

    class ElementWrapper implements Comparable<ElementWrapper> {
        Object obj;

        public ElementWrapper(Object obj) {
            this.obj = obj;
        }

        public int compareTo(ElementWrapper o) {
            return ((Comparable) obj).compareTo(o);
        }
    }

    class ElementWrapperComparator implements Comparator<ElementWrapper> {
        Comparator<Object> c;

        public int compare(ElementWrapper o1, ElementWrapper o2) {
            return c.compare(o1.obj, o2.obj);
        }

        public void setInternalComparator(Comparator<Object> c) {
            this.c = c;
        }
    }
}
