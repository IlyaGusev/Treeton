/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.fsm.CharFSMImpl;
import treeton.core.fsm.PackedCharFSM;

import java.util.Arrays;
import java.util.Iterator;

public class CharFeatureMapImpl implements CharFeatureMap {
    private CharFSMImpl features;
    private Object[] values;

    CharFeatureMapImpl(int capacity, Recoder recoder) {
        if (capacity < 0)
            throw new IllegalArgumentException();
        features = new CharFSMImpl(recoder);
        values = new Object[capacity];
    }

    CharFeatureMapImpl(CharFSMImpl cfsm) {
        features = cfsm;
        values = new Object[cfsm.getSize()];
        Arrays.fill(values, null);
    }

    public Object get(String key) {
        int i = features.get(key);
        if (i == -1)
            return null;
        return values[i];
    }

    public Object get(TString key) {
        int i = features.get(key);
        if (i == -1)
            return null;
        return values[i];
    }

    public void put(String key, Object value) {
        int i = features.addString(key);
        if (i >= values.length) {
            Object[] t = new Object[(int) (values.length * 1.5)];
            System.arraycopy(values, 0, t, 0, values.length);
            values = t;
        }
        values[i] = value;
    }

    public void put(TString key, Object value) {
        int i = features.addString(key);
        if (i >= values.length) {
            Object[] t = new Object[(int) (values.length * 1.5)];
            System.arraycopy(values, 0, t, 0, values.length);
            values = t;
        }
        values[i] = value;
    }

    public int size() {
        return features.getSize();
    }

    Object[] getValueArr() {
        return values;
    }

    CharFSMImpl getCharFSM() {
        return features;
    }

    public Iterator valueIterator() {
        return new ValueIterator();
    }

    public void pack() {
        Object[] newValues = new Object[size()];
        System.arraycopy(values, 0, newValues, 0, size());
        values = newValues;
        features = new PackedCharFSM(features);
    }

    public String getFullString() {
        return null;
    }

    public int readInFullString() {
        return 0;
    }

    private class ValueIterator implements Iterator {
        int i;
        int maxi;

        ValueIterator() {
            i = 0;
            maxi = features.getSize() - 1;
        }

        public void remove() {
        }

        public boolean hasNext() {
            if (i <= maxi) {
                return true;
            }
            return false;
        }

        public Object next() {
            Object o = values[i];
            i++;
            return o;
        }
    }
}
