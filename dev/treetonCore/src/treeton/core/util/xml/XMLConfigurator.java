/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class XMLConfigurator {
    private Element xmlElement;
    private URL context;

    public XMLConfigurator(String sourcePath, URL schemaPath) throws Exception {
        context = new File("./conf/").toURL();

        Document xml = XMLParser.parse(new URL(context, sourcePath).toString(),
                schemaPath == null ? null : schemaPath.toString());

        xmlElement = xml.getDocumentElement();
    }

    public XMLConfigurator(URL rootFolder, String sourcePath, String schemaPath) throws Exception {
        context = rootFolder;

        InputStream is = new URL(rootFolder, sourcePath).openStream();
        Document xml = XMLParser.parse(is,
                schemaPath == null ? null : new URL(rootFolder, schemaPath).toExternalForm());
        is.close();

        xmlElement = xml.getDocumentElement();
    }

    public URL getRootFolder() {
        return context;
    }

    public Element getRootElement() {
        return xmlElement;
    }
}
