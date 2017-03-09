/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeUtils;
import treeton.core.util.NumeratedObject;
import treeton.core.util.nu;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BlackBoardImpl implements BlackBoard {
    private Object[] board;
    private Object[] clean;
    private int[] lastStack;
    private int[] firstStack;
    private int nObjects;
    private int lastStackPtr;
    private int firstStackPtr;
    private boolean lock;

    BlackBoardImpl(int size, boolean _lock) {
        if (size <= 0)
            throw new IllegalArgumentException();
        lastStackPtr = 0;
        firstStackPtr = 0;
        board = new Object[size];
        clean = new Object[size];
        lastStack = new int[size + 1];
        lastStack[0] = -1;
        firstStack = new int[size + 1];
        firstStack[0] = -1;
        nObjects = 0;
        lock = _lock;
        Arrays.fill(clean, null);
        System.arraycopy(clean, 0, board, 0, size);
    }

    public void put(NumeratedObject[] arr) {
        if (arr == null)
            return;
        for (int i = 0; i < arr.length; i++) {
            put(arr[i].n, arr[i].o);
        }
    }

    public void put(NumeratedObject[] arr, BlackBoard featureFilter) {
        if (arr == null)
            return;
        for (int i = 0; i < arr.length; i++) {
            int n = arr[i].n;
            if (featureFilter.get(n) == null)
                put(n, arr[i].o);
        }
    }

    public void put(NumeratedObject[] arr, Set<Integer> filter) {
        if (arr == null)
            return;
        for (int i = 0; i < arr.length; i++) {
            int n = arr[i].n;
            if (!filter.contains(n))
                put(n, arr[i].o);
        }
    }


    public void put(int n, Object o, BlackBoard featureFilter) {
        if (featureFilter.get(n) == null)
            put(n, o);
    }

    public void put(int n, Object o) {
        if (n < 0)
            throw new IllegalArgumentException();

        if (o == null)
            o = nu.ll;

        if (n >= board.length) {
            Object[] t;
            int[] s1;
            int[] s2;
            int size = (int) (Math.max(board.length * 1.5, n + 1));
            t = new Object[size];
            clean = new Object[size];
            s1 = new int[size + 1];
            s2 = new int[size + 1];
            Arrays.fill(clean, null);
            System.arraycopy(clean, 0, t, 0, size);
            System.arraycopy(board, 0, t, 0, board.length);
            System.arraycopy(lastStack, 0, s1, 0, lastStack.length);
            System.arraycopy(firstStack, 0, s2, 0, firstStack.length);
            board = t;
            lastStack = s1;
            firstStack = s2;
        }

        if (n > lastStack[lastStackPtr]) {
            lastStackPtr++;
            lastStack[lastStackPtr] = n;
        }
        if (firstStack[firstStackPtr] == -1 || n < firstStack[firstStackPtr]) {
            firstStackPtr++;
            firstStack[firstStackPtr] = n;
        }
        if (board[n] == null)
            nObjects++;
        board[n] = o;
    }

    public void put(String s, TrnType type, Object o) {
        int n;
        try {
            n = type.getFeatureIndex(s);
        } catch (TreetonModelException e) {
            n = -1;
        }
        if (n == -1) {
            throw new IllegalArgumentException("Unregistered feature " + s);
        }
        put(n, type, o);
    }

    public void put(int n, TrnType type, Object o) {
        put(n, TrnTypeUtils.treatFeatureValue(type, n, o));
    }

    public void put(String s, TrnType type, Object o, BlackBoard featureFilter) {
        int n;
        try {
            n = type.getFeatureIndex(s);
        } catch (TreetonModelException e) {
            n = -1;
        }
        if (n == -1)
            throw new IllegalArgumentException("Unregistered feature " + s);
        else
            put(n, type, o, featureFilter);
    }

    public void put(int n, TrnType type, Object o, BlackBoard featureFilter) {
        put(n, TrnTypeUtils.treatFeatureValue(type, n, o), featureFilter);
    }

    public Object get(int n) {
        if (n < 0)
            throw new IllegalArgumentException();
        if (n >= board.length)
            return null;
        return board[n];
    }

    public boolean contains(int n) {
        if (n < 0)
            throw new IllegalArgumentException();
        if (n >= board.length)
            return false;
        return board[n] != null ? true : false;
    }

    public Object erase(int n) {
        if (lock) {
            return get(n);
        }
        Object t;
        if (n < 0)
            throw new IllegalArgumentException();
        if (n >= board.length)
            return null;
        if (n == lastStack[lastStackPtr]) {
            lastStackPtr--;
            while (lastStackPtr > 0 && board[lastStack[lastStackPtr]] == null) {
                lastStackPtr--;
            }
        }
        if (n == firstStack[firstStackPtr]) {
            firstStackPtr--;
            while (firstStackPtr > 0 && board[firstStack[firstStackPtr]] == null) {
                firstStackPtr--;
            }
        }
        t = board[n];
        if (t != null)
            nObjects--;
        board[n] = null;
        return t;
    }

    public boolean containsErase(int n) {
        Object t;
        if (lock) {
            return contains(n);
        }
        if (n < 0)
            throw new IllegalArgumentException();
        if (n >= board.length)
            return false;
        if (n == lastStack[lastStackPtr]) {
            lastStackPtr--;
            while (lastStackPtr > 0 && board[lastStack[lastStackPtr]] == null) {
                lastStackPtr--;
            }
        }
        if (n == firstStack[firstStackPtr]) {
            firstStackPtr--;
            while (firstStackPtr > 0 && board[firstStack[firstStackPtr]] == null) {
                firstStackPtr--;
            }
        }
        t = board[n];
        if (t != null)
            nObjects--;
        board[n] = null;
        return t != null ? true : false;
    }

    public void clean() {
        if (lastStackPtr > 0)
            System.arraycopy(clean, 0, board, 0, lastStack[lastStackPtr] + 1);
        lastStackPtr = 0;
        firstStackPtr = 0;
        nObjects = 0;
    }

    public int getDepth() {
        return lastStack[lastStackPtr];
    }

    public int getFirstNumber() {
        return firstStack[firstStackPtr];
    }

    public int getNumberOfObjects() {
        return nObjects;
    }

    public void fill(TrnType tp, Map map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            int i;
            try {
                i = tp.getFeatureIndex(e.getKey().toString());
            } catch (TreetonModelException e1) {
                i = -1;
            }
            if (i == -1) {
                throw new IllegalArgumentException("Unregistered feature " + e.getKey());
            }
            put(i, TrnTypeUtils.treatFeatureValue(tp, i, e.getValue()));
        }
    }

    public void appendTrnStringView(StringBuffer buf, TrnType type) {
        try {
            buf.append(type.getIndex());
        } catch (TreetonModelException e) {
            //do nothing
        }
        buf.append((char) 0);
        int l = 0;
        try {
            l = type.getFeaturesSize();
        } catch (TreetonModelException e) {
            l = -1;
        }
        for (int i = 0; i < l; i++) {
            Object o = erase(i);
            if (o == null) {
                buf.append((char) 1);
            } else {
                o = TrnTypeUtils.treatFeatureValue(type, i, o);
                if (o instanceof TStringImpl) {
                    ((TStringImpl) o).appendToStringBuffer(buf);
                    buf.append((char) 0);
                } else if (o instanceof Integer) {
                    buf.append(((Integer) o).intValue());
                    buf.append((char) 0);
                }
            }
        }
    }

    public void treatObjects(TrnType tp) {
        int d = getDepth();
        for (int i = 0; i <= d; i++) {
            Object o = board[i];
            if (o != null) {
                if (o instanceof Object[]) {
                    Object[] arr = (Object[]) o;
                    for (int j = 0; j < arr.length; j++) {
                        arr[j] = TrnTypeUtils.treatFeatureValue(tp, i, arr[j]);
                    }
                } else {
                    board[i] = TrnTypeUtils.treatFeatureValue(tp, i, o);
                }
            }
        }
    }

    public void put(BlackBoard board) {
        for (int i = 0; i <= board.getDepth(); i++) {
            Object o = board.get(i);
            if (o == null)
                continue;
            put(i, o);
        }
    }
}
