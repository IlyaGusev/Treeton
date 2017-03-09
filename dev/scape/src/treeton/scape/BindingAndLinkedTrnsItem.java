/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.Token;
import treeton.core.util.LinkedTrns;

public class BindingAndLinkedTrnsItem {
    LinkedTrns.Item item;
    ScapeDFSMBinding binding;
    Token start;
    Token end;
    int size;

    public void importInfo(BindingAndLinkedTrnsItem b) {
        item = b.item;
        binding = b.binding;
        start = b.start;
        end = b.end;
        size = b.size;
    }

    public void activateBinding() {
        binding.startToken = start;
        binding.endToken = end;
        binding.size = size;
        binding.trnItem = item;
    }

    public void deactivateBinding() {
        binding.startToken = null;
        binding.endToken = null;
        binding.size = -1;
        binding.trnItem = null;
    }
}
