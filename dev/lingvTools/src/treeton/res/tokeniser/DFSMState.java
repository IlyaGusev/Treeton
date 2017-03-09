/*
 *  DFSMState.java
 *
 *  Copyright (c) 1998-2001, The University of Sheffield.
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 27/06/2000
 *
 *  $Id: DFSMState.java,v 1.17 2002/07/02 13:15:46 nasso Exp $
 */
package treeton.res.tokeniser;

import treeton.core.TreetonFactory;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.NumeratedObject;

import java.util.LinkedList;
import java.util.StringTokenizer;


/**
 * Implements a state of the deterministic finite state machine of the
 * tokeniser.
 * It differs from {@link FSMState FSMState} by the definition of the
 * transition function which in this case maps character types to other states
 * as oposed to the transition function from FSMState which maps character
 * types to sets of states, hence the nondeterministic character.
 * {@see FSMState FSMState}
 */
public class DFSMState implements java.io.Serializable { //extends FSMState{
    /**
     * Used to generate unique indices for all the objects of this class
     */
    static int index;

    static {
        index = 0;
    }

    SimpleTokeniser owner;
    /**
     * A table of strings describing an annotation.
     * The first line of the table contains the annotation type on the first
     * position and nothing on the second.
     * Each line after the first one contains a attribute on the first position
     * and its associated value on the second.
     */
    NumeratedObject[] tokenDesc;
    TrnType tokenType;
    int kindIndex;
    int langIndex;
    /**
     * The transition function of this state.
     */
    DFSMState[] transitionFunction = new DFSMState[SimpleTokeniser.maxTypeId];
    /**
     * The string of the RHS of the rule from which the token
     * description is built
     */
    String rhs;
    /**
     * The unique index of this state
     */
    int myIndex;

    public DFSMState(SimpleTokeniser owner) {
        this.owner = owner;
        myIndex = index++;
        owner.dfsmStates.add(this);
    }

    void put(UnicodeType type, DFSMState state) {
        put(type.type, state);
    } // put(UnicodeType type, DFSMState state)

    void put(int index, DFSMState state) {
        transitionFunction[index] = state;
    } // put(int index, DFSMState state)

    /**
     * This method is used to access the transition function of this state.
     *
     * @param type the Unicode type identifier as the corresponding static value
     *             on {@link java.lang.Character}
     */
    public DFSMState next(int type) {//UnicodeType type){
        return transitionFunction[type];
    } // next

    /**
     * Returns a GML (Graph Modelling Language) representation of the edges
     * emerging from this state
     */
    String getEdgesGML() {
        ///String res = "";

        StringBuffer res = new StringBuffer();
        DFSMState nextState;

        for (int i = 0; i < transitionFunction.length; i++) {
            nextState = transitionFunction[i];
            if (null != nextState) {
        /*
        res += "edge [ source " + myIndex +
        " target " + nextState.getIndex() +
        " label \"";
        res += SimpleTokeniser.typeMnemonics[i];
        res += "\" ]\n";
        */
                res.append("edge [ source ");
                res.append(myIndex);
                res.append(" target ");
                res.append(nextState.getIndex());
                res.append(" label \"");
                res.append(SimpleTokeniser.typeMnemonics[i]);
                res.append("\" ]\n");
            }
        }
        ;
        return res.toString();
    } // getEdgesGML

    /**
     * Builds the token description for the token that will be generated when
     * this <b>final</b> state will be reached and the action associated with it
     * will be fired.
     * See also {@link #setRhs(String)}.
     */
    int buildTokenDesc(String langFeature) throws TokeniserException {
        String ignorables = " \t\f";
        String token = null,
                type = null,
                attribute = null,
                value = null
                        ///prefix = null,
                        ///read =""
                        ;

        StringBuffer prefix = new StringBuffer();
        StringBuffer read = new StringBuffer();

        LinkedList attributes = new LinkedList(),
                values = new LinkedList();
        StringTokenizer mainSt =
                new StringTokenizer(rhs, ignorables + "\\\";=", true);

        int phase = 0;

        while (mainSt.hasMoreTokens()) {
            token = SimpleTokeniser.skipIgnoreTokens(mainSt);

            if (token.equals("\\")) {
                if (null == prefix)
                    ///prefix = mainSt.nextToken();

                    prefix = new StringBuffer(mainSt.nextToken());
                else ///prefix += mainSt.nextToken();

                    prefix.append(mainSt.nextToken());
                continue;
            } else if (null != prefix) {
                ///read += prefix;

                read.append(prefix.toString());
                prefix = null;
            }

            if (token.equals("\"")) {
                ///read = mainSt.nextToken("\"");

                read = new StringBuffer(mainSt.nextToken("\""));
                if (read.equals("\"")) ///read = "";
                    read = new StringBuffer();
                else {
                    //delete the remaining enclosing quote and restore the delimiters
                    mainSt.nextToken(ignorables + "\\\";=");
                }

            } else if (token.equals("=")) {

                if (phase == 1) {
                    ///attribute = read;

                    attribute = read.toString();
                    ///read = "";

                    read = new StringBuffer();
                    phase = 2;
                } else throw new TokeniserException("Invalid attribute format: " +
                        read);
            } else if (token.equals(";")) {
                if (phase == 0) {
                    ///type = read;
                    type = read.toString();
                    ///read = "";
                    read = new StringBuffer();
                    //Out.print("Type: " + type);
                    attributes.addLast(type);
                    values.addLast("");
                    phase = 1;
                } else if (phase == 2) {
                    ///value = read;
                    value = read.toString();
                    ///read = "";
                    read = new StringBuffer();
                    phase = 3;
                } else throw new TokeniserException("Invalid value format: " +
                        read);
            } else ///read += token;
                read.append(token);

            if (phase == 3) {
                // Out.print("; " + attribute + "=" + value);
                attributes.addLast(attribute);
                values.addLast(value);
                phase = 1;
            }
        }
        //Out.println();
        if (attributes.size() < 1)
            throw new InvalidRuleException("Invalid right hand side " + rhs);
        try {
            tokenType = owner.getTrnContext().getType((String) attributes.get(0));
        } catch (TreetonModelException e) {
            throw new TokeniserException("Error with model!");
        }
        tokenDesc = new NumeratedObject[attributes.size() - 1];

        try {
            for (int i = 1; i < attributes.size(); i++) {
                int n = tokenType.getFeatureIndex((String) attributes.get(i));
                if (n == -1)
                    throw new IllegalArgumentException("Unregistered feature " + (String) attributes.get(i));
                tokenDesc[i - 1] = new NumeratedObject(n, TreetonFactory.newTString((String) values.get(i)));
            }

            langIndex = langFeature != null ? tokenType.getFeatureIndex(langFeature) : -1;
        } catch (TreetonModelException e) {
            throw new TokeniserException("Error with model");
        }

        return attributes.size();
        // for(int i = 0; i < attributes.size(); i++){
        //    Out.println(tokenDesc[i][0] + "=" +
        //                  tokenDesc[i][1]);
        // }
    } // buildTokenDesc

    /**
     * Returns the RHS string
     */
    String getRhs() {
        return rhs;
    }

    /**
     * Sets the right hand side associated with this state. The RHS is
     * represented as a string value that will be parsed by the
     * {@link #buildTokenDesc(String)} method being converted in a table of strings
     * with 2 columns and as many lines as necessary.
     *
     * @param rhs the RHS string
     */
    void setRhs(String rhs) {
        this.rhs = rhs;
    }

    /**
     * Checks whether this state is a final one
     */
    public boolean isFinal() {
        return (null != rhs);
    }

    /**
     * Returns the unique ID of this state.
     */
    int getIndex() {
        return myIndex;
    }

    /**
     * Returns the token description associated with this state. This description
     * is built by {@link #buildTokenDesc(String)} method and consists of a table of
     * strings having two columns.
     * The first line of the table contains the annotation type on the first
     * position and nothing on the second.
     * Each line after the first one contains a attribute on the first position
     * and its associated value on the second.
     */
    public NumeratedObject[] getTokenDesc() {
        return tokenDesc;
    }

    public TrnType getTokenType() {
        return tokenType;
    }

} // class DFSMState
