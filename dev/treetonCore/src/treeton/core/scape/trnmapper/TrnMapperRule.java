/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape.trnmapper;

import treeton.core.BlackBoard;
import treeton.core.Treenotation;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.scape.AssignmentVector;
import treeton.core.scape.ParseException;
import treeton.core.scape.ScapeVariable;
import treeton.core.scape.TrnTemplate;
import treeton.core.util.sut;

import java.util.HashMap;

public class TrnMapperRule implements ScapeVariable {
    TrnTemplate trntemp;
    AssignmentVector assignments;
    Treenotation value;
    TrnTypeStorage sourceTypes, targetTypes;
    HashMap<String, ScapeVariable> bindings;

    public TrnMapperRule(TrnTypeStorage sourceTypes, TrnTypeStorage targetTypes) {
        this.sourceTypes = sourceTypes;
        this.targetTypes = targetTypes;
    }

    int readIn(char s[], int pl, int endpl) throws ParseException {
        trntemp = new TrnTemplate();
        pl = sut.skipSpacesEndls(s, pl, endpl);
        pl = trntemp.readIn(sourceTypes, s, pl, endpl, null);
        pl = sut.skipSpacesEndls(s, pl, endpl);
        if (s[pl] != '-') {
            throw new ParseException("Missing \'-\'", null, s, pl, endpl);
        }
        pl++;
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '>') {
            throw new ParseException("Missing \'>\'", null, s, pl, endpl);
        }
        pl++;
        assignments = new AssignmentVector(sourceTypes, targetTypes);
        bindings = new HashMap<String, ScapeVariable>();
        bindings.put("@", this);

        pl = assignments.readIn(s, pl, endpl, bindings, null);
        pl = sut.skipSpacesEndls(s, pl, endpl);
        return pl;

    }

    public void bind(Treenotation trn) {
        this.value = trn;
    }

    public void unbind() {
        this.value = null;
    }

    public void assign(Treenotation trn) {
        this.assignments.assign(trn);
    }

    public void assign(BlackBoard board) {

        this.assignments.assign(board);
    }

    //implemented methods
    public TrnType getType() {
        return assignments.getType();
    }

    public Object getValue(int feature) {
        return value.get(feature);
    }

    public Object getValue(String featureName) {
        return value.get(featureName);
    }

    public void fillBlackBoard(BlackBoard board) {
        value.fillBlackBoard(board);
    }

    public TrnType getType(int n) {
        return value.getType();
    }

    public Object getValue(int n, int feature) {
        return value.get(feature);
    }

    public Object getValue(int n, String featureName) {
        return value.get(featureName);
    }

    public void fillBlackBoard(int n, BlackBoard board) {
        value.fillBlackBoard(board);
    }

    public TrnTemplate getTemplate() {
        return trntemp;
    }
}
