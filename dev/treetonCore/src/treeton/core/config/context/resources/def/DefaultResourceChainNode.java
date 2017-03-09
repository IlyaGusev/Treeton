/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.def;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.api.ResourceChainNode;
import treeton.core.config.context.resources.api.ResourceModel;

public class DefaultResourceChainNode implements ResourceChainNode {
    ResourceModel resModel;

    public DefaultResourceChainNode(ResourceModel resModel, ResourceChainModel resChainModel) {
        this.resModel = resModel;
    }

    public ResourceModel getResourceModel() throws ContextException {
        return resModel;
    }

    public boolean passParameters() {
        return false;
    }
}
