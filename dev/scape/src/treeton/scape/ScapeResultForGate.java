/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.Node;
import treeton.core.scape.ScapeRegexBinding;

import java.util.Iterator;

public class ScapeResultForGate {
    LinkedAnns.Item lastMatched;
    ScapeDFSMState currentState;
    BindingAndLinkedAnnsItem[] bindings;
    ScapePhase phase;
    int nBindings;
    ScapeRegexBinding[] regexBindings;
    int nRegexBindings;
    int size;
    Node start;
    Node end;
    private boolean firstTime;

    ScapeResultForGate(ScapePhase phase) {
        this.phase = phase;
        lastMatched = null;
        currentState = null;
        size = 0;
        start = null;
        end = null;
        nBindings = 0;
        bindings = new BindingAndLinkedAnnsItem[5];
        for (int i = 0; i < bindings.length; i++) {
            bindings[i] = new BindingAndLinkedAnnsItem();
        }
        regexBindings = new ScapeRegexBinding[5];
        nRegexBindings = 0;
    }

    void activateBindings() {
        phase.allBinding.attach(this);
        for (int i = 0; i < nBindings; i++) {
            BindingAndLinkedAnnsItem balt = bindings[i];
            balt.activateBinding();
            balt.binding.doc = phase.doc;
            if (balt.binding.regexBinding != null) {
                int j = 0;
                for (; j < nRegexBindings; j++) {
                    ScapeRegexBinding binding = regexBindings[j];
                    if (binding == balt.binding.regexBinding) {
                        break;
                    }
                }
                if (j == nRegexBindings) {
                    if (nRegexBindings == regexBindings.length) {
                        ScapeRegexBinding[] tarr = new ScapeRegexBinding[regexBindings.length * 3 / 2];
                        System.arraycopy(regexBindings, 0, tarr, 0, regexBindings.length);
                        regexBindings = tarr;
                    }
                    regexBindings[nRegexBindings++] = balt.binding.regexBinding;
                    balt.binding.regexBinding.setFSM(balt.binding.regexFSM);
                    Object val = balt.binding.getValue(balt.binding.regexFeature);
                    balt.binding.regexBinding.setString(val == null ? null : val.toString());
                    balt.binding.regexBinding.matchBindings();
                }
            }
        }
        firstTime = true;
    }

    boolean nextCombination() {
        if (firstTime) {
            firstTime = false;
            for (int i = 0; i < nRegexBindings; i++) {
                regexBindings[i].getFsm().next();
            }
            return true;
        } else {
            for (int i = nRegexBindings - 1; i >= 0; i--) {
                ScapeRegexBinding binding = regexBindings[i];
                if (binding.getFsm().next()) {
                    for (int j = i + 1; j < nRegexBindings; j++) {
                        regexBindings[j].getFsm().resetMatch();
                        regexBindings[j].getFsm().next();
                    }
                    return true;
                }
            }
            return false;
        }
    }

    void deactivateBindings() {
        for (int i = 0; i < nBindings; i++) {
            BindingAndLinkedAnnsItem balt = bindings[i];
            balt.binding.doc = null;
            balt.deactivateBinding();
        }
        for (int j = 0; j < nRegexBindings; j++) {
            ScapeRegexBinding binding = regexBindings[j];
            binding.getFsm().forgetMatch();
        }
        phase.allBinding.detach();
    }

    public void importBindings(ScapeResultForGate r) {
        nBindings = r.nBindings;
        if (nBindings > 0) {
            if (bindings.length < nBindings) {
                BindingAndLinkedAnnsItem[] tarr = new BindingAndLinkedAnnsItem[nBindings];
                int i;
                for (i = 0; i < bindings.length; i++) {
                    tarr[i] = bindings[i];
                }
                for (; i < tarr.length; i++) {
                    tarr[i] = new BindingAndLinkedAnnsItem();
                }
                bindings = tarr;
            }
            for (int i = 0; i < nBindings; i++) {
                bindings[i].importInfo(r.bindings[i]);
            }
        }
    }

    void addAnnToBindings(ScapeBindingSet bindingSet, Annotation ann) {
        if (bindingSet == null)
            return;
        for (int n = 0; n < bindingSet.size; n++) {
            ScapeDFSMBinding binding = (ScapeDFSMBinding) bindingSet.bindings[n];
            for (int i = 0; i < nBindings; i++) {
                BindingAndLinkedAnnsItem balt = bindings[i];
                if (balt.binding == binding) {
                    balt.item = LinkedAnns.addItem(ann, balt.item);
                    balt.start = ann.getStartNode().getOffset() < balt.start.getOffset() ? ann.getStartNode() : balt.start;
                    balt.end = ann.getEndNode().getOffset() > balt.end.getOffset() ? ann.getEndNode() : balt.end;
                    balt.size++;
                    return;
                }
            }
            if (nBindings == bindings.length) {
                BindingAndLinkedAnnsItem[] tarr = new BindingAndLinkedAnnsItem[bindings.length * 3 / 2];
                int i;
                for (i = 0; i < nBindings; i++) {
                    tarr[i] = bindings[i];
                }
                for (; i < tarr.length; i++) {
                    tarr[i] = new BindingAndLinkedAnnsItem();
                }
                bindings = tarr;
            }
            BindingAndLinkedAnnsItem balt = bindings[nBindings++];
            balt.start = ann.getStartNode();
            balt.end = ann.getEndNode();
            balt.size = 1;
            balt.binding = binding;
            balt.item = LinkedAnns.addItem(ann, null);
        }
    }

    public Iterator matchedAnnsIterator() {
        return LinkedAnns.newAnnsIterator(lastMatched);
    }

    public Node getStartNode() {
        return start;
    }

    public Node getEndNode() {
        return end;
    }

    public LinkedAnns.Item getLastMatched() {
        return lastMatched;
    }

    public int size() {
        return size;
    }

    public ScapeRuleSet getRules() {
        return ((ScapeDFSMStateFinal) currentState).ruleSet;
    }
}
