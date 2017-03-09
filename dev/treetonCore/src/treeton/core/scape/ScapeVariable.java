/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

import treeton.core.BlackBoard;
import treeton.core.model.TrnType;

public interface ScapeVariable {
    TrnType getType();

    Object getValue(int feature);

    Object getValue(String featureName);

    void fillBlackBoard(BlackBoard board);

    TrnType getType(int n);

    Object getValue(int n, int feature);

    Object getValue(int n, String featureName);

    void fillBlackBoard(int n, BlackBoard board);
}
