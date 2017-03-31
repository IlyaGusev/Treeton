/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.fsm;

import treeton.prosody.mdlcompiler.api.custom.Filter;

import java.util.List;

public interface Binded extends Comparable<Binded> {
    List<Binding> getBindings();
    List<BindingReset> getBindingsToReset();
    List<Filter> getActualFilters();
}
