/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res;

import treeton.core.TreenotationStorage;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;

import java.util.Map;

public class CleanUp extends Resource {
    protected String process(String texts, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        storage.forgetAll();
        return null;
    }

    protected void stop() {
    }

    protected void processTerminated() {
    }

    protected void init() throws ResourceInstantiationException {
    }

    protected void deInit() {
    }
}
