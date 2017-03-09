/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.xmlimpl;

import org.w3c.dom.Element;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.api.ResourceChainNode;
import treeton.core.config.context.resources.api.ResourceType;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.util.xml.XMLParser;

import java.util.List;
import java.util.Map;

public class ResourceChainModelXMLImpl implements ResourceChainModel {
    ResourcesContextXMLImpl initialContext;
    Element element;

    public ResourceChainModelXMLImpl(ResourcesContextXMLImpl initialContext, Element element) {
        this.initialContext = initialContext;
        this.element = element;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceChainModelXMLImpl that = (ResourceChainModelXMLImpl) o;

        return element.equals(that.element);

    }

    public int hashCode() {
        return element.hashCode();
    }

    public int size() {
        List<Element> list = XMLParser.getChildElementsByTagName(element, "REF");
        return list.size();
    }

    public ResourceChainNode get(int i) {
        List<Element> list = XMLParser.getChildElementsByTagName(element, "REF");
        return new ResourceChainNodeXMLImpl(this, list.get(i));
    }

    public Map<String, Object> getInitialParameters() {
        throw new UnsupportedOperationException("ResourceChain xml implementation doesn't support initial parameters");
    }

    public ResourceType getType() throws ContextException {
        throw new UnsupportedOperationException("ResourceChain xml implementation doesn't support model with ResourceType");
    }

    public String getName() {
        return ContextUtil.shortName(element.getAttribute("NAME"));
    }

    public ResourcesContext getInitialContext() {
        return initialContext;
    }
}
