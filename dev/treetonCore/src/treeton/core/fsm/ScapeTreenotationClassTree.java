/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import treeton.core.BlackBoard;
import treeton.core.IntFeatureMapStaticLog;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.fsm.logicset.LogicFSM;
import treeton.core.fsm.logicset.LogicState;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.util.BlockStack;
import treeton.core.util.RBTreeMap;
import treeton.core.util.collector.Collector;
import treeton.core.util.collector.CollectorException;
import treeton.core.util.collector.Mutable;
import treeton.core.util.nu;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class ScapeTreenotationClassTree implements TreeModel {
    static final int TYPE_DISTRIBUTER = -3;
    protected TrnTypeStorage types;
    protected Distributer root;
    protected TrnType curType = null;
    FinalNode firstFinal;
    int nFinalNodes;
    int size;
    Map<Integer, TrnType> classesId2types = new RBTreeMap();
    BlockStack pairsStack;
    BlackBoard sequenceBoard = null;
    HashMap<TrnType, HashSet<Integer>> caseInsensetiveFeatures;
    HashMap<TrnType, HashSet<Integer>> shadowedFeatures;
    private int maxId = -1;
    private int[] stack = new int[100];
    private int stackpos;
    private EntryFeatureMapsIterator fmIt;
    private LogicFSMIterator lfIt;
    private SequenceReader lastNd = null;

    private ScapeTreenotationClassTree() {
    } //do not remove

    //  }
    public ScapeTreenotationClassTree(TrnTypeStorage types) {
        this.types = types;
    }

    private static int getFeatureIndex(Object o, TrnType tp) throws CollectorException {
        try {
            if (o instanceof Integer) return (Integer) o;
            else if (o instanceof String) return tp.getFeatureIndex((String) o);
            else throw new CollectorException("Bad type");
        } catch (TreetonModelException e) {
            throw new CollectorException(e);
        }
    }

    private static Object getFeatureNameByIndex(int i, TrnType tp) throws CollectorException {
        try {
            if (i < 0) return i;
            else return tp.getFeatureNameByIndex(i);
        } catch (TreetonModelException e) {
            throw new CollectorException(e);
        }
    }

    private static HashMap<TrnType, HashSet<Integer>> convertFromMapWithNames(HashMap<TrnType, HashSet<String>> map) throws CollectorException {
        if (map == null)
            return null;

        HashMap<TrnType, HashSet<Integer>> res = new HashMap<TrnType, HashSet<Integer>>(map.size());
        for (Map.Entry<TrnType, HashSet<String>> e : map.entrySet()) {
            HashSet<Integer> set = new HashSet<Integer>(e.getValue().size());
            for (String s : e.getValue()) {
                set.add(getFeatureIndex(s, e.getKey()));
            }
            res.put(e.getKey(), set);
        }
        return res;
    }

    private static HashMap<TrnType, HashSet<String>> convertToMapWithNames(HashMap<TrnType, HashSet<Integer>> map) throws CollectorException {
        if (map == null)
            return null;

        HashMap<TrnType, HashSet<String>> res = new HashMap<TrnType, HashSet<String>>(map.size());
        for (Map.Entry<TrnType, HashSet<Integer>> e : map.entrySet()) {
            HashSet<String> set = new HashSet<String>(e.getValue().size());
            for (Integer index : e.getValue()) {
                set.add((String) getFeatureNameByIndex(index, e.getKey()));
            }
            res.put(e.getKey(), set);
        }
        return res;
    }

    public static TreeCellRenderer newJTreeCellRenderer() {
        return new JTreeCellRenderer();
    }

    private ListPair newListPair(Entry l1s, Entry l1e, int l1len) {
        if (pairsStack.isEmpty()) {
            return new ListPair(l1s, l1e, l1len);
        } else {
            ListPair lp = (ListPair) pairsStack.pop();
            lp.entryListStart = l1s;
            lp.entryListEnd = l1e;
            lp.entryListLen = l1len;
            lp.key = null;
            return lp;
        }
    }

    private ListPair newListPair() {
        if (pairsStack.isEmpty()) {
            return new ListPair();
        } else {
            ListPair lp = (ListPair) pairsStack.pop();
            lp.entryListStart = null;
            lp.entryListEnd = null;
            lp.entryListLen = 0;
            lp.key = null;
            return lp;
        }
    }

    private void freeListPair(ListPair lp) {
        pairsStack.push(lp);
    }

    public int getTreenotationClass(Treenotation trn) {
        FinalNode fn = getFinalState(trn);
        if (fn == null) {
            return -1;
        } else {
            return fn.unsafeGetId();
        }
    }

    public FinalNode getFinalState(Treenotation trn) {
        curType = trn.getType();
        Node cur = (Node) root.map.get(curType);
        if (cur == null)
            return null;

        while (true) {
            if (cur instanceof LogicFSMDistributer) {
                LogicFSMDistributer d = (LogicFSMDistributer) cur;
                if (TrnType.string_FEATURE == d.key || TrnType.orthm_FEATURE == d.key) {
                    String s = trn.getText();
                    LogicState state = d.map.match(s);
                    if (state == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    } else {
                        cur = (Node) state.getData();
                    }
                } else {
                    Object o = trn.get(d.key);
                    if (o == null) {
                        if (d.nullTransition != null) {
                            cur = d.nullTransition;
                            continue;
                        }
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    } else {
                        String s = o.toString();
                        LogicState state = d.map.match(s);
                        if (state == null) {
                            if (d.otherTransition != null) {
                                cur = d.otherTransition;
                                continue;
                            }
                            return null;
                        } else {
                            cur = (Node) state.getData();
                        }
                    }
                }
            } else if (cur instanceof Distributer) {
                Distributer d = (Distributer) cur;

                if (TrnType.string_FEATURE == d.key || TrnType.orthm_FEATURE == d.key) {
                    String s = trn.getText();
                    if (isCaseInsensetive(d.key)) {
                        s = s.toLowerCase();
                    }
                    cur = (Node) d.map.get(s);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                } else if (TrnType.length_FEATURE == d.key) {
                    Integer i = trn.getText().length();
                    cur = (Node) d.map.get(i);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                } else if (TrnType.start_FEATURE == d.key) {
                    Integer i = trn.getStartToken().getStartNumerator();
                    cur = (Node) d.map.get(i);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                } else if (TrnType.end_FEATURE == d.key) {
                    Integer i = trn.getEndToken().getEndNumerator();
                    cur = (Node) d.map.get(i);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                } else {
                    Object o = trn.get(d.key);
                    if (o == null) {
                        o = nu.ll;
                    } else if (isCaseInsensetive(d.key)) {
                        o = o.toString().toLowerCase();
                    }
                    cur = (Node) d.map.get(o);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                }
            } else if (cur instanceof SequenceReader) {
                SequenceReader sr = (SequenceReader) cur;
                for (int i = 0; i < sr.map.size(); i++) {
                    int key = sr.map.getIndexByNumber(i);

                    if (key == TrnType.string_FEATURE || key == TrnType.orthm_FEATURE) {
                        String s = trn.getText();
                        if (isCaseInsensetive(key)) {
                            s = s.toLowerCase();
                        }
                        if (!s.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    } else if (key == TrnType.length_FEATURE) {
                        Integer in = trn.getText().length();
                        if (!in.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    } else if (key == TrnType.start_FEATURE) {
                        Integer in = trn.getStartToken().getStartNumerator();
                        if (!in.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    } else if (key == TrnType.end_FEATURE) {
                        Integer in = trn.getEndToken().getEndNumerator();
                        if (!in.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    } else {
                        Object o = trn.get(key);
                        if (o == null) {
                            o = nu.ll;
                        } else if (isCaseInsensetive(key)) {
                            o = o.toString().toLowerCase();
                        }
                        if (!o.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    }
                }
                cur = sr.nd;
            } else if (cur instanceof FinalNode) {
                return (FinalNode) cur;
            }
        }
    }

    public Object getRoot() {
        return root;
    }

    public Object getChild(Object parent, int index) {
        return ((Node) parent).getChild(index);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return ((Node) parent).getIndexOfChild((Node) child);
    }

    public int getChildCount(Object parent) {
        return ((Node) parent).getChildCount();
    }

    public boolean isLeaf(Object node) {
        return node instanceof FinalNode;
    }

    public void addTreeModelListener(TreeModelListener l) {
    }

    public void removeTreeModelListener(TreeModelListener l) {
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    private void entryValuesToArray(Entry firstEntry, int len, Object[] arr) {
        int i = 0;
        while (firstEntry != null && i < len) {
            arr[i++] = firstEntry.value;
            firstEntry = firstEntry.next;
        }

    }

    public TrnType getClassType(int id) {
        return classesId2types.get(id);
    }

    public void makeFeatureCaseInsensetive(TrnType tp, String feature) {
        if (caseInsensetiveFeatures == null) {
            this.caseInsensetiveFeatures = new HashMap<TrnType, HashSet<Integer>>();
        }

        int i = 0;
        try {
            i = tp.getFeatureIndex(feature);
        } catch (TreetonModelException e) {
            i = -1;
        }
        if (i == -1) {
            return;
        }
        HashSet<Integer> hash = caseInsensetiveFeatures.get(tp);
        if (hash == null) {
            hash = new HashSet<Integer>();
            caseInsensetiveFeatures.put(tp, hash);
        }
        hash.add(i);
    }

    public void importCaseInsensetiveInfo(HashMap<TrnType, HashSet<Integer>> caseInsensetiveFeatures) {
        this.caseInsensetiveFeatures = new HashMap<TrnType, HashSet<Integer>>();
        if (caseInsensetiveFeatures == null)
            return;
        for (Map.Entry<TrnType, HashSet<Integer>> entry : caseInsensetiveFeatures.entrySet()) {
            HashSet<Integer> h = entry.getValue();
            HashSet<Integer> nh = new HashSet<Integer>();
            nh.addAll(h);
            this.caseInsensetiveFeatures.put(entry.getKey(), nh);
        }
    }

    public void shadowFeature(TrnType tp, String feature) {
        if (shadowedFeatures == null) {
            this.shadowedFeatures = new HashMap<TrnType, HashSet<Integer>>();
        }
        int i = 0;
        try {
            i = tp.getFeatureIndex(feature);
        } catch (TreetonModelException e) {
            i = -1;
        }
        if (i == -1) {
            return;
        }
        HashSet<Integer> hash = shadowedFeatures.get(tp);
        if (hash == null) {
            hash = new HashSet<Integer>();
            shadowedFeatures.put(tp, hash);
        }
        hash.add(i);
    }

    public void importShadowedFeaturesInfo(HashMap<TrnType, HashSet<Integer>> shadowedFeatures) {
        this.shadowedFeatures = new HashMap<TrnType, HashSet<Integer>>();
        if (shadowedFeatures == null)
            return;
        for (Map.Entry<TrnType, HashSet<Integer>> entry : shadowedFeatures.entrySet()) {
            HashSet<Integer> h = entry.getValue();
            HashSet<Integer> nh = new HashSet<Integer>();
            nh.addAll(h);
            this.shadowedFeatures.put(entry.getKey(), nh);
        }
    }

    private int shadowFeatures(TrnType tp) {
        if (shadowedFeatures == null) {
            return 0;
        }
        HashSet<Integer> h = shadowedFeatures.get(tp);
        if (h == null) {
            return 0;
        }

        int c = 0;
        for (Integer i : h) {
            stack[stackpos++] = i;
            c++;
        }
        return c;
    }

    protected boolean isCaseInsensetive(int feature) {
        if (caseInsensetiveFeatures == null)
            return false;
        HashSet<Integer> h = caseInsensetiveFeatures.get(curType);
        return h != null && h.contains(feature);
    }

    boolean isCaseInsensetive(int feature, TrnType tp) {
        if (caseInsensetiveFeatures == null)
            return false;
        HashSet<Integer> h = caseInsensetiveFeatures.get(tp);
        return h != null && h.contains(feature);
    }

    public void build(Iterator<Treenotation> keys, Iterator values) {
        pairsStack = new BlockStack(100);
        fmIt = new EntryFeatureMapsIterator(null, -1);
        lfIt = new LogicFSMIterator();

        nFinalNodes = 0;
        size = 0;
        firstFinal = null;
        root = null;

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

        Entry cur;
        cur = p.entryListStart;
        int i = 0;
        HashMap<Object, Object> hash = new HashMap<Object, Object>();
        while (cur != null && i < p.entryListLen) {
            TrnType t = cur.key.getType();
            ListPair np = (ListPair) hash.get(t);
            if (np == null) {
                hash.put(t, np = newListPair(cur, cur, 1));
                np.key = t;
            } else {
                np.entryListEnd.next = cur;
                np.entryListEnd = cur;
                np.entryListLen++;
            }
            cur = cur.next;
            i++;
        }

        freeListPair(p);

        root = new Distributer(null, null);
        root.map = hash;
        root.key = TYPE_DISTRIBUTER;

        for (Map.Entry<Object, Object> entry : root.map.entrySet()) {
            ListPair l = (ListPair) entry.getValue();
            curType = (TrnType) l.key;
            int c = shadowFeatures(curType);
            entry.setValue(buildTree(l, curType));
            stackpos -= c;
            freeListPair(l);
        }

        curType = null;
        pairsStack = null;
        fmIt = null;
        lfIt = null;
    }

    private Node buildTree(ListPair p, Object value) {
        fmIt.e = p.entryListStart;
        fmIt.len = p.entryListLen;
        fmIt.c = 0;
        int key = TreetonFactory.getMostRelevantKey(fmIt, stack, stackpos);
        if (key == -1) {
            FinalNode nd = new FinalNode(this, value);
            nd.type = curType;
            nd.values = new Object[p.entryListLen];
            entryValuesToArray(p.entryListStart, p.entryListLen, nd.values);
            if (firstFinal == null) {
                firstFinal = nd;
            } else {
                nd.nextFinal = firstFinal;
                firstFinal = nd;
            }
            nFinalNodes++;
            size += p.entryListLen;

            if (lastNd != null) {
                lastNd.map = new IntFeatureMapStaticLog(sequenceBoard);
                lastNd.nd = nd;
                lastNd = null;
            }

            return nd;
        }


        Entry cur = p.entryListStart;
        boolean fsmFound = false;

        int i = 0;
        while (cur != null && i < p.entryListLen) {
            if (cur.key.contains(key)) {
                Object val = cur.key.get(key);
                if (val instanceof LogicFSM) {
                    fsmFound = true;
                    break;
                }
            }
            cur = cur.next;
            i++;
        }

        if (fsmFound) {
            LogicFSMDistributer nd = new LogicFSMDistributer(curType, value);
            if (lastNd != null) {
                lastNd.map = new IntFeatureMapStaticLog(sequenceBoard);
                lastNd.nd = nd;
                lastNd = null;
            }

            lfIt.reset(p.entryListStart, p.entryListLen, key);
            LogicFSM commonFSM = LogicFSM.multipleUnion(lfIt);
            //FSMFrame.showFSMFrame(commonFSM);
            ListPair nullp = lfIt.nullp;
            ListPair otherp = lfIt.otherp;
            HashMap<HashSet<Entry>, Object> map = new HashMap<HashSet<Entry>, Object>();
            Iterator<LogicState> it = commonFSM.finalStatesIterator();
            while (it.hasNext()) {
                LogicState s = it.next();
                HashSet<Entry> hash = new HashSet<Entry>();
                s.addData(hash);
                ListPair np = (ListPair) map.get(hash);
                if (np == null) {
                    np = newListPair();
                    for (Entry e : hash) {
                        Entry ne = new Entry(e.key, e.value);
                        if (np.entryListStart == null) {
                            np.entryListStart = ne;
                            np.entryListEnd = ne;
                        } else {
                            np.entryListEnd.next = ne;
                            np.entryListEnd = ne;
                        }
                        np.entryListLen++;
                    }
                    map.put(hash, np);
                }
            }
            nd.key = key;
            nd.map = commonFSM;
            stack[stackpos++] = key;
            if (stackpos >= stack.length) {
                int[] t = new int[(int) (stack.length * 1.5)];
                System.arraycopy(stack, 0, t, 0, stack.length);
                stack = t;
            }


            if (otherp != null) {
                for (Object o : map.values()) {
                    ListPair np = (ListPair) o;
                    int j = 0;
                    cur = otherp.entryListStart;
                    while (cur != null && j < otherp.entryListLen) {
                        Entry clone = new Entry(cur.key, cur.value);
                        np.entryListEnd.next = clone;
                        np.entryListEnd = clone;
                        np.entryListLen++;
                        cur = cur.next;
                        j++;
                    }
                }
                if (nullp != null) {
                    int j = 0;
                    cur = otherp.entryListStart;
                    while (cur != null && j < otherp.entryListLen) {
                        Entry clone = new Entry(cur.key, cur.value);
                        nullp.entryListEnd.next = clone;
                        nullp.entryListEnd = clone;
                        nullp.entryListLen++;
                        cur = cur.next;
                        j++;
                    }
                }
                nd.otherTransition = buildTree(otherp, nu.other);
                freeListPair(otherp);
            }

            if (nullp != null) {
                nd.nullTransition = buildTree(nullp, nu.ll);
                freeListPair(nullp);
            }

            for (Map.Entry<HashSet<Entry>, Object> e : map.entrySet()) {
                ListPair np = (ListPair) e.getValue();
                e.setValue(buildTree(np, null));
                freeListPair(np);
            }

            stackpos--;

            it = commonFSM.finalStatesIterator();
            while (it.hasNext()) {
                LogicState s = it.next();
                HashSet<Entry> hash = new HashSet<Entry>();
                s.addData(hash);
                Node n = (Node) map.get(hash);
                s.setData(n);
            }
            commonFSM.forgetMadeFrom();
            return nd;
        } else {
            cur = p.entryListStart;
            boolean nullFound = false;
            i = 0;
            HashMap<Object, Object> hash = new HashMap<Object, Object>();
            Object lastVal = null;
            boolean caseIns = isCaseInsensetive(key);
            while (cur != null && i < p.entryListLen) {
                if (cur.key.contains(key)) {
                    Object val = cur.key.get(key);
                    if (caseIns) {
                        val = val.toString().toLowerCase();
                    } else if (key == 0 || key == 2) {
                        val = val.toString();
                    }
                    hash.put(val, null);
                    lastVal = val;
                } else {
                    nullFound = true;
                }
                cur = cur.next;
                i++;
            }

            if (hash.size() == 1 && !nullFound) {
                if (lastNd == null) {
                    lastNd = new SequenceReader(value, curType);
                }
                sequenceBoard.put(key, lastVal);
                stack[stackpos++] = key;
                if (stackpos >= stack.length) {
                    int[] t = new int[(int) (stack.length * 1.5)];
                    System.arraycopy(stack, 0, t, 0, stack.length);
                    stack = t;
                }
                SequenceReader t = lastNd;
                buildTree(p, "");
                stackpos--;
                return t;
            } else {
                Distributer nd = new Distributer(curType, value);
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
                        if (caseIns) {
                            val = val.toString().toLowerCase();
                        } else if (key == 0 || key == 2) {
                            val = val.toString();
                        }
                        Object o = hash.get(val);
                        if (o == null) {
                            ListPair np;
                            hash.put(val, np = newListPair(cur, cur, 1));
                            np.key = val;
                        } else {
                            ListPair np = (ListPair) o;
                            np.entryListEnd.next = cur;
                            np.entryListEnd = cur;
                            np.entryListLen++;
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
                    for (Map.Entry entry : nd.map.entrySet()) {
                        ListPair lp = (ListPair) entry.getValue();
                        if (otherp == lp)
                            throw new RuntimeException("Something gone wrong");
                        cur = otherp.entryListStart;
                        int j = 0;
                        while (cur != null && j < otherp.entryListLen) {
                            Entry clone = new Entry(cur.key, cur.value);
                            lp.entryListEnd.next = clone;
                            lp.entryListEnd = clone;
                            lp.entryListLen++;
                            cur = cur.next;
                            j++;
                        }
                    }
                    nd.otherTransition = buildTree(otherp, nu.other);
                    freeListPair(otherp);
                }
                for (Map.Entry<Object, Object> e : nd.map.entrySet()) {
                    ListPair lp = (ListPair) e.getValue();
                    e.setValue(buildTree(lp, e.getKey()));
                    freeListPair(lp);
                }
                stackpos--;
                return nd;
            }
        }
    }

    public Iterator iterator() {
        return new ValueIterator();
    }

    public Iterator<FinalNode> finalStatesIterator() {
        return new FinalSatesIterator();
    }

    public int size() {
        return size;
    }

    public int numberOfFinalNodes() {
        return nFinalNodes;
    }

    protected interface Node {
        Node getChild(int index);

        int getChildCount();

        int getIndexOfChild(Node child);

        Object getValue();
    }

    public static class ScapeTreenotationClassTreeCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            ScapeTreenotationClassTree t = (ScapeTreenotationClassTree) o;
            t.root = (Distributer) col.get();
            t.firstFinal = (FinalNode) col.get();
            t.nFinalNodes = (Integer) col.get();
            t.size = (Integer) col.get();
            t.shadowedFeatures = convertFromMapWithNames((HashMap<TrnType, HashSet<String>>) col.get());
            t.caseInsensetiveFeatures = convertFromMapWithNames((HashMap<TrnType, HashSet<String>>) col.get());
        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            ScapeTreenotationClassTree t = (ScapeTreenotationClassTree) o;
            col.put(t.root);
            col.put(t.firstFinal);
            col.put(t.nFinalNodes);
            col.put(t.size);
            col.put(convertToMapWithNames(t.shadowedFeatures));
            col.put(convertToMapWithNames(t.caseInsensetiveFeatures));
        }

        public Object newInstance(Collector col, Class c) throws IllegalAccessException, InstantiationException, CollectorException {
            return new ScapeTreenotationClassTree();
        }
    }

    public static class ScapeTreenotationClassTreeDistributerCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            Distributer t = (Distributer) o;
            t.otherTransition = (Node) col.get();
            t.tp = (TrnType) col.get();
            t.key = getFeatureIndex(col.get(), t.tp);
            t.value = col.get();
            t.map = (HashMap<Object, Object>) col.get();
        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            Distributer t = (Distributer) o;
            col.put(t.otherTransition);
            col.put(t.tp);
            col.put(getFeatureNameByIndex(t.key, t.tp));
            col.put(t.value);
            col.put(t.map);
        }

        public Object newInstance(Collector col, Class c) throws IllegalAccessException, InstantiationException, CollectorException {
            return new Distributer(null, null);
        }
    }

    public static class ScapeTreenotationClassTreeFinalNodeCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            FinalNode t = (FinalNode) o;
            t.values = (Object[]) col.get();
            t.nextFinal = (FinalNode) col.get();
            t.id = (Integer) col.get();
            t.type = (TrnType) col.get();
            t.value = col.get();

        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            FinalNode t = (FinalNode) o;

            col.put(t.values);
            col.put(t.nextFinal);
            col.put(t.id);
            col.put(t.type);
            col.put(t.value);
        }

        public Object newInstance(Collector col, Class c) throws IllegalAccessException, InstantiationException, CollectorException {
            return new FinalNode(null, null);
        }
    }

    public static class ScapeTreenotationClassTreeLogicFSMDistributerCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicFSMDistributer t = (LogicFSMDistributer) o;
            t.otherTransition = (Node) col.get();
            t.nullTransition = (Node) col.get();
            t.tp = (TrnType) col.get();
            t.key = getFeatureIndex(col.get(), t.tp);
            t.value = col.get();
            t.map = (LogicFSM) col.get();
        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            LogicFSMDistributer t = (LogicFSMDistributer) o;
            col.put(t.otherTransition);
            col.put(t.nullTransition);
            col.put(t.tp);
            col.put(getFeatureNameByIndex(t.key, t.tp));
            col.put(t.value);
            col.put(t.map);
        }

        public Object newInstance(Collector col, Class c) throws IllegalAccessException, InstantiationException, CollectorException {
            return new LogicFSMDistributer(null, null);
        }
    }

//  public ScapeTreenotationClassTree(Domain domain) {
//    this.domain = domain;

    public static class ScapeTreenotationClassTreeSequenceReaderCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            try {
                SequenceReader t = (SequenceReader) o;
                t.tp = (TrnType) col.get();
                t.map = new IntFeatureMapStaticLog((Map) col.get(), t.tp);
                t.nd = (Node) col.get();
                t.value = col.get();
            } catch (TreetonModelException e) {
                throw new CollectorException(e);
            }
        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            try {
                SequenceReader t = (SequenceReader) o;
                col.put(t.tp);
                col.put(t.map.toNamesMap(t.tp));
                col.put(t.nd);
                col.put(t.value);
            } catch (TreetonModelException e) {
                throw new CollectorException(e);
            }
        }

        public Object newInstance(Collector col, Class c) throws IllegalAccessException, InstantiationException, CollectorException {
            return new SequenceReader(null, null);
        }
    }

    static class JTreeCellRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf, int row,
                                                      boolean hasFocus) {
            String stringValue = tree.convertValueToText(value, sel,
                    expanded, leaf, row, hasFocus);

            this.hasFocus = hasFocus;
            setText(stringValue);
            if (sel)
                setForeground(getTextSelectionColor());
            else
                setForeground(getTextNonSelectionColor());
            if (!tree.isEnabled()) {
                setEnabled(false);
                if (leaf) {
                    setDisabledIcon(getLeafIcon());
                } else if (expanded) {
                    setDisabledIcon(getOpenIcon());
                } else {
                    setDisabledIcon(getClosedIcon());
                }
            } else {
                setEnabled(true);
                if (leaf) {
                    setIcon(getLeafIcon());
                } else if (expanded) {
                    setIcon(getOpenIcon());
                } else {
                    setIcon(getClosedIcon());
                }
            }
            setComponentOrientation(tree.getComponentOrientation());

            selected = sel;

            return this;
        }

    }

    private static class Entry {
        Treenotation key;
        Object value;
        Entry next;

        Entry(Treenotation _key, Object _value) {
            key = _key;
            value = _value;
            next = null;
        }
    }

    public static class Distributer implements Node {
        public HashMap<Object, Object> map;
        public int key;
        public Node otherTransition;
        TrnType tp;
        Object value;

        public Distributer(TrnType tp, Object value) {
            this.tp = tp;
            this.value = value;
        }

        public Node getChild(int index) {
            if (index == 0 && otherTransition != null) {
                return otherTransition;
            }
            int count = otherTransition != null ? 0 : -1;
            Node nd;
            for (Object o : map.values()) {
                nd = (Node) o;
                count++;
                if (count == index) {
                    return nd;
                }
            }
            return null;
        }

        public int getChildCount() {
            return map.size() + (otherTransition != null ? 1 : 0);
        }

        public int getIndexOfChild(Node child) {
            if (otherTransition == child) {
                return 0;
            }
            int count = otherTransition != null ? 0 : -1;
            Node nd;
            for (Object o : map.values()) {
                nd = (Node) o;
                count++;
                if (nd == child) {
                    return count;
                }
            }
            return -1;
        }

        public Object getValue() {
            return value;
        }

        public String toString() {
            try {
                return tp == null ? "TYPE" : value.toString() + "  |  " + tp.getFeatureNameByIndex(key);
            } catch (TreetonModelException e) {
                return "Exception";
            }
        }
    }

    public static class LogicFSMDistributer implements Node {
        public LogicFSM map;
        public int key;
        public Node otherTransition;
        public Node nullTransition;
        TrnType tp;
        Object value;

        public LogicFSMDistributer(TrnType tp, Object value) {
            this.tp = tp;
            this.value = value;
        }

        public Node getChild(int index) {
            if (index == 0 && otherTransition != null) {
                return otherTransition;
            } else if (index == 0 && nullTransition != null) {
                return nullTransition;
            }
            if (index == 1 && otherTransition != null && nullTransition != null) {
                return nullTransition;
            }
            int count = -1;
            if (nullTransition != null)
                count++;
            if (otherTransition != null)
                count++;

            Node nd;

            Iterator<LogicState> it = map.finalStatesIterator();
            while (it.hasNext()) {
                LogicState s = it.next();
                nd = (Node) s.getData();
                count++;
                if (count == index) {
                    return nd;
                }
            }
            return null;
        }

        public int getChildCount() {
            return map.nFinals() + (otherTransition != null ? 1 : 0) + (nullTransition != null ? 1 : 0);
        }

        public int getIndexOfChild(Node child) {
            if (otherTransition == child) {
                return 0;
            }
            if (nullTransition == child) {
                return (otherTransition == null) ? 0 : 1;
            }

            int count = -1;
            if (nullTransition != null)
                count++;
            if (otherTransition != null)
                count++;

            Node nd;
            Iterator<LogicState> it = map.finalStatesIterator();
            while (it.hasNext()) {
                LogicState s = it.next();
                nd = (Node) s.getData();
                count++;
                if (nd == child) {
                    return count;
                }
            }
            return -1;
        }

        public Object getValue() {
            return value;
        }

        public String toString() {
            try {
                return tp == null ? "TYPE" : value.toString() + "  |  " + tp.getFeatureNameByIndex(key);
            } catch (TreetonModelException e) {
                return "Exception";
            }
        }
    }

    public static class FinalNode implements Node {
        protected int id = -1;
        Object[] values;
        FinalNode nextFinal;
        TrnType type;
        Object value;
        private ScapeTreenotationClassTree scapeTreenotationClassTree;

        public FinalNode(ScapeTreenotationClassTree scapeTreenotationClassTree, Object value) {
            this.value = value;
            this.scapeTreenotationClassTree = scapeTreenotationClassTree;
        }

        public Object[] getValues() {
            return values;
        }

        public int unsafeGetId() {
            return id;
        }

        public int safeGetId() {
            if (id == -1) {
                id = ++scapeTreenotationClassTree.maxId;
                scapeTreenotationClassTree.classesId2types.put(id, type);
            }
            return id;
        }

        public Node getChild(int index) {
            return null;
        }

        public int getChildCount() {
            return 0;
        }

        public int getIndexOfChild(Node child) {
            return -1;
        }

        public Object getValue() {
            return value;
        }

        public String toString() {
            return (value != null ? value.toString() : "") + "  |  " + id;
        }

        public boolean contains(Object o) {
            for (Object v : values) {
                if (v == null && o == null || v != null && v.equals(o)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class SequenceReader implements Node {
        public IntFeatureMapStaticLog map;

        public Object value;
        public TrnType tp;
        public Node nd;

        public SequenceReader(Object value, TrnType tp) {
            this.value = value;
            this.tp = tp;
        }

        public Node getChild(int index) {
            return nd;
        }

        public int getChildCount() {
            return 1;
        }

        public int getIndexOfChild(Node child) {
            return 0;
        }

        public Object getValue() {
            return value;
        }

        public String toString() {
            return value.toString() + "  |  " + map.getString(tp);
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

    private class LogicFSMIterator implements Iterator<LogicFSM> {
        ListPair otherp;
        ListPair nullp;
        Entry e;
        int c;
        int len;
        int feature;
        boolean caseSens;

        LogicFSMIterator() {
        }

        void reset(Entry first, int _len, int _feature) {
            e = first;
            c = 0;
            len = _len;
            feature = _feature;
            caseSens = !isCaseInsensetive(feature);
            otherp = null;
            nullp = null;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return e != null && c < len;
        }

        public LogicFSM next() {
            Entry prev = e;
            e = e.next;
            c++;
            Object o = prev.key.get(feature);
            if (o == null) {
                if (otherp == null) {
                    otherp = newListPair();
                    otherp.entryListStart = prev;
                    otherp.entryListEnd = prev;
                } else {
                    otherp.entryListEnd.next = prev;
                    otherp.entryListEnd = prev;
                }
                otherp.entryListLen++;
                return null;
            } else if (o == nu.ll) {
                if (nullp == null) {
                    nullp = newListPair();
                    nullp.entryListStart = prev;
                    nullp.entryListEnd = prev;
                } else {
                    nullp.entryListEnd.next = prev;
                    nullp.entryListEnd = prev;
                }
                nullp.entryListLen++;
                return null;
            }
            LogicFSM res;
            if (o instanceof LogicFSM) {
                res = (LogicFSM) o;
            } else {
                res = new LogicFSM(o.toString(), caseSens);
            }
            res.assignData(prev);
            return res;
        }

    }

    private class ListPair {
        Entry entryListStart = null;
        Entry entryListEnd = null;
        Object key = null;
        int entryListLen = 0;

        ListPair(Entry l1s, Entry l1e, int l1len) {
            entryListStart = l1s;
            entryListEnd = l1e;
            entryListLen = l1len;
        }

        ListPair() {
        }
    }

    private class ValueIterator implements Iterator {
        FinalNode cur;
        int i;

        ValueIterator() {
            cur = firstFinal;
            i = 0;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return cur != null;
        }

        public Object next() {
            FinalNode t = cur;
            int ti = i++;
            if (i >= cur.values.length) {
                cur = cur.nextFinal;
                i = 0;
            }
            return t.values[ti];
        }
    }

    public class FinalSatesIterator implements Iterator<FinalNode> {
        FinalNode cur;
        FinalNode prev;

        FinalSatesIterator() {
            cur = firstFinal;
            prev = null;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return cur != null;
        }

        public FinalNode next() {
            prev = cur;
            cur = cur.nextFinal;
            return prev;
        }
    }
}
