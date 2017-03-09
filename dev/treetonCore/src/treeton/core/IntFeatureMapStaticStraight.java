/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.NumeratedObject;
import treeton.core.util.collector.Collector;
import treeton.core.util.collector.CollectorException;
import treeton.core.util.collector.Mutable;

import java.util.Iterator;

public class IntFeatureMapStaticStraight implements IntFeatureMapStatic {
    Object[] data;
    int shift;

    public IntFeatureMapStaticStraight() {
    } //do not remove


    public IntFeatureMapStaticStraight(BlackBoard board) {
        shift = board.getFirstNumber();
        int last = board.getDepth();
        data = new Object[last - shift + 1];

        int j = 0;
        for (int i = shift; i <= last; i++) {
            if (board.contains(i)) {
                data[j++] = board.get(i);
                board.erase(i);
            } else {
                data[j++] = null;
            }
        }
    }

    public static int getRetainedSize(int max, int min) {
        return 4 + 4 + (max - min + 1) * 4;
    }

    public int getRetainedSize() {
        return 4 + 4 + data.length * 4;
    }

    public Object get(int key) {
        int d = key - shift;
        if (d < 0 || d >= data.length)
            return null;
        return data[d];
    }

    public boolean contains(int key) {
        int d = key - shift;
        if (d < 0 || d >= data.length)
            return false;
        return data[d] == null ? false : true;
    }

    public int getMinFeature() {
        return shift;
    }

    public int getMaxFeature() {
        return data.length - 1 + shift;
    }

    public Iterator valueIterator() {
        return new ValueIterator();
    }

    public Iterator numeratedObjectIterator() {
        return new NumeratedObjectIterator();
    }

    public int size() {
        int count = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] != null) {
                count++;
            }
        }
        return count;
    }

    Object[] getData() {
        return data;
    }

    public String toString() {
        Iterator it = numeratedObjectIterator();
        String s = "";
        while (it.hasNext()) {
            NumeratedObject no = (NumeratedObject) it.next();
            s += String.valueOf(no.n) + "=" + no.o.toString() + "; ";
        }
        return s;
    }

    public String getString(TrnType type) {
        try {
            Iterator it = numeratedObjectIterator();
            String s = type.getName().toString() + ": ";
            while (it.hasNext()) {
                NumeratedObject no = (NumeratedObject) it.next();
                s += type.getFeatureNameByIndex(no.n).toString() + "=" + no.o.toString() + "; ";
            }
            return s;
        } catch (TreetonModelException e) {
            return "Exception";
        }
    }

    public String getHtmlString(TrnType type) {
        try {
            Iterator it = numeratedObjectIterator();
            String s = "<i>" + type.getName().toString() + "</i>: ";
            while (it.hasNext()) {
                NumeratedObject no = (NumeratedObject) it.next();
                s += "<b>" + type.getFeatureNameByIndex(no.n).toString() + "</b>=" + no.o.toString() + "; ";
            }
            return s;
        } catch (TreetonModelException e) {
            return "Exception";
        }
    }

    public static class IntFeatureMapStaticStraightCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            IntFeatureMapStaticStraight t = (IntFeatureMapStaticStraight) o;
            t.shift = (Integer) col.get();
            t.data = (Object[]) col.get();

        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            IntFeatureMapStaticStraight t = (IntFeatureMapStaticStraight) o;
            col.put(t.shift);
            col.put(t.data);
        }
    }

    private class ValueIterator implements Iterator {
        int i;

        ValueIterator() {
            i = 0;
        }

        public void remove() {
        }

        public boolean hasNext() {
            if (i < data.length) {
                return true;
            }
            return false;
        }

        public Object next() {
            return data[i++];
        }
    }

    private class NumeratedObjectIterator implements Iterator {
        int i;

        NumeratedObjectIterator() {
            i = 0;
            while (i < data.length && data[i] == null) {
                i++;
            }
        }

        public void remove() {
        }

        public boolean hasNext() {
            if (i < data.length) {
                return true;
            }
            return false;
        }

        public Object next() {
            Object o = new NumeratedObject(i + shift, data[i]);
            i++;
            while (i < data.length && data[i] == null) {
                i++;
            }
            return o;
        }
    }
}
