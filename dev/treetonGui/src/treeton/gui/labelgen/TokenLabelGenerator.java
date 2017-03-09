/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.TreenotationImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;

public class TokenLabelGenerator
        implements TrnLabelGenerator {

    boolean stringInСaption;
    boolean html;

    public TokenLabelGenerator() {
        this(true, false);
    }

    public TokenLabelGenerator(boolean stringInCaption, boolean html) {
        this.stringInСaption = stringInCaption;
        this.html = html;
    }


    public String generateCaption(Treenotation trn) {
        if (TrnLabelGenerator.DEBUG) {
            return _generateCaption(trn) + "[nView: " + ((TreenotationImpl) trn).getNView() + " id: " + trn.getId() + "]";
        } else {
            return _generateCaption(trn);
        }
    }

    public String _generateCaption(Treenotation trn) {
        return trn.getText();
    }

    public void init(TreenotationsContext context) {
    }

    public String generateLabel(Treenotation trn) {
        Object o1;
        if ((o1 = trn.get("orthm")) != null)
            return "<" + o1.toString() + ">";
        return "";
    }
}
