/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

import treeton.core.BlackBoard;
import treeton.core.Treenotation;
import treeton.core.model.TrnType;

public class DefaultScapeVariable implements ScapeVariable {
    Treenotation value;
    ScapeVariable other;

    public void setValue(Treenotation value) {
        this.value = value;
    }

    public void setValue(ScapeVariable other) {
        this.value = null;
        this.other = other;
    }

    public TrnType getType() {
        return value == null ? other.getType() : value.getType();
    }

    public Object getValue(int feature) {
        return value == null ? other.getValue(feature) : value.get(feature);
    }

    public Object getValue(String featureName) {
        return value == null ? other.getValue(featureName) : value.get(featureName);
    }

    public void fillBlackBoard(BlackBoard board) {
        if (value == null)
            other.fillBlackBoard(board);
        else
            value.fillBlackBoard(board);
    }

    public TrnType getType(int n) {
        return value == null ? other.getType(n) : value.getType();
    }

    public Object getValue(int n, int feature) {
        return value == null ? other.getValue(n, feature) : value.get(feature);
    }

    public Object getValue(int n, String featureName) {
        return value == null ? other.getValue(n, featureName) : value.get(featureName);
    }

    public void fillBlackBoard(int n, BlackBoard board) {
        if (value == null)
            other.fillBlackBoard(n, board);
        else
            value.fillBlackBoard(board);
    }
}
