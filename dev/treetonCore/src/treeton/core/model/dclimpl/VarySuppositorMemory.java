/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.Treenotation;

import java.util.HashMap;

public class VarySuppositorMemory {
    private HashMap<Treenotation, Treenotation[]> memory = new HashMap<Treenotation, Treenotation[]>();

    public Treenotation[] getArray(Treenotation trn) {
        return memory.get(trn);
    }

    public void store(Treenotation trn, Treenotation[] arr) {
        memory.put(trn, arr);
    }


}
