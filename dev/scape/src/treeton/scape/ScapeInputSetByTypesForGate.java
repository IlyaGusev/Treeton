/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import treeton.core.model.TrnTypeSet;

public interface ScapeInputSetByTypesForGate {
    void reset(TrnTypeSet trnTypes);

    boolean next();

    Annotation getAnnotation();

    int getClassId();
}
