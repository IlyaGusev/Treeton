/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.prosody.mdlcompiler.api.custom.Action;
import treeton.prosody.mdlcompiler.api.custom.Filter;
import treeton.prosody.mdlcompiler.api.custom.Term;
import treeton.prosody.mdlcompiler.api.fsm.*;
import org.apache.log4j.Logger;
import treeton.core.util.ObjectPair;
import treeton.prosody.mdlcompiler.grammar.ast.SyllBasicPatternElement;
import treeton.prosody.mdlcompiler.grammar.ast.SyllConstraint;
import treeton.prosody.mdlcompiler.grammar.ast.SyllConstraintList;
import treeton.prosody.mdlcompiler.grammar.ast.SyllConstraintOr;
import treeton.prosody.mdlcompiler.grammar.TreevialSymbol;
import treeton.prosody.mdlcompiler.grammar.ast.BaseNode;
import treeton.prosody.mdlcompiler.grammar.ast.Visitor;

import java.util.*;

public class MdlFSMBuilder<T> {
    private static final Logger logger = Logger.getLogger(MdlFSMBuilder.class);

    private MdlTermBuilder termBuilder;

    public MdlFSMBuilder() {
        termBuilder = new MdlTermBuilder();
    }

    private class Trio {
        private Trio(Set<BindingReset> resets, Set<Filter> filters, TermStatePair<T> pair) {
            this.resets = resets;
            this.filters = filters;
            this.pair = pair;
        }

        Set<BindingReset> resets;
        Set<Filter> filters;
        TermStatePair<T> pair;

        @SuppressWarnings({"unchecked"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Trio trio = (Trio) o;

            return filters.equals(trio.filters) && pair.equals(trio.pair) && resets.equals(trio.resets);
        }

        @Override
        public int hashCode() {
            int result = resets.hashCode();
            result = 31 * result + filters.hashCode();
            result = 31 * result + pair.hashCode();
            return result;
        }
    }

    void collectTrio(State<T> state, HashSet<State<T>> passed, Set<BindingReset> bindingResets, Set<Filter> actualFilters, List<Trio> uniqueTrios) {
        if (passed.contains(state))
            return;

        passed.add(state);

        for (Action<T> action : state.getActions()) {
            for (Filter filter : actualFilters) {
                //noinspection unchecked
                filter.addDependentAction(action);
            }
        }

        for (TermStatePair<T> pair : state.getTransitions()) {
            Set<Filter> addedFilters = new HashSet<Filter>();
            Set<BindingReset> addedResets = new HashSet<BindingReset>();


            List<Filter> fs = pair.getActualFilters();
            if (fs != null) {
                for (Filter f : fs) {
                    if (actualFilters.add(f))
                        addedFilters.add(f);
                }
            }

            List<BindingReset> brs = pair.getBindingsToReset();

            if (brs != null) {
                for (BindingReset br : brs) {
                    if(bindingResets.add(br))
                        addedResets.add(br);
                }
            }

            if (pair.getTerm()==null) {
                collectTrio(pair.getState(),passed,bindingResets,actualFilters,uniqueTrios);
            } else {
                if (!uniqueTrios.contains(new Trio(bindingResets,actualFilters,pair))) {
                    uniqueTrios.add(new Trio(new HashSet<BindingReset>(bindingResets), new HashSet<Filter>(actualFilters), pair));
                }
            }

            for (Filter f : addedFilters) {
                actualFilters.remove(f);
            }
            for (BindingReset br : addedResets) {
                bindingResets.remove(br);
            }

        }
        passed.remove(state);
    }

    public void removeEmptyTransitions(DefaultState<T> state) {
        state.visit(
                new StateVisitor<T>() {
                    public void execute(State<T> state) {

                        DefaultState<T> dst = (DefaultState<T>) state;

                        List<Trio> trios = new ArrayList<Trio>();
                        collectTrio(dst,new HashSet<State<T>>(), new HashSet<BindingReset>(), new HashSet<Filter>(), trios);
                        Set<State<T>> eclosure = new HashSet<State<T>>();
                        fillEClosure(dst,eclosure);

                        Iterator<TermStatePair<T>> it = dst.getTransitions().iterator();
                        while (it.hasNext()) {
                            TermStatePair<T> p = it.next();

                            if (p.getTerm() == null) {
                                it.remove();
                            }
                        }

                        for (Trio trio : trios) {
                            DefaultTermStatePair<T> newpair = new DefaultTermStatePair<T>(trio.pair.getTerm(),trio.pair.getState());
                            dst.addTransition(newpair);

                            List<Filter> af = trio.pair.getActualFilters();
                            if (af != null) {
                                for (Filter filter : af) {
                                    newpair.addActualFilter(filter);
                                }
                            }

                            List<Binding> bs = trio.pair.getBindings();
                            if (bs != null) {
                                for (Binding binding : bs) {
                                    newpair.addBinding(binding);
                                }
                            }

                            List<BindingReset> brs = trio.pair.getBindingsToReset();
                            if (brs != null) {
                                for (BindingReset reset : brs) {
                                    newpair.addBindingReset(reset);
                                }
                            }

                            for (Filter filter : trio.filters) {
                                newpair.addActualFilter(filter);
                            }

                            for (BindingReset reset : trio.resets) {
                                newpair.addBindingReset(reset);
                            }
                        }

                        for (State<T> state1 : eclosure) {
                            if (dst == state1)
                                continue;

                            dst.setFinal(dst.isFinal() || state1.isFinal());
                            for (Filter<T> filter : state1.getFilters()) {
                                dst.addFilter(filter);
                            }
                            for (Action<T> action : state1.getActions()) {
                                dst.addAction(action);
                            }
                        }
                    }

                    private void fillEClosure(State<T> state, Set<State<T>> eclosure) {
                        if (eclosure.contains(state)) {
                            return;
                        }

                        eclosure.add(state);
                        for (TermStatePair<T> pair : state.getTransitions()) {
                            if (pair.getTerm() == null) {
                                fillEClosure(pair.getState(),eclosure);
                            }
                        }
                    }
                },
                false,new HashSet<State<T>>()
        );

        state.visitExtended(new ExtendedTermStatePairVisitor<T, Object>() {
            public Object execute(TermStatePair<T> pair, Object straightRecursionObject) {
                ((DefaultTermStatePair<T>)pair).setHashable(false);
                return straightRecursionObject;
            }
        }, new HashSet<State<T>>(), true);
    }


    public ObjectPair<DefaultState<T>,DefaultState<T>> build(SyllConstraintOr constraintOr, final boolean reverseDirection) {
        final Map<BaseNode, ObjectPair<DefaultState<T>,DefaultState<T>>> map = new HashMap<BaseNode, ObjectPair<DefaultState<T>, DefaultState<T>>>();

        constraintOr.visit(
                new Visitor() {
                    public void execute(BaseNode node) {
                        DefaultState<T> first = new DefaultState<T>();
                        DefaultState<T> last = new DefaultState<T>();
                        if (node instanceof SyllConstraintOr) {
                            SyllConstraintOr cor = (SyllConstraintOr) node;

                            Iterable<SyllConstraintList> iterable;

                            if (!reverseDirection) {
                                iterable = cor;
                            } else {
                                Vector<SyllConstraintList> vect = new Vector<SyllConstraintList>();
                                vect.setSize(cor.size());

                                int i=vect.size()-1;
                                for (SyllConstraintList list : cor) {
                                    vect.setElementAt(list,i--);
                                }

                                iterable = vect;
                            }

                            for (SyllConstraintList list : iterable) {
                                ObjectPair<DefaultState<T>, DefaultState<T>> pair = map.get(list);
                                first.addTransition(new DefaultTermStatePair<T>(null,pair.getFirst()));
                                DefaultTermStatePair<T> eps = new DefaultTermStatePair<T>(null, last);
                                pair.getSecond().addTransition(eps);
                                for (Filter<T> filter : pair.getSecond().getFilters()) {
                                    eps.addActualFilter(filter);
                                }
                            }
                        } else if (node instanceof SyllConstraintList) {
                            SyllConstraintList clist = (SyllConstraintList) node;

                            Iterable<SyllConstraint> iterable;

                            if (!reverseDirection) {
                                iterable = clist;
                            } else {
                                Vector<SyllConstraint> vect = new Vector<SyllConstraint>();
                                vect.setSize(clist.size());

                                int i=vect.size()-1;
                                for (SyllConstraint c : clist) {
                                    vect.setElementAt(c,i--);
                                }

                                iterable = vect;
                            }


                            DefaultState<T> prev = null;
                            for (SyllConstraint c : iterable) {
                                ObjectPair<DefaultState<T>, DefaultState<T>> pair = map.get(c);
                                if (prev == null) {
                                    first = pair.getFirst();
                                } else {
                                    DefaultTermStatePair<T> eps = new DefaultTermStatePair<T>(null, pair.getFirst());
                                    prev.addTransition(eps);
                                    for (Filter<T> filter : prev.getFilters()) {
                                        eps.addActualFilter(filter);
                                    }
                                }
                                prev = pair.getSecond();
                            }
                            last = prev;
                        } else if (node instanceof SyllConstraint) {
                            SyllConstraint c = (SyllConstraint) node;
                            SyllConstraintOr constr = c.getConstraint();
                            if (constr != null) {
                                ObjectPair<DefaultState<T>, DefaultState<T>> pair = map.get(constr);

                                DefaultTermStatePair<T> eps = new DefaultTermStatePair<T>(null, pair.getFirst());
                                first.addTransition(eps);

                                addBindingsToReset(eps, pair.getFirst());

                                eps = new DefaultTermStatePair<T>(null, last);
                                pair.getSecond().addTransition(eps);
                                for (Filter<T> filter : pair.getSecond().getFilters()) {
                                    eps.addActualFilter(filter);
                                }

                                TreevialSymbol symbol = c.getKleene();
                                if (symbol != null) {
                                    if (symbol.getSymbolValue().equals("*")) {
                                        last.addTransition(new DefaultTermStatePair<T>(null,first));
                                        first.addTransition(new DefaultTermStatePair<T>(null,last));
                                    } else if (symbol.getSymbolValue().equals("+")) {
                                        last.addTransition(new DefaultTermStatePair<T>(null,first));
                                    } else if (symbol.getSymbolValue().equals("?")) {
                                        first.addTransition(new DefaultTermStatePair<T>(null,last));
                                    }
                                }
                            } else {
                                SyllBasicPatternElement elem = c.getBasicPatternElement();
                                if (elem != null) {
                                    Set<Term> set = termBuilder.getTerms(elem);

                                    for (Term term : set) {
                                        first.addTransition(new DefaultTermStatePair<T>(term,last));
                                    }
                                }
                            }
                        }
                        map.put(node,new ObjectPair<DefaultState<T>,DefaultState<T>>(first,last));
                    }
                }
                , true
        );


        return map.get(constraintOr);
    }

    private void addBindingsToReset(DefaultTermStatePair<T> pair, DefaultState<T> fragmentFirst) {
        final Set<Binding> bindings = new HashSet<Binding>();
        final Set<BindingUser> bindingUsers = new HashSet<BindingUser>();

        fragmentFirst.visit(
                new StateVisitor<T>() {
                    public void execute(State<T> state) {
                        for (Action<T> action : state.getActions()) {
                            bindingUsers.add(action);
                        }

                        for (Filter<T> filter : state.getFilters()) {
                            bindingUsers.add(filter);
                        }

                        for (TermStatePair<T> pair : state.getTransitions()) {
                            List<Binding> bs = pair.getBindings();
                            if (bs != null)
                                bindings.addAll(bs);
                        }
                    }
                }, true, new HashSet<State<T>>()
        );

        for (Binding bnd : bindings) {
            DefaultBindingReset reset = new DefaultBindingReset(bnd);
            boolean found = false;
            for (BindingUser user : bindingUsers) {
                Set<Binding> ub = user.getUsedBindings();

                if (ub == null || ub.contains(bnd)) {
                    user.addActualBindingReset(reset);
                    found = true;
                }
            }
            if (found) {
                pair.addBindingReset(reset);
            }
        }
    }
}
