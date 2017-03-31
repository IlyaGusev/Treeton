/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.metricsearch.andortree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class AndOperator<T> implements Iterable<OrOperator<T>> {
    ArrayList<T> userObjects = new ArrayList<>();
    ArrayList<OrOperator<T>> orOperators = new ArrayList<>();
    boolean negated = false;

    @Override
    public String toString() {
        return "AndOperator{}";
    }

    @Override
    public Iterator<OrOperator<T>> iterator() {
        return orOperators.iterator();
    }

    public void collectUserObjects( Collection<T> targetCollection ) {
        targetCollection.addAll( userObjects );
    }

    public int getNumberOfUserObjects() {
        return userObjects.size();
    }

    public int getNumberOfChildOperators() {
        return orOperators.size();
    }

    public boolean isValid() {
        return userObjects.size() + orOperators.size() > 0;
    }

    public boolean isNegated() {
        return negated;
    }
}
