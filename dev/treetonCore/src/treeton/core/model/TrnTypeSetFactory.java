/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model;

import treeton.core.util.RBTreeMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public class TrnTypeSetFactory {
    RBTreeMap typeSetsIndex = new RBTreeMap();
    private TrnType[] typesArr = new TrnType[100];
    private TrnTypeSet typeSetForSearch = new TrnTypeSet();
    private HashSet<TrnType> hs = new HashSet<TrnType>();

    public TrnTypeSet newTrnTypeSet(TrnType[] types, int size) {
        int n = size;
        if (n > typesArr.length) {
            TrnType[] tarr = new TrnType[(int) (Math.max(typesArr.length * 1.5, n))];
            System.arraycopy(typesArr, 0, tarr, 0, typesArr.length);
            typesArr = tarr;
        }

        System.arraycopy(types, 0, typesArr, 0, size);

        int j = 0;
        int max = -1;
        hs.clear();
        for (int i = 0; i < n; i++) {
            TrnType tp = typesArr[i];
            if (hs.contains(tp))
                continue;
            typesArr[j++] = tp;
            int index;
            try {
                index = tp.getIndex();
                if (index > max)
                    max = index;
                hs.add(tp);
            } catch (TreetonModelException e) {
                //do nothing
            }
        }
        n = j;
        Arrays.sort(typesArr, 0, n);
        typeSetForSearch.types = typesArr;
        typeSetForSearch.size = n;
        TrnTypeSet ts = (TrnTypeSet) typeSetsIndex.get(typeSetForSearch);
        if (ts == null) {
            ts = new TrnTypeSet();
            ts.types = new TrnType[n];
            ts.typesByIndex = new TrnType[max + 1];
            Arrays.fill(ts.typesByIndex, null);
            for (int i = 0; i < n; i++) {
                ts.types[i] = typesArr[i];
                try {
                    ts.typesByIndex[typesArr[i].getIndex()] = typesArr[i];
                } catch (TreetonModelException e) {
                    //do nothing
                }
            }
            ts.size = n;
            typeSetsIndex.put(ts, ts);
        }
        return ts;
    }

    public TrnTypeSet newTrnTypeSet(Iterator<TrnType> types) {
        int j = 0;
        int max = -1;
        hs.clear();
        while (types.hasNext()) {
            TrnType tp = types.next();
            if (hs.contains(tp))
                continue;
            if (j >= typesArr.length) {
                TrnType[] tarr = new TrnType[(int) (Math.max(typesArr.length * 1.5, j + 1))];
                System.arraycopy(typesArr, 0, tarr, 0, typesArr.length);
                typesArr = tarr;
            }
            typesArr[j++] = tp;
            int ind;
            try {
                ind = tp.getIndex();
                if (ind > max)
                    max = ind;
                hs.add(tp);
            } catch (TreetonModelException e) {
                //do nothing
            }
        }

        int n = j;
        Arrays.sort(typesArr, 0, n);
        typeSetForSearch.types = typesArr;
        typeSetForSearch.size = n;
        TrnTypeSet ts = (TrnTypeSet) typeSetsIndex.get(typeSetForSearch);
        if (ts == null) {
            ts = new TrnTypeSet();
            ts.types = new TrnType[n];
            ts.typesByIndex = new TrnType[max + 1];
            Arrays.fill(ts.typesByIndex, null);
            for (int i = 0; i < n; i++) {
                ts.types[i] = typesArr[i];
                try {
                    ts.typesByIndex[typesArr[i].getIndex()] = typesArr[i];
                } catch (TreetonModelException e) {
                    //do nothing
                }
            }
            ts.size = n;
            typeSetsIndex.put(ts, ts);
        }
        return ts;
    }
}
