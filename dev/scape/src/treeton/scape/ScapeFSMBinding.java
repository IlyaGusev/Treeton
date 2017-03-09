/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.Node;
import treeton.core.BlackBoard;
import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.fsm.logicset.LogicFSM;
import treeton.core.model.TrnType;
import treeton.core.scape.ScapeRegexBinding;
import treeton.core.scape.ScapeVariable;

public class ScapeFSMBinding extends ScapeBinding implements ScapeVariable {
    String name;
    ScapeRule rule;
    ScapeDFSMBinding newBinding;
    int nPairs;
    ScapeBindedPair[] pairs;
    ScapeRegexBinding regexBinding;
    LogicFSM regexFSM;
    int regexFeature;

    ScapeFSMBinding(String s, ScapeRule r) {
        super();
        name = s;
        rule = r;
        newBinding = null;
        nPairs = 0;
        pairs = null;
    }

    public TrnType getType() {
        if (newBinding == null)
            return null;
        return newBinding.getType();
    }


    public Object getValue(int feature) {
        if (newBinding == null)
            return null;
        return newBinding.getValue(feature);
    }

    public Object getValue(String featureName) {
        if (newBinding == null)
            return null;
        return newBinding.getValue(featureName);
    }

    public void fillBlackBoard(BlackBoard board) {
        if (newBinding == null)
            return;
        newBinding.fillBlackBoard(board);
    }

    public TrnType getType(int n) {
        if (newBinding == null)
            return null;
        return newBinding.getType(n);
    }

    public Object getValue(int n, int feature) {
        if (newBinding == null)
            return null;
        return newBinding.getValue(n, feature);
    }

    public Object getValue(int n, String featureName) {
        if (newBinding == null)
            return null;
        return newBinding.getValue(n, featureName);
    }

    public void fillBlackBoard(int n, BlackBoard board) {
        if (newBinding == null)
            return;
        newBinding.fillBlackBoard(n, board);
    }

    public Treenotation getTrn(int n) {
        if (newBinding == null)
            return null;
        return newBinding.getTrn(n);
    }

    public Annotation getAnn(int n) {
        if (newBinding == null)
            return null;
        return newBinding.getAnn(n);
    }

    public Token getStartToken() {
        if (newBinding == null)
            return null;
        return newBinding.getStartToken();
    }

    public Token getEndToken() {
        if (newBinding == null)
            return null;
        return newBinding.getEndToken();
    }

    public Node getStartNode() {
        if (newBinding == null)
            return null;
        return newBinding.getStartNode();
    }

    public Node getEndNode() {
        if (newBinding == null)
            return null;
        return newBinding.getEndNode();
    }

    public int getSize() {
        if (newBinding == null)
            return -1;
        return newBinding.getSize();
    }

    public Annotation[] toAnnArray() {
        if (newBinding == null)
            return null;
        return newBinding.toAnnArray();
    }

    public Treenotation[] toTrnArray() {
        if (newBinding == null)
            return null;
        return newBinding.toTrnArray();
    }

}
