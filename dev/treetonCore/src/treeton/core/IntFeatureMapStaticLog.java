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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IntFeatureMapStaticLog implements IntFeatureMapStatic {
    int[] indexes;
    Object[] data;

    public IntFeatureMapStaticLog() {
    } //do not remove

    public IntFeatureMapStaticLog(Map<String, Object> namesMap, TrnType tp) throws TreetonModelException {
        int i = 0, len = namesMap.size();
        indexes = new int[len];
        data = new Object[len];
        for (Map.Entry<String, Object> entry : namesMap.entrySet()) {
            indexes[i] = tp.getFeatureIndex(entry.getKey());
            data[i] = entry.getValue();
            i++;
        }
    }

    public IntFeatureMapStaticLog(BlackBoard board) {
        int len = board.getNumberOfObjects();
        indexes = new int[len];
        data = new Object[len];

        int j = 0;
        for (int i = 0; i <= board.getDepth(); i++) {
            if (board.contains(i)) {
                indexes[j] = i;
                data[j++] = board.get(i);
                board.erase(i);
            }
        }
    }

    public IntFeatureMapStaticLog(Map.Entry[] arr, int len) {
        indexes = new int[len];
        data = new Object[len];

        for (int i = 0; i < len; i++) {
            Map.Entry e = arr[i];
            indexes[i] = (Integer) e.getKey();
            data[i] = e.getValue();
        }
    }

    public Map<String, Object> toNamesMap(TrnType tp) throws TreetonModelException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Iterator it = numeratedObjectIterator();
        while (it.hasNext()) {
            NumeratedObject no = (NumeratedObject) it.next();
            map.put(tp.getFeatureNameByIndex(no.n), no.o);
        }
        return map;
    }

    public Object get(int key) {
        int first = 0;
        int last = indexes.length - 1;

        while (true) {
            int c = (first + last) / 2;

            if (indexes[c] == key) {
                return data[c];
            } else if (indexes[c] > key) {
                last = c - 1;
            } else if (indexes[c] < key) {
                first = c + 1;
            }

            if (first > last)
                return null;
        }
    }

    public boolean contains(int key) {
        int first = 0;
        int last = indexes.length - 1;

        while (true) {
            int c = (first + last) / 2;

            if (indexes[c] == key) {
                return true;
            } else if (indexes[c] > key) {
                last = c - 1;
            } else if (indexes[c] < key) {
                first = c + 1;
            }

            if (first > last)
                return false;
        }
    }

    public int getMinFeature() {
        return indexes[0];
    }

    public int getMaxFeature() {
        return indexes[indexes.length - 1];
    }

    public Object getByNumber(int n) {
        if (data == null || n >= data.length || n < 0) {
            return null;
        }
        return data[n];
    }

    public int getIndexByNumber(int n) {
        if (indexes == null || n >= indexes.length || n < 0) {
            return -1;
        }
        return indexes[n];
    }

    public Iterator valueIterator() {
        return new ValueIterator();
    }

    public Iterator numeratedObjectIterator() {
        return new NumeratedObjectIterator();
    }

    public int size() {
        return data.length;
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
            String s = "{ ";
            while (it.hasNext()) {
                NumeratedObject no = (NumeratedObject) it.next();
                s += type.getFeatureNameByIndex(no.n).toString() + "=" + no.o.toString() + "; ";
            }
            s += " }";
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

    public int getRetainedSize() {
        return 4 + indexes.length * 8;
    }

    public static class IntFeatureMapStaticLogCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            IntFeatureMapStaticLog t = (IntFeatureMapStaticLog) o;
            t.indexes = (int[]) col.get();
            t.data = (Object[]) col.get();
        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            IntFeatureMapStaticLog t = (IntFeatureMapStaticLog) o;
            col.put(t.indexes);
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
            Object o = new NumeratedObject(indexes[i], data[i]);
            i++;
            return o;
        }
    }

}
