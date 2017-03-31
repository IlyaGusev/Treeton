/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.prosody.mdlcompiler.api.fsm.State;
import treeton.prosody.mdlcompiler.api.fsm.StateVisitor;
import treeton.prosody.mdlcompiler.api.fsm.TermStatePair;
import treeton.core.util.ObjectPair;
import treeton.prosody.mdlcompiler.grammar.ast.*;

import java.util.*;

@SuppressWarnings({"unchecked"})
public class Meter implements Comparable<Meter> {
    private int priority;
    private int threshold;
    private String name;
    private int id;

    private DefaultState fsm;

    public Meter(MeterDescription description, MdlFSMBuilder fsmBuilder, boolean reverseAutomaton, int id ) {
        this.id = id;

        for (MeterDescriptionMemberDeclaration memberDeclaration : description.getMembers()) {
            BaseNode member = memberDeclaration.getDescriptionMember();

            if ( member instanceof PriorityDeclaration) {
                priority = ((PriorityDeclaration)member).getIntegerValue();
            } else if ( member instanceof ThresholdDeclaration ) {
                threshold = ((ThresholdDeclaration)member).getIntegerValue();
            } else if ( member instanceof MeterNameDeclaration) {
                name = ((MeterNameDeclaration)member).getStringValue();
            }
        }

        SyllConstraintOr p = description.getPattern();
        ObjectPair<DefaultState,DefaultState> pair = fsmBuilder.build(p, reverseAutomaton);
        pair.getSecond().setFinal(true);
        fsm = pair.getFirst();
        fsmBuilder.removeEmptyTransitions(fsm);
    }

    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        final Map<State,Integer> stateNumerator = new HashMap<State, Integer>();

        fsm.visit(new StateVisitor() {
            public void execute(State state) {
                Integer stateN = stateNumerator.get(state);
                if (stateN == null) {
                    stateN = stateNumerator.size();
                    stateNumerator.put(state,stateN);
                }
                List<TermStatePair> list = state.getTransitions();
                for (TermStatePair pair : list) {
                    if (buf.length()>0) {
                        buf.append(",");
                    }

                    Integer otherStateN = stateNumerator.get(pair.getState());
                    if (otherStateN == null) {
                        otherStateN = stateNumerator.size();
                        stateNumerator.put(pair.getState(),otherStateN);
                    }

                    buf.append(stateN).append(state.isFinal()?"!":"").append("-");
                    buf.append(pair.getTerm());
                    buf.append("->").append(otherStateN).append(pair.getState().isFinal()?"!":"");
                }
            }
        }, false, new HashSet());

        return "Meter{" +
                "\n  priority=" + priority +
                "\n  threshold=" + threshold +
                "\n  name='" + name + '\'' +
                "\n  fsm={" + buf + '}' +
                '}';
    }

    public DefaultState getFsm() {
        return fsm;
    }

    public String getName() {
        return name;
    }

    public int compareTo(Meter o) {
        int c = priority - o.priority;
        return c == 0 ? id - o.id : c;
    }

    public int getPriority() {
        return priority;
    }

    public int getThreshold() {
        return threshold;
    }
}
