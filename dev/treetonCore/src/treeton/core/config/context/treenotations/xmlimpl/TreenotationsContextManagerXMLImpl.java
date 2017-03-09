/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.treenotations.xmlimpl;

import org.w3c.dom.Element;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.config.context.treenotations.TreenotationsContextManager;
import treeton.core.util.xml.XMLParser;

import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;

public class TreenotationsContextManagerXMLImpl implements TreenotationsContextManager {
    private Element xmlElement;
    private URL rootFolder;

    public TreenotationsContextManagerXMLImpl(Element xmlElement, URL rootFolder) {
        this.xmlElement = xmlElement;
        this.rootFolder = rootFolder;
    }

    public TreenotationsContext get(String fullName) {
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
        return new TreenotationsContextXMLImpl(cur, rootFolder);
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
