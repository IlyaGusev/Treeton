/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogicPatternBindingResultSet {
    BinaryLogicPattern pattern;
    List<ScapeExpression.Node> nodes;
    int i = 0;
    private Map<AnyLogicPattern, Object> bindings = new HashMap<AnyLogicPattern, Object>();

    public LogicPatternBindingResultSet(BinaryLogicPattern pattern, List<ScapeExpression.Node> nodes) {
        this.nodes = nodes;
        this.pattern = pattern;
    }

    public boolean next() {
        while (i < nodes.size()) {
            bindings.clear();
            if (match(pattern, nodes.get(i++)))
                return true;
        }
        return false;
    }

    private boolean match(ScapeLogicPattern pattern, ScapeExpression.Node node) {
        if (pattern instanceof BinaryLogicPattern) {
            BinaryLogicPattern bpattern = (BinaryLogicPattern) pattern;
            if (node.opType == bpattern.getOperator()) {
                return match(bpattern.getSubject(), node.op1) && match(bpattern.getObject(), node.op2);
            }
        } else if (pattern instanceof AnyLogicPattern) {
            //if (node)
        }

        return false;  //TODO
    }

    public ScapeVariable getBindedVariable(AnyLogicPattern pattern) {
        Object o = bindings.get(pattern);
        if (o instanceof ScapeVariable) {
            return (ScapeVariable) o;
        }

        return null;
    }

    public String getBindedFeatureName(AnyLogicPattern pattern) {
        Object o = bindings.get(pattern);
        if (o instanceof String) {
            return (String) o;
        }

        return null;
    }
}
