/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.xmlimpl;

import org.w3c.dom.Element;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.api.ResourceChainNode;
import treeton.core.config.context.resources.api.ResourceModel;

public class ResourceChainNodeXMLImpl implements ResourceChainNode {
    ResourceChainModel parentModel;
    Element element;

    public ResourceChainNodeXMLImpl(ResourceChainModel parentModel, Element element) {
        this.parentModel = parentModel;
        this.element = element;
    }

    public ResourceModel getResourceModel() throws ContextException {
        String resName = element.getAttribute("RESOURCE");
        if (resName == null) {
            resName = element.getAttribute("CHAIN");
            if (resName == null)
                return null;
        }
        return parentModel.getInitialContext().getResourceModel(resName, true);
    }

    public boolean passParameters() {
        return "true".equals(element.getAttribute("PASSPARAMS"));
    }
}
