/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import treeton.core.TreenotationStorageImpl;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.util.xml.XMLParser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CorpusEntry {
    public CorpusEntry(String guid, Corpus corpus) {
        this.guid = guid;
        this.corpus = corpus;
        parentFolders = new HashMap<String, CorpusFolder>();
    }

    private Corpus corpus;
    private String guid;
    private String label;
    private int manualEditionStamp;

    private Map<String, CorpusFolder> parentFolders;

    private String text;
    private TreenotationStorageImpl metadata;

    public String getGuid() {
        return guid;
    }

    public void load(File sourceFolder, TreenotationsContext trnContext, Corpus corpus) throws CorpusException {
        File f = new File(sourceFolder, guid + ".txt");
        if (f.exists()) {
            try {
                text = FileUtils.readFileToString(f, "UTF-8");
            } catch (IOException e) {
                throw new CorpusException("Corrupted entry (problem with source text): " + guid, e);
            }
        }

        f = new File(sourceFolder, guid + ".meta.xml");

        if (f.exists()) {
            InputStream is;
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                throw new CorpusException("Corrupted entry (metadata is absent): " + guid);
            }

            metadata = new TreenotationStorageImpl(trnContext);
            try {
                metadata.importXML(is);
            } catch (Exception e) {
                throw new CorpusException("Corrupted entry (problem with metadata): " + guid, e);
            }
        }

        f = new File( sourceFolder, guid + ".info.xml" );
        Document doc;
        try {
            doc = XMLParser.parse( f, new File(BasicConfiguration.getResource("/schema/corpusEntrySchema.xsd").toString() ) );
        } catch (Exception e) {
            throw new CorpusException("Corrupted entry (problem with entry info): " + guid, e);
        }

        loadFromElement(doc.getDocumentElement(), corpus);
    }

    private void loadFromElement(Element e, Corpus corpus) throws CorpusException {
        label = e.getAttribute("label");
        manualEditionStamp = Integer.valueOf(e.getAttribute("manualEditionStamp"));

        Node xmlnd = e.getFirstChild();
        while (xmlnd != null) {
            if (xmlnd instanceof Element) {
                Element cur = (Element) xmlnd;
                if ("parent".equals(cur.getTagName())) {
                    String parentGuid = cur.getTextContent();
                    CorpusFolder folder = corpus.getFolder(parentGuid);
                    if (folder == null) {
                        throw new CorpusException("Corrupted entry (wrong parent guid " + parentGuid + " ): " + guid);
                    }
                    addParentFolder(folder);
                } else {
                    throw new CorpusException("Corrupted entry (xml node contains unknown elements): " + guid);
                }
            } else {
                throw new CorpusException("Corrupted entry (xml node contains unknown elements): " + guid);
            }
            xmlnd = xmlnd.getNextSibling();
        }
    }

    public void saveText(File targetFolder) throws CorpusException {
        if (text != null) {
            try {
                File f = new File(targetFolder, guid + ".txt");
                FileUtils.writeStringToFile(f, text, "UTF-8");
            } catch (IOException e) {
                throw new CorpusException("problems when trying to serialize text of the entry", e);
            }
        } else {
            File f = new File(targetFolder, guid + ".txt");
            if( f.exists() ) {
                if( !f.delete() ) {
                    throw new CorpusException("unable to delete file with entry text");
                }
            }
        }
    }

    public void saveMetadata(File targetFolder) throws CorpusException {
        if (metadata != null) {
            File f = new File(targetFolder, guid + ".meta.xml");
            Document metadataDocument;
            try {
                metadataDocument = metadata.exportXML();
            } catch (ParserConfigurationException e) {
                throw new CorpusException("problems when trying to build xml with entry metadata", e);
            }
            try {
                XMLParser.serialize(f, metadataDocument);
            } catch (IOException e) {
                throw new CorpusException("problems when trying to serialize xml with entry metadata", e);
            }
        } else {
            File f = new File(targetFolder, guid + ".meta.xml");
            if( f.exists() ) {
                if( !f.delete() ) {
                    throw new CorpusException("unable to delete xml with metadata");
                }
            }
        }
    }

    public void saveEntryInfo(File targetFolder) throws CorpusException {
        Document doc;
        try {
            doc = XMLParser.createDocument("http://starling.rinet.ru/treeton", "Document");
        } catch (ParserConfigurationException e) {
            throw new CorpusException("problem when trying to create xml with entry info", e);
        }

        Element root = doc.getDocumentElement();
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("xsi:schemaLocation", "http://starling.rinet.ru/treeton http://starling.rinet.ru/treeton/corpusEntrySchema.xsd");

        root.setAttribute("guid", guid);
        root.setAttribute("label", label);
        root.setAttribute("manualEditionStamp", Integer.toString(manualEditionStamp));

        for (CorpusFolder parentFolder : parentFolders.values()) {
            Element parent = doc.createElement("parent");
            parent.setTextContent(parentFolder.getGuid());
            root.appendChild(parent);
        }

        File f = new File(targetFolder, guid + ".info.xml");
        try {
            XMLParser.serialize(f, doc);
        } catch (IOException e) {
            throw new CorpusException("problem when trying to serialize xml with entry info", e);
        }
    }

    void save(File targetFolder) throws CorpusException {
        saveText(targetFolder);
        saveMetadata(targetFolder);
        saveEntryInfo(targetFolder);
    }

    public TreenotationStorageImpl getMetadata() {
        return metadata;
    }

    public String getText() {
        return text;
    }

    void setText(String text) {
        this.text = text;

        metadata = new TreenotationStorageImpl(corpus.getTrnContext());
        manualEditionStamp = -1;
    }

    public int getManualEditionStamp() {
        return manualEditionStamp;
    }

    void setManualEditionStamp(int manualEditionStamp) {
        this.manualEditionStamp = manualEditionStamp;
    }

    public void addParentFolder(CorpusFolder folder) {
        parentFolders.put(folder.getGuid(), folder);
        folder.addEntry(this);
    }

    public void removeParentFolder(CorpusFolder folder) {
        parentFolders.remove(folder.getGuid(), folder);
        folder.deleteEntry(this);
    }

    public Collection<CorpusFolder> getParentFolders() {
        return parentFolders.values();
    }

    void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public static String getGuidByFile(File file) {
        String name = file.getName();
        String suffix = ".info.xml";
        if( name.endsWith(suffix) ) {
            return name.substring(0,name.length() - suffix.length());
        }
        suffix = ".meta.xml";
        if( name.endsWith(suffix) ) {
            return name.substring(0,name.length() - suffix.length());
        }
        suffix = ".txt";
        if( name.endsWith(suffix) ) {
            return name.substring(0,name.length() - suffix.length());
        }
        return null;
    }

    public static void deleteEntryFiles(File targetFolder, String guid) throws CorpusException {
        File f = new File(targetFolder, guid + ".txt");
        if( f.exists() ) {
            if( !f.delete() ) {
                throw new CorpusException("unable to delete file with entry text");
            }
        }

        f = new File(targetFolder, guid + ".meta.xml");
        if( f.exists() ) {
            if( !f.delete() ) {
                throw new CorpusException("unable to delete xml with metadata");
            }
        }

        f = new File(targetFolder, guid + ".info.xml");
        if( f.exists() ) {
            if( !f.delete() ) {
                throw new CorpusException("unable to delete xml with entry info");
            }
        }
    }

    @Override
    public String toString() {
        return label;
    }

    public void reload() {

    }
}