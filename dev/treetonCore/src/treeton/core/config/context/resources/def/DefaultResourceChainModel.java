/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.def;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.api.ResourceChainNode;
import treeton.core.config.context.resources.api.ResourceType;
import treeton.core.config.context.resources.api.ResourcesContext;

import java.util.Map;

public class DefaultResourceChainModel implements ResourceChainModel {
    String name;
    ResourcesContext initialContext;

    public DefaultResourceChainModel(String name, ResourcesContext initialContext) {
        this.name = name;
        this.initialContext = initialContext;
    }

    public int size() {
        return 0;
    }

    public ResourceChainNode get(int i) {
        return null;
    }

    public Map<String, Object> getInitialParameters() {
        return null;
    }

    public ResourceType getType() throws ContextException {
        return null;
    }

    public String getName() {
        return name;
    }

    public ResourcesContext getInitialContext() {
        return initialContext;
    }
}
