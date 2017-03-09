/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.xmlimpl;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.WrongParametersException;
import treeton.core.config.context.resources.api.ResourceSignature;
import treeton.core.config.context.resources.api.ResourceType;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.util.xml.XMLParser;

public class ResourceTypeXMLImpl implements ResourceType {
    ResourcesContextXMLImpl initialContext;
    ResourceSignature signature;
    Class resourceClass;
    private Element element;

    public ResourceTypeXMLImpl(Element elem, ResourcesContextXMLImpl initialContext) throws WrongParametersException {
        element = elem;
        this.initialContext = initialContext;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceTypeXMLImpl that = (ResourceTypeXMLImpl) o;

        return element.equals(that.element);
    }

    public int hashCode() {
        return element.hashCode();
    }

    public Class getResourceClass() throws ContextException {
        NodeList l = element.getElementsByTagName("CLASS");
        try {
            return Class.forName((((Text) XMLParser.getFirstChild(l.item(0))).getData()));
        } catch (ClassNotFoundException e) {
            throw new ContextException("Problem with class", e);
        }
    }

    public String getName() {
        return element.getAttribute("NAME");
    }

    public ResourceSignature getSignature() throws WrongParametersException {
        return new ResourceSignatureXMLImpl(element);
    }

    public ResourcesContext getInitialContext() {
        return null;
    }
}
