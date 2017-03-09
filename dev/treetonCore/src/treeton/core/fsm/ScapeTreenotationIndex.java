/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import treeton.core.BlackBoard;
import treeton.core.IntFeatureMapStaticLog;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.model.TrnType;
import treeton.core.util.BlockStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ScapeTreenotationIndex {
    final static BlockStack pairsStack = new BlockStack(100);
    static final int TYPE_DISTRIBUTER = -3;
    static BlackBoard sequenceBoard = null;
    private static int[] stack = new int[100];
    private static int stackpos;
    private static EntryFeatureMapsIterator fmIt = null;
    private static SequenceReader lastNd = null;
    Distributer root;
    FinalNode firstFinal;
    int nFinalNodes;
    int size;

    public ScapeTreenotationIndex(Iterator mix) {
        nFinalNodes = 0;
        size = 0;
        firstFinal = null;
        root = null;

        if (fmIt == null)
            fmIt = new EntryFeatureMapsIterator(null, -1);
        if (sequenceBoard == null)
            sequenceBoard = TreetonFactory.newBlackBoard(100, false);
        if (!mix.hasNext())
            return;
        stackpos = 0;


        ListPair p = newListPair();
        while (mix.hasNext()) {
            Treenotation key = (Treenotation) mix.next();
            Object value = mix.next();
            Entry next = new Entry(key, value);
            if (p.entryListStart == null) {
                p.entryListStart = next;
                p.entryListEnd = next;
            } else {
                p.entryListEnd.next = next;
                p.entryListEnd = next;
            }
            p.entryListLen++;
        }

        HashMap hash = new HashMap();

        Entry cur = p.entryListStart;
        int i = 0;
        while (cur != null && i < p.entryListLen) {
            TrnType t = cur.key.getType();
            ListPair np = (ListPair) hash.get(t);
            if (np == null) {
                if (cur.nFeatures == 0) {
                    hash.put(t, newListPair(null, null, 0, cur, cur, 1));
                } else {
                    hash.put(t, newListPair(cur, cur, 1, null, null, 0));
                }
            } else {
                if (cur.nFeatures == 0) {
                    if (np.readyListStart == null) {
                        np.readyListStart = cur;
                        np.readyListEnd = cur;
                    } else {
                        np.readyListEnd.next = cur;
                        np.readyListEnd = cur;
                    }
                    np.readyListLen++;
                } else {
                    if (np.entryListStart == null) {
                        np.entryListStart = cur;
                        np.entryListEnd = cur;
                    } else {
                        np.entryListEnd.next = cur;
                        np.entryListEnd = cur;
                    }
                    np.entryListLen++;
                }
            }
            cur = cur.next;
            i++;
        }
        root = new Distributer();
        root.map = hash;
        root.key = TYPE_DISTRIBUTER;
        lastNd = null;
        Iterator it = root.map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            ListPair lp = (ListPair) e.getValue();
            if (lp == null)
                continue;
            e.setValue(buildTree(lp));
            freeListPair(lp);
        }
    }

    public ScapeTreenotationIndex(Iterator<Treenotation> keys, Iterator values) {
        nFinalNodes = 0;
        size = 0;
        firstFinal = null;
        root = null;

        if (fmIt == null)
            fmIt = new EntryFeatureMapsIterator(null, -1);
        if (sequenceBoard == null)
            sequenceBoard = TreetonFactory.newBlackBoard(100, false);
        if (!keys.hasNext())
            return;
        stackpos = 0;


        ListPair p = newListPair();
        while (keys.hasNext()) {
            Treenotation key = keys.next();
            Object value = values.next();
            Entry next = new Entry(key, value);
            if (p.entryListStart == null) {
                p.entryListStart = next;
                p.entryListEnd = next;
            } else {
                p.entryListEnd.next = next;
                p.entryListEnd = next;
            }
            p.entryListLen++;
        }

        HashMap hash = new HashMap();
        Entry cur = p.entryListStart;
        int i = 0;
        while (cur != null && i < p.entryListLen) {
            TrnType t = cur.key.getType();
            ListPair np = (ListPair) hash.get(t);
            if (np == null) {
                if (cur.nFeatures == 0) {
                    hash.put(t, newListPair(null, null, 0, cur, cur, 1));
                } else {
                    hash.put(t, newListPair(cur, cur, 1, null, null, 0));
                }
            } else {
                if (cur.nFeatures == 0) {
                    if (np.readyListStart == null) {
                        np.readyListStart = cur;
                        np.readyListEnd = cur;
                    } else {
                        np.readyListEnd.next = cur;
                        np.readyListEnd = cur;
                    }
                    np.readyListLen++;
                } else {
                    if (np.entryListStart == null) {
                        np.entryListStart = cur;
                        np.entryListEnd = cur;
                    } else {
                        np.entryListEnd.next = cur;
                        np.entryListEnd = cur;
                    }
                    np.entryListLen++;
                }
            }
            cur = cur.next;
            i++;
        }
        root = new Distributer();
        root.map = hash;
        root.key = TYPE_DISTRIBUTER;
        lastNd = null;

        Iterator it = root.map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            ListPair lp = (ListPair) e.getValue();
            if (lp == null)
                continue;
            e.setValue(buildTree(lp));
            freeListPair(lp);
        }
    }

    private static void entryValuesToArray(Entry firstEntry, int len, Object[] arr) {
        int i = 0;
        while (firstEntry != null && i < len) {
            arr[i++] = firstEntry.value;
            firstEntry = firstEntry.next;
        }

    }

    private ListPair newListPair(Entry l1s, Entry l1e, int l1len, Entry l2s, Entry l2e, int l2len) {
        synchronized (pairsStack) {
            if (pairsStack.isEmpty()) {
                return new ListPair(l1s, l1e, l1len, l2s, l2e, l2len);
            } else {
                ListPair lp = (ListPair) pairsStack.pop();
                lp.entryListStart = l1s;
                lp.entryListEnd = l1e;
                lp.entryListLen = l1len;
                lp.readyListStart = l2s;
                lp.readyListEnd = l2e;
                lp.readyListLen = l2len;
                return lp;
            }
        }
    }

    private ListPair newListPair() {
        synchronized (pairsStack) {
            if (pairsStack.isEmpty()) {
                return new ListPair();
            } else {
                ListPair lp = (ListPair) pairsStack.pop();
                lp.entryListStart = null;
                lp.entryListEnd = null;
                lp.entryListLen = 0;
                lp.readyListStart = null;
                lp.readyListEnd = null;
                lp.readyListLen = 0;
                return lp;
            }
        }
    }

    private void freeListPair(ListPair lp) {
        synchronized (pairsStack) {
            pairsStack.push(lp);
        }
    }

    private Node buildTree(ListPair p) {
        if (p.entryListLen == 0) {
            if (p.readyListLen > 0) {
                if (lastNd != null) {
                    lastNd.map = new IntFeatureMapStaticLog(sequenceBoard);
                    lastNd.nd = new FinalDistributer(p);
                    lastNd = null;
                    return null;
                }
                return new FinalDistributer(p);
            } else {
                throw new RuntimeException("Impossible");
            }
        }
        fmIt.e = p.entryListStart;
        fmIt.len = p.entryListLen;
        fmIt.c = 0;
        int key = TreetonFactory.getMostRelevantKey(fmIt, stack, stackpos);
        Entry cur = p.entryListStart;
        boolean nullFound = false;
        int i = 0;

        HashMap hash = new HashMap();

        Object lastVal = null;

        while (cur != null && i < p.entryListLen) {
            if (cur.key.contains(key)) {
                lastVal = cur.key.get(key);
                hash.put(lastVal, null);
            } else {
                nullFound = true;
            }
            cur = cur.next;
            i++;
        }


        if (hash.size() == 1 && !nullFound) {
            if (lastNd != null && p.readyListLen > 0) {
                lastNd.map = new IntFeatureMapStaticLog(sequenceBoard);
                lastNd.nd = new FinalSequenceReader(p);
                lastNd = (SequenceReader) lastNd.nd;
            } else if (lastNd == null) {
                if (p.readyListLen > 0) {
                    lastNd = new FinalSequenceReader(p);
                } else {
                    lastNd = new SequenceReader();
                }
            }
            sequenceBoard.put(key, lastVal);
            ListPair np = newListPair();
            cur = p.entryListStart;
            i = 0;
            while (cur != null && i < p.entryListLen) {
                cur.nFeatures--;
                if (cur.nFeatures == 0) {
                    if (np.readyListStart == null) {
                        np.readyListStart = cur;
                        np.readyListEnd = cur;
                    } else {
                        np.readyListEnd.next = cur;
                        np.readyListEnd = cur;
                    }
                    np.readyListLen++;
                } else {
                    if (np.entryListStart == null) {
                        np.entryListStart = cur;
                        np.entryListEnd = cur;
                    } else {
                        np.entryListEnd.next = cur;
                        np.entryListEnd = cur;
                    }
                    np.entryListLen++;
                }
                cur = cur.next;
                i++;
            }
            stack[stackpos++] = key;
            if (stackpos >= stack.length) {
                int[] t = new int[(int) (stack.length * 1.5)];
                System.arraycopy(stack, 0, t, 0, stack.length);
                stack = t;
            }
            SequenceReader t = lastNd;
            buildTree(np);
            freeListPair(np);
            stackpos--;
            return t;
        } else {
            Distributer nd;
            if (p.readyListLen > 0) {
                nd = new FinalDistributer(p);
            } else {
                nd = new Distributer();
            }
            if (lastNd != null) {
                lastNd.map = new IntFeatureMapStaticLog(sequenceBoard);
                lastNd.nd = nd;
                lastNd = null;
            }
            cur = p.entryListStart;
            ListPair otherp = null;
            i = 0;
            while (cur != null && i < p.entryListLen) {
                if (cur.key.contains(key)) {
                    Object val = cur.key.get(key);
                    cur.nFeatures--;
                    Object o = hash.get(val);
                    if (o == null) {
                        if (cur.nFeatures == 0) {
                            hash.put(val, newListPair(null, null, 0, cur, cur, 1));
                        } else {
                            hash.put(val, newListPair(cur, cur, 1, null, null, 0));
                        }
                    } else {
                        ListPair np = (ListPair) o;
                        if (cur.nFeatures == 0) {
                            if (np.readyListStart == null) {
                                np.readyListStart = cur;
                                np.readyListEnd = cur;
                            } else {
                                np.readyListEnd.next = cur;
                                np.readyListEnd = cur;
                            }
                            np.readyListLen++;
                        } else {
                            if (np.entryListStart == null) {
                                np.entryListStart = cur;
                                np.entryListEnd = cur;
                            } else {
                                np.entryListEnd.next = cur;
                                np.entryListEnd = cur;
                            }
                            np.entryListLen++;
                        }
                    }
                } else {
                    if (otherp == null) {
                        otherp = newListPair();
                        otherp.entryListStart = cur;
                        otherp.entryListEnd = cur;
                    } else {
                        otherp.entryListEnd.next = cur;
                        otherp.entryListEnd = cur;
                    }
                    otherp.entryListLen++;
                }
                cur = cur.next;
                i++;
            }
            nd.key = key;
            nd.map = hash;
            stack[stackpos++] = key;
            if (stackpos >= stack.length) {
                int[] t = new int[(int) (stack.length * 1.5)];
                System.arraycopy(stack, 0, t, 0, stack.length);
                stack = t;
            }
            if (otherp != null) {
                nd.otherTransition = buildTree(otherp);
                freeListPair(otherp);
            }

            Iterator it = nd.map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                ListPair lp = (ListPair) e.getValue();
                if (lp == null)
                    continue;
                e.setValue(buildTree(lp));
                freeListPair(lp);
            }
            stackpos--;
            return nd;
        }
    }

    public Iterator iterator() {
        return new TreenotationIndexValueIterator(this);
    }

    public ValueGroupsIterator valueGroupsIterator() {
        return new ValueGroupsIterator();
    }

    public int size() {
        return size;
    }

    interface Node {
    }

    public interface FinalNode extends Node {
        Object[] getValues();

        void setValues(Object[] arr);

        FinalNode getNextFinal();
    }

    private class Entry {
        Treenotation key;
        Object value;
        int nFeatures;
        Entry next;

        Entry(Treenotation _key, Object _value) {
            key = _key;
            value = _value;
            next = null;
            nFeatures = key.size();
        }
    }

    private class EntryFeatureMapsIterator implements Iterator {
        Entry e;
        int c;
        int len;

        EntryFeatureMapsIterator(Entry first, int _len) {
            e = first;
            c = 0;
            len = _len;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return e != null && c < len;
        }

        public Object next() {
            Entry res = e;
            e = e.next;
            c++;
            return res.key;
        }

    }

    class Distributer implements Node {
        HashMap map;
        int key = -1;
        Node otherTransition;
    }

    class FinalDistributer extends Distributer implements FinalNode {
        Object[] values;
        FinalNode nextFinal;

        FinalDistributer(ListPair p) {
            values = new Object[p.readyListLen];
            entryValuesToArray(p.readyListStart, p.readyListLen, values);
            nextFinal = firstFinal;
            firstFinal = this;
            nFinalNodes++;
            size += p.readyListLen;
        }

        public Object[] getValues() {
            return values;
        }

        public void setValues(Object[] arr) {
            values = arr;
        }

        public FinalNode getNextFinal() {
            return nextFinal;
        }

    }

    class SequenceReader implements Node {
        IntFeatureMapStaticLog map;
        Node nd;
    }

    class FinalSequenceReader extends SequenceReader implements FinalNode {
        Object[] values;
        FinalNode nextFinal;

        FinalSequenceReader(ListPair p) {
            values = new Object[p.readyListLen];
            entryValuesToArray(p.readyListStart, p.readyListLen, values);
            nextFinal = firstFinal;
            firstFinal = this;
            nFinalNodes++;
            size += p.readyListLen;
        }

        public Object[] getValues() {
            return values;
        }

        public void setValues(Object[] arr) {
            values = arr;
        }

        public FinalNode getNextFinal() {
            return nextFinal;
        }
    }

    private class ListPair {
        Entry entryListStart = null;
        Entry entryListEnd = null;
        Entry readyListStart = null;
        Entry readyListEnd = null;
        int entryListLen = 0;
        int readyListLen = 0;

        ListPair(Entry l1s, Entry l1e, int l1len, Entry l2s, Entry l2e, int l2len) {
            entryListStart = l1s;
            entryListEnd = l1e;
            entryListLen = l1len;
            readyListStart = l2s;
            readyListEnd = l2e;
            readyListLen = l2len;
        }

        ListPair() {
        }
    }

    private class ValueGroupsIterator implements Iterator {
        FinalNode cur;
        FinalNode prev;

        ValueGroupsIterator() {
            cur = firstFinal;
            prev = null;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return cur != null;
        }

        public Object next() {
            prev = cur;
            cur = cur.getNextFinal();
            return prev.getValues();
        }

        public void changeLastValues(Object[] arr) {
            int delta = arr.length - prev.getValues().length;
            prev.setValues(arr);
            size += delta;
        }
    }
}
