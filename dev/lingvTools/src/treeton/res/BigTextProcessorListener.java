/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res;

import treeton.core.TreenotationStorage;
import treeton.core.config.context.resources.ExecutionException;

public interface BigTextProcessorListener {
    void blockProcessed(String blockSource, String result, TreenotationStorage storage);

    void exceptionDuringExecution(String blockSource, ExecutionException e);
}
