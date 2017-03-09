/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import java.nio.ByteBuffer;

public class IntMapperStorage {
    private static IntMapperStorage instance = new IntMapperStorage();
    private IntMapper root;
    private IntMapper fake;
    private BlackBoard localBoard;
    private int nNodes;
    private int nNonBlanks;
    private int nLocatedTables;

    public IntMapperStorage() {
        root = new IntMapper((short) 0, null, null, 0);
        fake = new IntMapper((short) -1, null, null, -1);
        localBoard = new BlackBoardImpl(100, false);
        nNodes = 1;
        nNonBlanks = 0;
        nLocatedTables = 0;
    }

    public static IntMapperStorage getInstance() {
        return instance;
    }

    void pack() {
        pack(root);
    }

    private void pack(IntMapper m) {
        if (m != null) {
            pack(m.t);
            pack(m.f);
            m.get(0);
        }
    }

    IntMapper getIntMapper(int arr[]) {
        localBoard.clean();

        for (int i = 0; i < arr.length; i++) {
            localBoard.put(arr[i], localBoard); //It's not important which pointer you'll put there.
            //The important thing is that It must not be null-pointer

        }
        return getIntMapper((Object[]) null, localBoard);
    }

    public IntMapper getIntMapper(byte[] buf, int from) {
        byte length = buf[from++];

        localBoard.clean();

        for (int i = 0; i < length; i++) {
            localBoard.put(buf[from + i], localBoard); //It's not important which pointer you'll put there.
            //The important thing is that It must not be null-pointer

        }
        return getIntMapper((Object[]) null, localBoard);
    }

    public IntMapper getIntMapper(ByteBuffer buf) {
        byte length = buf.get();

        localBoard.clean();

        for (int i = 0; i < length; i++) {
            localBoard.put(buf.get(), localBoard); //It's not important which pointer you'll put there.
            //The important thing is that It must not be null-pointer

        }
        return getIntMapper((Object[]) null, localBoard);
    }

    IntMapper getIntMapper(int arr[], Object[] objs) {
        if (arr.length != objs.length)
            throw new IllegalArgumentException();

        localBoard.clean();

        for (int i = 0; i < arr.length; i++) {
            localBoard.put(arr[i], objs[i]);
        }
        return getIntMapper(objs, localBoard);
    }

    public IntMapper getIntMapper(BlackBoard board) {
        return getIntMapper((Object[]) null, board);
    }

    IntMapper getIntMapper(Object[] array2fill, BlackBoard board) {
        IntMapper cur = root, next;
        IntMapper donor = null;
        int i;
        if (board == null)
            throw new RuntimeException();
        if (array2fill != null && array2fill.length < board.getNumberOfObjects())
            throw new IllegalArgumentException();

        int bdepth;
        if ((bdepth = board.getDepth()) < 0)
            throw new RuntimeException();

        if (bdepth != 0) {
            i = 0;
            while (i <= bdepth) {
                if (board.contains(i)) {
                    i++;
                    if (i > bdepth)
                        continue;
                    if (cur.t != null) {
                        next = cur.t;
                        while (board.contains(i) && i < next.depth && i < bdepth) {
                            i++;
                        }
                        if (i < next.depth) {
                            next = new IntMapper((short) i, next, null, nNodes++);
                            cur.t = next;
                        }
                    } else {
                        while (board.contains(i) && i < bdepth) {
                            i++;
                        }
                        next = new IntMapper((short) i, null, null, nNodes++);
                        cur.t = next;
                    }
                    if (cur.mapOwner == null && cur.map != null) {
                        donor = cur;
                    }
                } else {
                    i++;
                    if (cur.f != null) {
                        next = cur.f;
                        while (!board.contains(i) && i < next.depth && i < bdepth) {
                            i++;
                        }
                        if (i < next.depth) {
                            next = new IntMapper((short) i, null, next, nNodes++);
                            cur.f = next;
                        }
                    } else {
                        while (!board.contains(i) && i < bdepth) {
                            i++;
                        }
                        next = new IntMapper((short) i, null, null, nNodes++);
                        cur.f = next;
                    }
                }
                cur = next;
            }
        }

        if (!cur.ready()) {
            if (donor == null) {
                cur.assignMap(board, array2fill);
                nLocatedTables++;
            } else {
                cur.useDonorMap(donor, board, array2fill);
            }
            nNonBlanks++;
        } else {
            cur.arrayFill(board, array2fill);
        }

        return cur;
    }

    IntMapper getExtension(Object[] array2fill, Object[] data, IntMapper m, int key, Object value) {
        IntMapper cur = root, next;
        IntMapper donor = null;
        int i;

        if (m == null) {
            if (data != null)
                throw new IllegalArgumentException();
            return getExtension(array2fill, null, fake, key, value);
        }

        if (key < 0)
            throw new IllegalArgumentException();

        if (m.get((short) key) != -1)
            return m;

        int depth = key > m.depth ? key : m.depth;

        i = 0;
        while (i <= depth) {
            if (i == key || m.get(i) != -1) {
                i++;
                if (i > depth)
                    continue;
                if (cur.t != null) {
                    next = cur.t;
                    while ((i == key || m.get(i) != -1) && i < next.depth && i < depth) {
                        i++;
                    }
                    if (i < next.depth) {
                        next = new IntMapper((short) i, next, null, nNodes++);
                        cur.t = next;
                    }
                } else {
                    while ((i == key || m.get(i) != -1) && i < depth) {
                        i++;
                    }
                    next = new IntMapper((short) i, null, null, nNodes++);
                    cur.t = next;
                }
                if (cur.mapOwner == null && cur.map != null) {
                    donor = cur;
                }
            } else {
                i++;
                if (cur.f != null) {
                    next = cur.f;
                    while ((i != key && m.get(i) == -1) && i < next.depth && i < depth) {
                        i++;
                    }
                    if (i < next.depth) {
                        next = new IntMapper((short) i, null, next, nNodes++);
                        cur.f = next;
                    }
                } else {
                    while ((i != key && m.get(i) == -1) && i < depth) {
                        i++;
                    }
                    next = new IntMapper((short) i, null, null, nNodes++);
                    cur.f = next;
                }
            }
            cur = next;
        }

        if (!cur.ready()) {
            if (donor == null) {
                cur.assignMap(m, depth + 1, key, value, array2fill, data);
                nLocatedTables++;
            } else {
                cur.useDonorMap(donor, m, depth + 1, key, value, array2fill, data);
            }
            nNonBlanks++;
        } else {
            cur.arrayFill(m, depth + 1, key, value, array2fill, data);
        }
        return cur;
    }

    public IntMapper getExtension(IntMapper m, int key) {
        IntMapper cur = root, next;
        IntMapper donor = null;
        int i;

        if (m == null) {
            return getExtension(fake, key);
        }

        if (key < 0)
            throw new IllegalArgumentException();

        if (m.get((short) key) != -1)
            return m;

        int depth = key > m.depth ? key : m.depth;

        i = 0;
        while (i <= depth) {
            if (i == key || m.get(i) != -1) {
                i++;
                if (i > depth)
                    continue;
                if (cur.t != null) {
                    next = cur.t;
                    while ((i == key || m.get(i) != -1) && i < next.depth && i < depth) {
                        i++;
                    }
                    if (i < next.depth) {
                        next = new IntMapper((short) i, next, null, nNodes++);
                        cur.t = next;
                    }
                } else {
                    while ((i == key || m.get(i) != -1) && i < depth) {
                        i++;
                    }
                    next = new IntMapper((short) i, null, null, nNodes++);
                    cur.t = next;
                }
                if (cur.mapOwner == null && cur.map != null) {
                    donor = cur;
                }
            } else {
                i++;
                if (cur.f != null) {
                    next = cur.f;
                    while ((i != key && m.get(i) == -1) && i < next.depth && i < depth) {
                        i++;
                    }
                    if (i < next.depth) {
                        next = new IntMapper((short) i, null, next, nNodes++);
                        cur.f = next;
                    }
                } else {
                    while ((i != key && m.get(i) == -1) && i < depth) {
                        i++;
                    }
                    next = new IntMapper((short) i, null, null, nNodes++);
                    cur.f = next;
                }
            }
            cur = next;
        }

        if (!cur.ready()) {
            if (donor == null) {
                cur.assignMap(m, depth + 1, key);
                nLocatedTables++;
            } else {
                cur.useDonorMap(donor, m, depth + 1, key);
            }
            nNonBlanks++;
        }
        return cur;
    }

    public int getNumberOfNodes() {
        return nNodes;
    }

    public int getNumberOfNonBlanks() {
        return nNonBlanks;
    }

    public int getNumberOfLocatedTables() {
        return nLocatedTables;
    }

}
