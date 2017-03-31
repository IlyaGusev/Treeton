/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.fsm;

import java.util.Set;
import java.util.List;

public interface BindingUser {
    Set<Binding> getUsedBindings();
    List<BindingReset> getActualBindingResets(Binding b);
    void addActualBindingReset(BindingReset bindingReset);
}
