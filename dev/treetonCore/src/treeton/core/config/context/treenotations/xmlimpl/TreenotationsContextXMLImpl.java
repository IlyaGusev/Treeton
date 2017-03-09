/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.treenotations.xmlimpl;

import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import treeton.core.config.context.Context;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.dclimpl.TrnTypeStorageDclImpl;
import treeton.core.model.xmlimpl.TrnRelationTypeStorageXMLImpl;
import treeton.core.scape.ParseException;
import treeton.core.util.FileMapper;
import treeton.core.util.sut;
import treeton.core.util.xml.XMLParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

public class TreenotationsContextXMLImpl extends TreenotationsContextBasic {
    private static char[][] keywords = {
            "type".toCharArray(),
            "tokentype".toCharArray()
    };
    public boolean active = false;
    Element xmlElement;
    private Logger logger = Logger.getLogger(TreenotationsContextXMLImpl.class);
    private URL rootFolder;

    public TreenotationsContextXMLImpl(Element xmlElement, URL rootFolder) {
        this.xmlElement = xmlElement;
        this.rootFolder = rootFolder;
    }

    public void activate() {
        if (active)
            return;

        try {
            try {
                if (getParent() != null) {
                    TreenotationsContextXMLImpl parent = (TreenotationsContextXMLImpl) getParent();
                    parent.activate();
                    types = new TrnTypeStorageDclImpl(this, (TrnTypeStorageDclImpl) parent.types);
                    relations = new TrnRelationTypeStorageXMLImpl(this, (TrnRelationTypeStorageXMLImpl) parent.relations);
                } else {
                    types = new TrnTypeStorageDclImpl(this, null);
                    relations = new TrnRelationTypeStorageXMLImpl(this, (TrnRelationTypeStorageXMLImpl) null);
                }
            } catch (TreetonModelException e) {
                throw new ContextException("TreetonModel exception during activation", e);
            }


            List<Element> l = XMLParser.getChildElementsByTagName(xmlElement, "TYPESDCL");
            if (l.size() != 1) {
                throw new ContextException("Domain " + xmlElement.getAttribute("NAME") + " contains more than one <TYPESDCL> element");
            }
            Element elem = l.get(0);
            Text txt = (Text) XMLParser.getFirstChild(elem);
            if (txt == null) {
                throw new ContextException("Wrong <TYPESDCL> declaration in the " + xmlElement.getAttribute("NAME") + " domain");
            }
            URL path;
            try {
                path = new URL(getFolder(), txt.getData());
                try {
                    readInTypes(path);
                } catch (ParseException e) {
                    throw new ContextException("Error during parsing " + path + "int the " + xmlElement.getAttribute("NAME") + " domain: " + e.getMessage());
                } catch (IOException e) {
                    throw new ContextException("Some IOException with " + path + " int the " + xmlElement.getAttribute("NAME") + " domain: " + e.getMessage());
                } catch (TreetonModelException e) {
                    throw new ContextException("TreetonModel exception during activation " + path + " int the " + xmlElement.getAttribute("NAME") + " domain: " + e.getMessage());
                }
            } catch (MalformedURLException e) {
                throw new ContextException("Malformed URL " + txt.getData() + " in the <TYPESDCL> element in the " + xmlElement.getAttribute("NAME") + " domain");
            }

            l = XMLParser.getChildElementsByTagName(xmlElement, "RELSXML");
            if (l.size() != 1) {
                throw new ContextException("Domain " + xmlElement.getAttribute("NAME") + " contains more than one <RELSXML> element");
            }
            elem = l.get(0);
            txt = (Text) XMLParser.getFirstChild(elem);
            if (txt == null) {
                throw new ContextException("Wrong <RELSXML> declaration in the " + xmlElement.getAttribute("NAME") + " domain");
            }
            try {
                path = new URL(getFolder(), txt.getData());
                try {
                    readInRelations(path);
                } catch (Exception e) {
                    throw new ContextException("Error during parsing " + path + "int the " + xmlElement.getAttribute("NAME") + " domain: " + e.getMessage());
                }
            } catch (MalformedURLException e) {
                throw new ContextException("Malformed URL " + txt.getData() + " in the <TYPESDCL> element in the " + xmlElement.getAttribute("NAME") + " domain");
            }
            active = true;
        } catch (ContextException e) {
            relations = null;
            types = null;
            active = false;

            logger.error(e);
        }
    }

    public URL getFolder() throws MalformedURLException {
        return new URL(rootFolder, xmlElement.getAttribute("DIR"));
    }


    private void readInRelations(URL path) throws Exception {
        Document document;

        document = XMLParser.parse(path.openStream());
        Element e = document.getDocumentElement();

        String domain = e.getAttribute("DOMAIN");

        String fn = ContextUtil.getFullName(this);
        if (!domain.equals(fn)) {
            throw new SAXException("Wrong domain name " + domain + ". Must be " + fn);
        }

        NodeList l = e.getElementsByTagName("RELATION");

        for (int i = 0; i < l.getLength(); i++) {
            Element element = (Element) l.item(i);
            ((TrnRelationTypeStorageXMLImpl) relations).register(element.getAttribute("NAME"), false);
        }
    }

    void readInTypes(URL path) throws ParseException, IOException, TreetonModelException, ContextException {
        char[] s = FileMapper.map2memory(path, "UTF8");
        int pl = 0, endpl = s.length - 1;

        pl = sut.skipSpacesEndls(s, pl, endpl);
        pl = sut.readInString(s, pl, endpl, "domain".toCharArray());
        pl = sut.skipSpacesEndls(s, pl, endpl);

        int beg = pl;
        pl = sut.skipVarValueName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing domain name", null, s, pl, endpl);
        }
        String N = new String(s, beg, pl - beg);
        if (!N.equals(ContextUtil.getFullName(this))) {
            throw new ParseException("wrong domain name " + N, null, s, pl, endpl);
        }

        while (true) {
            pl = sut.skipSpacesEndls(s, pl, endpl);
            if (pl > endpl)
                break;
            int n = sut.checkDelims(s, pl, endpl, keywords);
            if (n != -1) {
                pl = ((TrnTypeStorageDclImpl) types).readIn(s, pl, endpl);
                pl++;
            } else {
                throw new ParseException("Wrong syntax", path.toString(), s, pl, endpl);
            }
        }
    }

    public TreenotationsContext getParent() {
        Node nd = xmlElement.getParentNode();
        if (nd instanceof Element) {
            Element p = (Element) nd;
            if (p.getTagName().equals("DOMAIN")) {
                return new TreenotationsContextXMLImpl(p, rootFolder);
            }
        }
        return null;
    }

    public String getName() {
        return xmlElement.getAttribute("NAME");
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TreenotationsContextXMLImpl that = (TreenotationsContextXMLImpl) o;

        return xmlElement.equals(that.xmlElement);
    }

    public int hashCode() {
        return xmlElement.hashCode();
    }


    public int getChildCount() {
        List<Element> list = XMLParser.getChildElementsByTagName(xmlElement, "DOMAIN");
        return list.size();
    }

    public Iterator<Context> childIterator() throws ContextException {
        List<Element> list = XMLParser.getChildElementsByTagName(xmlElement, "DOMAIN");
        return new ChildIterator(list.iterator());
    }

    private class ChildIterator implements Iterator<Context> {
        Iterator<Element> it;
        String name;

        public ChildIterator(Iterator<Element> it) throws ContextException {
            this.it = it;
            name = ContextUtil.getFullName(TreenotationsContextXMLImpl.this);
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public Context next() {
            Element e = it.next();
            return new TreenotationsContextXMLImpl(e, rootFolder);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
