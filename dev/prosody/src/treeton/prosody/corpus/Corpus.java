/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.util.xml.XMLParser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Corpus {
    public Corpus(String rootFolder, TreenotationsContext trnContext ) throws CorpusException {
        this.trnContext = trnContext;

        entries = new HashMap<String, CorpusEntry>();
        folders = new HashMap<String, CorpusFolder>();
        rootCorpusFolders = new HashMap<String, CorpusFolder>();

        rootPath = new File( rootFolder );
        entriesPath = new File( rootFolder, "./entries" );
        foldersPath = new File( rootFolder, "./folders" );

        checkPaths();
    }

    private TreenotationsContext trnContext;
    private String corpusLabel;
    private Map<String,String> globalProperties = new HashMap<String, String>();
    private Map<String,CorpusEntry> entries;
    private Map<String,CorpusFolder> folders;
    private Map<String,CorpusFolder> rootCorpusFolders;
    private File rootPath;
    private File entriesPath;
    private File foldersPath;
    private ArrayList<CorpusListener> listeners = new ArrayList<CorpusListener>();

    public void addListener( CorpusListener listener ) {
        listeners.add( listener );
    }

    public void removeListener( CorpusListener listener ) {
        listeners.remove(listener);
    }

    public CorpusFolder getFolder( String guid ) {
        return folders.get( guid );
    }

    public CorpusEntry getEntry( String guid ) {
        return entries.get( guid );
    }

    public CorpusEntry createEntry( String label, CorpusFolder parentFolder ) throws CorpusException {
        assert parentFolder != null;

        UUID uid = UUID.randomUUID();
        CorpusEntry entry = new CorpusEntry( uid.toString(), this );
        entry.addParentFolder( parentFolder );
        entry.setLabel( label );

        entries.put( entry.getGuid(), entry );
        entry.save(entriesPath);

        for (CorpusListener listener : listeners) {
            listener.entryCreated( entry );
        }

        return entry;
    }

    public void deleteEntry( CorpusEntry entry ) throws CorpusException {
        ArrayList<CorpusFolder> folders = new ArrayList<CorpusFolder>( entry.getParentFolders() );
        for( CorpusFolder folder : folders ) {
            entry.removeParentFolder( folder );
        }

        entries.remove( entry.getGuid() );
        CorpusEntry.deleteEntryFiles( entriesPath, entry.getGuid() );

        for (CorpusListener listener : listeners) {
            listener.entryDeleted(entry,folders);
        }
    }

    public void renameEntry( CorpusEntry entry, String newLabel ) throws CorpusException {
        if( !entry.getLabel().equals( newLabel ) ) {
            entry.setLabel( newLabel );
            entry.saveEntryInfo(entriesPath);

            for (CorpusListener listener : listeners) {
                listener.entryNameChanged(entry);
            }
        }
    }

    public void changeEntryText( CorpusEntry entry, String newtext ) throws CorpusException {
        entry.setText( newtext );
        entry.saveText(entriesPath);

        for (CorpusListener listener : listeners) {
            listener.entryTextChanged(entry);
        }
    }

    public void metadataWasManuallyEdited( CorpusEntry entry ) throws CorpusException {
        entry.setManualEditionStamp(entry.getManualEditionStamp() + 1);
        entry.saveEntryInfo(entriesPath);
        entry.saveMetadata(entriesPath);

        for (CorpusListener listener : listeners) {
            listener.entryMetadataManuallyEdited(entry);
        }
    }

    public void metadataWasReloaded( CorpusEntry entry ) throws CorpusException {
        entry.setManualEditionStamp(-1);
        entry.saveEntryInfo(entriesPath);
        entry.saveMetadata(entriesPath);

        for (CorpusListener listener : listeners) {
            listener.entryMetadataReloaded(entry);
        }
    }

    public CorpusFolder createFolder( String label, CorpusFolder parentFolder ) throws CorpusException {
        UUID uid = UUID.randomUUID();
        CorpusFolder folder = new CorpusFolder( uid.toString(), this );
        folder.setLabel( label );
        folder.setParentFolder(parentFolder);

        folders.put( folder.getGuid(), folder );
        folder.save(foldersPath);

        if( parentFolder == null ) {
            rootCorpusFolders.put( folder.getGuid(), folder );
        }

        for (CorpusListener listener : listeners) {
            listener.folderCreated(folder);
        }

        return folder;
    }

    public void renameFolder( CorpusFolder folder, String newLabel ) throws CorpusException {
        if( !folder.getLabel().equals( newLabel ) ) {
            folder.setLabel( newLabel );
            folder.save(foldersPath);
        }

        for (CorpusListener listener : listeners) {
            listener.folderNameChanged(folder);
        }
    }

    public void changeFolderParent( CorpusFolder folder, CorpusFolder newParent ) throws CorpusException {
        CorpusFolder oldParent = folder.getParentFolder();

        if( oldParent == null ) {
            if( newParent == null ) {
                return;
            }

            rootCorpusFolders.remove( folder.getGuid() );
        }

        folder.setParentFolder(newParent);
        folder.save(foldersPath);

        if( newParent == null ) {
            rootCorpusFolders.put(folder.getGuid(), folder);
        }

        for (CorpusListener listener : listeners) {
            listener.folderParentChanged(folder, oldParent);
        }
    }

    public void putEntryIntoFolder( CorpusEntry entry, CorpusFolder folder ) throws CorpusException {
        if( entry.getParentFolders().contains(folder) ) {
            return;
        }

        entry.addParentFolder(folder);
        entry.saveEntryInfo( entriesPath );

        for (CorpusListener listener : listeners) {
            listener.entryWasPlacedIntoFolder( entry, folder );
        }
    }

    public void removeEntryFromFolder( CorpusFolder folder, CorpusEntry entry ) throws CorpusException {
        assert entry.getParentFolders().size() > 1;

        entry.removeParentFolder(folder);
        entry.saveEntryInfo(entriesPath);

        for (CorpusListener listener : listeners) {
            listener.entryWasRemovedFromFolder(entry, folder);
        }
    }

    public void deleteFolder( CorpusFolder folder ) throws CorpusException {
        ArrayList<CorpusFolder> foldersArray = new ArrayList<CorpusFolder>( folder.getChildFolders() );

        for (CorpusFolder corpusFolder : foldersArray) {
            deleteFolder( corpusFolder );
        }

        assert folder.getChildFolders().isEmpty();

        ArrayList<CorpusEntry> entries = new ArrayList<CorpusEntry>( folder.getEntries() );

        for (CorpusEntry entry : entries) {
            if( entry.getParentFolders().size() > 1 ) {
                removeEntryFromFolder( folder, entry );
            } else {
                deleteEntry( entry );
            }
        }

        changeFolderParent( folder, null );
        folders.remove( folder.getGuid() );
        CorpusFolder.deleteFolderFiles( foldersPath, folder.getGuid() );

        if( rootCorpusFolders.containsKey( folder.getGuid() ) ) {
            rootCorpusFolders.remove( folder.getGuid() );
        }

        for (CorpusListener listener : listeners) {
            listener.folderDeleted(folder);
        }
    }

    public void setCorpusLabel(String label) throws CorpusException {
        if( this.corpusLabel == null || !this.corpusLabel.equals( label )) {
            this.corpusLabel = label;
            saveGlobalSettings();
        }

        for (CorpusListener listener : listeners) {
            listener.corpusLabelChanged();
        }
    }

    public void setGlobalProperty( String propertyName, String propertyValue ) throws CorpusException {
        assert( propertyName != null );

        if( propertyValue == null ) {
            if( globalProperties.containsKey( propertyName ) ) {
                globalProperties.remove( propertyName );
                saveGlobalSettings();

                for (CorpusListener listener : listeners) {
                    listener.globalCorpusPropertyChanged( propertyName );
                }
            }
            return;
        }

        if( propertyValue.equals( globalProperties.get( propertyName ) ) ) {
            return;
        }

        globalProperties.put( propertyName, propertyValue );
        saveGlobalSettings();

        for (CorpusListener listener : listeners) {
            listener.globalCorpusPropertyChanged( propertyName );
        }
    }

    public void load() throws CorpusException {
        File f = new File( rootPath, "corpus.info.xml" );
        Document doc;
        try {
            doc = XMLParser.parse( f, new File(BasicConfiguration.getResource("/schema/corpusGlobalSettingsSchema.xsd").toString() ) );
        } catch (Exception e) {
            throw new CorpusException( "Problems with corpus.info.xml", e );
        }
        Element rootElement = doc.getDocumentElement();

        corpusLabel = rootElement.getAttribute( "label" );

        globalProperties.clear();

        Element globalPropertiesElement = (Element) rootElement.getFirstChild();
        if( globalPropertiesElement != null ) {
            NamedNodeMap nodeMap = globalPropertiesElement.getAttributes();
            for (int i = 0; i < nodeMap.getLength(); i++) {
                Node node = nodeMap.item(i);
                globalProperties.put(node.getNodeName(), node.getNodeValue());
            }
        }

        assert folders.isEmpty();

        File[] files = foldersPath.listFiles();

        if( files != null ) {
            for (File file : files) {
                String guid = CorpusFolder.getGuidByFile(file);

                if (guid == null ) {
                    throw new CorpusException("unnecessary file detected" + file.getPath());
                }

                if( folders.containsKey( guid )) {
                    continue;
                }

                CorpusFolder folder = new CorpusFolder( guid, this );
                folders.put( guid, folder );
            }

            for (CorpusFolder folder : folders.values()) {
                folder.load(foldersPath,this);

                if( folder.getParentFolder() == null ) {
                    rootCorpusFolders.put( folder.getGuid(), folder );
                }
            }
        }

        assert entries.isEmpty();

        files = entriesPath.listFiles();

        if( files != null ) {
            for (File file : files) {
                String guid = CorpusEntry.getGuidByFile(file);

                if (guid == null ) {
                    throw new CorpusException("unnecessary file detected" + file.getPath());
                }

                if( entries.containsKey( guid )) {
                    continue;
                }

                CorpusEntry entry = new CorpusEntry( guid, this );
                entry.load( entriesPath, trnContext, this );

                entries.put( guid, entry );
            }
        }
    }

    private void checkPaths() throws CorpusException {
        if( !rootPath.exists() && !rootPath.mkdirs() ) {
            throw new CorpusException("problem when trying to create target directory " + rootPath.getPath() );
        }

        if( !entriesPath.exists() && !entriesPath.mkdirs() ) {
            throw new CorpusException("problem when trying to create target entries directory " + entriesPath.getPath() );
        }

        if( !foldersPath.exists() && !foldersPath.mkdirs() ) {
            throw new CorpusException("problem when trying to create target folders directory " + foldersPath.getPath() );
        }
    }

    public void saveSingleEntry( CorpusEntry entry ) throws CorpusException {
        entry.save(entriesPath);
    }

    public void save() throws CorpusException {
        checkPaths();

        saveGlobalSettings();

        for (CorpusEntry entry : entries.values()) {
            entry.save(entriesPath);
        }

        File[] files = entriesPath.listFiles();

        if( files != null ) {
            for (File file : files) {
                String guid = CorpusEntry.getGuidByFile(file);

                if (guid == null || !entries.containsKey(guid)) {
                    if (!file.delete()) {
                        throw new CorpusException("unable to delete unnecessary file " + file.getPath());
                    }
                }
            }
        }

        for (CorpusFolder folder : folders.values()) {
            folder.save(foldersPath);
        }

        files = foldersPath.listFiles();

        if( files != null ) {
            for (File file : files) {
                String guid = CorpusFolder.getGuidByFile(file);

                if (guid == null || !folders.containsKey(guid)) {
                    if (!file.delete()) {
                        throw new CorpusException("unable to delete unnecessary file " + file.getPath());
                    }
                }
            }
        }
    }

    private void saveGlobalSettings() throws CorpusException {
        Document doc;
        try {
            doc = XMLParser.createDocument("http://starling.rinet.ru/treeton", "Document");
        } catch (ParserConfigurationException e) {
            throw new CorpusException("problem when trying to create document", e);
        }

        Element documentElement = doc.getDocumentElement();

        documentElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        documentElement.setAttribute("xsi:schemaLocation",
                "http://starling.rinet.ru/treeton http://starling.rinet.ru/treeton/corpusGlobalSettingsSchema.xsd");
        documentElement.setAttribute("label", corpusLabel);
        Element globalPropertiesElement = doc.createElement("globalProperties");
        for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
            globalPropertiesElement.setAttribute(entry.getKey(),entry.getValue());
        }
        documentElement.appendChild(globalPropertiesElement);

        File f = new File(rootPath, "corpus.info.xml");
        try {
            XMLParser.serialize(f, doc);
        } catch (IOException e) {
            throw new CorpusException("problem when trying to serialize corpus.info.xml", e);
        }
    }

    public TreenotationsContext getTrnContext() {
        return trnContext;
    }

    public File getRootPath() {
        return rootPath;
    }

    public String getCorpusLabel() {
        return corpusLabel;
    }

    public String getGlobalProperty( String propertyName ) {
        return globalProperties.get( propertyName );
    }

    public Collection<CorpusFolder> getRootCorpusFolders() {
        return rootCorpusFolders.values();
    }

    public void reloadEntry(CorpusEntry entry) throws CorpusException {
        entry.load( entriesPath, trnContext, this );
        for (CorpusListener listener : listeners) {
            listener.entryMetadataReloaded(entry);
        }
    }
}
