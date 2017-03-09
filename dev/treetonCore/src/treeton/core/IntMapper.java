/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import java.nio.ByteBuffer;

public class IntMapper {
    short map[];
    short keys[];
    IntMapper t, f;
    IntMapper mapOwner;
    short depth;

    int index;

    IntMapper(short _depth, IntMapper _t, IntMapper _f, int index) {
        mapOwner = null;
        map = null;
        keys = null;
        depth = _depth;
        t = _t;
        f = _f;

        this.index = index;
    }

    short[] getMap() {
        if (map == null || map[0] == -2) {
            map = mapOwner.getMap();
        }
        return map;
    }

    short[] getKeys() {
        if (keys == null || keys[0] == -2) {
            keys = mapOwner.getKeys();
        }
        return keys;
    }


    public short get(int key) {
        if (key > depth) {
            return -1;
        }

        if (map == null || map[0] == -2) {
            if (mapOwner == null)
                return -1;
            map = mapOwner.getMap();
            keys = mapOwner.getKeys();
        }


        return map[key];
    }

    public short getKey(int n) {
        if (map == null || map[0] == -2) {
            if (mapOwner == null)
                return -1;
            map = mapOwner.getMap();
            keys = mapOwner.getKeys();
        }

        if (n > map[depth]) {
            return -1;
        }


        return keys[n];
    }

    public short getMaxValue() {
        if (map == null || map[0] == -2) {
            map = mapOwner.getMap();
            keys = mapOwner.getKeys();
        }

        return map[depth];
    }

    boolean ready() {
        return !(map == null && mapOwner == null);
    }

    void assignMap(BlackBoard board, Object[] array2fill) {
        if (map != null)
            throw new RuntimeException();

        map = new short[board.getDepth() + 1];
        keys = new short[board.getNumberOfObjects()];

        mapOwner = null;

        short curn = 0;
        for (short i = 0; i < board.getDepth() + 1; i++) {
            Object o = board.get(i);
            if (o != null) {
                if (array2fill != null) {
                    array2fill[curn] = o;
                }
                keys[curn] = i;
                map[i] = curn++;
                board.erase(i);
            } else {
                map[i] = (short) -1;
            }
        }
    }

    void assignMap(IntMapper m, int length, int key, Object value, Object[] array2fill, Object[] data) {
        if (map != null)
            throw new RuntimeException();

        map = new short[length];

        mapOwner = null;

        short curn = (short) (m.get(m.depth) + 1);

        keys = new short[curn + 1];

        for (short i = (short) (length - 1); i >= 0; i--) {
            if (i == key) {
                array2fill[curn] = value;
                keys[curn] = i;
                map[i] = curn--;
            } else {
                int idx = m.get(i);
                if (idx != -1) {
                    array2fill[curn] = data[idx];
                    keys[curn] = i;
                    map[i] = curn--;
                } else {
                    map[i] = (short) -1;
                }
            }
        }
    }

    void assignMap(IntMapper m, int length, int key) {
        if (map != null)
            throw new RuntimeException();

        map = new short[length];

        mapOwner = null;

        short curn = (short) (m.get(m.depth) + 1);

        keys = new short[curn + 1];

        for (short i = (short) (length - 1); i >= 0; i--) {
            if (i == key) {
                keys[curn] = i;
                map[i] = curn--;
            } else {
                int idx = m.get(i);
                if (idx != -1) {
                    keys[curn] = i;
                    map[i] = curn--;
                } else {
                    map[i] = (short) -1;
                }
            }
        }
    }

    void useDonorMap(IntMapper d, BlackBoard board, Object[] array2fill) {
        if (map != null)
            throw new RuntimeException();

        map = new short[board.getDepth() + 1];
        keys = new short[board.getNumberOfObjects()];

        d.mapOwner = this;
        mapOwner = null;

        short curn = 0;
        for (short i = 0; i < board.getDepth() + 1; i++) {
            Object o = board.get(i);
            if (o != null) {
                if (array2fill != null) {
                    array2fill[curn] = o;
                }
                keys[curn] = i;
                map[i] = curn++;
                board.erase(i);
            } else {
                map[i] = (short) -1;
            }
        }

        d.keys[0] = -2;
        d.keys = null;
        d.map[0] = -2;
        d.map = null;
    }

    void arrayFill(BlackBoard board, Object[] array2fill) {
        short curn = 0;
        for (int i = 0; i < board.getDepth() + 1; i++) {
            Object o = board.get(i);
            if (o != null) {
                if (array2fill != null) {
                    array2fill[curn++] = o;
                }
                board.erase(i);
            }
        }
    }

    void useDonorMap(IntMapper d, IntMapper m, int length, int key, Object value, Object[] array2fill, Object[] data) {
        if (map != null)
            throw new RuntimeException();

        map = new short[length];

        d.mapOwner = this;
        mapOwner = null;

        short curn = (short) (m.get(m.depth) + 1);
        keys = new short[curn + 1];
        for (short i = (short) (length - 1); i >= 0; i--) {
            if (i == key) {
                array2fill[curn] = value;
                keys[curn] = i;
                map[i] = curn--;
            } else {
                int idx = m.get(i);
                if (idx != -1) {
                    array2fill[curn] = data[idx];
                    keys[curn] = i;
                    map[i] = curn--;
                } else {
                    map[i] = (short) -1;
                }
            }
        }


        d.map[0] = -2;
        d.map = null;
        d.keys[0] = -2;
        d.keys = null;
    }

    void useDonorMap(IntMapper d, IntMapper m, int length, int key) {
        if (map != null)
            throw new RuntimeException();

        map = new short[length];

        d.mapOwner = this;
        mapOwner = null;

        short curn = (short) (m.get(m.depth) + 1);
        keys = new short[curn + 1];
        for (short i = (short) (length - 1); i >= 0; i--) {
            if (i == key) {
                keys[curn] = i;
                map[i] = curn--;
            } else {
                int idx = m.get(i);
                if (idx != -1) {
                    keys[curn] = i;
                    map[i] = curn--;
                } else {
                    map[i] = (short) -1;
                }
            }
        }


        d.map[0] = -2;
        d.map = null;
        d.keys[0] = -2;
        d.keys = null;
    }

    void arrayFill(IntMapper m, int length, int key, Object value, Object[] array2fill, Object[] data) {
        short curn = (short) (m.get(m.depth) + 1);
        for (int i = length - 1; i >= 0; i--) {
            if (i == key) {
                array2fill[curn--] = value;
            } else {
                int idx = m.get(i);
                if (idx != -1) {
                    array2fill[curn--] = data[idx];
                }
            }
        }
    }

    public String toString() {
        String t = "( ";
        boolean first = true;
        if (map == null && mapOwner == null)
            return "( blank )";


        for (int i = 0; i <= depth; i++) {
            if (get((short) i) != -1) {
                if (!first) {
                    t += ", ";
                } else {
                    first = false;
                }
                t += i + " -> " + get((short) i);
            }
        }
        if (mapOwner == null)
            t += " )*";
        else
            t += " )";
        return t;
    }

    public void appendSelf(ByteBuffer buf) {
        int l = getMaxValue() + 1;
        buf.put((byte) l);
        for (int i = 0; i < l; i++) {
            buf.put((byte) keys[i]);
        }
    }

    public byte[] getByteRepresentation() {
        byte[] result = new byte[getByteSize()];

        int l = getMaxValue() + 1;

        result[0] = (byte) l;
        for (int i = 0; i < l; i++) {
            result[1 + i] = (byte) keys[i];
        }

        return result;
    }

    public int getByteSize() {
        return 1 + getMaxValue() + 1;
    }

    public int getIndex() {
        return index;
    }
}
