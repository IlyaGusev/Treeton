/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.util.NumeratedObject;

import java.util.Iterator;

public class IntFeatureMapStaticNOIterator implements Iterator<NumeratedObject> {
    int i;
    IntFeatureMapStaticStraight fms;
    IntFeatureMapStaticLog fml;
    NumeratedObject result = new NumeratedObject();

    public IntFeatureMapStaticNOIterator(IntFeatureMapStatic fm) {
        if (fm instanceof IntFeatureMapStaticStraight) {
            fms = (IntFeatureMapStaticStraight) fm;
            fml = null;
            i = 0;
            while (i < fms.data.length && fms.data[i] == null) {
                i++;
            }
        } else if (fm instanceof IntFeatureMapStaticLog) {
            fml = (IntFeatureMapStaticLog) fm;
            i = 0;
        }
    }

    public void reset(IntFeatureMapStatic fm) {
        if (fm instanceof IntFeatureMapStaticStraight) {
            fms = (IntFeatureMapStaticStraight) fm;
            fml = null;
            i = 0;
            while (i < fms.data.length && fms.data[i] == null) {
                i++;
            }
        } else if (fm instanceof IntFeatureMapStaticLog) {
            fml = (IntFeatureMapStaticLog) fm;
            i = 0;
        }
    }

    public void remove() {
    }

    public boolean hasNext() {
        if (fms != null) {
            if (i < fms.data.length) {
                return true;
            }
        } else if (fml != null) {
            if (i < fml.data.length) {
                return true;
            }
        }
        return false;
    }

    public NumeratedObject next() {
        if (fms != null) {
            result.n = i + fms.shift;
            result.o = fms.data[i];
            i++;
            while (i < fms.data.length && fms.data[i] == null) {
                i++;
            }
            return result;
        } else if (fml != null) {
            result.n = fml.indexes[i];
            result.o = fml.data[i];
            i++;
            return result;
        }
        return null;
    }
}
