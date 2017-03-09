/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.IntFeatureMap;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.util.nu;
import treeton.core.util.sut;

import java.util.*;

public class TrnFeature {
    private static char[][] keywords = {
            "feature".toCharArray(),
            "openset".toCharArray(),
            "optional".toCharArray(),
            "mark".toCharArray(),
            "private".toCharArray(),
            "value".toCharArray()
    };
    TrnTypeDclImpl tp;
    boolean openset = false;
    boolean optional = false;
    int featureIndex;
    HashMap<Object, TrnValue> values = new HashMap<Object, TrnValue>(); //{Object} -> {TrnValue}*
    TrnValue[] allNotNullValues = null; //by pjalybin for typesviewer
    HashSet marks = new HashSet();
    HashSet privateObjects = new HashSet();
    HashSet fHash = new HashSet();

    static TrnFeature copyValidationTree(TrnFeature source, TrnTypeDclImpl tp, int newFn, boolean ignorePrivate) throws TreetonModelException {
        TrnFeature feature = new TrnFeature();
        feature.tp = tp;
        feature.featureIndex = newFn;
        Iterator it = source.values.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (!ignorePrivate && source.privateObjects.contains(e.getValue()))
                continue;

            feature.values.put(e.getKey(), TrnValue.copyValidationTree((TrnValue) e.getValue(), tp, newFn, ignorePrivate));
        }
        it = source.marks.iterator();
        while (it.hasNext()) {
            MarkInIntroduction mark = (MarkInIntroduction) it.next();
            if (!ignorePrivate && source.privateObjects.contains(mark))
                continue;
            MarkInIntroduction newMark = tp.getDeclaredMark(mark.name);
            feature.marks.add(newMark);
        }
        return feature;
    }


    static TrnFeature refactorValidationTree(TrnFeature source, HashSet markNames, int mode) {
        if (source.featureIndex != -1 && !ValidationTree.checkKeepCondition(source.marks, markNames, mode)) {
            return null;
        }

        TrnFeature feature = new TrnFeature();
        feature.tp = source.tp;
        feature.featureIndex = source.featureIndex;
        Iterator it = source.values.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            TrnValue val = TrnValue.refactorValidationTree((TrnValue) e.getValue(), markNames, mode);
            if (val != null)
                feature.values.put(e.getKey(), val);
        }
        it = source.marks.iterator();
        while (it.hasNext()) {
            MarkInIntroduction mark = (MarkInIntroduction) it.next();
            feature.marks.add(mark);
        }
        it = source.privateObjects.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            feature.privateObjects.add(o);
        }
        return feature;
    }

    static TrnFeature copyValidationTreeWithoutCasting(TrnFeature source) {
        TrnFeature feature = new TrnFeature();
        feature.tp = source.tp;
        feature.featureIndex = source.featureIndex;
        Iterator it = source.values.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            feature.values.put(e.getKey(), TrnValue.copyValidationTreeWithoutCasting((TrnValue) e.getValue()));
        }
        it = source.marks.iterator();
        while (it.hasNext()) {
            MarkInIntroduction mark = (MarkInIntroduction) it.next();
            feature.marks.add(mark);
        }

        it = source.privateObjects.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            feature.privateObjects.add(o);
        }
        return feature;
    }

    int readIn(char s[], int pl, int endpl) throws ParseException, TreetonModelException {
        openset = false;
        optional = false;

        while (true) {
            pl = sut.skipSpacesEndls(s, pl, endpl);
            int n = sut.checkDelims(s, pl, endpl, keywords);
            if (n == 0) { // feature
                pl += keywords[n].length;
                break;
            } else {
                if (n == 1) { //openset
                    if (openset) {
                        throw new ParseException("\"openset\" already specified", null, s, pl, endpl);
                    }
                    openset = true;
                } else if (n == 2) { //optional
                    if (optional) {
                        throw new ParseException("\"optional\" already specified", null, s, pl, endpl);
                    }
                    optional = true;
                } else {
                    throw new ParseException("Wrong syntax", null, s, pl, endpl);
                }
                pl += keywords[n].length;
            }
        }

        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing feature name", null, s, pl, endpl);
        }
        featureIndex = tp.getFeatureIndex(new String(s, beg, pl - beg));
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);

        if (s[pl] == ';') {
            if (!openset)
                throw new ParseException("missing '{'", null, s, pl, endpl);
        } else {
            if (s[pl] == '{') {
                pl++;
                while (true) {
                    pl = sut.skipSpacesEndls(s, pl, endpl);
                    sut.checkEndOfStream(s, pl, endpl);
                    if (s[pl] == '}') {
                        break;
                    }
                    int n = sut.checkDelims(s, pl, endpl, keywords);
                    boolean priv = false;
                    if (n == 4) { //private
                        priv = true;
                        pl += keywords[n].length;
                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        n = sut.checkDelims(s, pl, endpl, keywords);
                    }

                    if (n == 3) { //mark
                        pl += keywords[n].length;
                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        beg = pl;
                        pl = sut.skipVarValue(s, pl, endpl);
                        if (pl == beg) {
                            throw new ParseException("missing mark name", null, s, pl, endpl);
                        }
                        String name = sut.extractString(s, beg, pl - beg);
                        MarkInIntroduction mark = tp.getDeclaredMark(name);
                        if (mark == null) {
                            throw new ParseException("undeclared mark " + name, null, s, pl, endpl);
                        }

                        marks.add(mark);

                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        sut.checkEndOfStream(s, pl, endpl);
                        if (s[pl] != ';') {
                            throw new ParseException("missing ';'", null, s, pl, endpl);
                        }
                        if (priv) {
                            privateObjects.add(mark);
                        }
                    } else if (n == 5) { //value
                        TrnValue value = new TrnValue();
                        value.tp = tp;
                        value.featureIndex = featureIndex;
                        pl = value.readIn(s, pl, endpl, true);
                        if (values.get(value.value) != null) {
                            throw new ParseException("duplicated value " + value.value, null, s, pl, endpl);
                        }

                        if (value.value == nu.other) {
                            if (!openset) {
                                throw new ParseException("you can't specify other value for non-openset feature", null, s, pl, endpl);
                            }
                        }

                        values.put(value.value, value);
                        if (priv) {
                            privateObjects.add(value);
                        }
                    } else {
                        throw new ParseException("Wrong syntax", null, s, pl, endpl);
                    }

                    pl++;
                }
            } else {
                throw new ParseException("missing '{'", null, s, pl, endpl);
            }
        }

        if (openset) {
            if (values.get(nu.other) == null) {
                TrnValue value = new TrnValue();
                value.featureIndex = featureIndex;
                value.value = nu.other;
                values.put(nu.other, value);
            }
        }

        if (optional) {
            if (values.get(nu.ll) == null) {
                TrnValue value = new TrnValue();
                value.tp = tp;
                value.featureIndex = featureIndex;
                value.value = nu.ll;
                values.put(nu.ll, value);
            }
        }

        Iterator<TrnValue> it = values.values().iterator();
        while (it.hasNext()) {
            TrnValue val = it.next();
            val.marks.addAll(marks);
        }

        return pl;
    }

    public void mergeWith(TrnFeature newFeature) {
        Iterator<Map.Entry<Object, TrnValue>> it = newFeature.values.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, TrnValue> e = it.next();
            TrnValue value = (TrnValue) values.get(e.getKey());
            if (value == null) {
                values.put(e.getKey(), value = TrnValue.copyValidationTreeWithoutCasting(e.getValue()));
                value.marks.addAll(marks);
            } else {
                value.mergeWith(e.getValue());
            }
        }
        marks.addAll(newFeature.marks);
    }

    public String toString() {
        try {
            return tp.getFeatureNameByIndex(featureIndex).toString();
        } catch (TreetonModelException e) {
            return "Exception occured";
        }
    }

    public int getChildCount() {
        return values.size();
    }

    public boolean isLeaf() {
        return values.isEmpty();
    }

    public Object getChild(int index) {
        Iterator<TrnValue> it = values.values().iterator();
        int count = 0;
        while (it.hasNext()) {
            TrnValue value = it.next();
            if (count == index) {
                return value;
            }
            count++;
        }
        return null;
    }

    public int getIndexOfChild(Object child) {
        Iterator<TrnValue> it = values.values().iterator();
        int count = 0;
        while (it.hasNext()) {
            TrnValue value = it.next();
            if (value == child) {
                return count;
            }
            count++;
        }
        return -1;
    }

    TList vary(IntFeatureMap initialData, HashSet varyMarks, boolean skip) {
        if (
                ValidationTree.checkKeepCondition(marks, varyMarks, ValidationTree.KEEP_HASHED_MARKS) &&
                        (skip || initialData.get(featureIndex) == null)
                ) {
            Iterator<Map.Entry<Object, TrnValue>> it = values.entrySet().iterator();
            TList result = new TList();
            while (it.hasNext()) {
                Map.Entry<Object, TrnValue> e = it.next();
                Object value = e.getKey();
                TrnValue val = e.getValue();
                if (ValidationTree.checkKeepCondition(val.marks, varyMarks, ValidationTree.KEEP_HASHED_MARKS)) {
                    TList l = val.vary(initialData, varyMarks, skip);
                    fillUpList(l, value);
                    result.concat(l);
                }
            }
            return result;
        } else {
            Object o = initialData.get(featureIndex);
            if (o == null) {
                o = nu.ll;
            }
            TrnValue v = values.get(o);
            if (v == null) {
                if (o == nu.ll) {
                    return new TList();
                }
                v = values.get(nu.other);
                if (v == null) {
                    return new TList();
                }
            }
            TList result = v.vary(initialData, varyMarks, skip);
            fillUpList(result, o);
            return result;
        }
    }

    private void fillUpList(TList l, Object value) {
        TListEntry cur = l.first;
        while (cur != null) {
            IntFeatureMap m = (IntFeatureMap) cur.o;
            if (value == nu.ll) {
            } else if (value == nu.other) {
                m.put(featureIndex, nu.other);
            } else {
                m.put(featureIndex, value);
            }
            cur = cur.next;
        }
    }

    public void addFeatureNamesMarkedWith(HashSet<String> result, MarkInIntroduction declaredMark) throws TreetonModelException {
        if (marks.contains(declaredMark)) {
            result.add(tp.getFeatureNameByIndex(featureIndex).toString());
        }
        for (TrnValue trnValue : values.values()) {
            trnValue.addFeatureNamesMarkedWith(result, declaredMark);
        }
    }

    public void filterOutContext(IntFeatureMap context, IntFeatureMap newContext) {
        Object o = context.get(featureIndex);
        if (o != null) {
            newContext.put(featureIndex, o);
        }

        for (TrnValue trnValue : values.values()) {
            trnValue.filterOutContext(context, newContext);
        }
    }

    public boolean isContextValid(IntFeatureMap context, HashSet vHash, int vFeature, HashSet varyMarks) {
        boolean vary = true;
        Object o = context.get(featureIndex);

        if (!ValidationTree.checkKeepCondition(marks, varyMarks, ValidationTree.KEEP_HASHED_MARKS)) {
            vary = false;
        } else if (vFeature != featureIndex) {
            vary = o == null;
        }

        context.remove(featureIndex);
        fHash.clear();
        if (vary) {
            Iterator<Map.Entry<Object, TrnValue>> it = values.entrySet().iterator();

            boolean result = false;

            while (it.hasNext()) {
                Map.Entry<Object, TrnValue> e = it.next();
                TrnValue v = e.getValue();
                if (!ValidationTree.checkKeepCondition(v.marks, varyMarks, ValidationTree.KEEP_HASHED_MARKS)) {
                    continue;
                }

                HashSet tHash = new HashSet();

                if (v.isContextValid((IntFeatureMap) context.clone(), vFeature, tHash, varyMarks)) {
                    result = true;
                    if (vFeature == featureIndex) {
                        vHash.add(e.getKey());
                    } else {
                        vHash.addAll(v.vHash);
                    }
                    fHash.addAll(tHash);
                }
            }
            if (result)
                fHash.add(new Integer(featureIndex));
            return result;
        } else {
            if (o == null) {
                o = nu.ll;
            }
            TrnValue val = (TrnValue) values.get(o);
            if (val == null && o != nu.ll) {
                val = (TrnValue) values.get(nu.other);
            }
            if (val == null) {
                return false;
            }
            if (val.isContextValid(context, vFeature, fHash, varyMarks)) {
                vHash.addAll(val.vHash);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean filterTemplate(IntFeatureMap context, HashSet varyMarks) {
        boolean vary;
        Object o = context.get(featureIndex);
        if (!ValidationTree.checkKeepCondition(marks, varyMarks, ValidationTree.KEEP_HASHED_MARKS)) {
            vary = false;
        } else {
            vary = o == null;
        }

        context.remove(featureIndex);
        fHash.clear();
        fHash.add(featureIndex);
        if (vary) {
            Iterator<Map.Entry<Object, TrnValue>> it = values.entrySet().iterator();
            boolean result = false;

            while (it.hasNext()) {
                Map.Entry<Object, TrnValue> e = it.next();
                TrnValue v = e.getValue();
                if (!ValidationTree.checkKeepCondition(v.marks, varyMarks, ValidationTree.KEEP_HASHED_MARKS)) {
                    continue;
                }

                if (v.filterTemplate((IntFeatureMap) context.clone(), fHash, varyMarks)) {
                    result = true;
                }
            }

            return result;
        } else {
            if (o == null) {
                o = nu.ll;
            }
            TrnValue val = (TrnValue) values.get(o);
            if (val == null && o != nu.ll) {
                val = (TrnValue) values.get(nu.other);
            }
            if (val != null) {
                return val.filterTemplate(context, fHash, varyMarks);
            }
            return false;
        }
    }

    public MarkInIntroduction[] getMarks() { //by pjalybin 18.11.05
        if (marks != null) {
            MarkInIntroduction[] res = new MarkInIntroduction[marks.size()];
            marks.toArray(res);
            return res;
        } else return null;
    }

    public TrnType getTrnType() {//by pjalybin 16.11.05
        return tp;
    }

    public int getFeatureIndex() {//by pjalybin 16.11.05
        return featureIndex;
    }

    public boolean isOpenset() {
        if (openset) return true;
        for (TrnValue v : values.values()) {
            if (v.value.equals(nu.other)) return true;
        }
        return false;
    }

    public boolean isOptional() {
        if (optional) return true;
        for (TrnValue v : values.values()) {
            if (v.value.equals(nu.ll)) return true;
        }
        return false;
    }

    public TrnValue[] getAllValues() {
        if (allNotNullValues == null && values != null) {
            ArrayList<TrnValue> notNullValues = new ArrayList<TrnValue>();
            Collection<TrnValue> allValues = values.values();
            for (TrnValue v : allValues) {
                if ((v != null)
                        && (v.value != null)
                        && !(v.value.equals(nu.other) && v.getAllFeatures() == null)
                        && !(v.value.equals(nu.ll) && v.getAllFeatures() == null))
                    notNullValues.add(v);
            }
            allNotNullValues = new TrnValue[notNullValues.size()];
            notNullValues.toArray(allNotNullValues);
            java.util.Arrays.sort(allNotNullValues, new Comparator<TrnValue>() {
                public int compare(TrnValue o1, TrnValue o2) {
                    if (o1 == null) return (o2 == null) ? 0 : 1;
                    if (o2 == null) return -1;
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });
        }
        return allNotNullValues;
    }

}
