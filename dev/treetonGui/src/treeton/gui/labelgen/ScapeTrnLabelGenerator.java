/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.Treenotation;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.scape.ParseException;
import treeton.core.scape.ScapeExpression;

public class ScapeTrnLabelGenerator implements TrnLabelGenerator {
    private String stringCaptionExpression;
    private ScapeExpression scapeCaptionExpression;
    private String stringBodyExpression;
    private ScapeExpression scapeBodyExpression;

    public ScapeTrnLabelGenerator(String captionExpression, String bodyExpression) {
        this.stringCaptionExpression = captionExpression + ";";
        this.stringBodyExpression = bodyExpression + ";";
    }

    public void init(TreenotationsContext context) {
        scapeCaptionExpression = new ScapeExpression();
        char[] arr = stringCaptionExpression.toCharArray();
        char[][] delims = new char[][]{";".toCharArray(), ",".toCharArray()};
        try {
            scapeCaptionExpression.readIn(arr, 0, arr.length - 1, delims);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        scapeBodyExpression = new ScapeExpression();
        arr = stringBodyExpression.toCharArray();
        delims = new char[][]{";".toCharArray(), ",".toCharArray()};
        try {
            scapeBodyExpression.readIn(arr, 0, arr.length - 1, delims);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateLabel(Treenotation trn) {
        Object o = scapeBodyExpression.evaluate();
        return o instanceof String ? (String) o : "ClassCastError!!!";
    }

    public String generateCaption(Treenotation trn) {
        Object o = scapeCaptionExpression.evaluate();
        return o instanceof String ? (String) o : "ClassCastError!!!";
    }
}
