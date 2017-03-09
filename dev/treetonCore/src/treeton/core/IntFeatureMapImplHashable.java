/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

public class IntFeatureMapImplHashable extends IntFeatureMapImpl {
    public IntFeatureMapImplHashable() {
        super();
    }

    public IntFeatureMapImplHashable(int _blockSize) {
        super(_blockSize);
    }

    public IntFeatureMapImplHashable(BlackBoard board) {
        super(board);
    }

    public IntFeatureMapImplHashable(BlackBoard board, int _blockSize) {
        super(board, _blockSize);
    }

    public int hashCode() {
        int h = 0;
        int l = mapper == null ? -1 : mapper.getMaxValue();
        for (int i = 0; i <= l; i++) {
            if (data[i] != null) {
                h = 31 * h + data[i].hashCode();
            }
        }
        return h;
    }

    public Object clone() {
        IntFeatureMapImplHashable t = new IntFeatureMapImplHashable();
        t.blockSize = blockSize;
        t.mapper = mapper;
        if (mapper != null) {
            t.data = new Object[data.length];
            System.arraycopy(data, 0, t.data, 0, data.length);
        } else {
            t.data = null;
        }
        return t;
    }

    public boolean equals(Object o) {
        if (o instanceof IntFeatureMapImplHashable) {
            IntFeatureMapImplHashable other = (IntFeatureMapImplHashable) o;
            if (other.mapper != mapper) {
                return false;
            }
            return featureEquals(other);
        }
        return false;
    }

    private boolean featureEquals(Object o) {
        IntFeatureMapImpl other = (IntFeatureMapImpl) o;
        if (other.mapper != mapper) {
            return false;
        }

        int l = mapper == null ? -1 : mapper.getMaxValue();
        for (int i = 0; i <= l; i++) {
            if (data[i] == null) {
                if (other.data[i] != null)
                    return false;
            } else if (other.data[i] == null) {
                if (data[i] != null)
                    return false;
            } else if (!other.data[i].equals(this.data[i])) {
                return false;
            }
        }
        return true;
    }

}
