/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.queueview;

public interface WeightController {
    double getMinWeight();

    double getMaxWeight();

    double getWeight(Object obj);
}
