/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;
import treeton.core.TypeIteratorInterface;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;

import java.util.Arrays;

public class MorphRuStorageLabelGenerator implements TrnStorageLabelGenerator {
    TrnType mtp;
    GrammLabelGenerator generator;
    StringBuffer buf = new StringBuffer();
    boolean[] arr = new boolean[10];

    public MorphRuStorageLabelGenerator() {
    }

    public void init(TreenotationsContext context) {
        try {
            mtp = context.getType("Morph");
        } catch (TreetonModelException e) {
            mtp = null;
        }
        generator = new GrammLabelGenerator(true, true, false, false);
    }

    public String generateLabel(TreenotationStorage storage) {
        TypeIteratorInterface it = storage.typeIterator(mtp);
        int s = storage.firstToken().getStartNumerator();
        int e = storage.lastToken().getEndNumerator();
        int len = e - s;
        if (arr.length < len) {
            arr = new boolean[len];
        } else {
            Arrays.fill(arr, 0, len, false);
        }
        buf.setLength(0);
        while (it.hasNext()) {
            Treenotation trn = (Treenotation) it.next();
            s = trn.getStartToken().getStartNumerator();
            e = trn.getEndToken().getEndNumerator();
            for (; s < e; s++) {
                arr[s] = true;
            }
            buf.append(generator.generateCaption(trn));
            buf.append(" ");
            buf.append(generator.generateLabel(trn));
            if (it.hasNext()) {
                buf.append("| ");
            }
        }
        int ntrue = 0;
        for (int i = 0; i < len; i++) {
            if (arr[i])
                ntrue++;
        }
        String p = Integer.toString((int) ((((double) ntrue) / len) * 100));
        if (p.length() == 1) {
            p = "00" + p;
        } else if (p.length() == 2) {
            p = "0" + p;
        }
        return p + "% " + buf.toString();
    }
}
