/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.prosody.mdlcompiler.api.custom.Filter;
import treeton.prosody.mdlcompiler.api.fsm.Binded;
import treeton.prosody.mdlcompiler.api.fsm.Binding;
import treeton.prosody.mdlcompiler.api.fsm.BindingReset;

import java.util.*;

public class DefaultBinded implements Binded {
    private List<Binding> bindings = null;
    private List<Filter> actualFilters = null;
    private List<BindingReset> bindingResets = null;

    public DefaultBinded() {
    }

    public DefaultBinded(Binded binded) {
        if (binded == null)
            return;
        
        List<Filter> filters = binded.getActualFilters();
        if (filters != null) {
            this.actualFilters = new ArrayList<Filter>();
            cleanAndAddSorted(actualFilters,filters);
        }
        List<Binding> bindings = binded.getBindings();
        if (bindings != null) {
            this.bindings = new ArrayList<Binding>();
            cleanAndAddSorted(this.bindings,bindings);
        }
        List<BindingReset> resets = binded.getBindingsToReset();
        if (resets != null) {
            this.bindingResets = new ArrayList<BindingReset>();
            cleanAndAddSorted(this.bindingResets,resets);
        }
    }

    public List<Binding> getBindings() {
        return bindings;
    }

    public List<Filter> getActualFilters() {
        return actualFilters;
    }

    public List<BindingReset> getBindingsToReset() {
        return bindingResets;
    }

    /*public void createBitMask() {
        if (bitMask == null) {
            int sz=-1;
            for (Binding b : bindings) {
                if (b.getIndex() > sz) {
                    sz = b.getIndex();
                }
            }
            sz++;
            sz = sz % Integer.SIZE > 0 ? (sz / Integer.SIZE) + 1 : sz / Integer.SIZE;

            bitMask = new int[sz];

            for (Binding b : bindings) {
                int idx = b.getIndex();
                int i = idx / Integer.SIZE;
                int shift = idx % Integer.SIZE;
                bitMask[i] |= 1 << shift;
            }
        }
    } */

    public void addBinding(Binding binding) {
        if (bindings == null)
            bindings = new ArrayList<Binding>();

        addSorted(bindings, binding);
    }

    public void addActualFilter(Filter filter) {
        if (actualFilters == null)
            actualFilters = new ArrayList<Filter>();
        addSorted(actualFilters, filter);
    }

    public void addBindingReset(BindingReset reset) {
        if (bindingResets == null)
            bindingResets = new ArrayList<BindingReset>();
        addSorted(bindingResets, reset);
    }

    public static <T extends Comparable<T>>void cleanAndAddSorted(List<T> objects, Collection<T> objs) {
        objects.clear();
        objects.addAll(objs);
        Collections.sort(objects);
    }

    public static <T extends Comparable<T>>void addSorted(List<T> objects, T obj) {
        int i=0;

        for (;i<objects.size();i++) {
            T o = objects.get(i);
            if (o.equals(obj))
                return;

            if (o.compareTo(obj) > 0) {
                break;
            }
        }
        objects.add(null);
        for (int j=objects.size()-1;j>i;j--) {
            objects.set(j,objects.get(j-1));
        }
        objects.set(i,obj);
    }


    public int compareTo(Binded other) {
        Iterator<Binding> it = other.getBindings() == null ? null : other.getBindings().iterator();
        int i=0;
        for (;bindings != null && it != null && i<bindings.size()&&it.hasNext();i++) {
            Binding b1 = bindings.get(i);
            Binding b2 = it.next();

            int c = b1.compareTo(b2);
            if (c < 0) {
                return -1;
            } else if (c > 0) {
                return 1;
            }
        }

        if (i==(bindings == null ? 0 : bindings.size()) && (it == null || !it.hasNext())) {
        } else if (it == null || !it.hasNext()) {
            return -1;
        } else {
            return 1;
        }
        Iterator<Filter> it1 = other.getActualFilters() == null ? null : other.getActualFilters().iterator();

        i=0;
        for (;actualFilters != null && it1 != null && i<actualFilters.size()&&it1.hasNext();i++) {
            Filter f1 = actualFilters.get(i);
            Filter f2 = it1.next();

            //noinspection unchecked
            int c = f1.compareTo(f2);
            if (c < 0) {
                return -1;
            } else if (c > 0) {
                return 1;
            }
        }

        if (i==(actualFilters == null ? 0 : actualFilters.size()) && (it1 == null || !it1.hasNext())) {
        } else if (it1 == null || !it1.hasNext()) {
            return -1;
        } else {
            return 1;
        }
        Iterator<BindingReset> it2 = other.getBindingsToReset() == null ? null : other.getBindingsToReset().iterator();
        i=0;
        for (;bindingResets != null && it2 != null && i < bindingResets.size()&&it2.hasNext();i++) {
            BindingReset br1 = bindingResets.get(i);
            BindingReset br2 = it2.next();

            int c = br1.compareTo(br2);
            if (c < 0) {
                return -1;
            } else if (c > 0) {
                return 1;
            }
        }

        if (i==(bindingResets == null ? 0 : bindingResets.size()) && (it2 == null || !it2.hasNext())) {
        } else if (it2 == null || !it2.hasNext()) {
            return -1;
        } else {
            return 1;
        }
        return 0;
    }

    private <V>boolean fullEquals(List<V> list1, List<V> list2) {
        if (list2 == null)
            return false;
        if (list1.size()!=list2.size())
            return false;
        for (V v : list1) {
            if (!list2.contains(v))
                return false;
        }
        return true;
    }

    @SuppressWarnings({"RedundantIfStatement", "EqualsWhichDoesntCheckParameterClass"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null)
            return false;

        DefaultBinded that = (DefaultBinded) o;

        if (actualFilters != null ? !fullEquals(actualFilters,that.actualFilters) : that.actualFilters != null)
            return false;
        if (bindingResets != null ? !fullEquals(bindingResets,that.bindingResets) : that.bindingResets != null)
            return false;
        if (bindings != null ? !fullEquals(bindings,that.bindings) : that.bindings != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = bindings != null ? bindings.hashCode() : 0;
        result += (actualFilters != null ? actualFilters.hashCode() : 0);
        result += (bindingResets != null ? bindingResets.hashCode() : 0);
        return result;
    }
}
