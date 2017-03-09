/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.xml;

import com.sun.org.apache.xml.internal.serializer.ToXMLStream;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;

public class XMLParser implements DOMErrorHandler, LSParserFilter {
    public static Document createDocument(String namespace, String rootElement) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        DOMImplementation impl = builder.getDOMImplementation();
        return impl.createDocument(namespace, rootElement, null);
    }

    public static Document parse(File f, File schemaFile) throws Exception {
        return parse(f.getPath(), schemaFile == null ? null : schemaFile.getPath());
    }

    public static Document parse(String f, String schemaFile) throws Exception {
        try {
            System.setProperty(DOMImplementationRegistry.PROPERTY, "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
        } catch (AccessControlException e) {
            //do nothing
        }
        DOMImplementationRegistry registry =
                DOMImplementationRegistry.newInstance();

        DOMImplementationLS impl =
                (DOMImplementationLS) registry.getDOMImplementation("LS");

        // create DOMBuilder
        LSParser builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, XMLConstants.W3C_XML_SCHEMA_NS_URI);

        DOMConfiguration config = builder.getDomConfig();

        // create Error Handler
        DOMErrorHandler errorHandler = new XMLParser();

        // create filter
        LSParserFilter filter = new XMLParser();

        builder.setFilter(filter);

        // set error handler
        config.setParameter("error-handler", errorHandler);

        // set schema language
        if (schemaFile != null) {
            config.setParameter("validate", Boolean.TRUE);
            config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
            config.setParameter("schema-location", schemaFile.replaceAll(" ", "%20"));
        } else {
            config.setParameter("validate", Boolean.FALSE);
        }
        //config.setParameter("element-content-whitespace", Boolean.FALSE);

        return builder.parseURI(f);
    }

    public static Document parse(InputStream is) throws Exception {
        return parse(is, (String) null);
    }

    public static Document parse(InputStream is, File schemaFile) throws Exception {
        return parse(is, schemaFile.getPath());
    }

    public static Document parse(InputStream is, String schemaFile) throws Exception {
        try {
            System.setProperty(DOMImplementationRegistry.PROPERTY, "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
        } catch (AccessControlException e) {
            // do nothing
        }
        DOMImplementationRegistry registry =
                DOMImplementationRegistry.newInstance();

        DOMImplementationLS impl =
                (DOMImplementationLS) registry.getDOMImplementation("LS");

        // create DOMBuilder
        LSParser builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, XMLConstants.W3C_XML_SCHEMA_NS_URI);

        DOMConfiguration config = builder.getDomConfig();

        // create Error Handler
        DOMErrorHandler errorHandler = new XMLParser();

        // create filter
        LSParserFilter filter = new XMLParser();

        builder.setFilter(filter);

        // set error handler
        config.setParameter("error-handler", errorHandler);

        if (schemaFile != null) {
            config.setParameter("validate", Boolean.TRUE);
            config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
            config.setParameter("schema-location", schemaFile.replaceAll(" ", "%20"));
        } else {
            config.setParameter("validate", Boolean.FALSE);
        }

        LSInput input = impl.createLSInput();
        input.setByteStream(is);

        return builder.parse(input);
    }

    public static void parseWithSAX(File source, File schemaFile, ContentHandler handler) throws SAXException, IOException {
        XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        parser.setFeature("http://xml.org/sax/features/validation",
                true);
        parser.setFeature("http://apache.org/xml/features/validation/schema",
                true);
        parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking",
                true);

        parser.setContentHandler(handler);
        parser.parse(source.getPath().replaceAll(" ", "%20"));
    }

    public static void serialize(File f, Node node) throws IOException {
        ToXMLStream s = new ToXMLStream();
        OutputStream out;
        out = new FileOutputStream(f);
        s.setOutputStream(out);
        s.setIndent(true);
        s.setIndentAmount(2);
        s.serialize(node);
        out.close();
    }

    public static String serialize(Node nd) throws IOException {
        ToXMLStream s = new ToXMLStream();
        ByteArrayOutputStream out;
        out = new ByteArrayOutputStream();
        s.setOutputStream(out);
        s.setIndent(true);
        s.setIndentAmount(2);
        s.serialize(nd);
        String res = out.toString();
        out.close();
        return res;
    }

    public static String serialize(Node nd, String encoding) throws IOException {
        ToXMLStream s = new ToXMLStream();
        ByteArrayOutputStream out;
        out = new ByteArrayOutputStream();
        s.setEncoding(encoding);
        s.setOutputStream(out);
        s.setIndent(true);
        s.setIndentAmount(2);
        s.serialize(nd);
        String res = new String(out.toByteArray(), encoding);
        out.close();
        return res;
    }

    public static List<Element> getChildElementsByTagName(Element parent, String name) {
        NodeList l = parent.getChildNodes();
        ArrayList<Element> res = new ArrayList<Element>();
        for (int i = 0; i < l.getLength(); i++) {
            Node cur = l.item(i);
            if (cur instanceof Element && (((Element) cur).getTagName().equals(name))) {
                res.add((Element) cur);
            }
        }
        return res;
    }

    public static Node getFirstChild(Node parent) {
        Node cur = parent.getFirstChild();
        while (cur != null && !(cur instanceof Element || cur instanceof Text)) {
            cur = cur.getNextSibling();
        }
        return cur;
    }

    public static Node getNextSibling(Node elem) {
        Node cur = elem.getNextSibling();
        while (cur != null && !(cur instanceof Element || cur instanceof Text)) {
            cur = cur.getNextSibling();
        }
        return cur;
    }

    public boolean handleError(DOMError error) {
        short severity = error.getSeverity();
        if (severity == DOMError.SEVERITY_ERROR) {
            System.out.println("[dom3-error]: " + error.getMessage());
        }

        if (severity == DOMError.SEVERITY_WARNING) {
            System.out.println("[dom3-warning]: " + error.getMessage());
        }
        return true;
    }

    public short startElement(Element elementArg) {
        return LSParserFilter.FILTER_ACCEPT;
    }

    public short acceptNode(Node nodeArg) {
        if (((Text) nodeArg).getData().replaceAll("\n", " ").trim().length() == 0)
            return LSParserFilter.FILTER_REJECT;
        return LSParserFilter.FILTER_ACCEPT;
    }

    public int getWhatToShow() {
        return NodeFilter.SHOW_TEXT;
    }
}
