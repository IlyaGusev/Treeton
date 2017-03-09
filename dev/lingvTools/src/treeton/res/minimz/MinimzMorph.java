/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz;

import treeton.res.minimz.elector.ElectorEnddot;

import java.util.ArrayList;
import java.util.Iterator;

public class MinimzMorph implements Iterable<TypeMatrixTriplet> {
    ArrayList<TypeMatrixTriplet> a = new ArrayList<TypeMatrixTriplet>();

    public MinimzMorph() {
        TypeMatrix matrix = new TypeMatrix(new String[]{"Gramm"}, new ElectorEnddot());
        a.add(new TypeMatrixTriplet(null, null, matrix));
    }

    public Iterator<TypeMatrixTriplet> iterator() {
        return a.iterator();
    }
}