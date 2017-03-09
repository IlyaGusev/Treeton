/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import treeton.core.BlackBoard;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.BlockStack;
import treeton.core.util.NumeratedObjectEx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class ScapeTreenotationClassIterator {
    private final static int POP_EQ = 0;
    private final static int POP_INEQ = 1;
    private final BlockStack noStack = new BlockStack(100);
    public Object[] values;
    public int valuesLen;
    public int[] classes;
    public int classesLen;
    public BlackBoard equalities;
    public BlackBoard inequalities;
    TrnType curType = null;
    private ScapeTreenotationClassTree bigTree;
    private BlockStack stack = new BlockStack(100);
    private int[] featureShadower = new int[100];
    private int shadowerPos;
    private HashMap<Object, Object> hash = new HashMap<Object, Object>();
    private EntryFeatureMapsIterator fmIt = new EntryFeatureMapsIterator(null, -1);
    private NumeratedObjectEx[] eqArr = new NumeratedObjectEx[100];
    private int eqArrLen = 0;
    private NumeratedObjectEx[] ineqArr = new NumeratedObjectEx[100];
    private int ineqArrLen = 0;

    public ScapeTreenotationClassIterator() {
        classes = new int[100];
        classesLen = 0;
        values = new Object[100];
        valuesLen = 0;
        equalities = TreetonFactory.newBlackBoard(100, false);
        inequalities = TreetonFactory.newBlackBoard(100, false);
    }

    private NumeratedObjectEx newNumeratedObjectEx(int n, Object o) {
        if (noStack.isEmpty()) {
            return new NumeratedObjectEx(n, o);
        } else {
            NumeratedObjectEx nn = (NumeratedObjectEx) noStack.pop();
            nn.n = n;
            nn.o = o;
            return nn;
        }
    }

    private void freeNumeratedObjectEx(NumeratedObjectEx nn) {
        noStack.push(nn);
    }

    public void next() {
        findNextFinal();
        if (!isValid()) {
            hash.clear();
        }
    }

    public boolean isValid() {
        return valuesLen > 0;
    }

    public TrnType getType() {
        return curType;
    }

    private void entryValuesToArray(Entry firstEntry, int len, Object[] arr) {
        int i = 0;
        while (firstEntry != null && i < len) {
            arr[i++] = firstEntry.value;
            firstEntry = firstEntry.next;
        }
    }

    public void reset(Iterator<Treenotation> mix, ScapeTreenotationClassTree _bigTree) {
        stack.clean();
        hash = new HashMap<Object, Object>();
        valuesLen = 0;
        bigTree = _bigTree;
        curType = null;

        if (!mix.hasNext())
            return;

        ListPair p = new ListPair();
        while (mix.hasNext()) {
            Treenotation key = mix.next();
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

        Entry cur;
        cur = p.entryListStart;
        int i = 0;
        while (cur != null && i < p.entryListLen) {
            TrnType t = cur.key.getType();
            ListPair np = (ListPair) hash.get(t);
            if (np == null) {
                hash.put(t, np = new ListPair(cur, cur, 1));
                np.type = t;
                stack.push(np);
            } else {
                np.entryListEnd.next = cur;
                np.entryListEnd = cur;
                np.entryListLen++;
            }
            cur = cur.next;
            i++;
        }
        hash.clear();
        findNextFinal();
    }

    public void reset(Iterator<Treenotation> keys, Iterator<Object> values, ScapeTreenotationClassTree _bigTree) {
        stack.clean();
        hash = new HashMap<Object, Object>();
        valuesLen = 0;
        bigTree = _bigTree;
        curType = null;

        if (!keys.hasNext())
            return;


        ListPair p = new ListPair();
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
        while (cur != null && i < p.entryListLen) {
            TrnType t = cur.key.getType();
            ListPair np = (ListPair) hash.get(t);
            if (np == null) {
                hash.put(t, np = new ListPair(cur, cur, 1));
                np.type = t;
                stack.push(np);
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
        hash.clear();
        findNextFinal();
    }

    private int shadowFeatures(TrnType tp) {
        if (bigTree.shadowedFeatures == null) {
            return 0;
        }
        HashSet<Integer> h = bigTree.shadowedFeatures.get(tp);
        if (h == null) {
            return 0;
        }

        int c = 0;
        for (Integer i : h) {
            featureShadower[shadowerPos++] = i;
            c++;
        }
        return c;
    }

    private void findNextFinal() {
        while (true) {
            if (stack.isEmpty()) {
                valuesLen = 0;
                break;
            } else {
                Object so = stack.pop();
                if (so instanceof Popper) {
                    Popper pp = (Popper) so;
                    if (pp.type == POP_EQ) {
                        freeNumeratedObjectEx(eqArr[--eqArrLen]);
                    } else if (pp.type == POP_INEQ) {
                        for (int i = 0; i < pp.nineq; i++) {
                            freeNumeratedObjectEx(ineqArr[--ineqArrLen]);
                        }
                    }
                    if (shadowerPos > 0)
                        shadowerPos--;
                    continue;
                }
                ListPair p = (ListPair) so;
                if (p.type == null) {
                    if (shadowerPos >= featureShadower.length) {
                        int[] t = new int[(int) (featureShadower.length * 1.5)];
                        System.arraycopy(featureShadower, 0, t, 0, featureShadower.length);
                        featureShadower = t;
                    }
                    featureShadower[shadowerPos++] = p.key;
                    if (p.eq != null) {
                        if (eqArrLen >= eqArr.length) {
                            NumeratedObjectEx[] t = new NumeratedObjectEx[(int) (eqArr.length * 1.5)];
                            System.arraycopy(eqArr, 0, t, 0, eqArr.length);
                            eqArr = t;
                        }
                        eqArr[eqArrLen++] = newNumeratedObjectEx(p.key, p.eq);
                    } else if (p.nIneq > 0) {
                        for (int i = 0; i < p.nIneq; i++) {
                            if (ineqArrLen >= ineqArr.length) {
                                NumeratedObjectEx[] t = new NumeratedObjectEx[(int) (ineqArr.length * 1.5)];
                                System.arraycopy(ineqArr, 0, t, 0, ineqArr.length);
                                ineqArr = t;
                            }
                            ineqArr[ineqArrLen++] = newNumeratedObjectEx(p.key, p.ineq[i]);
                        }
                    }
                } else {
                    curType = p.type;
                    shadowerPos = 0;
                    shadowFeatures(curType);
                }

                fmIt.e = p.entryListStart;
                fmIt.len = p.entryListLen;
                fmIt.c = 0;
                int key = TreetonFactory.getMostRelevantKey(fmIt, featureShadower, shadowerPos);
                if (key == -1) {
                    if (p.entryListLen > values.length) {
                        Object[] t = new Object[(int) Math.max(values.length * 1.5, p.entryListLen)];
                        System.arraycopy(values, 0, t, 0, values.length);
                        values = t;
                    }

                    entryValuesToArray(p.entryListStart, p.entryListLen, values);
                    valuesLen = p.entryListLen;

                    equalities.clean();
                    for (int i = 0; i < eqArrLen; i++) {
                        equalities.put(eqArr[i].n, eqArr[i]);
                    }
                    inequalities.clean();
                    for (int i = ineqArrLen - 1; i >= 0; i--) {
                        ineqArr[i].next = (NumeratedObjectEx) inequalities.get(ineqArr[i].n);
                        inequalities.put(ineqArr[i].n, ineqArr[i]);
                    }

                    countClasses();

                    break;
                }


                Entry cur = p.entryListStart;
                ListPair otherp = null;
                hash.clear();

                int i = 0;
                while (cur != null && i < p.entryListLen) {
                    if (cur.key.contains(key)) {
                        Object val = cur.key.get(key);
                        if (bigTree.isCaseInsensetive(key, curType)) {
                            val = val.toString().toLowerCase();
                        } else if (key == 0 || key == 2) {
                            val = val.toString();
                        }
                        Object o = hash.get(val);
                        if (o == null) {
                            ListPair np;
                            hash.put(val, np = new ListPair(cur, cur, 1));
                            stack.push(new Popper(POP_EQ, -1));
                            np.key = key;
                            np.eq = val;
                            stack.push(np);
                        } else {
                            ListPair np = (ListPair) o;
                            np.entryListEnd.next = cur;
                            np.entryListEnd = cur;
                            np.entryListLen++;
                        }
                    } else {
                        if (otherp == null) {
                            otherp = new ListPair();
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

                if (otherp != null) {
                    int l = 0;
                    for (Map.Entry<Object, Object> e : hash.entrySet()) {
                        if (l >= otherp.ineq.length) {
                            Object[] tarr = new Object[(int) (otherp.ineq.length * 1.5)];
                            System.arraycopy(otherp.ineq, 0, tarr, 0, otherp.ineq.length);
                            otherp.ineq = tarr;
                        }
                        otherp.ineq[l++] = e.getKey();
                        ListPair lp = (ListPair) e.getValue();

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
                    otherp.key = key;
                    otherp.nIneq = l;
                    stack.push(new Popper(POP_INEQ, l));
                    stack.push(otherp);
                }
            }
        }
    }

    private void countClasses() {
        Object nd = bigTree.root.map.get(curType);
        long position = stack.getPosition();
        stack.push(nd);

        classesLen = 0;
        while (true) {
            if (stack.getPosition() == position) {
                break;
            } else {
                Object o = stack.pop();
                if (o instanceof ScapeTreenotationClassTree.Distributer) {
                    ScapeTreenotationClassTree.Distributer distr = (ScapeTreenotationClassTree.Distributer) o;
                    NumeratedObjectEx v;
                    if ((v = (NumeratedObjectEx) equalities.get(distr.key)) != null) {
                        stack.push(distr.map.get(v.o));
                    } else if ((v = (NumeratedObjectEx) inequalities.get(distr.key)) != null) {
                        Iterator<Map.Entry<Object, Object>> it = distr.map.entrySet().iterator();
                        NumeratedObjectEx start = v;
                        while (it.hasNext()) {
                            Map.Entry<Object, Object> e = it.next();
                            Object cur = e.getKey();
                            v = start;
                            while (v != null) {
                                if (v.o.equals(cur)) {
                                    break;
                                }
                                v = v.next;
                            }
                            if (v == null) {
                                stack.push(e.getValue());
                            }
                        }
                        if (distr.otherTransition != null) {
                            stack.push(distr.otherTransition);
                        }
                    } else {
                        for (Object o1 : distr.map.values()) {
                            stack.push(o1);
                        }
                        if (distr.otherTransition != null) {
                            stack.push(distr.otherTransition);
                        }
                    }
                } else if (o instanceof ScapeTreenotationClassTree.SequenceReader) {
                    ScapeTreenotationClassTree.SequenceReader reader = (ScapeTreenotationClassTree.SequenceReader) o;
                    stack.push(reader.nd);
                } else if (o instanceof ScapeTreenotationClassTree.FinalNode) {
                    ScapeTreenotationClassTree.FinalNode fnode = (ScapeTreenotationClassTree.FinalNode) o;
                    if (classesLen >= classes.length) {
                        int[] t = new int[(int) (classes.length * 1.5)];
                        System.arraycopy(classes, 0, t, 0, classes.length);
                        classes = t;
                    }
                    classes[classesLen++] = fnode.safeGetId();
                }
            }
        }
    }

    public String getExpression() {
        try {
            StringBuffer buf = new StringBuffer();
            TrnType tp = curType;
            buf.append(tp.getName().toString());
            buf.append(": ");

            for (int i = 0; i <= equalities.getDepth(); i++) {
                NumeratedObjectEx nn = (NumeratedObjectEx) equalities.get(i);
                if (nn == null)
                    continue;
                buf.append(tp.getFeatureNameByIndex(nn.n).toString());
                buf.append("=");
                buf.append(nn.o.toString());
                buf.append(", ");
            }
            for (int i = 0; i <= inequalities.getDepth(); i++) {
                NumeratedObjectEx nn = (NumeratedObjectEx) inequalities.get(i);
                if (nn == null)
                    continue;
                String f = tp.getFeatureNameByIndex(nn.n).toString();

                while (nn != null) {
                    buf.append(f);
                    buf.append("!=");
                    buf.append(nn.o.toString());
                    buf.append(", ");

                    nn = nn.next;
                }

            }
            return buf.toString();
        } catch (TreetonModelException e) {
            return "Error with model";
        }
    }

    private class Entry {
        Treenotation key;
        Object value;
        Entry next;

        Entry(Treenotation _key, Object _value) {
            key = _key;
            value = _value;
            next = null;
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

    private class ListPair {
        Entry entryListStart = null;
        Entry entryListEnd = null;
        int entryListLen = 0;
        TrnType type;
        int key;
        Object eq;
        Object[] ineq;
        int nIneq;

        ListPair(Entry l1s, Entry l1e, int l1len) {
            entryListStart = l1s;
            entryListEnd = l1e;
            entryListLen = l1len;
            type = null;
            key = -1;
            eq = null;
            ineq = new Object[10];
            nIneq = 0;
        }

        ListPair() {
            entryListStart = null;
            entryListEnd = null;
            entryListLen = 0;
            type = null;
            key = -1;
            eq = null;
            ineq = new Object[10];
            nIneq = 0;
        }
    }

    private class Popper {
        int type;
        int nineq;

        Popper(int _type, int _nineq) {
            type = _type;
            nineq = _nineq;
        }
    }
}

