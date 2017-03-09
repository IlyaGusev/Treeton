/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.BlackBoard;
import treeton.core.IntFeatureMap;
import treeton.core.IntFeatureMapImpl;
import treeton.core.TreetonFactory;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeUtils;
import treeton.core.scape.ParseException;
import treeton.core.util.nu;
import treeton.core.util.sut;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

public class TrnValue {
    private static final BlackBoard localBoard = TreetonFactory.newBlackBoard(50, true);
    private static char[][] keywords = {
            "value".toCharArray(),
            "mark".toCharArray(),
            "feature".toCharArray(),
            "openset".toCharArray(),
            "optional".toCharArray(),
            "private".toCharArray()
    };
    TrnTypeDclImpl tp;
    int featureIndex;
    Object value;
    TrnFeature[] features = null;
    TrnFeature[] notNullFeatures = null; //by pjalybin
    HashSet marks = new HashSet();
    HashSet privateObjects = new HashSet();
    HashSet vHash = new HashSet();

    static TrnValue copyValidationTree(TrnValue source, TrnTypeDclImpl tp, int newFn, boolean ignorePrivate) throws TreetonModelException {
        TrnValue val = new TrnValue();
        val.tp = tp;
        val.featureIndex = newFn;
        val.value = source.value;
        if (source.features != null) {
            for (int i = 0; i < source.features.length; i++) {
                TrnFeature feature = source.features[i];
                if (feature == null)
                    continue;
                if (!ignorePrivate && source.privateObjects.contains(feature))
                    continue;
                int fn = tp.getFeatureIndex(feature.tp.getFeatureNameByIndex(feature.featureIndex));
                if (val.features == null) {
                    val.features = new TrnFeature[fn + 1];
                }

                if (val.features.length <= fn) {
                    TrnFeature[] tarr = new TrnFeature[Math.max(val.features.length * 3 / 2, fn + 1)];
                    System.arraycopy(val.features, 0, tarr, 0, val.features.length);
                    val.features = tarr;
                }
                val.features[fn] = TrnFeature.copyValidationTree(feature, tp, fn, ignorePrivate);
            }
        } else {
            val.features = null;
        }

        Iterator it = source.marks.iterator();
        while (it.hasNext()) {
            MarkInIntroduction mark = (MarkInIntroduction) it.next();
            if (!ignorePrivate && source.privateObjects.contains(mark))
                continue;
            MarkInIntroduction newMark = tp.getDeclaredMark(mark.name);
            val.marks.add(newMark);
        }
        return val;
    }

    static TrnValue refactorValidationTree(TrnValue source, HashSet markNames, int mode) {
        if (source.featureIndex != -1 && !ValidationTree.checkKeepCondition(source.marks, markNames, mode)) {
            return null;
        }

        TrnValue val = new TrnValue();
        val.tp = source.tp;
        val.featureIndex = source.featureIndex;
        val.value = source.value;
        if (source.features != null) {
            for (int i = 0; i < source.features.length; i++) {
                TrnFeature feature = source.features[i];
                if (feature == null)
                    continue;
                int fn = feature.featureIndex;
                if (val.features == null) {
                    val.features = new TrnFeature[fn + 1];
                }

                if (val.features.length <= fn) {
                    TrnFeature[] tarr = new TrnFeature[Math.max(val.features.length * 3 / 2, fn + 1)];
                    System.arraycopy(val.features, 0, tarr, 0, val.features.length);
                    val.features = tarr;
                }
                val.features[fn] = TrnFeature.refactorValidationTree(feature, markNames, mode);
            }
        } else {
            val.features = null;
        }
        Iterator it = source.marks.iterator();
        while (it.hasNext()) {
            MarkInIntroduction mark = (MarkInIntroduction) it.next();
            val.marks.add(mark);
        }
        it = source.privateObjects.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            val.privateObjects.add(o);
        }
        return val;
    }

    static TrnValue copyValidationTreeWithoutCasting(TrnValue source) {
        TrnValue val = new TrnValue();
        val.tp = source.tp;
        val.featureIndex = source.featureIndex;
        val.value = source.value;
        if (source.features != null) {
            for (int i = 0; i < source.features.length; i++) {
                TrnFeature feature = source.features[i];
                if (feature == null)
                    continue;
                int fn = feature.featureIndex;
                if (val.features == null) {
                    val.features = new TrnFeature[fn + 1];
                }

                if (val.features.length <= fn) {
                    TrnFeature[] tarr = new TrnFeature[Math.max(val.features.length * 3 / 2, fn + 1)];
                    System.arraycopy(val.features, 0, tarr, 0, val.features.length);
                    val.features = tarr;
                }
                val.features[fn] = TrnFeature.copyValidationTreeWithoutCasting(feature);
            }
        } else {
            val.features = null;
        }
        Iterator it = source.marks.iterator();
        while (it.hasNext()) {
            MarkInIntroduction mark = (MarkInIntroduction) it.next();
            val.marks.add(mark);
        }
        it = source.privateObjects.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            val.privateObjects.add(o);
        }
        return val;
    }

    public void registerBasis() throws TreetonModelException {
        String[] arr = new String[]{TrnType.start_FEATURE_name, TrnType.length_FEATURE_name, TrnType.orthm_FEATURE_name, TrnType.start_FEATURE_name, TrnType.end_FEATURE_name};

        MarkInIntroduction[] marks = new MarkInIntroduction[]{
                tp.getDeclaredMark("stringMark"),
                tp.getDeclaredMark("stringMark"),
                tp.getDeclaredMark("orthmMark"),
                tp.getDeclaredMark("stringMark"),
                tp.getDeclaredMark("stringMark"),
        };

        for (int i = 0; i < arr.length; i++) {
            TrnFeature feature = new TrnFeature();
            feature.tp = tp;

            feature.openset = true;
            feature.optional = true;
            feature.featureIndex = tp.getFeatureIndex(arr[i]);

            TrnValue val = new TrnValue();
            val.tp = tp;
            val.featureIndex = feature.featureIndex;
            val.value = nu.ll;
            val.marks.add(marks[i]);
            feature.values.put(nu.ll, val);

            val = new TrnValue();
            val.tp = tp;
            val.featureIndex = feature.featureIndex;
            val.value = nu.other;
            val.marks.add(marks[i]);
            feature.values.put(nu.other, val);

            feature.marks.add(marks[i]);

            int fn = feature.featureIndex;
            if (features == null) {
                features = new TrnFeature[fn + 1];
            } else if (features.length <= fn) {
                TrnFeature[] tarr = new TrnFeature[Math.max(features.length * 3 / 2, fn + 1)];
                System.arraycopy(features, 0, tarr, 0, features.length);
                features = tarr;
            }

            features[fn] = feature;
        }
    }

    int readIn(char s[], int pl, int endpl, boolean readKeywordAndName) throws ParseException, TreetonModelException {
        if (readKeywordAndName) {
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            int n = sut.checkDelims(s, pl, endpl, keywords);
            if (n == 0) { // value
                pl += keywords[n].length;
            } else {
                throw new ParseException("Wrong syntax", null, s, pl, endpl);
            }

            pl = sut.skipSpacesEndls(s, pl, endpl);
            int beg = pl;
            pl = sut.skipVarValue(s, pl, endpl);
            if (pl == beg) {
                throw new ParseException("missing feature value", null, s, pl, endpl);
            }
            Object V;
            if (pl - beg == 4 && s[beg] == 'n' && s[beg + 1] == 'u' && s[beg + 2] == 'l' && s[beg + 3] == 'l') {
                V = nu.ll;
            } else if (pl - beg == 5 && s[beg] == 'o' && s[beg + 1] == 't' && s[beg + 2] == 'h' && s[beg + 3] == 'e' && s[beg + 3] == 'r') {
                V = nu.other;
            } else {
                if (s[beg] == '"') {
                    V = sut.extractTString(s, beg + 1, pl - beg - 2);
                } else {
                    V = sut.extractTString(s, beg, pl - beg);
                }
            }

            value = TrnTypeUtils.treatFeatureValue(tp, featureIndex, V);

            if (V != nu.ll && V != nu.other && !tp.isValueDeclared(featureIndex, value)) {
                throw new ParseException("value " + V + " is not presented in the introduction block", null, s, pl, endpl);
            }
        }

        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] == ';') {
            return pl;
        }

        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }
        pl++;

        while (true) {
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] == '}') {
                break;
            }

            int n = sut.checkDelims(s, pl, endpl, keywords);
            boolean priv = false;
            if (n == 5) { //private
                priv = true;
                pl += keywords[n].length;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                n = sut.checkDelims(s, pl, endpl, keywords);
            }

            if (n == 1) { // mark
                pl += keywords[n].length;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                int beg = pl;
                pl = sut.skipVarValue(s, pl, endpl);
                if (pl == beg) {
                    throw new ParseException("missing mark name", null, s, pl, endpl);
                }
                String name = new String(s, beg, pl - beg);
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
            } else if (n == 2 || n == 3 || n == 4) { //feature or openset or optional
                TrnFeature feature = new TrnFeature();
                feature.tp = tp;
                pl = feature.readIn(s, pl, endpl);
                int fn = feature.featureIndex;
                if (features == null) {
                    features = new TrnFeature[fn + 1];
                }

                if (features.length <= fn) {
                    TrnFeature[] tarr = new TrnFeature[Math.max(features.length * 3 / 2, fn + 1)];
                    System.arraycopy(features, 0, tarr, 0, features.length);
                    features = tarr;
                } else if (features[fn] != null) {
                    throw new ParseException("duplicated feature " + tp.getFeatureNameByIndex(feature.featureIndex), null, s, pl, endpl);
                }
                features[fn] = feature;
                if (priv) {
                    privateObjects.add(feature);
                }
            } else {
                throw new ParseException("Wrong syntax", null, s, pl, endpl);
            }

            pl++;
        }
        return pl;
    }

    public void mergeWith(TrnValue newValue) {
        if (newValue.features != null) {
            if (features == null) {
                features = new TrnFeature[newValue.features.length];
            }
            if (newValue.features.length > features.length) {
                TrnFeature[] tarr = new TrnFeature[newValue.features.length];
                System.arraycopy(features, 0, tarr, 0, features.length);
                features = tarr;
            }
            for (int i = 0; i < newValue.features.length; i++) {
                TrnFeature newFeature = newValue.features[i];
                if (newFeature != null) {
                    TrnFeature feature = features[i];
                    if (feature == null) {
                        features[i] = TrnFeature.copyValidationTreeWithoutCasting(newFeature);
                    } else {
                        feature.mergeWith(newFeature);
                    }
                }
            }
        }
        marks.addAll(newValue.marks);
    }

    public String toString() {
        return value != null ? value.toString() : "";
    }

    public int getChildCount() {
        if (features == null)
            return 0;
        int count = 0;
        for (int i = 0; i < features.length; i++) {
            TrnFeature feature = features[i];
            if (feature != null) {
                count++;
            }
        }
        return count;
    }

    public boolean isLeaf() {
        return features == null;
    }

    public Object getChild(int index) {
        int count = 0;
        for (int i = 0; i < features.length; i++) {
            TrnFeature feature = features[i];
            if (feature != null) {
                if (count == index) {
                    return feature;
                }
                count++;
            }
        }
        return null;
    }

    public int getIndexOfChild(Object child) {
        int count = 0;
        for (int i = 0; i < features.length; i++) {
            TrnFeature feature = features[i];
            if (feature != null) {
                if (feature == child) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    private void multiply(TList a, TList b) {
        if (b.first == null) {
            a.first = a.last = null;
            return;
        }
        synchronized (localBoard) {
            TListEntry cur = b.first.next;
            TListEntry first;

            TList l = new TList();
            while (cur != null) {
                localBoard.clean();
                ((IntFeatureMap) cur.o).fillBlackBoard(localBoard);

                first = a.first;
                while (first != null) {
                    TListEntry e = new TListEntry();
                    e.o = ((IntFeatureMap) first.o).clone();
                    ((IntFeatureMap) e.o).put(localBoard);

                    l.add(e);
                    first = first.next;
                }
                cur = cur.next;
            }

            cur = b.first;
            localBoard.clean();
            ((IntFeatureMap) cur.o).fillBlackBoard(localBoard);

            first = a.first;
            while (first != null) {
                ((IntFeatureMap) first.o).put(localBoard);
                first = first.next;
            }
            a.concat(l);
        }
    }

    TList vary(IntFeatureMap initialData, HashSet varyMarks, boolean skip) {
        TList current = new TList();
        TListEntry e = new TListEntry();
        e.o = new IntFeatureMapImpl();
        current.add(e);
        if (features != null) {
            for (int i = 0; i < features.length; i++) {
                TrnFeature feature = features[i];
                if (feature != null) {
                    TList b = feature.vary(initialData, varyMarks, skip);
                    if (b.first == null) //TODO: check presence of obligatory features somewhere else; it must be checked not only in InvarsConverter!
                        throw new RuntimeException("No obligatory feature '" + feature.toString() + "' found for annotation " + initialData);
                    multiply(current, b);
                }
            }
        }
        return current;
    }

    public void addFeatureNamesMarkedWith(HashSet<String> result, MarkInIntroduction declaredMark) throws TreetonModelException {
        if (features != null) {
            for (TrnFeature feature : features) {
                if (feature != null) {
                    feature.addFeatureNamesMarkedWith(result, declaredMark);
                }
            }
        }
    }

    public boolean isContextValid(IntFeatureMap context, int vFeature, HashSet fHash, HashSet varyMarks) {
        if (features != null) {
            vHash.clear();
            for (int i = 0; i < features.length; i++) {
                TrnFeature f = features[i];
                if (f == null)
                    continue;

                IntFeatureMap newContext = new IntFeatureMapImpl();
                f.filterOutContext(context, newContext);
                context.leaveDifference(newContext);
                if (!f.isContextValid(newContext, vHash, vFeature, varyMarks)) {
                    return false;
                } else {
                    fHash.addAll(f.fHash);
                }
            }
        }

        if (context.size() != 0) {
            return false;
        }

        return true;
    }


    public void filterOutContext(IntFeatureMap context, IntFeatureMap newContext) {
        if (features != null) {
            for (int i = 0; i < features.length; i++) {
                TrnFeature f = features[i];
                if (f == null)
                    continue;

                f.filterOutContext(context, newContext);
            }
        }
    }

    public boolean filterTemplate(IntFeatureMap context, HashSet fHash, HashSet varyMarks) {
        if (features != null) {
            for (int i = 0; i < features.length; i++) {
                TrnFeature f = features[i];
                if (f == null)
                    continue;

                IntFeatureMap newContext = new IntFeatureMapImpl();
                f.filterOutContext(context, newContext);
                context.leaveDifference(newContext);

                if (f.filterTemplate(newContext, varyMarks)) {
                    fHash.addAll(f.fHash);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public HashSet getMarks() { //by pjalybin 16.11.05
        return marks;
    }

    public TrnType getTrnType() {
        return tp;
    }

    public int getFeatureIndex() {
        return featureIndex;
    }

    public Object getValue() {
        return value;
    }

    public TrnFeature[] getAllFeatures() {
        if (notNullFeatures == null && features != null) {
            int len = 0;
            for (TrnFeature feature : features) if (feature != null) len++;
            notNullFeatures = new TrnFeature[len];
            len = 0;
            for (TrnFeature feature : features)
                if (feature != null)
                    notNullFeatures[len++] = feature;
            java.util.Arrays.sort(notNullFeatures, new Comparator<TrnFeature>() {
                public int compare(TrnFeature o1, TrnFeature o2) {
                    if (o1 == null) return (o2 == null) ? 0 : 1;
                    if (o2 == null) return -1;
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });
        }
        return notNullFeatures;
    }

}
