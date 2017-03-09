/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.Treenotation;
import treeton.core.fsm.TreenotationTerm;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.NumeratedObject;

import java.awt.*;
import java.util.Iterator;

public class ScapeTreenotationTerm extends ScapeTerm implements TreenotationTerm {
    public static final ScapeTreenotationTerm nullTerm = new ScapeTreenotationTerm(null, null);
    public static Font font = new Font("Courier", 0, 12);
    Treenotation trn;
    ScapeRule rule;

    public ScapeTreenotationTerm(Treenotation _trn, ScapeRule _rule) {
        trn = _trn;
        rule = _rule;
    }

    public String getString() {
        try {
            StringBuffer buf = new StringBuffer();

            if (trn == null) {
                buf.append("null");
                return buf.toString();
            }

            TrnType tp = trn.getType();
            buf.append(tp.getName().toString());
            buf.append(": ");

            Iterator it = trn.numeratedObjectIterator();
            while (it.hasNext()) {
                NumeratedObject no = (NumeratedObject) it.next();
                buf.append(tp.getFeatureNameByIndex(no.n).toString());
                buf.append("=");
                buf.append(no.o != null ? no.o.toString() : "null");
                if (it.hasNext())
                    buf.append(", ");
            }
            return buf.toString();
        } catch (TreetonModelException e) {
            return "Problem with model!!!";
        }
    }

    public Treenotation getTreenotation() {
        return trn;
    }
}
