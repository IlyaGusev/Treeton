/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.BlackBoard;
import treeton.core.TString;
import treeton.core.TreetonFactory;
import treeton.core.scape.LogicTree;
import treeton.core.scape.ParseException;
import treeton.core.scape.RegexpVariable;
import treeton.core.scape.ScapeVariable;
import treeton.core.util.sut;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ScapeRule implements Comparable {
    private static char[][] constraintDelims = new char[][]{new char[]{'-', '>'}, new char[]{','}};
    private static char[][] delims = new char[][]{{':', ':'}, {'-', '>'}};
    private static char[][] keywords = new char[][]{
            "create".toCharArray(),
            "modify".toCharArray(),
            "remove".toCharArray(),
            "match".toCharArray(),
            "unmatch".toCharArray(),
            "do".toCharArray(),
            "java".toCharArray()
    };
    private static int SYSN = 0;
    String name;
    int index;
    HashMap<String, ScapeVariable> bindings;
    HashMap<String, RegexpVariable> regexBindings;
    ScapeFSM fsm;
    ScapePhase phase;
    BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);
    ArrayList<ScapeRHSAction> rhs = new ArrayList<ScapeRHSAction>();
    ScapeBindingSet sampleBindingSet;
    ArrayList<LogicTree> constraints = new ArrayList<LogicTree>();
    long label = -1;

    public ScapeRule() {
        name = null;
        bindings = new HashMap<String, ScapeVariable>();
        regexBindings = new HashMap<String, RegexpVariable>();
        fsm = new ScapeFSM();
        index = -1;
    }

    public ScapeRule(String s) {
        name = s;
        bindings = new HashMap<String, ScapeVariable>();
        regexBindings = new HashMap<String, RegexpVariable>();
        fsm = new ScapeFSM();
        index = -1;
    }

    public ScapeRule(TString s) {
        name = s.toString();
    }

    public String getName() {
        return name;
    }

    public ScapeFSM getFsm() {
        return fsm;
    }

    public ArrayList<ScapeRHSAction> getRhs() {
        return rhs;
    }

    public int readIn(char s[], int pl, int endpl, ScapePhase phase) throws ParseException, IOException {
        this.phase = phase;
        bindings.put("all", phase.allBinding);

        pl = sut.skipSpacesEndls(s, pl, endpl);
        pl = sut.readInString(s, pl, endpl, ScapePhase.keywords[ScapePhase.ruleKeywordNumber]);
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing rule name", null, s, pl, endpl);
        }
        name = new String(s, beg, pl - beg);
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }
        pl++;

        pl = fsm.readIn(s, pl, endpl, delims, this);

        pl = sut.skipSpacesEndls(s, pl, endpl);

        int delim = sut.checkDelims(s, pl, endpl, delims);

        if (delim == -1) {
            throw new ParseException("Wrong syntax", null, s, pl, endpl);
        }
        pl += delims[delim].length;

        pl = sut.skipSpacesEndls(s, pl, endpl);

        if (delim == 0) {
      /*Constraints*/
            while (true) {
                LogicTree constraint = new LogicTree();
                pl = constraint.readIn(s, pl, endpl, constraintDelims);
                constraint.validateExpression(s, endpl, bindings, regexBindings);
                constraints.add(constraint);
                if (s[pl] == '-')
                    break;
                pl++;
                pl = sut.skipSpacesEndls(s, pl, endpl);
            }
            pl += 2;
            pl = sut.skipSpacesEndls(s, pl, endpl);
        }
    /*RHS*/

        int nJava = 0;

        while (true) {
            delim = sut.checkDelims(s, pl, endpl, keywords);

            if (delim == 0) { //create
                ScapeRHSCreateAction cr = new ScapeRHSCreateAction(this);
                pl = cr.readIn(phase.types, s, pl, endpl, bindings, regexBindings);
                rhs.add(cr);
            } else if (delim == 1) { //modify
                ScapeRHSModifyAction mo = new ScapeRHSModifyAction(this);
                pl = mo.readIn(phase.types, s, pl, endpl, bindings);
                rhs.add(mo);
            } else if (delim == 2) { //remove
                ScapeRHSRemoveAction re = new ScapeRHSRemoveAction(this);
                pl = re.readIn(s, pl, endpl, bindings);
                rhs.add(re);
            } else if (delim == 3 || delim == 4) { //match or unmatch
                if (!phase.isOrtho) {
                    throw new ParseException("match keyword can only be used in orthophase scope", null, s, pl, endpl);
                }
                ScapeOrthoRHSAction or = new ScapeOrthoRHSAction(this);
                pl = or.readIn(s, pl, endpl);
                rhs.add(or);
            } else if (delim == 5) { //do
                ScapeDoAction da = new ScapeDoAction(this);
                pl = da.readIn(s, pl, endpl);
                rhs.add(da);
            } else if (delim == 6) { //java
                ScapeRHSJavaAction ja = new ScapeRHSJavaAction(this, nJava++);
                pl = ja.readIn(s, pl, endpl);
                phase.addJavaAction(ja);
                rhs.add(ja);
            }
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] == '}')
                break;
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
        }

        /*****/

        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '}') {
            throw new ParseException("missing '}'", null, s, pl, endpl);
        }
        pl++;
        return pl;
    }

    public void build(String name, ScapeFSM fsm) {
        this.phase = null;
        this.name = name;
        this.fsm = fsm;
        this.bindings = new HashMap<String, ScapeVariable>();

        for (ScapeFSMState state1 : this.fsm.states) {
            state1.rule = this;
            Iterator sit = state1.pairsIterator();
            while (sit.hasNext()) {
                ScapeBindedPair p = (ScapeBindedPair) sit.next();

                ScapeTreenotationTerm term = (ScapeTreenotationTerm) p.getTerm();
                term.rule = this;
            }
        }
    }

    public int compareTo(Object o) {
        ScapeRule r = (ScapeRule) o;
        if (index < r.index) {
            return -1;
        } else if (index > r.index) {
            return 1;
        }
        return 0;
    }

    public String nextSysName() {
        return "#" + SYSN++;
    }

    public boolean potentialSysName(String s) {
        if (s.charAt(0) == '#' || s.equals("all")) {
            return true;
        }
        return false;
    }

    public boolean checkConstraints() {
        for (LogicTree logicTree : constraints) {
            if (logicTree.evaluate() != Boolean.TRUE) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return name;
    }
}
