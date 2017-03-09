/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

public interface IsLogicSet {
    IsLogicSet and(IsLogicSet b);

    IsLogicSet not();   //Disjunction

    boolean isEmpty();

    boolean isMember(Object o);
}
