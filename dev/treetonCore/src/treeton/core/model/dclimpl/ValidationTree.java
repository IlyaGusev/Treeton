/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.*;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.util.sut;

import java.util.ArrayList;
import java.util.HashSet;

public class ValidationTree {
    public static final int KEEP_HASHED_MARKS = 0;
    public static final int REMOVE_HASHED_MARKS = 1;
    private static char[][] keywords = {
            "extends".toCharArray(),
            "hierarchy".toCharArray()
    };
    final BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);
    String name;
    TrnTypeDclImpl tp;
    TrnTypeDclImpl basisTp;
    TrnValue root;
    ArrayList<DescendantInfo> KEEP_descendants = new ArrayList<DescendantInfo>();
    ArrayList<DescendantInfo> REMOVE_descendants = new ArrayList<DescendantInfo>();

    public ValidationTree(TrnTypeDclImpl tp, TrnTypeDclImpl basisTp) {
        this.tp = tp;
        this.basisTp = basisTp;
    }

    static boolean checkKeepCondition(HashSet nodeHash, HashSet markNames, int mode) {
        if (markNames == null)
            return true;

        for (Object m : nodeHash) {
            if (markNames.contains(m)) {
                return mode == KEEP_HASHED_MARKS;
            }
        }
        return mode != KEEP_HASHED_MARKS;
    }

    public int readIn(char[] s, int pl, int endpl, boolean fromParentDomain) throws ParseException, TreetonModelException {
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        if (n != 1) { //hierarchy
            throw new ParseException("wrong syntax (hierarchy expected)", null, s, pl, endpl);
        }
        pl += keywords[n].length;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing hierarchy name", null, s, pl, endpl);
        }
        String N = new String(s, beg, pl - beg);
        if (fromParentDomain) {
            if (tp.getHierarchy(N) == null) {
                throw new ParseException("wrong hierarchy name " + N, null, s, pl, endpl);
            }
        } else {
            if (tp.getHierarchy(N) != null) {
                throw new ParseException("hierarchy " + N + " already declared", null, s, pl, endpl);
            }
        }
        name = N;
        pl = sut.skipSpacesEndls(s, pl, endpl);

        n = sut.checkDelims(s, pl, endpl, keywords);

        if (n == 0) { //extends
            if (fromParentDomain) {
                throw new ParseException("\"extends\" keyword can not be used here because the hierarchy is defined in the parent domain", null, s, pl, endpl);
            }
            pl += keywords[n].length;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            beg = pl;
            pl = sut.skipVarName(s, pl, endpl);
            if (pl == beg) {
                throw new ParseException("missing type name", null, s, pl, endpl);
            }
            N = new String(s, beg, pl - beg);
            TrnTypeDclImpl basisTp;
            if ((basisTp = (TrnTypeDclImpl) tp.getStorage().get(N)) == null) {
                throw new ParseException("type " + N + " is undeclared", null, s, pl, endpl);
            }
            if (this.basisTp != basisTp) {
                throw new ParseException("hierarchy can only extend hierarchy from the basis type", null, s, pl, endpl);
            }

            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] != '.') {
                throw new ParseException("missing '.'", null, s, pl, endpl);
            }
            pl++;
            beg = pl;
            pl = sut.skipVarName(s, pl, endpl);
            if (pl == beg) {
                throw new ParseException("missing hierarchy name", null, s, pl, endpl);
            }
            N = new String(s, beg, pl - beg);
            ValidationTree basisTree = basisTp.getHierarchy(N);
            if (basisTree == null) {
                throw new ParseException("there is no hierarchy named " + N + " in " + basisTp.getName() + " type", null, s, pl, endpl);
            }

            root = TrnValue.copyValidationTree(basisTree.root, tp, -1, false);
            TrnValue newValidationTree = new TrnValue();
            newValidationTree.tp = tp;
            newValidationTree.value = null;
            newValidationTree.featureIndex = -1;
            pl = newValidationTree.readIn(s, pl, endpl, false);
            root.mergeWith(newValidationTree);
        } else {
            root = new TrnValue();
            root.tp = tp;
            root.value = null;
            root.featureIndex = -1;
            if (!fromParentDomain) {
                root.registerBasis();
            }
            pl = root.readIn(s, pl, endpl, false);
        }

        return pl;
    }

    public String[] getFeatureNamesMarkedWith(MarkInIntroduction declaredMark) throws TreetonModelException {
        HashSet<String> result = new HashSet<String>();
        root.addFeatureNamesMarkedWith(result, declaredMark);
        return result.toArray(new String[result.size()]);
    }

    public ValidationTree refactor(HashSet markNames, int mode) {
        if (mode == KEEP_HASHED_MARKS) {
            for (Object KEEP_descendant : KEEP_descendants) {
                DescendantInfo info = (DescendantInfo) KEEP_descendant;
                if (info.markNames.containsAll(markNames) && info.markNames.size() == markNames.size()) {
                    return info.descendant;
                }
            }
        } else {
            for (Object REMOVE_descendant : REMOVE_descendants) {
                DescendantInfo info = (DescendantInfo) REMOVE_descendant;
                if (info.markNames.containsAll(markNames) && info.markNames.size() == markNames.size()) {
                    return info.descendant;
                }
            }
        }

        ValidationTree result = new ValidationTree(tp, basisTp);
        result.name = name;
        result.root = TrnValue.refactorValidationTree(root, markNames, mode);
        DescendantInfo info = new DescendantInfo();
        info.mode = mode;
        info.markNames = (HashSet) markNames.clone();
        info.descendant = result;
        if (mode == KEEP_HASHED_MARKS) {
            KEEP_descendants.add(info);
        } else {
            REMOVE_descendants.add(info);
        }
        return result;
    }

    public Treenotation[] vary(Token start, Token end, IntFeatureMap initialData, HashSet<MarkInIntroduction> varyMarks, boolean skip) {
        TList result = root.vary(initialData, varyMarks, skip);

        Treenotation[] arr = new Treenotation[result.size()];
        TListEntry cur = result.first;

        int i = 0;
        while (cur != null) {
            arr[i++] = ((IntFeatureMap) cur.o).convertToTreenotation(start, end, tp);
            cur = cur.next;
        }

        return arr;
    }

    public HashSet<Object> getPossibleValuesForFeature(IntFeatureMap context, int feature, HashSet varyMarks) {
        synchronized (this) {
            HashSet<Object> result = new HashSet<Object>();
            HashSet dummy = new HashSet();
            if (root.isContextValid((IntFeatureMap) context.clone(), feature, dummy, varyMarks)) {
                result.addAll(root.vHash);
            }
            return result;
        }
    }

    public HashSet getPossibleFeatures(IntFeatureMap context, HashSet varyMarks) {
        synchronized (this) {
            HashSet result = new HashSet();
            root.isContextValid((IntFeatureMap) context.clone(), -1, result, varyMarks);
            return result;
        }
    }

    public void filterTemplate(IntFeatureMap context, HashSet varyMarks) {
        synchronized (this) {
            HashSet result = new HashSet();
            if (root.filterTemplate((IntFeatureMap) context.clone(), result, varyMarks)) {
                for (int i = 0; i < context.size(); ) {
                    int kn = context.getKey(i);
                    if (!result.contains(kn)) {
                        context.remove(kn);
                    } else {
                        i++;
                    }
                }
            } else {
                context.removeAll();
            }
        }
    }

    public String getName() {  //getters by pjalybin 18.11.05
        return name;
    }

    public TrnType getTrnType() {
        return tp;
    }

    public TrnType getBasisTp() {
        return basisTp;
    }

    public TrnValue getRoot() {
        return root;
    }

    public String toString() { // by pjalybin 18.11.05
        return name;
    }

    class DescendantInfo {
        int mode;
        HashSet markNames;
        ValidationTree descendant;
    }

}
