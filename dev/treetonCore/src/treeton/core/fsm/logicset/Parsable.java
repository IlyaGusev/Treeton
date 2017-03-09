/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

public interface Parsable {
    public int parse(String s);  //returns -1 if string doesnt match
}                              //or a number of read chars
