/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

import treeton.prosody.mdlcompiler.api.fsm.BindingUser;
import treeton.prosody.mdlcompiler.api.RuntimeContext;

import java.util.Set;

public interface Filter<T> extends BindingUser, Comparable<Filter<T>> {
    boolean accept(RuntimeContext<T> context, InputObjectInfoProvider<T> inputObjectInfoProvider);

    Set<Action<T>> getDependentActions();
    void addDependentAction(Action<T> action);

    void optimize() throws Exception;
}
