/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

public interface AdjoiningDetector {
    boolean adjoin(ScapeVariable other);

    boolean adjoinR(ScapeVariable other);
}
