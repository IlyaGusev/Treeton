/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz;

import treeton.core.Treenotation;
import treeton.core.model.TreetonModelException;
import treeton.res.minimz.elector.ElectorEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TypeMatrix {

    public static final int SYM_NO = 0;
    public static final int SYM_INV = 2;
    HashMap<String, Integer> tp2int;
    ArrayList<String> int2tp;
    Elector[][] matrix;

    public TypeMatrix(TypePriority[] priorities, int sym) {
        tp2int = new HashMap<String, Integer>();
        int2tp = new ArrayList<String>();
        int i = 0;
        for (TypePriority tp : priorities) {
            if (!tp2int.containsKey(tp.sType1)) {
                tp2int.put(tp.sType1, i);
                int2tp.add(tp.sType1);
                i++;
            }
            if (!tp2int.containsKey(tp.sType2)) {
                tp2int.put(tp.sType2, i);
                int2tp.add(tp.sType2);
                i++;
            }
        }
        int n = int2tp.size();
        matrix = new Elector[n][n];

        for (TypePriority tp : priorities) {
            put(tp.sType1, tp.sType2, tp.cmpr);
        }

        if (sym == SYM_INV) {
            makeInverseSymmetric();
        }

        for (i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == null) {
                    matrix[i][j] = createEqualsElector();
                }
            }
        }
    }

    public TypeMatrix(String[] types, Elector defaultElector) {
        tp2int = new HashMap<String, Integer>();
        int2tp = new ArrayList<String>();
        int i = 0;
        for (String tp : types) {
            if (!tp2int.containsKey(tp)) {
                tp2int.put(tp, i);
                int2tp.add(tp);
                i++;
            }
        }
        int n = int2tp.size();
        matrix = new Elector[n][n];

        for (i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = defaultElector;
            }
        }
    }

    public static int safeVote(TypeMatrix m, Treenotation t1, Treenotation t2) {
        int rslt = 0;
        if (m != null) {
            try {
                rslt = m.vote(t1, t2);
            } catch (IndexOutOfBoundsException e) {
                // do nothing
            }
        }
        return rslt;
    }

    protected Elector createEqualsElector() {
        return new ElectorEquals();
    }

    public Elector get(int iType1, int iType2) {
        return matrix[iType1][iType2];
    }

    public Elector get(
            String sType1, String sType2) {
        Elector rslt;
        Integer iType1 = tp2int.get(sType1);
        Integer iType2 = tp2int.get(sType2);
        if (iType1 == null || iType2 == null) {
            throw new IndexOutOfBoundsException();
        } else {
            rslt = get(iType1, iType2);
        }
        return rslt;
    }

    public void put(
            String sType1, String sType2, Elector cmpr) {
        Integer iType1 = tp2int.get(sType1);
        Integer iType2 = tp2int.get(sType2);
        if (iType1 == null || iType2 == null) {
            throw new IndexOutOfBoundsException();
        } else {
            matrix[iType1][iType2] = cmpr;
        }
    }

    public void makeInverseSymmetric() {
        Elector e;
        int n = int2tp.size();
        for (int i = 0; i < (n - 1); i++) {
            for (int j = (i + 1); j < n; j++) {
                if (matrix[j][i] != null && matrix[i][j] == null) {
                    e = (Elector) matrix[j][i].clone();
                    e.inverse();
                    matrix[i][j] = e;
                } else if (matrix[j][i] == null && matrix[i][j] != null) {
                    e = (Elector) matrix[i][j].clone();
                    e.inverse();
                    matrix[j][i] = e;
                }
            }
        }
    }

    public Set<String> getTypes() {
        return new HashSet<String>(int2tp);
    }

    public int vote(Treenotation t1, Treenotation t2) {
        int rslt = 0;
        Elector elc;
        try {
            elc = get(t1.getType().getName(), t2.getType().getName());
        } catch (TreetonModelException e) {
            elc = null;
        }
        if (elc != null) {
            rslt = elc.vote(t1, t2);
        }
        return rslt;
    }

}
