/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Node;

public class BindingAndLinkedAnnsItem {
    LinkedAnns.Item item;
    ScapeDFSMBinding binding;
    Node start;
    Node end;
    int size;

    public void importInfo(BindingAndLinkedAnnsItem b) {
        item = b.item;
        binding = b.binding;
        start = b.start;
        end = b.end;
        size = b.size;
    }

    public void activateBinding() {
        binding.startNode = start;
        binding.endNode = end;
        binding.size = size;
        binding.annItem = item;
    }

    public void deactivateBinding() {
        binding.startNode = null;
        binding.endNode = null;
        binding.size = -1;
        binding.annItem = null;
    }
}
