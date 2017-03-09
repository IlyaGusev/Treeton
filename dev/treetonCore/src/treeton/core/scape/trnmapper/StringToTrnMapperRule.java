/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape.trnmapper;

import treeton.core.BlackBoard;
import treeton.core.Treenotation;
import treeton.core.fsm.logicset.LogicFSM;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.scape.*;
import treeton.core.util.sut;

import java.util.HashMap;

public class StringToTrnMapperRule {
    private static char[][] delim = {
            ">".toCharArray(),
            "->".toCharArray(),
    };
    TrnTemplate trntemp;
    AssignmentVector assignments;
    TrnTypeStorage targetTypes;
    TrnType targetTp;
    ScapeRegexBinding binding;

    public StringToTrnMapperRule(TrnTypeStorage targetTypes, TrnType targetTp) {
        this.targetTypes = targetTypes;
        this.targetTp = targetTp;
    }

    public StringToTrnMapperRule(TrnTypeStorage targetTypes) {
        this.targetTypes = targetTypes;
        this.targetTp = null;
    }

    int readIn(char s[], int pl, int endpl) throws ParseException {
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '<') {
            throw new ParseException("missing '<'", null, s, pl, endpl);
        }
        pl++;
        int beg = pl;
        while (pl <= endpl) {
            if (s[pl] == '\\') { //todo этого мало, потенциально возможны ошибки
                pl += 2;
            } else {
                int n = sut.checkDelims(s, pl, endpl, delim);
                if (n == 0) {
                    break;
                }
                pl++;
            }
        }
        if (pl > endpl) {
            throw new ParseException("missing '>'", null, s, pl, endpl);
        }
        String regstr = new String(s, beg, pl - beg);
        char[] tarr = ("{Atom:string=<" + regstr + ">:@}").toCharArray();
        trntemp = new TrnTemplate();
        trntemp.readIn(targetTypes, tarr, 0, tarr.length - 1, null);
        pl++;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, delim);
        if (n != 1) {
            throw new ParseException("missing '->'", null, s, pl, endpl);
        }
        pl += 2;
        assignments = targetTp == null ? new AssignmentVector(targetTypes, targetTypes) : new AssignmentVector(targetTp);
        HashMap<String, RegexpVariable> bindings = new HashMap<String, RegexpVariable>();
        binding = new ScapeRegexBinding("@");
        binding.setFSM((LogicFSM) trntemp.iterator().next().get("string"));
        bindings.put("@", binding);

        pl = assignments.readIn(s, pl, endpl, null, bindings);
        pl = sut.skipSpacesEndls(s, pl, endpl);
        return pl;

    }

    public void bind(String s) {
        binding.setString(s);
        binding.matchBindings();
    }

    public boolean next() {
        return binding.next();
    }

    public void unbind() {
    }

    public void assign(Treenotation trn) {
        this.assignments.assign(trn);
    }

    public void assign(BlackBoard board) {
        this.assignments.assign(board);
    }
}
