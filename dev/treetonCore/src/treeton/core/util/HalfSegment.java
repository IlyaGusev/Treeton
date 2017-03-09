/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public class HalfSegment {
    double edge;
    int side; //negative means left side, positive means right side, zero means point

    public HalfSegment(double edge, int side) {
        this.edge = edge;
        this.side = side;
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean mayBeLess(HalfSegment v2, boolean strict) {
        if (side < 0) {
            return true;
        } else if (side == 0) {
            if (v2.side == 0) {
                return strict ? edge < v2.edge : edge <= v2.edge;
            } else if (v2.side > 0) {
                return true;
            } else {
                if (edge < v2.edge) {
                    return true;
                } else if (edge > v2.edge) {
                    return false;
                } else {
                    return !strict;
                }
            }
        } else { //side > 0
            if (v2.side <= 0) {
                return strict ? edge < v2.edge : edge <= v2.edge;
            } else {
                return true;
            }
        }
    }

    public boolean mayBeLess(Integer v2, boolean strict) {
        return side < 0 || (strict ? edge < v2 : edge <= v2);
    }

    public boolean mayBeMore(Integer v2, boolean strict) {
        return side > 0 || (strict ? edge > v2 : edge >= v2);
    }
}
