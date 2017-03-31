/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.metricsearch.andortree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class OrOperator<T> implements Iterable<AndOperator<T>> {
    ArrayList<T> userObjects = new ArrayList<>();
    ArrayList<AndOperator<T>> andOperators = new ArrayList<>();
    boolean negated = false;

    @Override
    public String toString() {
        return "OrOperator{}";
    }

    @Override
    public Iterator<AndOperator<T>> iterator() {
        return andOperators.iterator();
    }

    public void collectUserObjects( Collection<T> targetCollection ) {
        targetCollection.addAll( userObjects );
    }

    public int getNumberOfUserObjects() {
        return userObjects.size();
    }

    public int getNumberOfChildOperators() {
        return andOperators.size();
    }

    public boolean isValid() {
        return userObjects.size() + andOperators.size() > 0;
    }

    public boolean isNegated() {
        return negated;
    }
}
