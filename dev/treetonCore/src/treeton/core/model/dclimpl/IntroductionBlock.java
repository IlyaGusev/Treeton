/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.model.TreetonModelException;
import treeton.core.scape.ParseException;
import treeton.core.util.sut;

import java.util.Comparator;
import java.util.HashMap;

public class IntroductionBlock {
    private static char[][] keywords = {
            "introduction".toCharArray(),
            "feature".toCharArray(),
            "Integer".toCharArray(),
            "String".toCharArray(),
            "mark".toCharArray(),
            "Boolean".toCharArray(),
            "Treenotation".toCharArray(),
    };
    TrnFeatureInIntroduction[] features = null;
    TrnFeatureInIntroduction[] sortedFeatures = null;
    MarkInIntroduction[] sortedMarks = null;
    HashMap<String, MarkInIntroduction> marks = new HashMap<String, MarkInIntroduction>();
    TrnTypeDclImpl tp;

    public IntroductionBlock(TrnTypeDclImpl tp) {
        this.tp = tp;
    }

    public static void copyIntroductionBlock(IntroductionBlock dest, IntroductionBlock source) throws TreetonModelException {
        if (source.features != null) {
            for (TrnFeatureInIntroduction feature : source.features) {
                if (feature == null)
                    continue;
                int fn = dest.tp.getFeatureIndex(source.tp.getFeatureNameByIndex(feature.featureIndex));
                if (dest.features == null) {
                    dest.features = new TrnFeatureInIntroduction[fn + 1];
                }

                if (dest.features.length <= fn) {
                    TrnFeatureInIntroduction[] tarr = new TrnFeatureInIntroduction[Math.max(dest.features.length * 3 / 2, fn + 1)];
                    System.arraycopy(dest.features, 0, tarr, 0, dest.features.length);
                    dest.features = tarr;
                }
                TrnFeatureInIntroduction newFeature = new TrnFeatureInIntroduction();
                newFeature.tp = dest.tp;
                newFeature.viewname = feature.viewname;
                newFeature.description = feature.description;
                newFeature.featureIndex = fn;
                newFeature.values = new HashMap<Object, TrnValueInIntroduction>();
                for (TrnValueInIntroduction value : feature.values.values()) {
                    TrnValueInIntroduction newValue = new TrnValueInIntroduction();
                    newValue.tp = dest.tp;
                    newValue.value = value.value;
                    newValue.featureIndex = fn;
                    newValue.viewname = value.viewname;
                    newValue.description = value.description;
                    newFeature.values.put(newValue.value, newValue);
                }
                dest.features[fn] = newFeature;
                dest.tp.setFeatureType(fn, source.tp.getFeatureTypeByIndex(feature.featureIndex));
            }
        } else {
            dest.features = null;
        }

        for (MarkInIntroduction mark : source.marks.values()) {
            MarkInIntroduction newMark = new MarkInIntroduction();

            newMark.name = mark.name;
            newMark.viewname = mark.viewname;
            newMark.description = mark.description;
            newMark.tp = dest.tp;

            dest.marks.put(newMark.name, newMark);
        }
    }

    public void registerBasis() throws TreetonModelException {
        TrnFeatureInIntroduction feature = new TrnFeatureInIntroduction();
        feature.tp = tp;
        feature.viewname = "string";
        feature.featureIndex = tp.getFeatureIndex("string");
        feature.description = "String corresponding the annotation";

        int fn = feature.featureIndex;
        if (features == null) {
            features = new TrnFeatureInIntroduction[fn + 1];
        } else if (features.length <= fn) {
            TrnFeatureInIntroduction[] tarr = new TrnFeatureInIntroduction[Math.max(features.length * 3 / 2, fn + 1)];
            System.arraycopy(features, 0, tarr, 0, features.length);
            features = tarr;
        }

        features[fn] = feature;

        feature = new TrnFeatureInIntroduction();
        feature.tp = tp;
        feature.viewname = "length";
        feature.featureIndex = tp.getFeatureIndex("length");
        feature.description = "Length of the annotation";

        fn = feature.featureIndex;
        if (features == null) {
            features = new TrnFeatureInIntroduction[fn + 1];
        } else if (features.length <= fn) {
            TrnFeatureInIntroduction[] tarr = new TrnFeatureInIntroduction[Math.max(features.length * 3 / 2, fn + 1)];
            System.arraycopy(features, 0, tarr, 0, features.length);
            features = tarr;
        }

        features[fn] = feature;

        feature = new TrnFeatureInIntroduction();
        feature.tp = tp;
        feature.viewname = "orthm";
        feature.featureIndex = tp.getFeatureIndex("orthm");
        feature.description = "Orth mask of the annotation";

        fn = feature.featureIndex;
        if (features == null) {
            features = new TrnFeatureInIntroduction[fn + 1];
        } else if (features.length <= fn) {
            TrnFeatureInIntroduction[] tarr = new TrnFeatureInIntroduction[Math.max(features.length * 3 / 2, fn + 1)];
            System.arraycopy(features, 0, tarr, 0, features.length);
            features = tarr;
        }

        features[fn] = feature;

        feature = new TrnFeatureInIntroduction();
        feature.tp = tp;
        feature.viewname = "start";
        feature.featureIndex = tp.getFeatureIndex("start");
        feature.description = "Start offset of the annotation";

        fn = feature.featureIndex;
        if (features == null) {
            features = new TrnFeatureInIntroduction[fn + 1];
        } else if (features.length <= fn) {
            TrnFeatureInIntroduction[] tarr = new TrnFeatureInIntroduction[Math.max(features.length * 3 / 2, fn + 1)];
            System.arraycopy(features, 0, tarr, 0, features.length);
            features = tarr;
        }

        features[fn] = feature;
        feature = new TrnFeatureInIntroduction();
        feature.tp = tp;
        feature.viewname = "end";
        feature.featureIndex = tp.getFeatureIndex("end");
        feature.description = "End offset of the annotation";

        fn = feature.featureIndex;
        if (features == null) {
            features = new TrnFeatureInIntroduction[fn + 1];
        } else if (features.length <= fn) {
            TrnFeatureInIntroduction[] tarr = new TrnFeatureInIntroduction[Math.max(features.length * 3 / 2, fn + 1)];
            System.arraycopy(features, 0, tarr, 0, features.length);
            features = tarr;
        }

        features[fn] = feature;

        MarkInIntroduction m = new MarkInIntroduction();
        m.description = "marks 'string','length','start','end' features";
        m.name = "stringMark";
        m.tp = tp;
        m.viewname = "stringMark";
        marks.put(m.name, m);

        m = new MarkInIntroduction();
        m.description = "marks orthm feature";
        m.name = "orthmMark";
        m.tp = tp;
        m.viewname = "orthmMark";
        marks.put(m.name, m);

        m = new MarkInIntroduction();
        m.description = "Очень широкий класс фич. Не поддается емкому описанию :(";
        m.name = "sys";
        m.tp = tp;
        m.viewname = "Системные фичи";
        marks.put(m.name, m);

        m = new MarkInIntroduction();
        m.description = "Атрибуты, которые тем или иным образом характеризуют конкретную лексему";
        m.name = "lex";
        m.tp = tp;
        m.viewname = "Лексические атрибуты";
        marks.put(m.name, m);

        m = new MarkInIntroduction();
        m.description = "Атрибуты, которые тем или иным образом характеризуют конкретный элемент парадигмы некоторой лексемы";
        m.name = "infl";
        m.tp = tp;
        m.viewname = "Словоизменительные атрибуты";
        marks.put(m.name, m);

        m = new MarkInIntroduction();
        m.description = "Этот маркер используется тогда, когда нужно исключить некоторые клетки\n" +
                "               парадигмы грамматически неизменяемого объекта (INVAR=invar) при\n" +
                "               искуственном размножении его словоформ.";
        m.name = "invar_excl";
        m.tp = tp;
        m.viewname = "Метка исключения при \"склонении\" неизменяемых";
        marks.put(m.name, m);

        m = new MarkInIntroduction();
        m.description = "Фичи, имеющие отношение к семантике";
        m.name = "sem";
        m.tp = tp;
        m.viewname = "Семантические фичи";
        marks.put(m.name, m);

        m = new MarkInIntroduction();
        m.description = "Фичи, характеризующие синтаксическое поведение, основанное на семантике конкретных лексем";
        m.name = "semsynt";
        m.tp = tp;
        m.viewname = "Синтактико-семантические фичи";
        marks.put(m.name, m);
    }

    int readIn(char s[], int pl, int endpl) throws ParseException {
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        if (n == 0) { // introduction
            pl += keywords[n].length;
        } else {
            throw new ParseException("Wrong syntax", null, s, pl, endpl);
        }

        pl = sut.skipSpacesEndls(s, pl, endpl);

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

            n = sut.checkDelims(s, pl, endpl, keywords);

            if (n == 1 || n == 2 || n == 3 || n == 5 || n == 6) { //feature, Integer, String, Boolean, Treenotation
                TrnFeatureInIntroduction feature = new TrnFeatureInIntroduction();
                feature.tp = tp;
                pl = feature.readIn(s, pl, endpl);
                int fn = feature.featureIndex;
                if (features == null) {
                    features = new TrnFeatureInIntroduction[fn + 1];
                }

                if (features.length <= fn) {
                    TrnFeatureInIntroduction[] tarr = new TrnFeatureInIntroduction[Math.max(features.length * 3 / 2, fn + 1)];
                    System.arraycopy(features, 0, tarr, 0, features.length);
                    features = tarr;
                    features[fn] = feature;
                } else if (features[fn] != null) {
                    features[fn].mergeWith(feature);
                } else {
                    features[fn] = feature;
                }
            } else if (n == 4) { //mark
                MarkInIntroduction mark = new MarkInIntroduction();
                mark.tp = tp;
                pl = mark.readIn(s, pl, endpl);
                if (marks.get(mark.name) != null) {
                    throw new ParseException("duplicated mark declaration" + mark.name, null, s, pl, endpl);
                }
                marks.put(mark.name, mark);
            } else {
                throw new ParseException("Wrong syntax", null, s, pl, endpl);
            }

            pl++;
        }
        return pl;
    }

    public TrnFeatureInIntroduction getFeature(int featureIndex) {
        if (featureIndex < 0 || featureIndex >= features.length) {
            return null;
        }
        return features[featureIndex];
    }

    public TrnFeatureInIntroduction[] getAllFeatures() {            //by pjalybin 18.11.05
        if (sortedFeatures == null && features != null) {
            int notNullNum = 0;
            for (TrnFeatureInIntroduction f : features) if (f != null) notNullNum++;
            sortedFeatures = new TrnFeatureInIntroduction[notNullNum];
            int i = 0;
            for (TrnFeatureInIntroduction f : features) if (f != null) sortedFeatures[i++] = f;
            java.util.Arrays.sort(sortedFeatures, new Comparator<TrnFeatureInIntroduction>() {
                public int compare(TrnFeatureInIntroduction o1, TrnFeatureInIntroduction o2) {
                    if (o1 == null) return (o2 == null) ? 0 : 1;
                    if (o2 == null) return -1;
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });
        }
        return sortedFeatures;
    }

    public MarkInIntroduction[] getAllMarks() {
        if (sortedMarks == null && marks != null) {
            int notNullNum = 0;
            for (MarkInIntroduction m : marks.values()) if (m != null) notNullNum++;
            sortedMarks = new MarkInIntroduction[notNullNum];
            int i = 0;
            for (MarkInIntroduction m : marks.values()) if (m != null) sortedMarks[i++] = m;
            java.util.Arrays.sort(sortedMarks, new Comparator<MarkInIntroduction>() {
                public int compare(MarkInIntroduction o1, MarkInIntroduction o2) {
                    if (o1 == null) return (o2 == null) ? 0 : 1;
                    if (o2 == null) return -1;
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });
        }
        return sortedMarks;
    }

    public String toString() {
        return "introduction";
    }

}


