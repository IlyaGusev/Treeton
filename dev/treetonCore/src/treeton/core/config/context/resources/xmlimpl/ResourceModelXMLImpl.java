/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.xmlimpl;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.api.ResourceModel;
import treeton.core.config.context.resources.api.ResourceType;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.util.xml.XMLParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ResourceModelXMLImpl implements ResourceModel {
    ResourcesContextXMLImpl initialContext;
    Element elem;

    public ResourceModelXMLImpl(ResourcesContextXMLImpl initialContext, Element elem) {
        this.initialContext = initialContext;
        this.elem = elem;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceModelXMLImpl that = (ResourceModelXMLImpl) o;

        return elem.equals(that.elem);
    }

    public int hashCode() {
        return elem.hashCode();
    }

    public Map<String, Object> getInitialParameters() {
        return readInitialParameters();
    }

    public ResourceType getType() throws ContextException {
        return getInitialContext().getResourceType(elem.getAttribute("TYPE"), true);
    }

    public String getName() {
        return ContextUtil.shortName(elem.getAttribute("NAME"));
    }

    public ResourcesContext getInitialContext() {
        return initialContext;
    }

    private HashMap<String, Object> readInitialParameters() {
        HashMap<String, Object> initialParameters = new HashMap<String, Object>();

        Node pnd = XMLParser.getFirstChild(elem);

        while (pnd != null) {
            Element pndElem = (Element) pnd;
            if (pndElem.getTagName().equals("PARAM")) {
                String nm = pndElem.getAttribute("NAME");
                NodeList vl = pndElem.getElementsByTagName("VALUE");
                if (vl.getLength() == 1) {
                    initialParameters.put(nm, ((Text) XMLParser.getFirstChild(vl.item(0))).getData());
                } else if (vl.getLength() > 1) {
                    ArrayList<String> vals = new ArrayList<String>();
                    for (int j = 0; j < vl.getLength(); j++) {
                        vals.add(((Text) XMLParser.getFirstChild(vl.item(j))).getData());
                    }
                    initialParameters.put(nm, vals);
                }
            }

            pnd = XMLParser.getNextSibling(pnd);
        }
        return initialParameters;
    }

}
