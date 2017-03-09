/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.TString;
import treeton.core.Treenotation;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.util.sut;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

public class TrnFeatureInIntroduction {
    private static char[][] keywords = {
            "feature".toCharArray(),
            "Integer".toCharArray(),
            "String".toCharArray(),
            "viewname".toCharArray(),
            "description".toCharArray(),
            "value".toCharArray(),
            "Boolean".toCharArray(),
            "Treenotation".toCharArray()
    };
    TrnTypeDclImpl tp;
    int featureIndex;
    String viewname;
    String description;
    TrnValueInIntroduction[] allValues = null;//by pjalybin
    HashMap<Object, TrnValueInIntroduction> values = new HashMap<Object, TrnValueInIntroduction>(); //{Object} -> {TrnValueInIntroduction}*

    int readIn(char s[], int pl, int endpl) throws ParseException {
        int startPl = pl;
        boolean typeSpecified = false;
        Class type = TString.class;

        while (true) {
            pl = sut.skipSpacesEndls(s, pl, endpl);
            int n = sut.checkDelims(s, pl, endpl, keywords);
            if (n == 0) { // feature
                pl += keywords[n].length;
                break;
            } else {
                if (n == 1) { //Integer
                    if (typeSpecified) {
                        throw new ParseException("Feature type already specified", null, s, pl, endpl);
                    }
                    typeSpecified = true;
                    type = Integer.class;
                } else if (n == 6) { //Boolean
                    if (typeSpecified) {
                        throw new ParseException("Type already specified", null, s, pl, endpl);
                    }
                    typeSpecified = true;
                    type = Boolean.class;
                } else if (n == 7) { //Treenotation
                    if (typeSpecified) {
                        throw new ParseException("Type already specified", null, s, pl, endpl);
                    }
                    typeSpecified = true;
                    type = Treenotation.class;
                } else if (n == 2) { //String
                    if (typeSpecified) {
                        throw new ParseException("Type already specified", null, s, pl, endpl);
                    }
                    typeSpecified = true;
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
        String N = new String(s, beg, pl - beg);
        Class oldType = null;
        try {
            featureIndex = tp.getFeatureIndex(N);
            oldType = tp.getFeatureTypeByIndex(featureIndex);
        } catch (TreetonModelException e) {
            throw new ParseException("wrong feature declaration, feature " + N, null, s, pl, endpl);
        }
        if (oldType != null && oldType != type) {
            throw new ParseException("wrong type declaration, feature " + N + " already has '" + oldType + "' type", null, s, pl, endpl);
        }
        if (oldType == null) {
            tp.setFeatureType(featureIndex, type);
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);

        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }

        pl++;
        boolean nameFound = false;
        boolean descriptionFound = false;
        while (true) {
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] == '}') {
                break;
            }

            int n = sut.checkDelims(s, pl, endpl, keywords);

            if (n == 3) { //viewname
                pl += keywords[n].length;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                int start = pl;
                pl = sut.skipBracesContent(s, pl, endpl);
                viewname = new String(s, start + 1, pl - 1 - start - 1);
                nameFound = true;
            } else if (n == 4) { //description
                pl += keywords[n].length;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                int start = pl;
                pl = sut.skipBracesContent(s, pl, endpl);
                description = new String(s, start + 1, pl - 1 - start - 1);
                descriptionFound = true;
            } else if (n == 5) { //value
                TrnValueInIntroduction value = new TrnValueInIntroduction();
                value.tp = tp;
                value.featureIndex = featureIndex;
                pl = value.readIn(s, pl, endpl);
                if (values.get(value.value) != null) {
                    throw new ParseException("duplicated value " + value.value, null, s, pl, endpl);
                }
                values.put(value.value, value);
                pl++;
            } else {
                throw new ParseException("Wrong syntax", null, s, pl, endpl);
            }
        }

        if (!descriptionFound) {
            throw new ParseException("missing description block", null, s, startPl, endpl);
        }

        if (!nameFound) {
            throw new ParseException("missing viewname block", null, s, startPl, endpl);
        }

        return pl;
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

    public void mergeWith(TrnFeatureInIntroduction feature) {
        viewname = viewname + "<br>" + feature.viewname;
        description = description + "<br>" + feature.description;
        values.putAll(feature.values);
    }

    public TrnType getTrnType() { // getters by pjalybin 17.11.05
        return tp;
    }

    public int getFeatureIndex() {
        return featureIndex;
    }

    public String getViewname() {
        return viewname;
    }

    public String getDescription() {
        return description;
    }

    public TrnValueInIntroduction[] getAllValues() {
        if (allValues == null && values != null) {
            Collection<TrnValueInIntroduction> valuesCol = values.values();
            allValues = new TrnValueInIntroduction[valuesCol.size()];
            valuesCol.toArray(allValues);
            java.util.Arrays.sort(allValues, new Comparator<TrnValueInIntroduction>() {
                public int compare(TrnValueInIntroduction o1, TrnValueInIntroduction o2) {
                    if (o1 == null) return (o2 == null) ? 0 : 1;
                    if (o2 == null) return -1;
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });
        }
        return allValues;
    }

    public TrnValueInIntroduction getValueByValue(Object value) {
        if (value != null && values != null && values.containsKey(value))
            return values.get(value);
        else return null;
    }

}
