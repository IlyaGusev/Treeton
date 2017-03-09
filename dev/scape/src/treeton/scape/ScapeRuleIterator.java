/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.Treenotation;
import treeton.core.scape.TrnTemplate;

import java.util.ArrayList;
import java.util.Iterator;

public class ScapeRuleIterator implements Iterator<ScapeRule> {
    Iterator it;
    Iterator itObjects;
    int n;
    boolean old;

    public ScapeRuleIterator(ArrayList templates) {
        old = true;
        it = templates.iterator();
        n = 0;
    }

    public ScapeRuleIterator(Iterator<? extends Iterable<TrnTemplate>> templates, Iterator mappedObjects) {
        old = false;
        it = templates;
        itObjects = mappedObjects;
        n = 0;
    }

    public void remove() {
    }

    public boolean hasNext() {
        return it.hasNext();
    }

    public ScapeRule next() {
        if (old) {
            Treenotation[] treenotations = (Treenotation[]) it.next();
            ScapeFSM fsm = new ScapeFSM();
            ScapeFSMState prev = fsm.addStartState();

            for (Treenotation trn : treenotations) {
                ScapeFSMState next = fsm.addState();
                prev.put(new ScapeTreenotationTerm(trn, null), next);
                prev = next;
            }
            prev.finalizeState();

            ScapeRule r = new ScapeRule();
            r.build(Integer.toString(n++), fsm);
            return r;
        } else {
            Iterable<TrnTemplate> templates = (Iterable<TrnTemplate>) it.next();
            ScapeFSM fsm = new ScapeFSM();
            ScapeFSMState prev = fsm.addStartState();

            for (TrnTemplate templ : templates) {
                ScapeFSMState next = fsm.addState();
                for (Treenotation trn : templ.trns) {
                    prev.put(new ScapeTreenotationTerm(trn, null), next);
                }
                prev = next;
            }
            prev.finalizeState();

            ScapeRule r = new ScapeRule();
            r.build(Integer.toString(n++), fsm);
            if (itObjects != null) {
                ArrayList<ScapeRHSAction> arr = new ArrayList<ScapeRHSAction>();
                arr.add(new ScapeRHSObject(r, itObjects.next()));
                r.rhs = arr;
            }
            return r;
        }
    }
}
