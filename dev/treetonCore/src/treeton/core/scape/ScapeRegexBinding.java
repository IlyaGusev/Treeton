/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

import treeton.core.fsm.logicset.LogicFSM;

public class ScapeRegexBinding implements RegexpVariable {
    String name;
    LogicFSM fsm;
    String s;

    public ScapeRegexBinding(String name) {
        this.name = name;
    }

    public void matchBindings() {
        fsm.matchBindings(s);
    }

    public boolean next() {
        return fsm.next();
    }

    public String getBindingValue(int n) {
        return fsm.getBinding(n);
    }

    public void setFSM(LogicFSM logicFSM) {
        fsm = logicFSM;
    }

    public void setString(String s) {
        this.s = s;
    }

    public LogicFSM getFsm() {
        return fsm;
    }

    public LogicFSM setFsm(LogicFSM fsm) {
        return this.fsm = fsm;
    }
}
