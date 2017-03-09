/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.morph;

import treeton.core.TString;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.dict.Dictionary;
import treeton.res.tokeniser.SimpleTokeniser;
import treeton.scape.ScapeOutputItem;
import treeton.scape.ScapePhase;
import treeton.scape.ScapeRuleIterator;

import java.io.IOException;
import java.util.*;

public class PredMorphPhaseBuilder {
    TreenotationsContext context;
    SimpleTokeniser tokeniser;
    Map<TrnType, Set<Integer>> filter;
    TString searcher = TreetonFactory.newTString(new char[70]);

    public PredMorphPhaseBuilder(TreenotationsContext context, SimpleTokeniser tokeniser, Map<TrnType, Set<Integer>> filter) {
        this.context = context;
        this.tokeniser = tokeniser;
        this.filter = filter;
    }

    public ScapePhase buildPredMorphPhase(Dictionary dict) throws IOException, TreetonModelException {
        HashMap<TString, char[]> templates = new HashMap<TString, char[]>();
        StringBuffer tbuf = new StringBuffer();

        Iterator<String> iterator = dict.lemmaIterator();
        while (iterator.hasNext()) {
            String lemma = iterator.next();

            tbuf.setLength(0);
            tokeniser.tokeniseIntoSBuffer(lemma, tbuf, filter);
            searcher.slurp(tbuf);
            char[] tmpl = templates.get(searcher);
            if (tmpl == null) {
                TString tstr = TreetonFactory.newTString(tbuf.toString());
                templates.put(tstr, tstr.toCharArray());
            }
        }

        Iterator it = templates.values().iterator();

        ScapePhase predMorphPhase;
        if (!it.hasNext()) {
            return null;
        }

        ArrayList<Treenotation> tarr = new ArrayList<Treenotation>();
        ArrayList<Treenotation[]> newTemplates = new ArrayList<Treenotation[]>();

        String dot = ".";
        tbuf.setLength(0);
        tokeniser.tokeniseIntoSBuffer(dot, tbuf, filter);
        char[] dotTemplate = tbuf.toString().toCharArray();


        while (it.hasNext()) {
            char[] tmpl = (char[]) it.next();
            int pl = 0;
            tarr.clear();
            while (pl < tmpl.length) {
                Treenotation trn = TreetonFactory.newTreenotation(null, null, (TrnType) null);
                pl = trn.readInFromStringView(context.getTypes(), tmpl, pl);
                tarr.add(trn);
            }
            newTemplates.add(tarr.toArray(new Treenotation[tarr.size()]));
            for (int i = 0; i < tarr.size(); i++) {
                Treenotation treenotation = tarr.get(i);
                tarr.set(i, (Treenotation) treenotation.clone());
            }

            Treenotation dotTrn = TreetonFactory.newTreenotation(null, null, (TrnType) null);
            dotTrn.readInFromStringView(context.getTypes(), dotTemplate, 0);

            tarr.add(dotTrn);
            newTemplates.add(tarr.toArray(new Treenotation[tarr.size()]));
        }

        predMorphPhase = new ScapePhase(context.getTypes());
        predMorphPhase.build(new ScapeRuleIterator(newTemplates), "PredMorph",
                ScapePhase.CONTROL_TYPE_APPELT,
                context.getTypes().getAllTypes(),
                new ScapeOutputItem[]{}
        );
        predMorphPhase.initialize();

        return predMorphPhase;
    }
}
