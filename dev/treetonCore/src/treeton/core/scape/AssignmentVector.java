/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

import treeton.core.BlackBoard;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.util.NumeratedObject;
import treeton.core.util.nu;
import treeton.core.util.sut;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AssignmentVector {
    private static final ArrayList<NumeratedObject> tValues = new ArrayList<NumeratedObject>();
    private static final BlackBoard localboard = TreetonFactory.newBlackBoard(10, false);
    private static final BlackBoard localboard1 = TreetonFactory.newBlackBoard(10, false);
    private static char[][] delims = new char[][]{new char[]{';'}};
    TrnType tp;

    // Пусть no -- элемент массива values
    // 1-ый случай: *:=A.*
    // в этом случае no.n == -2, no.o - либо объект класса ScapeVariable, либо Object[]{ScapeVariable var,Integer modif}
    // (т.е. переменная, из которой забираются все фичи (A))
    // 2-ой случай: <feature1>:=ScapeExpression
    NumeratedObject[] values;
    TrnTypeStorage sourceTypes;
    TrnTypeStorage targetTypes;

    public AssignmentVector(TrnType type) {
        this.tp = type;
        values = null;

    }

    public AssignmentVector(TrnTypeStorage sourceTypes, TrnTypeStorage targetTypes) {
        this.tp = null;
        values = null;
        this.sourceTypes = sourceTypes;
        this.targetTypes = targetTypes;
    }

    public AssignmentVector(TrnTypeStorage sourceTypes, TrnTypeStorage targetTypes, TrnType type) {
        values = null;
        if (type == null) {
            throw new NullPointerException("Type==null");
        }
        this.sourceTypes = sourceTypes;
        this.targetTypes = targetTypes;

        try {
            if (targetTypes.get(type.getName()) != type) {
                throw new RuntimeException("target type doesn't belong to the target storage");
            }
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error when trying to get the type's name");
        }

        tp = type;
    }

    public AssignmentVector(TrnType tp, ArrayList assignments, HashMap<String, ScapeVariable> variables, HashMap<String, RegexpVariable> regexvars) throws ParseException {
        synchronized (tValues) {
            tValues.clear();
            this.tp = tp;
            if (assignments != null) {
                for (Object assignment : assignments) {
                    NumeratedObject no = (NumeratedObject) assignment;
                    char[] arr = ((String) no.o).toCharArray();
                    readValue(arr, 0, arr.length - 1, variables, regexvars, no.n);
                }
            }
            values = new NumeratedObject[tValues.size()];
            tValues.toArray(values);

        }
    }

    int readStarValue(char s[], int pl, int endpl, HashMap<String, ? extends ScapeVariable> variables, int feature) throws ParseException {
        int ptr;
        String V;
        if (feature == -2) {
            pl = sut.skipSpacesEndls(s, pl, endpl);
            int beg = pl;
            pl = sut.skipVarValueWithDotAndBrackets(s, pl, endpl);
            if (pl == beg) {
                throw new ParseException("missing feature value", null, s, pl, endpl);
            }
            pl = sut.skipSpacesEndls(s, pl, endpl);
            V = sut.extractString(s, beg, pl - beg);

            if ((ptr = V.indexOf('.')) != -1) {
                String varName = V.substring(0, ptr);
                Integer modif = null;
                int startBracketPl = varName.indexOf('[');
                if (startBracketPl >= 0) {
                    int endBracketPl = varName.indexOf(']');
                    String modifStr = varName.substring(startBracketPl + 1, endBracketPl).trim();
                    modif = Integer.parseInt(modifStr);
                    varName = varName.substring(0, startBracketPl).trim();
                }
                if (variables == null) {
                    throw new ParseException("Variable " + varName + " not defined", null, s, beg, endpl);
                }
                ScapeVariable var = variables.get(varName);
                if (var == null) {
                    throw new ParseException("Variable " + varName + " not defined", null, s, beg, endpl);
                }
                ptr++;
                String fName = V.substring(ptr, V.length());
                if (!"*".equals(fName)) {
                    throw new ParseException("Wrong \"*=VAR.*\" construction syntax", null, s, beg, endpl);
                }
                if (modif == null) {
                    tValues.add(new NumeratedObject(feature, var));
                } else {
                    tValues.add(new NumeratedObject(feature, new Object[]{var, modif}));
                }
            } else {
                throw new ParseException("Wrong \"*=VAR.*\" construction syntax", null, s, beg, endpl);
            }
        }
        return pl;
    }

    int readValue(char s[], int pl, int endpl, HashMap<String, ? extends ScapeVariable> variables, HashMap<String, RegexpVariable> regexvars, int feature) throws ParseException {
        pl = sut.skipSpacesEndls(s, pl, endpl);

        ScapeExpression se;
        se = new ScapeExpression();
        pl = se.readIn(s, pl, endpl, delims);
        se.validateExpression(s, endpl, variables, regexvars);
        tValues.add(new NumeratedObject(feature, se));
        return pl;

    }

    int readEqual(char s[], int pl, int endpl, HashMap<String, ? extends ScapeVariable> variables, HashMap<String, RegexpVariable> regexvars) throws ParseException {
        int beg;
        String N;
        int feature;

        pl = sut.skipSpacesEndls(s, pl, endpl);
        beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing feature name", null, s, pl, endpl);
        }
        N = new String(s, beg, pl - beg);
        if (N.equals("*")) {
            feature = -2;
        } else {
            try {
                feature = tp.getFeatureIndex(N);
            } catch (TreetonModelException e) {
                feature = -1;
            }
            if (feature == -1) {
                throw new ParseException("unregistered feature " + N, null, s, pl, endpl);
            }
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl++] != ':') {
            throw new ParseException("missing ':'", null, s, pl, endpl);
        }
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl++] != '=') {
            throw new ParseException("missing '='", null, s, pl, endpl);
        }
        if (feature != -2) {
            pl = readValue(s, pl, endpl, variables, regexvars, feature);
        } else pl = readStarValue(s, pl, endpl, variables, feature);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] == ';') {
            return pl;
        }
        throw new ParseException("wrong equation syntax", null, s, pl, endpl);
    }

    public int readIn(char[] s, int pl, int endpl, HashMap<String, ? extends ScapeVariable> variables, HashMap<String, RegexpVariable> regexvars) throws ParseException {
        synchronized (tValues) {
            tValues.clear();
            pl = sut.skipSpacesEndls(s, pl, endpl);
            if (tp == null) {
                int i = pl;
                pl = sut.skipVarName(s, pl, endpl);
                if (pl == i) {
                    throw new ParseException("missing type name", null, s, pl, endpl);
                }
                String t = new String(s, i, pl - i);

                try {
                    if ((tp = targetTypes.get(t)) == null) {
                        throw new ParseException("unregistered type " + t, null, s, pl, endpl);
                    }
                } catch (TreetonModelException e) {
                    throw new ParseException("unregistered type " + t, null, s, pl, endpl);
                }
                pl = sut.skipSpacesEndls(s, pl, endpl);
            }
            if (pl > endpl) {
                return pl;
            }
            if (s[pl] != '(') {
                throw new ParseException("missing '('", null, s, pl, endpl);
            }
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);

            while (pl <= endpl) {
                sut.checkEndOfStream(s, pl, endpl);
                if (s[pl] == ')') {
                    break;
                }
                pl = readEqual(s, pl, endpl, variables, regexvars);
                pl++;
                pl = sut.skipSpacesEndls(s, pl, endpl);
            }
            if (pl <= endpl && s[pl] == ')') {
                values = new NumeratedObject[tValues.size()];
                tValues.toArray(values);
                return pl + 1;
            }
            throw new ParseException("wrong AssignmentVector syntax", null, s, pl, endpl);
        }
    }

    public TrnType getType() {
        return tp;
    }

    public Iterator<NumeratedObject> numeratedObjectIterator() {
        return new NumeratedObjectIterator();
    }

    public void assign(Treenotation dest) {
        synchronized (localboard) {
            assign(localboard);

            if (!dest.getType().equals(tp)) {
                synchronized (localboard1) {
                    localboard1.put(localboard);
                    localboard.clean();
                    convert(localboard, tp, dest.getType());
                }
            }
            dest.put(localboard);
        }
    }

    public void assign(BlackBoard board) {
        if (values != null) {
            for (NumeratedObject no : values) {
                if (no.n == -2) {
                    if (no.o instanceof Object[]) {
                        Object[] arr = (Object[]) no.o;

                        ScapeVariable variable = (ScapeVariable) arr[0];
                        if (variable.getType().equals(tp)) {
                            variable.fillBlackBoard((Integer) arr[1], board);
                        } else {

                            synchronized (localboard1) {
                                variable.fillBlackBoard((Integer) arr[1], localboard1);
                                convert(board, variable.getType(), tp);
                            }
                        }


                    } else {
                        ScapeVariable variable = (ScapeVariable) no.o;
                        if (variable.getType().equals(tp)) {
                            variable.fillBlackBoard(board);
                        } else {

                            synchronized (localboard1) {
                                variable.fillBlackBoard(localboard1);
                                convert(board, variable.getType(), tp);
                            }
                        }
                    }
                } else {
                    ScapeExpression ex = (ScapeExpression) no.o;
                    Object r = ex.evaluate();

                    if (r == nu.ll) {
                        board.erase(no.n);
                    } else if (r != null) {
                        board.put(no.n, tp, r);
                    }
                }
            }
        }
    }

    private void convert(BlackBoard board, TrnType sourceTp, TrnType destTp) {
        for (int i = 0; i <= localboard1.getDepth(); i++) {
            Object o = localboard1.erase(i);
            if (o != null) {
                try {
                    board.put(destTp.getFeatureIndex(sourceTp.getFeatureNameByIndex(i)), o);
                } catch (TreetonModelException e) {
                    try {
                        System.err.println("Error when trying to transfer features from " + sourceTp.getName() + " to " + destTp.getName());
                    } catch (TreetonModelException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    public void resetBindings(HashMap<String, ? extends ScapeVariable> oldMapping, HashMap<String, ? extends ScapeVariable> variables) {
        if (values != null) {
            for (NumeratedObject no : values) {
                if (no.n == -2) {
                    if (no.o instanceof Object[]) {
                        Object[] arr = (Object[]) no.o;
                        String nm = findName((ScapeVariable) arr[0], oldMapping);
                        arr[0] = variables.get(nm);
                    } else {
                        String nm = findName((ScapeVariable) no.o, oldMapping);
                        no.o = variables.get(nm);
                    }
                } else {
                    ScapeExpression ex = (ScapeExpression) no.o;
                    ex.resetBindings(variables);
                }
            }
        }
    }

    private String findName(ScapeVariable variable, HashMap<String, ? extends ScapeVariable> mapping) {
        for (Map.Entry<String, ? extends ScapeVariable> e : mapping.entrySet()) {
            if (e.getValue() == variable)
                return e.getKey();
        }
        return null;
    }

    private class NumeratedObjectIterator implements Iterator<NumeratedObject> {
        int i;

        NumeratedObjectIterator() {
            i = 0;
            if (values != null) {
                while (i < values.length && values[i] == null) {
                    i++;
                }
            } else {
                i = -1;
            }
        }

        public void remove() {
        }

        public boolean hasNext() {
            return i >= 0 && i < values.length;
        }

        public NumeratedObject next() {
            NumeratedObject no = values[i];
            i++;
            while (i < values.length && values[i] == null) {
                i++;
            }
            return no;
        }
    }

}


