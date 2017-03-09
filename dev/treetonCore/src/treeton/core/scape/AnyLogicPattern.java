/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

public class AnyLogicPattern extends ScapeLogicPattern {
    private ScapeVariable bindedVariable;
    private String bindedFeatureName;

    public ScapeVariable getBindedVariable() {
        return bindedVariable;
    }

    void setBindedVariable(ScapeVariable bindedVariable) {
        this.bindedVariable = bindedVariable;
    }

    public String getBindedFeatureName() {
        return bindedFeatureName;
    }

    void setBindedFeatureName(String bindedFeatureName) {
        this.bindedFeatureName = bindedFeatureName;
    }
}
