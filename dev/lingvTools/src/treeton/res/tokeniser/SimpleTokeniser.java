/*
 *  DefaultTokeniser.java
 *
 *  Copyright (c) 1998-2001, The University of Sheffield.
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 2000
 *
 *  $Id: SimpleTokeniser.java,v 1.13 2002/03/06 17:15:45 kalina Exp $
 */

package treeton.res.tokeniser;


import treeton.core.*;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.sut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

/**
 * Implementation of a Unicode rule based tokeniser.
 * The tokeniser gets its rules from a file an {@link java.io.InputStream
 * InputStream} or a {@link java.io.Reader Reader} which should be sent to one
 * of the constructors.
 * The implementations is based on a finite state machine that is built based
 * on the set of rules.
 * A rule has two sides, the left hand side (LHS)and the right hand side (RHS)
 * that are separated by the &quot;&gt;&quot; character. The LHS represents a
 * regular expression that will be matched against the input while the RHS
 * describes a Gate2 annotation in terms of annotation type and attribute-value
 * pairs.
 * The matching is done using Unicode enumarated types as defined by the {@link
 * java.lang.Character Character} class. At the time of writing this class the
 * suported Unicode categories were:
 * <ul>
 * <li>UNASSIGNED
 * <li>UPPERCASE_LETTER
 * <li>LOWERCASE_LETTER
 * <li>TITLECASE_LETTER
 * <li>MODIFIER_LETTER
 * <li>OTHER_LETTER
 * <li>NON_SPACING_MARK
 * <li>ENCLOSING_MARK
 * <li>COMBINING_SPACING_MARK
 * <li>DECIMAL_DIGIT_NUMBER
 * <li>LETTER_NUMBER
 * <li>OTHER_NUMBER
 * <li>SPACE_SEPARATOR
 * <li>LINE_SEPARATOR
 * <li>PARAGRAPH_SEPARATOR
 * <li>CONTROL
 * <li>FORMAT
 * <li>PRIVATE_USE
 * <li>SURROGATE
 * <li>DASH_PUNCTUATION
 * <li>START_PUNCTUATION
 * <li>END_PUNCTUATION
 * <li>CONNECTOR_PUNCTUATION
 * <li>OTHER_PUNCTUATION
 * <li>MATH_SYMBOL
 * <li>CURRENCY_SYMBOL
 * <li>MODIFIER_SYMBOL
 * <li>OTHER_SYMBOL
 * </ul>
 * The accepted operators for the LHS are "+", "*" and "|" having the usual
 * interpretations of "1 to n occurences", "0 to n occurences" and
 * "boolean OR".
 * For instance this is a valid LHS:
 * <br>"UPPERCASE_LETTER" "LOWERCASE_LETTER"+
 * <br>meaning an uppercase letter followed by one or more lowercase letters.
 * <p/>
 * The RHS describes an annotation that is to be created and inserted in the
 * annotation set provided in case of a match. The new annotation will span the
 * text that has been recognised. The RHS consists in the annotation type
 * followed by pairs of attributes and associated values.
 * E.g. for the LHS above a possible RHS can be:<br>
 * Token;kind=upperInitial;<br>
 * representing an annotation of type &quot;Token&quot; having one attribute
 * named &quot;kind&quot; with the value &quot;upperInitial&quot;<br>
 * The entire rule willbe:<br>
 * <pre>"UPPERCASE_LETTER" "LOWERCASE_LETTER"+ > Token;kind=upperInitial;</pre>
 * <br>
 * The tokeniser ignores all the empty lines or the ones that start with # or
 * //.
 */
public class SimpleTokeniser extends Resource {
    /**
     * maps from int (the static value on {@link java.lang.Character} to int
     * the internal value used by the tokeniser. The ins values used by the
     * tokeniser are consecutive values, starting from 0 and going as high as
     * necessary.
     * They map all the public static int members on{@link java.lang.Character}
     */
    public static Map typeIds;
    /**
     * The maximum int value used internally as a type i
     */
    public static int maxTypeId;
    /**
     * Creates a tokeniser
     */
    /**
     * Maps the internal type ids to the type name
     */
    public static String[] typeMnemonics;
    /**
     * Maps from type names to type internal id
     */
    public static Map stringTypeIds;
    /**
     * The separator from LHS to RH
     */
    static String LHStoRHS = ">";
    /**
     * A set of string representing tokens to be ignored (e.g. blanks
     */
    static Set ignoreTokens;

  /* Computes the lambda-closure (aka epsilon closure) of the given set of
  * states, that is the set of states that are accessible from any of the
  * states in the given set using only unrestricted transitions.
  * @return a set containing all the states accessible from this state via
  * transitions that bear no restrictions.
  */

    static {
        Field[] characterClassFields;

        try {
            characterClassFields = Class.forName("java.lang.Character").getFields();
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Could not find the java.lang.Character class!");
        }

        Collection staticFields = new LinkedList();
        // JDK 1.4 introduced directionality constants that have the same values as
        //character types; we need to skip those as well
        for (int i = 0; i < characterClassFields.length; i++)
            if (Modifier.isStatic(characterClassFields[i].getModifiers()) &&
                    characterClassFields[i].getName().indexOf("DIRECTIONALITY") == -1)
                staticFields.add(characterClassFields[i]);

        typeIds = new HashMap();
        maxTypeId = staticFields.size() - 1;
        typeMnemonics = new String[maxTypeId + 1];
        stringTypeIds = new HashMap();

        Iterator staticFieldsIter = staticFields.iterator();
        Field currentField;
        int currentId = 0;
        String fieldName;

        try {
            while (staticFieldsIter.hasNext()) {
                currentField = (Field) staticFieldsIter.next();
                if (currentField.getType().toString().equals("byte")) {
                    fieldName = currentField.getName();
                    typeIds.put(new Integer(currentField.getInt(null)),
                            new Integer(currentId));
                    typeMnemonics[currentId] = fieldName;
                    stringTypeIds.put(fieldName, new Integer(currentId));
                    currentId++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        ignoreTokens = new HashSet();
        ignoreTokens.add(" ");
        ignoreTokens.add("\t");
        ignoreTokens.add("\f");
    }

    /**
     * The method that does the actual tokenisation.
     */
    private final BlackBoard board = TreetonFactory.newBlackBoard(10, false);
    /**
     * The initial state of the non deterministic machin
     */
    protected FSMState initialState;
    /**
     * A set containng all the states of the non deterministic machin
     */
    protected Set fsmStates = new HashSet();
    /**
     * The initial state of the deterministic machin
     */
    protected DFSMState dInitialState;
    /**
     * A set containng all the states of the deterministic machin
     */
    protected Set dfsmStates = new HashSet();
    protected transient Map newStates = new HashMap();
    TrnType atomicType;
    TrnType DEFAULT_TOKEN_type;
    Map<Character, Character> errorMatches = new HashMap<>();
    private String kindFeatureName;
    private String langFeatureName;
    private char[] chars = new char[100];

    /**
     * Skips the ignorable tokens from the input returning the first significant
     * token.
     * The ignorable tokens are defined by {@link #ignoreTokens a set}
     */
    protected static String skipIgnoreTokens(StringTokenizer st) {
        Iterator ignorables;
        boolean ignorableFound = false;
        String currentToken;

        while (true) {
            if (st.hasMoreTokens()) {
                currentToken = st.nextToken();
                ignorables = ignoreTokens.iterator();
                ignorableFound = false;

                while (!ignorableFound && ignorables.hasNext()) {
                    if (currentToken.equals((String) ignorables.next()))
                        ignorableFound = true;
                }

                if (!ignorableFound) return currentToken;
            } else return null;
        }
    }//skipIgnoreTokens

    /**
     * Parses one input line containing a tokeniser rule.
     * This will create the necessary FSMState objects and the links
     * between them.
     *
     * @param line the string containing the rule
     */
    void parseRule(String line) throws TokeniserException {
        //ignore comments
        if (line.startsWith("#")) return;

        if (line.startsWith("//")) return;

        StringTokenizer st = new StringTokenizer(line, "()+*|\" \t\f>", true);
        FSMState newState = new FSMState(this);

        initialState.put(null, newState);
        FSMState finalState = parseLHS(newState, st, LHStoRHS);
        String rhs = "";

        if (st.hasMoreTokens()) rhs = st.nextToken("\f");

        if (rhs.length() > 0) finalState.setRhs(rhs);
    } // parseRule

    /**
     * Parses a part or the entire LHS.
     *
     * @param startState a FSMState object representing the initial state for
     *                   the small FSM that will recognise the (part of) the rule parsed by this
     *                   method.
     * @param st         a {@link java.util.StringTokenizer StringTokenizer} that
     *                   provides the input
     * @param until      the string that marks the end of the section to be
     *                   recognised. This method will first be called by {@link
     *                   #parseRule(String)} with &quot; &gt;&quot; in order to parse the entire
     *                   LHS. when necessary it will make itself another call to {@link #parseLHS
     *                   parseLHS} to parse a region of the LHS (e.g. a
     *                   &quot;(&quot;,&quot;)&quot; enclosed part.
     */
    FSMState parseLHS(FSMState startState, StringTokenizer st, String until)
            throws TokeniserException {

        FSMState currentState = startState;
        boolean orFound = false;
        List orList = new LinkedList();
        String token;
        token = skipIgnoreTokens(st);

        if (null == token) return currentState;

        FSMState newState;
        Integer typeId;
        UnicodeType uType;

        bigwhile:
        while (!token.equals(until)) {
            if (token.equals("(")) {//(..)
                newState = parseLHS(currentState, st, ")");
            } else if (token.equals("\"")) {//"unicode_type"
                String sType = parseQuotedString(st, "\"");
                newState = new FSMState(this);
                typeId = (Integer) stringTypeIds.get(sType);

                if (null == typeId)
                    throw new InvalidRuleException("Invalid type: \"" + sType + "\"");
                else uType = new UnicodeType(typeId.intValue());

                currentState.put(uType, newState);
            } else {// a type with no quotes
                String sType = token;
                newState = new FSMState(this);
                typeId = (Integer) stringTypeIds.get(sType);

                if (null == typeId)
                    throw new InvalidRuleException("Invalid type: \"" + sType + "\"");
                else uType = new UnicodeType(typeId.intValue());

                currentState.put(uType, newState);
            }
            //treat the operators
            token = skipIgnoreTokens(st);
            if (null == token) throw
                    new InvalidRuleException("Tokeniser rule ended too soon!");

            if (token.equals("|")) {

                orFound = true;
                orList.add(newState);
                token = skipIgnoreTokens(st);
                if (null == token) throw
                        new InvalidRuleException("Tokeniser rule ended too soon!");

                continue bigwhile;
            } else if (orFound) {//done parsing the "|"
                orFound = false;
                orList.add(newState);
                newState = new FSMState(this);
                Iterator orListIter = orList.iterator();

                while (orListIter.hasNext())
                    ((FSMState) orListIter.next()).put(null, newState);
                orList.clear();
            }

            if (token.equals("+")) {

                newState.put(null, currentState);
                currentState = newState;
                newState = new FSMState(this);
                currentState.put(null, newState);
                token = skipIgnoreTokens(st);

                if (null == token) throw
                        new InvalidRuleException("Tokeniser rule ended too soon!");
            } else if (token.equals("*")) {

                currentState.put(null, newState);
                newState.put(null, currentState);
                currentState = newState;
                newState = new FSMState(this);
                currentState.put(null, newState);
                token = skipIgnoreTokens(st);

                if (null == token) throw
                        new InvalidRuleException("Tokeniser rule ended too soon!");
            }
            currentState = newState;
        }
        return currentState;
    } // parseLHS

    /**
     * Parses from the given string tokeniser until it finds a specific
     * delimiter.
     * One use for this method is to read everything until the first quote.
     *
     * @param st    a {@link java.util.StringTokenizer StringTokenizer} that
     *              provides the input
     * @param until a String representing the end delimiter.
     */
    String parseQuotedString(StringTokenizer st, String until)
            throws TokeniserException {

        String token;

        if (st.hasMoreElements()) token = st.nextToken();
        else return null;

        ///String type = "";
        StringBuffer type = new StringBuffer();

        while (!token.equals(until)) {
            //type += token;
            type.append(token);
            if (st.hasMoreElements()) token = st.nextToken();
            else throw new InvalidRuleException("Tokeniser rule ended too soon!");
        }
        return type.toString();
    } // parseQuotedString

    /**
     * Converts the finite state machine to a deterministic one.
     *
     * @param s
     */
    private AbstractSet lambdaClosure(Set s) {

        //the stack/queue used by the algorithm
        LinkedList list = new LinkedList(s);

        //the set to be returned
        AbstractSet lambdaClosure = new HashSet(s);

        FSMState top;
        FSMState currentState;
        Set nextStates;
        Iterator statesIter;

        while (!list.isEmpty()) {
            top = (FSMState) list.removeFirst();
            nextStates = top.nextSet(null);

            if (null != nextStates) {
                statesIter = nextStates.iterator();

                while (statesIter.hasNext()) {
                    currentState = (FSMState) statesIter.next();
                    if (!lambdaClosure.contains(currentState)) {
                        lambdaClosure.add(currentState);
                        list.addFirst(currentState);
                    }//if(!lambdaClosure.contains(currentState))
                }//while(statesIter.hasNext())

            }//if(null != nextStates)
        }
        return lambdaClosure;
    } // lambdaClosure

    /**
     * Converts the FSM from a non-deterministic to a deterministic one by
     * eliminating all the unrestricted transitions.
     */
    void eliminateVoidTransitions() throws TokeniserException {

        //kalina:clear() faster than init() which is called with init()
        newStates.clear();
        Set sdStates = new HashSet();
        LinkedList unmarkedDStates = new LinkedList();
        DFSMState dCurrentState = new DFSMState(this);
        Set sdCurrentState = new HashSet();

        sdCurrentState.add(initialState);
        sdCurrentState = lambdaClosure(sdCurrentState);
        newStates.put(sdCurrentState, dCurrentState);
        sdStates.add(sdCurrentState);

        //find out if the new state is a final one
        Iterator innerStatesIter = sdCurrentState.iterator();
        String rhs;
        FSMState currentInnerState;
        Set rhsClashSet = new HashSet();
        boolean newRhs = false;

        while (innerStatesIter.hasNext()) {
            currentInnerState = (FSMState) innerStatesIter.next();
            if (currentInnerState.isFinal()) {
                rhs = currentInnerState.getRhs();
                rhsClashSet.add(rhs);
                dCurrentState.rhs = rhs;
                newRhs = true;
            }
        }

        if (rhsClashSet.size() > 1) {
            System.err.println("Warning, rule clash: " + rhsClashSet +
                    "\nSelected last definition: " + dCurrentState.rhs);
        }

        if (newRhs) {
            dCurrentState.buildTokenDesc(langFeatureName);
        }
        rhsClashSet.clear();
        unmarkedDStates.addFirst(sdCurrentState);
        dInitialState = dCurrentState;
        Set nextSet;

        while (!unmarkedDStates.isEmpty()) {
            //Out.println("\n\n=====================" + unmarkedDStates.size());
            sdCurrentState = (Set) unmarkedDStates.removeFirst();
            for (int type = 0; type < maxTypeId; type++) {
                //Out.print(type);
                nextSet = new HashSet();
                innerStatesIter = sdCurrentState.iterator();

                while (innerStatesIter.hasNext()) {
                    currentInnerState = (FSMState) innerStatesIter.next();
                    Set tempSet = currentInnerState.nextSet(type);
                    if (null != tempSet) nextSet.addAll(tempSet);
                }//while(innerStatesIter.hasNext())

                if (!nextSet.isEmpty()) {
                    nextSet = lambdaClosure(nextSet);
                    dCurrentState = (DFSMState) newStates.get(nextSet);

                    if (dCurrentState == null) {

                        //we have a new DFSMState
                        dCurrentState = new DFSMState(this);
                        sdStates.add(nextSet);
                        unmarkedDStates.add(nextSet);

                        //check to see whether the new state is a final one
                        innerStatesIter = nextSet.iterator();
                        newRhs = false;

                        while (innerStatesIter.hasNext()) {
                            currentInnerState = (FSMState) innerStatesIter.next();
                            if (currentInnerState.isFinal()) {
                                rhs = currentInnerState.getRhs();
                                rhsClashSet.add(rhs);
                                dCurrentState.rhs = rhs;
                                newRhs = true;
                            }
                        }

                        if (rhsClashSet.size() > 1) {
                            System.err.println("Warning, rule clash: " + rhsClashSet +
                                    "\nSelected last definition: " + dCurrentState.rhs);
                        }

                        if (newRhs) {
                            dCurrentState.buildTokenDesc(langFeatureName);
                        }
                        rhsClashSet.clear();
                        newStates.put(nextSet, dCurrentState);
                    }
                    ((DFSMState) newStates.get(sdCurrentState)).put(type, dCurrentState);
                } // if(!nextSet.isEmpty())

            } // for(byte type = 0; type < 256; type++)

        } // while(!unmarkedDStates.isEmpty())

    } // eliminateVoidTransitions

    /**
     * Returns a string representation of the non-deterministic FSM graph using
     * GML (Graph modelling language).
     */
    public String getFSMgml() {
        String res = "graph[ \ndirected 1\n";
        ///String nodes = "", edges = "";
        StringBuffer nodes = new StringBuffer(),
                edges = new StringBuffer();

        Iterator fsmStatesIter = fsmStates.iterator();
        while (fsmStatesIter.hasNext()) {
            FSMState currentState = (FSMState) fsmStatesIter.next();
            int stateIndex = currentState.getIndex();
      /*nodes += "node[ id " + stateIndex +
             " label \"" + stateIndex;
      */
            nodes.append("node[ id ");
            nodes.append(stateIndex);
            nodes.append(" label \"");
            nodes.append(stateIndex);

            if (currentState.isFinal()) {
                ///nodes += ",F\\n" + currentState.getRhs();
                nodes.append(",F\\n" + currentState.getRhs());
            }
            ///nodes +=  "\"  ]\n";
            nodes.append("\"  ]\n");
            ///edges += currentState.getEdgesGML();
            edges.append(currentState.getEdgesGML());
        }
        res += nodes.toString() + edges.toString() + "]\n";
        return res;
    } // getFSMgml

    /**
     * Returns a string representation of the deterministic FSM graph using
     * GML.
     */
    public String getDFSMgml() {
        String res = "graph[ \ndirected 1\n";
        ///String nodes = "", edges = "";
        StringBuffer nodes = new StringBuffer(),
                edges = new StringBuffer();

        Iterator dfsmStatesIter = dfsmStates.iterator();
        while (dfsmStatesIter.hasNext()) {
            DFSMState currentState = (DFSMState) dfsmStatesIter.next();
            int stateIndex = currentState.getIndex();
/*      nodes += "node[ id " + stateIndex +
               " label \"" + stateIndex;
*/
            nodes.append("node[ id ");
            nodes.append(stateIndex);
            nodes.append(" label \"");
            nodes.append(stateIndex);

            if (currentState.isFinal()) {
///              nodes += ",F\\n" + currentState.getRhs();
                nodes.append(",F\\n" + currentState.getRhs());
            }
///             nodes +=  "\"  ]\n";
            nodes.append("\"  ]\n");
///      edges += currentState.getEdgesGML();
            edges.append(currentState.getEdgesGML());
        }
        res += nodes.toString() + edges.toString() + "]\n";
        return res;
    } // getDFSMgml

    public void tokenise(String content, TreenotationStorage trnStorage) throws ExecutionException {
        //check the input
        if (content == null) {
            throw new RuntimeException(
                    "No content to tokenise!"
            );
        }


        int length = content.length();
        char currentChar;

        DFSMState graphPosition = dInitialState;

        //the index of the first character of the token trying to be recognised
        int tokenStart = 0;

        //the index of the last character of the last token recognised
        int lastMatch = -1;

        DFSMState lastMatchingState = null;
        DFSMState nextState;
        String tokenString;
        int charIdx = 0;
        TrnType errTp = null;
        errTp = DEFAULT_TOKEN_type;

        while (charIdx < length) {
            currentChar = content.charAt(charIdx);
            nextState = graphPosition.next((Integer) typeIds.get(
                    Character.getType(currentChar)));

            if (null != nextState) {
                graphPosition = nextState;
                if (graphPosition.isFinal()) {
                    lastMatch = charIdx;
                    lastMatchingState = graphPosition;
                }
                charIdx++;
            } else {//we have a match!

                if (null == lastMatchingState) {
                    tokenString = content.substring(tokenStart, tokenStart + 1);

                    Token t = trnStorage.addToken(1, 1, atomicType, null, tokenString);
                    Treenotation trn = TreetonFactory.newTreenotation(t, t, errTp, board);
                    trnStorage.add(trn);
                    charIdx = tokenStart + 1;
                } else {
                    tokenString = content.substring(tokenStart, lastMatch + 1);
                    synchronized (board) {
                        board.put(lastMatchingState.getTokenDesc());

                        if (lastMatchingState.langIndex != -1) {
                            String lang = sut.detectLang(tokenString);
                            if (!lang.equals(sut.LANG_UNDEFINED)) {
                                board.put(lastMatchingState.langIndex, lang);
                                if ("mix".equals(lang) && tokenString.length() > 2) {
                                    int n = 0;
                                    int lastPlace = -1;
                                    for (int i = 0; i < tokenString.length(); i++) {
                                        char c = tokenString.charAt(i);

                                        if (errorMatches.containsKey(c)) {
                                            lastPlace = i;
                                            n++;
                                            if (n > 1) {
                                                break;
                                            }
                                        }
                                    }

                                    if (n == 1) {
                                        char c = tokenString.charAt(lastPlace);
                                        tokenString = tokenString.substring(0, lastPlace) + errorMatches.get(c) + tokenString.substring(lastPlace + 1);
                                        lang = sut.detectLang(tokenString);
                                        if (!lang.equals(sut.LANG_UNDEFINED)) {
                                            board.put(lastMatchingState.langIndex, lang);
                                        }
                                    }
                                }
                            }
                        }

                        Token t = trnStorage.addToken(lastMatch + 1 - tokenStart, 1, atomicType, null, tokenString);
                        Treenotation trn = TreetonFactory.newTreenotation(t, t, lastMatchingState.getTokenType(), board);
                        trnStorage.add(trn);
                    }
                    charIdx = lastMatch + 1;
                }

                lastMatchingState = null;
                graphPosition = dInitialState;
                tokenStart = charIdx;
            }
        } // while(charIdx < length)

        if (null != lastMatchingState) {
            tokenString = content.substring(tokenStart, lastMatch + 1);

            synchronized (board) {
                board.put(lastMatchingState.getTokenDesc());

                if (lastMatchingState.langIndex != -1) {
                    String lang = sut.detectLang(tokenString);
                    if (!lang.equals(sut.LANG_UNDEFINED)) {
                        board.put(lastMatchingState.langIndex, lang);
                        if ("mix".equals(lang) && tokenString.length() > 2) {
                            int n = 0;
                            int lastPlace = -1;
                            for (int i = 0; i < tokenString.length(); i++) {
                                char c = tokenString.charAt(i);

                                if (errorMatches.containsKey(c)) {
                                    lastPlace = i;
                                    n++;
                                    if (n > 1) {
                                        break;
                                    }
                                }
                            }

                            if (n == 1) {
                                char c = tokenString.charAt(lastPlace);
                                tokenString = tokenString.substring(0, lastPlace) + errorMatches.get(c) + tokenString.substring(lastPlace + 1);
                                lang = sut.detectLang(tokenString);
                                if (!lang.equals(sut.LANG_UNDEFINED)) {
                                    board.put(lastMatchingState.langIndex, lang);
                                }
                            }
                        }
                    }
                }
                Token t = trnStorage.addToken(lastMatch + 1 - tokenStart, 1, atomicType, null, tokenString);
                Treenotation trn = TreetonFactory.newTreenotation(t, t, lastMatchingState.getTokenType(), board);
                trnStorage.add(trn);
            }
        }
    } // run

    public void tokeniseIntoSBuffer(String s, StringBuffer buf, BlackBoard featureFilter) throws RuntimeException {
        synchronized (board) {
            int len = s.length();
            if (len > chars.length) {
                char[] tarr = new char[(int) (chars.length * 1.5)];
                System.arraycopy(chars, 0, tarr, 0, len);
                chars = tarr;
            }
            s.getChars(0, len, chars, 0);
            tokeniseIntoSBuffer(chars, 0, len, buf, featureFilter);
        }
    }

    public void tokeniseIntoSBuffer(StringBuffer s, StringBuffer buf, BlackBoard featureFilter) throws RuntimeException {
        synchronized (board) {
            int len = s.length();
            if (len > chars.length) {
                char[] tarr = new char[(int) (chars.length * 1.5)];
                System.arraycopy(chars, 0, tarr, 0, len);
                chars = tarr;
            }
            s.getChars(0, len, chars, 0);
            tokeniseIntoSBuffer(chars, 0, len, buf, featureFilter);
        }
    }

    public void tokeniseIntoSBuffer(TString s, StringBuffer buf, BlackBoard featureFilter) throws RuntimeException {
        synchronized (board) {
            int len = s.length();
            if (len > chars.length) {
                char[] tarr = new char[(int) (chars.length * 1.5)];
                System.arraycopy(chars, 0, tarr, 0, len);
                chars = tarr;
            }
            s.getChars(0, len, chars, 0);
            tokeniseIntoSBuffer(chars, 0, len, buf, featureFilter);
        }
    }

    public void tokeniseIntoSBuffer(String s, StringBuffer buf, Map<TrnType, Set<Integer>> featureFilter) throws RuntimeException {
        synchronized (board) {
            int len = s.length();
            if (len > chars.length) {
                char[] tarr = new char[(int) (chars.length * 1.5)];
                System.arraycopy(chars, 0, tarr, 0, len);
                chars = tarr;
            }
            s.getChars(0, len, chars, 0);
            tokeniseIntoSBuffer(chars, 0, len, buf, featureFilter);
        }
    }

    private void tokeniseIntoSBuffer(char[] content, int from, int to, StringBuffer buffer, BlackBoard featureFilter) throws RuntimeException {
        char currentChar;

        DFSMState graphPosition = dInitialState;

        //the index of the first character of the token trying to be recognised
        int tokenStart = 0;

        //the index of the last character of the last token recognised
        int lastMatch = -1;

        DFSMState lastMatchingState = null;
        DFSMState nextState;
        TString tokenString;
        int charIdx = from;
        TrnType errTp;
        errTp = DEFAULT_TOKEN_type;

        while (charIdx < to) {
            currentChar = content[charIdx];
            nextState = graphPosition.next((Integer) typeIds.get(
                    Character.getType(currentChar)));

            if (null != nextState) {
                graphPosition = nextState;
                if (graphPosition.isFinal()) {
                    lastMatch = charIdx;
                    lastMatchingState = graphPosition;
                }
                charIdx++;
            } else {//we have a match!

                if (null == lastMatchingState) {
                    board.appendTrnStringView(buffer, errTp);
                    charIdx = tokenStart + 1;
                } else {
                    tokenString = TreetonFactory.newTString(content, tokenStart, lastMatch + 1 - tokenStart);
                    board.put(lastMatchingState.getTokenDesc(), featureFilter);
                    if (lastMatchingState.langIndex != -1) {
                        String lang = sut.detectLang(tokenString);
                        if (!lang.equals(sut.LANG_UNDEFINED)) {
                            board.put(lastMatchingState.langIndex, lang);
                        }
                    }
                    board.appendTrnStringView(buffer, lastMatchingState.getTokenType());
                    charIdx = lastMatch + 1;
                }

                lastMatchingState = null;
                graphPosition = dInitialState;
                tokenStart = charIdx;
            }
        } // while(charIdx < length)

        if (null != lastMatchingState) {
            tokenString = TreetonFactory.newTString(content, tokenStart, lastMatch + 1 - tokenStart);
            board.put(lastMatchingState.getTokenDesc(), featureFilter);
            if (lastMatchingState.langIndex != -1) {
                String lang = sut.detectLang(tokenString);
                if (!lang.equals(sut.LANG_UNDEFINED)) {
                    board.put(lastMatchingState.langIndex, lang);
                }
            }
            board.appendTrnStringView(buffer, lastMatchingState.getTokenType());
        }
    }

    private void tokeniseIntoSBuffer(char[] content, int from, int to, StringBuffer buffer, Map<TrnType, Set<Integer>> featureFilter) throws RuntimeException {
        char currentChar;

        DFSMState graphPosition = dInitialState;

        //the index of the first character of the token trying to be recognised
        int tokenStart = 0;

        //the index of the last character of the last token recognised
        int lastMatch = -1;

        DFSMState lastMatchingState = null;
        DFSMState nextState;
        TString tokenString;
        int charIdx = from;
        TrnType errTp;
        errTp = DEFAULT_TOKEN_type;

        while (charIdx < to) {
            currentChar = content[charIdx];
            nextState = graphPosition.next((Integer) typeIds.get(
                    Character.getType(currentChar)));

            if (null != nextState) {
                graphPosition = nextState;
                if (graphPosition.isFinal()) {
                    lastMatch = charIdx;
                    lastMatchingState = graphPosition;
                }
                charIdx++;
            } else {//we have a match!

                if (null == lastMatchingState) {
                    board.appendTrnStringView(buffer, errTp);
                    charIdx = tokenStart + 1;
                } else {
                    tokenString = TreetonFactory.newTString(content, tokenStart, lastMatch + 1 - tokenStart);
                    Set<Integer> set = featureFilter.get(lastMatchingState.getTokenType());
                    if (set != null) {
                        board.put(lastMatchingState.getTokenDesc(), set);
                    } else {
                        board.put(lastMatchingState.getTokenDesc());
                    }
                    if (lastMatchingState.langIndex != -1) {
                        String lang = sut.detectLang(tokenString);
                        if (!lang.equals(sut.LANG_UNDEFINED)) {
                            board.put(lastMatchingState.langIndex, lang);
                        }
                    }
                    board.appendTrnStringView(buffer, lastMatchingState.getTokenType());
                    charIdx = lastMatch + 1;
                }

                lastMatchingState = null;
                graphPosition = dInitialState;
                tokenStart = charIdx;
            }
        } // while(charIdx < length)

        if (null != lastMatchingState) {
            tokenString = TreetonFactory.newTString(content, tokenStart, lastMatch + 1 - tokenStart);
            Set<Integer> set = featureFilter.get(lastMatchingState.getTokenType());
            if (set != null) {
                board.put(lastMatchingState.getTokenDesc(), set);
            } else {
                board.put(lastMatchingState.getTokenDesc());
            }
            if (lastMatchingState.langIndex != -1) {
                String lang = sut.detectLang(tokenString);
                if (!lang.equals(sut.LANG_UNDEFINED)) {
                    board.put(lastMatchingState.langIndex, lang);
                }
            }
            board.appendTrnStringView(buffer, lastMatchingState.getTokenType());
        }
    }

    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        tokenise(text, storage);
        return null;
    }

    protected void stop() {
    }

    protected void processTerminated() {
    }

    protected void init() throws ResourceInstantiationException {
        try {
            URL rulesURL = new URL(getResContext().getFolder(), (String) getInitialParameters().get("rulesPath"));
            init(rulesURL);
        } catch (IOException e) {
            throw new ResourceInstantiationException("Some IO problem during tokeniser instantiation", e);
        } catch (TokeniserException e) {
            throw new ResourceInstantiationException("Some problem during tokeniser instantiation", e);
        } catch (ContextException e) {
            throw new ResourceInstantiationException("Some Context problem during tokeniser instantiation", e);
        }
    }

    protected void init(URL rulesURL) throws ResourceInstantiationException, IOException, TokeniserException {
        Reader rulesReader;
        try {
            atomicType = getTrnContext().getType((String) getInitialParameters().get("atomicType"));
            DEFAULT_TOKEN_type = getTrnContext().getType((String) getInitialParameters().get("defaultTokenType"));
            kindFeatureName = (String) getInitialParameters().get("kindFeature");
            langFeatureName = (String) getInitialParameters().get("langFeature");
        } catch (TreetonModelException e) {
            throw new ResourceInstantiationException("Error with model", e);
        }
        rulesReader = new InputStreamReader(rulesURL.openStream());
        initialState = new FSMState(this);
        BufferedReader bRulesReader = new BufferedReader(rulesReader);
        String line = bRulesReader.readLine();
        StringBuilder toParse = new StringBuilder();

        while (line != null) {
            if (line.endsWith("\\")) {
                toParse.append(line.substring(0, line.length() - 1));
            } else {
                toParse.append(line);
                parseRule(toParse.toString());
                toParse.delete(0, toParse.length());
            }
            line = bRulesReader.readLine();
        }
        eliminateVoidTransitions();

        try {
            URL errorMatchesUrl = new URL(getResContext().getFolder(), (String) getInitialParameters().get("errorMatchesPath"));
            Reader reader = new InputStreamReader(errorMatchesUrl.openStream());
            BufferedReader bReader = new BufferedReader(reader);

            while (bReader.ready()) {
                String s = bReader.readLine();

                if (s.isEmpty()) {
                    continue;
                }

                int i = s.indexOf("->");
                if (i == -1) {
                    throw new ResourceInstantiationException("File: " + errorMatchesUrl.getPath() + ", wrong line " + s);
                }

                String left = s.substring(0, i).trim();
                String right = s.substring(i + 2).trim();

                if (left.length() > 1 || right.length() > 1) {
                    throw new ResourceInstantiationException("File: " + errorMatchesUrl.getPath() + ", wrong line " + s);
                }

                errorMatches.put(left.charAt(0), right.charAt(0));
            }

        } catch (ContextException e) {
            throw new ResourceInstantiationException("Error with model", e);
        }


    }

    protected void deInit() {
        initialState = null;
    }
}
