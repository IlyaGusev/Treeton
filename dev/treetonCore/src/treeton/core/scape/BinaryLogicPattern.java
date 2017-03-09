/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

public class BinaryLogicPattern extends ScapeLogicPattern {
    ScapeExpression.OperatorType operator;
    ScapeLogicPattern subject;
    ScapeLogicPattern object;

    public BinaryLogicPattern(ScapeExpression.OperatorType operator, ScapeLogicPattern subject, ScapeLogicPattern object) {
        assert operator != ScapeExpression.LT_AND;

        this.operator = operator;
        this.subject = subject;
        this.object = object;

    }

    public ScapeExpression.OperatorType getOperator() {
        return operator;
    }

    public ScapeLogicPattern getSubject() {
        return subject;
    }

    public ScapeLogicPattern getObject() {
        return object;
    }
}
