/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.fsm.logicset.LogicFSM;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeUtils;
import treeton.core.util.*;

import java.util.Arrays;
import java.util.Iterator;

public class IntFeatureMapImpl implements IntFeatureMap {
    final static Class fakeClass = _fake.class;
    private static final BlackBoard localBoard = TreetonFactory.newBlackBoard(20, false);
    static int[] arr = new int[100];
    static Class[] clsArr = new Class[100];
    static TString fakeTString = TreetonFactory.newTString("");

    static {
        Arrays.fill(arr, 0);
        Arrays.fill(clsArr, fakeClass);
    }

    IntMapper mapper;
    int blockSize;
    Object[] data;

    public IntFeatureMapImpl() {
        this(null, -1);
    }

    public IntFeatureMapImpl(int _blockSize) {
        this(null, _blockSize);
    }

    public IntFeatureMapImpl(BlackBoard board) {
        this(board, -1);
    }

    public IntFeatureMapImpl(BlackBoard board, int _blockSize) {
        int n;
        IntMapperStorage storage = IntMapperStorage.getInstance();
        data = null;
        mapper = null;
        blockSize = _blockSize;
        if (board == null || (n = board.getNumberOfObjects()) <= 0)
            return;
        if (blockSize <= 0) {
            blockSize = n;
        }
        data = new Object[(((n - 1) / blockSize) + 1) * blockSize];
        mapper = storage.getIntMapper(data, board);
    }

    public static int getMostRelevantKey(Iterator featureMaps, int[] filter, int filterLength, Class[] _cls) {
        synchronized (fakeClass) {
            int maxDepth = 0;

            if (_cls != null && _cls.length < 1)
                throw new IllegalArgumentException();

            for (int i = 0; i < filterLength; i++) {
                int f = filter[i];
                if (f >= arr.length) {
                    int[] t = new int[f + 1];
                    System.arraycopy(arr, 0, t, 0, arr.length);
                    Arrays.fill(t, arr.length, f + 1, 0);
                    arr = t;
                    Class[] tc = new Class[f + 1];
                    System.arraycopy(clsArr, 0, tc, 0, clsArr.length);
                    Arrays.fill(tc, clsArr.length, f + 1, fakeClass);
                    clsArr = tc;
                }
                arr[f] = -1;
                if (f > maxDepth)
                    maxDepth = f;
            }
            int max = -1, j, maxj = -1;
            while (featureMaps.hasNext()) {
                IntFeatureMapImpl fm = (IntFeatureMapImpl) featureMaps.next();
                int l = fm.mapper == null ? -1 : fm.mapper.getMaxValue();
                for (int i = 0; i <= l; i++) {
                    Object o;
                    j = fm.mapper.keys[i];
                    if (j > maxDepth)
                        maxDepth = j;
                    if (j >= arr.length) {
                        int[] t = new int[j + 1];
                        System.arraycopy(arr, 0, t, 0, arr.length);
                        Arrays.fill(t, arr.length, j + 1, 0);
                        arr = t;
                        Class[] tc = new Class[j + 1];
                        System.arraycopy(clsArr, 0, tc, 0, clsArr.length);
                        Arrays.fill(tc, clsArr.length, j + 1, fakeClass);
                        clsArr = tc;
                    }
                    if (arr[j] != -1) {
                        arr[j]++;
                        if ((o = fm.data[i]) != null) {
                            if (clsArr[j] == fakeClass) {
                                clsArr[j] = o.getClass();
                            } else if (clsArr[j] != null) {
                                if (clsArr[j] != o.getClass()) {
                                    clsArr[j] = null;
                                }
                            }
                        } else {
                            if (clsArr[j] == fakeClass) {
                                clsArr[j] = fakeTString.getClass();
                            } else if (clsArr[j] != null) {
                                if (clsArr[j] != fakeTString.getClass()) {
                                    clsArr[j] = null;
                                }
                            }
                        }
                        if (arr[j] > max) {
                            max = arr[j];
                            maxj = j;
                        }
                    }
                }
            }

            if (maxj == -1) {
                _cls[0] = null;

                Arrays.fill(arr, 0, maxDepth + 1, 0);
                Arrays.fill(clsArr, 0, maxDepth + 1, fakeClass);

                return -1;
            }
            _cls[0] = clsArr[maxj];

            Arrays.fill(arr, 0, maxDepth + 1, 0);
            Arrays.fill(clsArr, 0, maxDepth + 1, fakeClass);

            return maxj;
        }
    }

    public static int getMostRelevantKey(Iterator featureMaps, int[] filter, int filterLength) {
        synchronized (fakeClass) {
            int maxDepth = 0;

            for (int i = 0; i < filterLength; i++) {
                int f = filter[i];
                if (f >= arr.length) {
                    int[] t = new int[f + 1];
                    System.arraycopy(arr, 0, t, 0, arr.length);
                    Arrays.fill(t, arr.length, f + 1, 0);
                    arr = t;
                    Class[] tc = new Class[f + 1];
                    Arrays.fill(tc, 0, f + 1, fakeClass);
                    clsArr = tc;
                }
                arr[f] = -1;
                if (f > maxDepth)
                    maxDepth = f;
            }
            int max = -1, j, maxj = -1;
            while (featureMaps.hasNext()) {
                IntFeatureMapImpl fm = (IntFeatureMapImpl) featureMaps.next();
                int l = fm.mapper == null ? -1 : fm.mapper.getMaxValue();
                for (int i = 0; i <= l; i++) {
                    j = fm.mapper.keys[i];
                    if (j > maxDepth)
                        maxDepth = j;
                    if (j >= arr.length) {
                        int[] t = new int[j + 1];
                        System.arraycopy(arr, 0, t, 0, arr.length);
                        Arrays.fill(t, arr.length, j + 1, 0);
                        arr = t;
                        Class[] tc = new Class[j + 1];
                        Arrays.fill(tc, 0, j + 1, fakeClass);
                        clsArr = tc;
                    }
                    if (arr[j] != -1) {
                        arr[j]++;
                        if (arr[j] > max) {
                            max = arr[j];
                            maxj = j;
                        }
                    }
                }
            }

            Arrays.fill(arr, 0, maxDepth + 1, 0);

            return maxj;
        }
    }

    public static boolean eq(IntFeatureMap i1, IntFeatureMap i2) {
        if (i1.size() != i2.size())
            return false;

        int len = i1.size();

        for (int i = 0; i < len; i++) {
            int key = i1.getKey(i);
            Object o1 = i1.getByIndex(i);
            Object o2 = i2.get(key);

            if (!Utils.smartEquals(o1, o2))
                return false;
        }

        return true;
    }

    public IntMapper getIntMapper() {
        return mapper;
    }

    public Object get(int key) {
        if (mapper == null)
            return null;
        int idx = mapper.get(key);
        if (idx < 0)
            return null;
        return data[idx];
    }

    public Object getByIndex(int idx) {
        if (idx < 0 || idx >= data.length)
            return null;
        return data[idx];
    }

    public void putByIndex(int idx, Object o) {
        if (idx < 0 || idx >= data.length)
            return;
        data[idx] = o;
    }

    public boolean contains(int key) {
        if (mapper == null)
            return false;
        int idx = mapper.get(key);
        if (idx >= 0)
            return true;
        return false;
    }

    public void put(int key, Object value) {
        IntMapperStorage storage = IntMapperStorage.getInstance();

        if (mapper == null) {
            if (blockSize <= 0)
                blockSize = 16;
            data = new Object[blockSize];
            mapper = storage.getExtension(data, null, null, key, value);
            return;
        }
        int idx = mapper.get(key);
        if (idx != -1) {
            data[idx] = value;
        } else {
            Object t[];
            int max;
            if ((max = mapper.getMaxValue()) == (data.length - 1)) {
                t = new Object[(((max + 1) / blockSize) + 1) * blockSize];
            } else {
                t = data;
            }
            mapper = storage.getExtension(t, data, mapper, key, value);
            data = t;
        }
    }

    public void put(int key, Object value, TrnType tp) {
        value = TrnTypeUtils.treatFeatureValue(tp, key, value);
        IntMapperStorage storage = IntMapperStorage.getInstance();

        if (mapper == null) {
            if (blockSize <= 0)
                blockSize = 16;
            data = new Object[blockSize];
            mapper = storage.getExtension(data, null, null, key, value);
            return;
        }
        int idx = mapper.get(key);
        if (idx != -1) {
            data[idx] = value;
        } else {
            Object t[];
            int max;
            if ((max = mapper.getMaxValue()) == (data.length - 1)) {
                t = new Object[(((max + 1) / blockSize) + 1) * blockSize];
            } else {
                t = data;
            }
            mapper = storage.getExtension(t, data, mapper, key, value);
            data = t;
        }
    }

    public void removeLight(int key) {
        if (mapper == null) {
            return;
        }
        int idx = mapper.get(key);
        if (idx != -1) {
            data[idx] = null;
        }
    }

    public void remove(int key) {
        synchronized (localBoard) {
            if (mapper == null)
                return;

            int j = mapper.get(key);
            if (j < 0)
                return;

            int len = mapper.getMaxValue();

            if (len == 0) {
                mapper = null;
                data = null;
                return;
            }


            for (int i = j; i < len; i++) {
                data[i] = data[i + 1];
            }
            short[] arr = mapper.getKeys();

            j = 0;
            for (int i = 0; i <= len; i++) {
                if (arr[i] != key)
                    localBoard.put(arr[i], data[j++]);
            }
            mapper = IntMapperStorage.getInstance().getIntMapper(localBoard);
        }
    }

    public void removeAll() {
        mapper = null;
        data = null;
    }

    public void put(BlackBoard board) {
        IntMapperStorage storage = IntMapperStorage.getInstance();

        if (mapper == null) {
            if (board == null)
                return;
            int n = board.getNumberOfObjects();
            if (n <= 0)
                return;
            if (blockSize <= 0)
                blockSize = n;
            data = new Object[(((n - 1) / blockSize) + 1) * blockSize];
            mapper = storage.getIntMapper(data, board);
        } else {
            for (int i = 0; i <= board.getDepth(); i++) {
                if (board.contains(i)) {
                    Object o = board.erase(i);
                    put(i, o);
                }
            }
        }
    }

    public void put(BlackBoard board, TrnType tp) {
        if (board == null)
            return;
        board.treatObjects(tp);

        IntMapperStorage storage = IntMapperStorage.getInstance();

        if (mapper == null) {
            int n = board.getNumberOfObjects();
            if (n <= 0)
                return;
            if (blockSize <= 0)
                blockSize = n;
            data = new Object[(((n - 1) / blockSize) + 1) * blockSize];
            mapper = storage.getIntMapper(data, board);
        } else {
            for (int i = 0; i <= board.getDepth(); i++) {
                if (board.contains(i)) {
                    Object o = board.erase(i);
                    put(i, o);
                }
            }
        }
    }

    public Iterator valueIterator() {
        return new ValueIterator();
    }

    public Iterator numeratedObjectIterator() {
        return new NumeratedObjectIterator();
    }

    public Treenotation convertToTreenotation(TrnType tp) {
        TreenotationImpl t = new TreenotationImpl(null, null, tp);
        if (mapper != null) {
            t.mapper = mapper;
            t.blockSize = blockSize;
            t.data = new Object[data.length];
            System.arraycopy(data, 0, t.data, 0, data.length);
        }
        return t;
    }

    public Treenotation convertToTreenotation(Token start, Token end, TrnType tp) {
        TreenotationImpl t = new TreenotationImpl(start, end, tp);
        if (mapper != null) {
            t.mapper = mapper;
            t.blockSize = blockSize;
            t.data = new Object[data.length];
            System.arraycopy(data, 0, t.data, 0, data.length);
        }
        return t;
    }

    public void fillBlackBoard(BlackBoard board) {
        if (mapper == null)
            return;
        int l = mapper.getMaxValue();
        for (int i = 0; i <= l; i++) {
            if (data[i] != null) {
                board.put(mapper.keys[i], data[i]);
            }
        }
    }

    public int size() {
        if (mapper != null)
            return mapper.getMaxValue() + 1;
        return 0;
    }

    public void importFrom(IntFeatureMapImpl m) {
        mapper = m.mapper;
        data = m.data;
        blockSize = m.blockSize;
        m.mapper = null;
        m.data = null;
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
                String val;
                if (no.o instanceof Object[]) {
                    val = "[";
                    Object[] arr = (Object[]) no.o;
                    for (int i = 0; i < arr.length; i++) {
                        if (i > 0) {
                            val = val + ",";
                        }
                        if (arr[i] instanceof Treenotation) {
                            val = val + "Trn#" + ((Treenotation) arr[i]).getId();
                        } else {
                            val = val + arr[i].toString();
                        }
                    }
                    val = val + ']';
                } else {
                    if (no.o instanceof Treenotation) {
                        val = "Trn#" + ((Treenotation) no.o).getId();
                    } else {
                        val = no.o.toString();
                    }
                }
                s += "<b>" + type.getFeatureNameByIndex(no.n).toString() + "</b>=" + val + "; ";
            }
            return s;
        } catch (TreetonModelException e) {
            return "Exception";
        }
    }

    public void appendSelf(StringBuffer buf, FeaturesValueCollector collector) {
        buf.append(sut.integerToChars(collector.getValueIndex(mapper)));
        int maxi = mapper.getMaxValue(), i = 0;
        while (i <= maxi) {
            buf.append(sut.integerToChars(collector.getValueIndex(data[i])));
            i++;
        }
    }

    public Object clone() {
        IntFeatureMapImpl t = new IntFeatureMapImpl();
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

    public void makeCopyFrom(IntFeatureMapImpl other) {
        blockSize = other.blockSize;
        mapper = other.mapper;
        if (mapper != null) {
            data = new Object[other.data.length];
            System.arraycopy(other.data, 0, data, 0, other.data.length);
        } else {
            data = null;
        }
    }

    public boolean matches(IntFeatureMap template) {
        if (template == null) {
            throw new IllegalArgumentException();
        }
        IntFeatureMapImpl smap = (IntFeatureMapImpl) template;

        if (smap.mapper == null) {
            return true;
        }

        int l = smap.mapper.getMaxValue();
        for (int i = 0; i <= l; i++) {
            Object o = smap.data[i];
            if (o != null) {
                int other_feature = smap.mapper.keys[i];
                if (
                        other_feature == TrnType.string_FEATURE ||
                                other_feature == TrnType.length_FEATURE ||
                                other_feature == TrnType.orthm_FEATURE ||
                                other_feature == TrnType.start_FEATURE ||
                                other_feature == TrnType.end_FEATURE
                        )
                    continue;
                Object o1 = get(other_feature);
                if (o instanceof LogicFSM) {
                    LogicFSM fsm = (LogicFSM) o;
                    if (o1 == null || fsm.match(o1.toString()) == null) {
                        return false;
                    }
                } else {
                    if (o == nu.ll) {
                        if (o1 != null) {
                            return false;
                        }
                    } else {
                        if (o1 == null) {
                            return false;
                        }
                        if (!o.equals(o1)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public void leaveIntersection(IntFeatureMap map) {
        synchronized (localBoard) {
            if (mapper == null)
                return;

            int len = mapper.getMaxValue();

            short[] arr = mapper.getKeys();

            for (int i = 0; i <= len; i++) {
                int key = arr[i];
                Object o = map.get(key);
                if (o != null && o.equals(data[i])) {
                    localBoard.put(arr[i], o);
                }
            }
            mapper = null;
            data = null;
            put(localBoard);
        }
    }

    public void leaveIntersection(Iterable<? extends IntFeatureMap> maps) {
        synchronized (localBoard) {
            if (mapper == null)
                return;

            int len = mapper.getMaxValue();

            short[] arr = mapper.getKeys();

            for (int i = 0; i <= len; i++) {
                int key = arr[i];
                boolean leave = true;

                for (IntFeatureMap map : maps) {
                    Object o = map.get(key);
                    if (o == null || !o.equals(data[i])) {
                        leave = false;
                    }
                }
                if (leave)
                    localBoard.put(arr[i], data[i]);
            }
            mapper = null;
            data = null;
            put(localBoard);
        }
    }

    public void leaveDifference(IntFeatureMap map) {
        synchronized (localBoard) {
            if (mapper == null)
                return;

            int len = mapper.getMaxValue();

            short[] arr = mapper.getKeys();

            for (int i = 0; i <= len; i++) {
                int key = arr[i];
                Object o = map.get(key);
                if (o == null || !o.equals(data[i])) {
                    localBoard.put(arr[i], data[i]);
                }
            }
            mapper = null;
            data = null;
            put(localBoard);
        }
    }

    public int getKey(int n) {
        if (mapper == null) {
            return -1;
        } else {
            return mapper.getKey(n);
        }
    }

    private class ValueIterator implements Iterator {
        int i;
        int maxi;

        ValueIterator() {
            i = 0;
            if (mapper != null) {
                maxi = mapper.getMaxValue();
                while (i <= maxi && data[i] == null) i++;
            } else {
                maxi = -1;
            }
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
            Object o = data[i];
            i++;
            while (i <= maxi && data[i] == null) i++;
            return o;
        }
    }

    private class NumeratedObjectIterator implements Iterator {
        int i;
        int maxi;

        NumeratedObjectIterator() {
            i = 0;
            if (mapper != null) {
                maxi = mapper.getMaxValue();

                while (i <= maxi && data[i] == null) {
                    i++;
                }
            } else {
                maxi = -1;
            }
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
            Object o = new NumeratedObject(mapper.keys[i], data[i]);
            i++;
            while (i <= maxi && data[i] == null) {
                i++;
            }
            return o;
        }
    }

    private class _fake {
    }
}
