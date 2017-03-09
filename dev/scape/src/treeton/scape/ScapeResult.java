/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.scape.ScapeRegexBinding;
import treeton.core.util.LinkedTrns;

import java.util.Iterator;

public class ScapeResult {
    LinkedTrns.Item lastMatched;
    ScapeDFSMState currentState;
    BindingAndLinkedTrnsItem[] bindings;
    ScapePhase phase;
    int nBindings;
    ScapeRegexBinding[] regexBindings;
    int nRegexBindings;

    int size;
    Token start;
    Token end;
    private boolean firstTime;

    ScapeResult(ScapePhase phase) {
        this.phase = phase;
        lastMatched = null;
        currentState = null;
        size = 0;
        start = null;
        end = null;
        nBindings = 0;
        bindings = new BindingAndLinkedTrnsItem[5];
        for (int i = 0; i < bindings.length; i++) {
            bindings[i] = new BindingAndLinkedTrnsItem();
        }
        regexBindings = new ScapeRegexBinding[5];
        nRegexBindings = 0;
    }

    public void activateBindings() {
        phase.allBinding.attach(this);
        for (int i = 0; i < nBindings; i++) {
            BindingAndLinkedTrnsItem balt = bindings[i];
            balt.activateBinding();
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
                    balt.binding.regexBinding.setFsm(balt.binding.regexFSM);
                    Object val = balt.binding.getValue(balt.binding.regexFeature);
                    balt.binding.regexBinding.setString(val == null ? null : val.toString());
                    balt.binding.regexBinding.matchBindings();
                }
            }
        }
        firstTime = true;
    }

    public boolean nextCombination() {
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
                        regexBindings[i].getFsm().resetMatch();
                        regexBindings[i].getFsm().next();
                    }
                    return true;
                }
            }
            return false;
        }
    }

    public void deactivateBindings() {
        for (int i = 0; i < nBindings; i++) {
            BindingAndLinkedTrnsItem balt = bindings[i];
            balt.deactivateBinding();
        }
        for (int j = 0; j < nRegexBindings; j++) {
            ScapeRegexBinding binding = regexBindings[j];
            binding.getFsm().forgetMatch();
        }
        phase.allBinding.detach();
    }

    public void importBindings(ScapeResult r) {
        nBindings = r.nBindings;
        if (nBindings > 0) {
            if (bindings.length < nBindings) {
                BindingAndLinkedTrnsItem[] tarr = new BindingAndLinkedTrnsItem[nBindings];
                int i;
                for (i = 0; i < bindings.length; i++) {
                    tarr[i] = bindings[i];
                }
                for (; i < tarr.length; i++) {
                    tarr[i] = new BindingAndLinkedTrnsItem();
                }
                bindings = tarr;
            }
            for (int i = 0; i < nBindings; i++) {
                bindings[i].importInfo(r.bindings[i]);
            }
        }
    }

    void addTrnToBindings(ScapeBindingSet bindingSet, Treenotation trn) {
        if (bindingSet == null)
            return;
        for (int n = 0; n < bindingSet.size; n++) {
            ScapeDFSMBinding binding = (ScapeDFSMBinding) bindingSet.bindings[n];
            for (int i = 0; i < nBindings; i++) {
                BindingAndLinkedTrnsItem balt = bindings[i];
                if (balt.binding == binding) {
                    balt.item = LinkedTrns.addItem(trn, balt.item);
                    balt.start = trn.getStartToken().compareTo(balt.start) < 0 ? trn.getStartToken() : balt.start;
                    balt.end = trn.getEndToken().compareTo(balt.end) > 0 ? trn.getEndToken() : balt.end;
                    balt.size++;
                    return;
                }
            }
            if (nBindings == bindings.length) {
                BindingAndLinkedTrnsItem[] tarr = new BindingAndLinkedTrnsItem[bindings.length * 3 / 2];
                int i;
                for (i = 0; i < nBindings; i++) {
                    tarr[i] = bindings[i];
                }
                for (; i < tarr.length; i++) {
                    tarr[i] = new BindingAndLinkedTrnsItem();
                }
                bindings = tarr;
            }
            BindingAndLinkedTrnsItem balt = bindings[nBindings++];
            balt.start = trn.getStartToken();
            balt.end = trn.getEndToken();
            balt.size = 1;
            balt.binding = binding;
            balt.item = LinkedTrns.addItem(trn, null);
        }
    }


    public Iterator matchedTrnsIterator() {
        return LinkedTrns.newTrnsIterator(lastMatched);
    }

    public Token getStartToken() {
        return start;
    }

    public Token getEndToken() {
        return end;
    }

    public LinkedTrns.Item getLastMatched() {
        return lastMatched;
    }

    public int size() {
        return size;
    }

    public ScapeRuleSet getRules() {
        return ((ScapeDFSMStateFinal) currentState).ruleSet;
    }
}
