/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.collector;

public interface CollectorEnvironment extends MutableCollectable {
    public Object getObject(Class name);
}
