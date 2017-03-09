/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.util.collector.CollectorException;
import treeton.core.util.collector.Mutable;

public class StringArray {
    int size;
    String[] elements;

    public StringArray(int size) {
        this.size = size;
        elements = new String[size];
    }


    public StringArray() {
    } //do not remove

    public void set(int index, String s) {
        elements[index] = s;
    }

    public String get(int index) {
        return elements[index];
    }

    public int size() {
        return size;
    }

    public boolean equals(Object _other) {
        if (_other == null || !(_other instanceof StringArray)) return false;
        StringArray other = (StringArray) _other;
        if (other.size != size) {
            return false;
        }
        for (int i = 0; i < elements.length; i++) {
            String s = elements[i];
            if (s == null) {
                if (other.elements[i] != null) {
                    return false;
                }
            } else {
                if (!s.equals(other.elements[i]))
                    return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        if (elements != null) {
            for (int i = 0; i < elements.length; i++) {
                String element = elements[i];
                if (element == null) {
                    continue;
                }
                int len = element.length();
                for (int j = 0; j < len; j++) {
                    h = 31 * h + element.charAt(j);
                }
            }
        }
        return h;
    }

    public static class StringArrayCollectable extends Mutable {
        public void readIn(treeton.core.util.collector.Collector col, Object o) throws CollectorException, ClassCastException {
            StringArray t = (StringArray) o;
            t.size = (Integer) col.get();
            t.elements = (String[]) col.get();

        }

        public void append(treeton.core.util.collector.Collector col, Object o) throws CollectorException, ClassCastException {
            StringArray t = (StringArray) o;
            col.put(t.size);
            col.put(t.elements);
        }
    }

}
