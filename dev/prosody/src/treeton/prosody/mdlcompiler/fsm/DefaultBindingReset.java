/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.prosody.mdlcompiler.api.fsm.Binding;
import treeton.prosody.mdlcompiler.api.fsm.BindingReset;

public class DefaultBindingReset implements BindingReset {
    private Binding binding;
    private int index;

    public DefaultBindingReset(Binding binding) {
        this.binding = binding;
    }

    public Binding getBinding() {
        return binding;
    }

    public int compareTo(BindingReset o) {
        return index - ((DefaultBindingReset) o).index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
