/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.Treenotation;
import treeton.core.util.Suppositor;

import java.util.HashMap;
import java.util.HashSet;

public class VarySuppositor extends Suppositor {
    Treenotation[] arr;
    Treenotation trn;
    HashMap<Treenotation, Treenotation> suppositions;
    int i;

    public VarySuppositor(Treenotation trn, ValidationTree tree, HashSet<MarkInIntroduction> inflHash, HashMap<Treenotation, Treenotation> suppositions, VarySuppositorMemory memory) {
        super();
        Treenotation[] tarr;
        if (memory != null) {
            tarr = memory.getArray(trn);
            if (tarr == null) {
                tarr = tree.vary(trn.getStartToken(), trn.getEndToken(), trn, inflHash, true);
                if (tarr != null) {
                    memory.store(trn, tarr);
                }
            }
        } else {
            tarr = tree.vary(trn.getStartToken(), trn.getEndToken(), trn, inflHash, true);
        }

        if (tarr == null) {
            arr = null;
            i = -2;
            this.suppositions = null;
            this.trn = null;
        } else {
            arr = tarr;
            this.suppositions = suppositions;
            this.trn = trn;
            i = -1;
        }
    }

    public boolean next() {
        i++;
        if (i != -1 && i < arr.length) {
            suppositions.put(trn, arr[i]);
            return true;
        } else {
            return false;
        }
    }

    public void reset() {
        if (i != -2) {
            if (suppositions != null) {
                suppositions.remove(trn);
                suppositions.remove(trn);
            }
            i = -1;
        }
    }

    public Treenotation getTreenotation() {
        return arr[i];
    }

    public Treenotation getSourceTrn() {
        return trn;
    }

    public void finish() {
        if (i != -1) {
            if (suppositions != null) {
                suppositions.remove(trn);
                suppositions = null;
            }
            trn = null;
            arr = null;
        }
    }
}
