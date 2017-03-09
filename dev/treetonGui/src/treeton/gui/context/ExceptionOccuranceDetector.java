/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import treeton.core.config.context.resources.Resource;

public interface ExceptionOccuranceDetector {
    public boolean exceptionOccured(Resource res);
}
