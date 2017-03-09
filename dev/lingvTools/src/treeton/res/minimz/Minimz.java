/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz;

import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeSet;
import treeton.core.util.TrnOffsetLengthComparator;

import java.util.*;

public class Minimz {
    /**
     * original
     */
    protected static final int ORI = 0;
    /**
     * equals
     */
    protected static final int EQL = 1;
    /**
     * intersection
     */
    protected static final int ITS = 2;
    /**
     * inside
     */
    protected static final int ISD = 3;
    protected TypeMatrix equals;
    protected TypeMatrix intersects;
    protected TypeMatrix inside;

    public Minimz(TypeMatrix equals,
                  TypeMatrix intersects,
                  TypeMatrix inside) {
        this.equals = equals;
        this.intersects = intersects;
        this.inside = inside;
    }

    public Minimz(TypePriority[] tpEquals,
                  TypePriority[] tpIntersects,
                  TypePriority[] tpInside) {
        this.equals = new TypeMatrix(tpEquals, TypeMatrix.SYM_INV);
        this.intersects = new TypeMatrix(tpIntersects, TypeMatrix.SYM_INV);
        this.inside = new TypeMatrix(tpInside, TypeMatrix.SYM_NO);
    }

    protected static void addStat(HashMap<String, MinimzStat> hm, String aType, int what) {
        MinimzStat s = hm.get(aType);
        if (s == null) {
            s = new MinimzStat();
            hm.put(aType, s);
        }
        switch (what) {
            case ORI:
                s.oridinal++;
                break;
            case EQL:
                s.delEquals++;
                break;
            case ITS:
                s.delIntersects++;
                break;
            case ISD:
                s.delInside++;
                break;
        }
    }

    public static void minimzByMatrix(TreenotationStorage storage, TreenotationsContext context, TypeMatrixTriplet triplet) {
        minimzByMatrix(storage, context, triplet.getEqualsMatrix(), triplet.getIntersectMatrix(), triplet.getInsideMatrix());
    }

    public static void minimzByMatrix(TreenotationStorage storage, TreenotationsContext context,
                                      TypeMatrix equals,
                                      TypeMatrix intersects,
                                      TypeMatrix inside) {
        Minimz m = new Minimz(equals, intersects, inside);
        m.applyTo(context, storage);
    }

    public static void minimzByMatrix(TreenotationStorage storage, TreenotationsContext context,
                                      TypePriority[] equals,
                                      TypePriority[] intersects,
                                      TypePriority[] inside) {
        minimzByMatrix(storage, context,
                new TypeMatrix(equals, TypeMatrix.SYM_INV),
                new TypeMatrix(intersects, TypeMatrix.SYM_INV),
                new TypeMatrix(inside, TypeMatrix.SYM_NO));
    }

    public TypeMatrix getEquals() {
        return equals;
    }

    public TypeMatrix getIntersects() {
        return intersects;
    }

    public TypeMatrix getInside() {
        return inside;
    }

    public void applyTo(TreenotationsContext context, TreenotationStorage storage) {
        HashSet<String> hsTypes = new HashSet<String>();
        if (equals != null) {
            hsTypes.addAll(equals.getTypes());
        }
        if (intersects != null) {
            hsTypes.addAll(intersects.getTypes());
        }
        if (inside != null) {
            hsTypes.addAll(inside.getTypes());
        }

        TrnType[] types = TrnTypeSet.getTrnTypeArrFromHashSet(context, hsTypes);

        Iterator<Treenotation> it = storage.typeIterator(types);
        if (it.hasNext()) {
            ArrayList<Treenotation> arr = new ArrayList<Treenotation>();
            while (it.hasNext()) {
                Treenotation trn = it.next();
                arr.add(trn);
            }
            Treenotation[] trnArray = arr.toArray(new Treenotation[arr.size()]);
            Comparator<Treenotation> cc = new TrnOffsetLengthComparator();
            Arrays.sort(trnArray, cc);

            int n = trnArray.length;
            boolean[] deleted = new boolean[n];
            Arrays.fill(deleted, false);
            ArrayList<Treenotation> inScope = new ArrayList<Treenotation>();
            ArrayList<Integer> inScopeI = new ArrayList<Integer>();

            for (int i = 0; i < n; i++) {
                Treenotation ti = trnArray[i];

                long start = ti.getStartToken().getStartNumerator();
                long end = ti.getEndToken().getEndNumerator();

                int j = 0;
                while (j < inScope.size()) {
                    Treenotation tj = inScope.get(j);
                    int jPos = inScopeI.get(j);

                    long jStart = tj.getStartToken().getStartNumerator();
                    long jEnd = tj.getEndToken().getEndNumerator();

                    if (jEnd <= start) {
                        inScope.remove(j);
                        inScopeI.remove(j);
                    } else {
                        int vote = 0;
                        if (start == jStart && end == jEnd) {
                            // ai равна aj
                            vote = TypeMatrix.safeVote(equals, ti, tj);
                        } else if ((start >= jStart && end < jEnd) ||
                                (start > jStart && end <= jEnd)) {
                            // ai вложена в aj
                            vote = -TypeMatrix.safeVote(inside, tj, ti);
                        } else if ((start <= jStart && end > jEnd) ||
                                (start < jStart && end <= jEnd)) {
                            // aj вложена в ai
                            vote = TypeMatrix.safeVote(inside, ti, tj);
                        } else if (start < jStart && end > jStart && end < jEnd) {
                            // ai пересекается с aj и находится левее
                            vote = TypeMatrix.safeVote(intersects, ti, tj);
                        } else if (start > jStart && start < jEnd && end > jEnd) {
                            // ai пересекается с aj и находится правее
                            vote = -TypeMatrix.safeVote(intersects, tj, ti);
                        }
                        if (vote > 0) {
                            // удаляем aj
                            if (!deleted[jPos]) {
                                deleted[jPos] = true;
                            }
                        } else if (vote < 0) {
                            // удаляем ai
                            if (!deleted[i]) {
                                deleted[i] = true;
                            }
                        }
                        j++;
                    }
                }

                inScope.add(ti);
                inScopeI.add(i);
            }

            for (int i = 0; i < n; i++) {
                if (deleted[i]) {
                    storage.remove(trnArray[i]);
                }
            }
        }
    }

    protected static class MinimzStat {
        public int oridinal;
        public int delEquals;
        public int delIntersects;
        public int delInside;

        boolean hasAnyValue() {
            return (oridinal + delEquals + delIntersects + delInside) > 0;
        }
    }
}
