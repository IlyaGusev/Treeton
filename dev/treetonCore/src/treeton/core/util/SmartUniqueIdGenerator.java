/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public interface SmartUniqueIdGenerator extends UniqueIDGenerator {
    public void reset();

    public void freeID(int id);
}
