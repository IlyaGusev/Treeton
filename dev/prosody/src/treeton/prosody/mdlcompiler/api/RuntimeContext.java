/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api;

import treeton.prosody.mdlcompiler.api.custom.Output;
import treeton.prosody.mdlcompiler.api.fsm.Binding;
import treeton.prosody.mdlcompiler.api.fsm.BindingUser;

import java.util.List;

public interface RuntimeContext<T> {
    List<T> getBindedObjects(Binding binding, BindingUser bounding);
    Output<T> getOutput();
    AllBindingsProvider<T> getAllBindingsProvider();
}
