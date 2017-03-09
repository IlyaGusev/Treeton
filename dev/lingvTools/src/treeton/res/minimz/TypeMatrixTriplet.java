/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz;

public class TypeMatrixTriplet {
    TypeMatrix equalsMatrix;
    TypeMatrix intersectMatrix;
    TypeMatrix insideMatrix;

    public TypeMatrixTriplet(TypeMatrix equalsMatrix, TypeMatrix intersectMatrix, TypeMatrix insideMatrix) {
        this.equalsMatrix = equalsMatrix;
        this.intersectMatrix = intersectMatrix;
        this.insideMatrix = insideMatrix;
    }

    public TypeMatrix getEqualsMatrix() {
        return equalsMatrix;
    }

    public TypeMatrix getIntersectMatrix() {
        return intersectMatrix;
    }

    public TypeMatrix getInsideMatrix() {
        return insideMatrix;
    }
}
