/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.TreenotationImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;

public class PhonemeLabelGenerator
        implements TrnLabelGenerator {

    boolean stringInСaption;

    public PhonemeLabelGenerator() {
        this(true);
    }

    public PhonemeLabelGenerator(boolean stringInCaption) {
        this.stringInСaption = stringInCaption;
    }


    public String generateCaption(Treenotation trn) {
        if (TrnLabelGenerator.DEBUG) {
            return _generateCaption(trn)+"[nView: "+((TreenotationImpl)trn).getNView()+" id: "+trn.getId()+"]";
        } else {
            return _generateCaption(trn);
        }
    }

    public String _generateCaption(Treenotation trn) {
        return trn.get("notation").toString();
    }

    public void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void init(TreenotationsContext context) {
    }

    public String generateLabel(Treenotation trn) {
        return "";
    }
}
