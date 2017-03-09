/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.Treenotation;

public abstract class ScapeRHSJavaWrapper {
    private ScapeRHSComplexActionResult res;

    protected abstract void execute() throws Exception;

    void reset() {
        res = new ScapeRHSComplexActionResult();
    }

    void deinit() {
        res = null;
    }

    protected void Add(Treenotation t) {
        res.add(new ScapeRHSCreateActionResult(t));
    }

    public ScapeRHSComplexActionResult getResult() {
        return res;
    }

}
