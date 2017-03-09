/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

public class BinaryRelation {
    ScapeVariable subject;
    ScapeVariable object;

    ScapeExpressionFunction function;
    ScapeExpression.OperatorType opType;

    public ScapeVariable getSubject() {
        return subject;
    }

    public ScapeVariable getObject() {
        return object;
    }

    public ScapeExpressionFunction getFunction() {
        return function;
    }

    public ScapeExpression.OperatorType getOpType() {
        return opType;
    }
}
