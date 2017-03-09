/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.xmlimpl;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import treeton.core.config.context.Context;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.WrongParametersException;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.api.ResourceModel;
import treeton.core.config.context.resources.api.ResourceType;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.util.xml.XMLParser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResourcesContextXMLImpl implements ResourcesContext {
    protected Element xmlElement;
    private URL rootFolder;

    protected ResourcesContextXMLImpl(Element xmlElement, URL rootFolder) {
        this.xmlElement = xmlElement;
        this.rootFolder = rootFolder;
    }

    public ResourcesContextXMLImpl(ResourcesContextXMLImpl context) {
        this.xmlElement = context.xmlElement;
        this.rootFolder = context.rootFolder;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourcesContextXMLImpl that = (ResourcesContextXMLImpl) o;

        return xmlElement.equals(that.xmlElement);
    }

    public int hashCode() {
        return xmlElement.hashCode();
    }

    public ResourcesContext getParent() {
        Node nd = xmlElement.getParentNode();
        if (nd instanceof Element) {
            Element p = (Element) nd;
            if (p.getTagName().equals("DOMAIN")) {
                return new ResourcesContextXMLImpl(p, rootFolder);
            }
        }
        return null;
    }

    public String getName() {
        return xmlElement.getAttribute("NAME");
    }

    public ResourceType getResourceType(String name, boolean closed) throws ContextException {
        ResourcesContextXMLImpl cur = this;
        boolean full = name.indexOf('.') >= 0;
        do {
            List<Element> l = XMLParser.getChildElementsByTagName(cur.xmlElement, "RESOURCETYPE");

            String ctxName = ContextUtil.getFullName(cur);

            for (Element e : l) {
                String rnm = ContextUtil.fullName(ctxName, e.getAttribute("NAME"));
                if (rnm.equals(full ? name : ContextUtil.fullName(ctxName, name))) {
                    try {
                        return new ResourceTypeXMLImpl(e, cur);
                    } catch (WrongParametersException e1) {
                        throw new ContextException("Wrong Parameters", e1);
                    }
                }
            }
            if (!closed) {
                return null;
            }
            cur = (ResourcesContextXMLImpl) cur.getParent();
        } while (cur != null);
        return null;
    }

    public ResourceModel getResourceModel(String name, boolean closed) throws ContextException {
        ResourcesContextXMLImpl[] arr = new ResourcesContextXMLImpl[1];
        Element element = findResourceElement(name, arr, closed);
        if (element == null) {
            return null;
        }
        return new ResourceModelXMLImpl(arr[0], element);
    }


    protected Element findElement(String name, ResourcesContextXMLImpl[] arr, boolean closed, String elementTagName) throws ContextException {
        ResourcesContextXMLImpl cur = this;
        boolean full = name.indexOf('.') >= 0;
        do {
            List<Element> l = XMLParser.getChildElementsByTagName(cur.xmlElement, elementTagName);
            String ctxName = ContextUtil.getFullName(cur);

            for (Element e : l) {
                String rnm = ContextUtil.fullName(ctxName, e.getAttribute("NAME"));
                if (rnm.equals(full ? name : ContextUtil.fullName(ctxName, name))) {
                    if (arr != null)
                        arr[0] = cur;
                    return e;
                }
            }
            if (!closed) {
                return null;
            }

            cur = (ResourcesContextXMLImpl) cur.getParent();
        } while (cur != null);
        return null;
    }

    Element findResourceElement(String name, ResourcesContextXMLImpl[] arr, boolean closed) throws ContextException {
        return findElement(name, arr, closed, "RESOURCE");
    }

    Element findResourceChainElement(String chainName, ResourcesContextXMLImpl[] arr, boolean closed) throws ContextException {
        return findElement(chainName, arr, closed, "CHAIN");
    }

    public ResourceChainModel getResourceChainModel(String name, boolean closed) throws ContextException {
        ResourcesContextXMLImpl[] arr;
        Element e = findResourceChainElement(name, arr = new ResourcesContextXMLImpl[1], closed);
        if (e == null) {
            return null;
        }

        return new ResourceChainModelXMLImpl(arr[0], e);
    }


    public int getResourcesCount() {
        List<Element> l = XMLParser.getChildElementsByTagName(xmlElement, "RESOURCE");
        return l.size();
    }

    public int getResourceChainsCount() {
        List<Element> l = XMLParser.getChildElementsByTagName(xmlElement, "CHAIN");
        return l.size();
    }

    public Iterator<String> resourcesIterator() throws ContextException {
        List<Element> l = XMLParser.getChildElementsByTagName(xmlElement, "RESOURCE");
        List<String> arr = new ArrayList<String>();
        String ctxName = ContextUtil.getFullName(this);
        for (Element e : l) {
            arr.add(ContextUtil.fullName(ctxName, e.getAttribute("NAME")));
        }
        return arr.iterator();
    }

    public Iterator<String> resourceChainsIterator() throws ContextException {
        List<Element> l = XMLParser.getChildElementsByTagName(xmlElement, "CHAIN");
        List<String> arr = new ArrayList<String>();
        String ctxName = ContextUtil.getFullName(this);
        for (Element e : l) {
            arr.add(ContextUtil.fullName(ctxName, e.getAttribute("NAME")));
        }
        return arr.iterator();
    }

    public String getMainResourceChain() throws ContextException {
        return null;  //Todo
    }

    public URL getFolder() throws ContextException {
        try {
            return new URL(rootFolder, xmlElement.getAttribute("DIR"));
        } catch (MalformedURLException e) {
            throw new ContextException("Malformed url", e);
        }
    }

    public int getChildCount() {
        List<Element> list = XMLParser.getChildElementsByTagName(xmlElement, "DOMAIN");
        return list.size();
    }

    public Iterator<Context> childIterator() throws ContextException {
        List<Element> list = XMLParser.getChildElementsByTagName(xmlElement, "DOMAIN");
        return new ChildIterator(list.iterator());
    }

    public String findInstanceOfResourceType(ResourceType tp) throws ContextException {
        ResourcesContextXMLImpl cur = this;
        do {
            List<Element> l = XMLParser.getChildElementsByTagName(cur.xmlElement, "RESOURCE");
            String ctxName = ContextUtil.getFullName(cur);

            for (Element e : l) {
                String tpnm = e.getAttribute("TYPE");
                if (tp.equals(cur.getResourceType(tpnm, true))) {
                    return ContextUtil.fullName(ctxName, e.getAttribute("NAME"));
                }
            }

            cur = (ResourcesContextXMLImpl) cur.getParent();
        } while (cur != null);
        return null;
    }

    public String toString() {
        return getName();
    }

    private class ChildIterator implements Iterator<Context> {
        Iterator<Element> it;
        String name;

        public ChildIterator(Iterator<Element> it) throws ContextException {
            this.it = it;
            name = ContextUtil.getFullName(ResourcesContextXMLImpl.this);
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public Context next() {
            Element e = it.next();
            return new ResourcesContextXMLImpl(e, rootFolder);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
