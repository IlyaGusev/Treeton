/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.xmlimpl;

import org.w3c.dom.Element;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.config.context.resources.api.ResourcesContextManager;
import treeton.core.util.xml.XMLParser;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ResourcesContextManagerXMLImpl implements ResourcesContextManager {
    private Element xmlElement;
    private URL rootFolder;

    public ResourcesContextManagerXMLImpl(Element xmlElement, URL rootFolder) {
        this.xmlElement = xmlElement;
        this.rootFolder = rootFolder;
    }

    public ResourcesContext get(String fullName) {
        List<Element> l = XMLParser.getChildElementsByTagName(xmlElement, "DOMAIN");

        if (l.size() == 0)
            return null;

        Element common = l.get(0);

        StringTokenizer st = new StringTokenizer(fullName, ".", false);
        if (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (!s.equals(common.getAttribute("NAME")))
                return null;
        }
        Element cur = common;
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            cur = getChildByName(cur, s);
            if (cur == null) {
                return null;
            }
        }
        return new ResourcesContextXMLImpl(cur, rootFolder);
    }

    public ResourcesContext getRootContext() {
        List<Element> l = XMLParser.getChildElementsByTagName(xmlElement, "DOMAIN");

        if (l.size() == 0)
            return null;

        Element common = l.get(0);

        return new ResourcesContextXMLImpl(common, rootFolder);
    }

    public ResourcesContext[] getAllContexts() {
        List<ResourcesContext> result = new ArrayList<ResourcesContext>();

        List<Element> l = XMLParser.getChildElementsByTagName(xmlElement, "DOMAIN");

        if (l.size() == 0)
            return null;

        Element common = l.get(0);

        appendContext(result, common);

        return result.toArray(new ResourcesContext[result.size()]);
    }

    private void appendContext(List<ResourcesContext> result, Element xmlElement) {
        ResourcesContext c = new ResourcesContextXMLImpl(xmlElement, rootFolder);
        result.add(c);
        List<Element> list = XMLParser.getChildElementsByTagName(xmlElement, "DOMAIN");
        for (Element e : list) {
            appendContext(result, e);
        }
    }

    private Element getChildByName(Element cur, String name) {
        List<Element> l = XMLParser.getChildElementsByTagName(cur, "DOMAIN");
        for (Element e : l) {
            if (e.getAttribute("NAME").equals(name)) {
                return e;
            }
        }
        return null;
    }
}
