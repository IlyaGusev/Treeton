/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import treeton.core.config.BasicConfiguration;
import treeton.core.util.xml.XMLParser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CorpusFolder {
    public CorpusFolder(String guid, Corpus corpus) {
        this.guid = guid;
        this.corpus = corpus;
        entries = new HashMap<String, CorpusEntry>();
        childFolders = new HashMap<String, CorpusFolder>();
    }

    private Corpus corpus;
    private String guid;
    private String label;
    private CorpusFolder parentFolder;
    private Map<String,CorpusEntry> entries;
    private Map<String,CorpusFolder> childFolders;

    public String getGuid() {
        return guid;
    }

    public CorpusFolder getParentFolder() {
        return parentFolder;
    }

    void setParentFolder(CorpusFolder parentFolder) {
        if( this.parentFolder != null ) {
            this.parentFolder.removeChildFolder( this );
        }

        this.parentFolder = parentFolder;

        if( parentFolder != null ) {
            parentFolder.addChildFolder(this);
        }
    }

    void addChildFolder(CorpusFolder folder) {
        childFolders.put( folder.getGuid(), folder );
    }

    void removeChildFolder(CorpusFolder folder) {
        childFolders.remove( folder.getGuid(), folder );
    }

    void addEntry( CorpusEntry entry ) {
        entries.put( entry.getGuid(), entry );
    }

    void deleteEntry( CorpusEntry entry ) {
        entries.remove(entry.getGuid(), entry);
    }

    void load( File sourceFolder, Corpus corpus ) throws CorpusException {
        File f = new File( sourceFolder, guid + ".info.xml" );
        Document doc;
        try {
            doc = XMLParser.parse(f, new File(BasicConfiguration.getResource("/schema/corpusFolderSchema.xsd").toString()));
        } catch (Exception e) {
            throw new CorpusException("Corrupted entry (problem with folder info): " + guid, e);
        }
        Element e = doc.getDocumentElement();

        label = e.getAttribute("label");

        Node xmlnd = e.getFirstChild();
        if( xmlnd != null ) {
            if (xmlnd instanceof Element) {
                Element cur = (Element) xmlnd;
                if ("parent".equals(cur.getTagName())) {
                    String parentGuid = cur.getTextContent();
                    CorpusFolder pFolder = corpus.getFolder(parentGuid);
                    if (pFolder == null) {
                        throw new CorpusException("Corrupted folder (wrong parent folder " + parentGuid + " ): " + guid);
                    }
                    setParentFolder( pFolder );
                } else {
                    throw new CorpusException("Corrupted folder (xml node contains unknown elements): " + guid);
                }
            } else {
                throw new CorpusException("Corrupted folder (xml node contains unknown elements): " + guid);
            }
        }
    }

    void save(File targetFolder) throws CorpusException {
        Document doc;
        try {
            doc = XMLParser.createDocument("http://starling.rinet.ru/treeton", "Document");
        } catch (ParserConfigurationException e) {
            throw new CorpusException("problem when trying to create xml with folder info", e);
        }

        Element result = doc.getDocumentElement();
        result.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        result.setAttribute("xsi:schemaLocation", "http://starling.rinet.ru/treeton http://starling.rinet.ru/treeton/corpusFolderSchema.xsd");

        result.setAttribute( "guid", guid );
        result.setAttribute("label", label);

        if( parentFolder != null ) {
            Element parent = doc.createElement("parent");
            parent.setTextContent(parentFolder.getGuid());

            result.appendChild(parent);
        }

        File f = new File(targetFolder, guid + ".info.xml");
        try {
            XMLParser.serialize(f, doc);
        } catch (IOException e) {
            throw new CorpusException("problem when trying to serialize xml with folder info", e);
        }
    }

    public Collection<CorpusFolder> getChildFolders() {
        return childFolders.values();
    }

    public Collection<CorpusEntry> getEntries() {
        return entries.values();
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public String getLabel() {
        return label;
    }

    void setLabel(String label) {
        this.label = label;
    }

    public static String getGuidByFile(File file) {
        String name = file.getName();
        String suffix = ".info.xml";
        if( name.endsWith(suffix) ) {
            return name.substring(0,name.length() - suffix.length());
        }
        return null;
    }

    public static void deleteFolderFiles(File targetFolder, String guid) throws CorpusException {
        File f = new File(targetFolder, guid + ".info.xml");
        if( f.exists() ) {
            if( !f.delete() ) {
                throw new CorpusException("unable to delete xml with folder info");
            }
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
