/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

public abstract class ScapeRHSAction {
    ScapeRule rule;

    protected ScapeRHSAction(ScapeRule rule) {
        this.rule = rule;
    }

    public abstract ScapeRHSActionResult buildResult();
}
