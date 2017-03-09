/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.treenotations;

import treeton.core.config.context.ContextException;

public interface TreenotationsContextManager {
    public TreenotationsContext get(String fullName) throws ContextException;
}
