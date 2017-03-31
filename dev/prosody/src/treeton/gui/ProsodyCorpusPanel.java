/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import org.jdesktop.swingx.JXTree;
import treeton.core.Treenotation;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.gui.metricsearch.MetricSearchPanel;
import treeton.gui.util.DialogHandler;
import treeton.gui.util.ExceptionDialog;
import treeton.gui.util.MessageBox;
import treeton.prosody.SyllableInfo;
import treeton.prosody.VerseProcessingUtilities;
import treeton.prosody.corpus.Corpus;
import treeton.prosody.corpus.CorpusEntry;
import treeton.prosody.corpus.CorpusException;
import treeton.prosody.corpus.CorpusFolder;
import treeton.prosody.metricindex.MetricIndex;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class ProsodyCorpusPanel extends JPanel implements ActionListener {
    private JSplitPane splitPane;

    public ProsodyCorpusPanel( TreenotationsContext trnContext ) throws MalformedURLException, TreetonModelException, ContextException {
        this.trnContext = trnContext;
        init();
    }

    private TreenotationsContext trnContext;
    private Corpus currentCorpus;
    private MetricIndex currentMetricIndex;
    private Collection<CorpusFolder> selectedFolders = new HashSet<CorpusFolder>();
    private ArrayList<CorpusEntry> selectedEntries = new ArrayList<CorpusEntry>();
    private ArrayList<CorpusFolder> selectedEntriesParents = new ArrayList<CorpusFolder>();

    private static String LOAD_COMMAND = "Load corpus";
    private static String NEW_FOLDER = "Create new folder";
    private static String RENAME = "Rename";
    private static String DELETE = "Remove";
    private static String MOVE = "Move";
    private static String SETTINGS = "Settings";
    private static String METRIC_SEARCH = "Metric search";
    private static String EXPORT_SYLLABLE_INFO = "Export syllable info";
    private JButton newFolderButton;
    private JButton renameButton;
    private JButton deleteButton;
    private JButton moveButton;
    private JButton settingsButton;
    private JButton metricSearchButton;
    private JButton exportSyllableInfoButton;
    private ProsodyCorpusTreePanel treePanel;
    private ProsodyCorpusEntryEditor entryEditor;

    private void init() throws MalformedURLException, TreetonModelException, ContextException {
        this.setLayout(new BorderLayout());
        JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
        toolbar.setRollover(true);
        JButton loadCorpusButton = GuiResources.tbf.addToolBarButton(toolbar, "open.gif", LOAD_COMMAND, LOAD_COMMAND, this);
        toolbar.add(loadCorpusButton);
        newFolderButton = GuiResources.tbf.addToolBarButton(toolbar, "newfolder.gif", NEW_FOLDER, NEW_FOLDER, this);
        renameButton = GuiResources.tbf.addToolBarButton(toolbar, "saveas.gif", RENAME, RENAME, this);
        deleteButton = GuiResources.tbf.addToolBarButton(toolbar, "delfile.gif", DELETE, DELETE, this);
        moveButton = GuiResources.tbf.addToolBarButton(toolbar, "moveto.gif", MOVE, MOVE, this);
        settingsButton = GuiResources.tbf.addToolBarButton(toolbar, "settings.gif", SETTINGS, SETTINGS, this);
        metricSearchButton = GuiResources.tbf.addToolBarButton(toolbar, "filter.gif", METRIC_SEARCH, METRIC_SEARCH, this);
        exportSyllableInfoButton = GuiResources.tbf.addToolBarButton(toolbar, "forward.gif", EXPORT_SYLLABLE_INFO, EXPORT_SYLLABLE_INFO, this);

        add(toolbar, BorderLayout.PAGE_START);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOneTouchExpandable(true);

        initScrollPane(new JPanel());

        add(splitPane, BorderLayout.CENTER);
        updateButtons();
    }

    private void updateButtons() {
        newFolderButton.setEnabled(currentCorpus != null && ( selectedEntries.isEmpty() && selectedFolders.size() <= 1 ));
        renameButton.setEnabled(currentCorpus != null && ( ( selectedEntries.size() + selectedFolders.size() ) == 1 ));
        deleteButton.setEnabled(currentCorpus != null && ( ( selectedEntries.size() > 0 ) ^ ( selectedFolders.size() > 0 ) ) );
        moveButton.setEnabled(currentCorpus != null &&
                ( ( selectedEntries.size() > 0 ) ^ ( selectedFolders.size() > 0 && foldersHaveSameParent( selectedFolders ) ) ) );
        settingsButton.setEnabled( currentCorpus != null );
        metricSearchButton.setEnabled( currentCorpus != null &&
                ( ( selectedEntries.size() == 0 ) && ( selectedFolders.size() > 0 && foldersHaveSameParent( selectedFolders ) ) ) );
        exportSyllableInfoButton.setEnabled( currentCorpus != null &&
                ( ( selectedEntries.size() == 0 ) && ( selectedFolders.size() == 1 ) ) );
    }

    private boolean foldersHaveSameParent(Collection<CorpusFolder> selectedFolders) {
        if( selectedFolders.isEmpty() ) {
            return true;
        }

        Iterator<CorpusFolder> iterator = selectedFolders.iterator();

        CorpusFolder firstFolder = iterator.next();

        while( iterator.hasNext() ) {
            if( iterator.next().getParentFolder() != firstFolder.getParentFolder() ) {
                return false;
            }
        }

        return true;
    }

    private void initScrollPane(Component view) throws TreetonModelException, MalformedURLException, ContextException {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setMinimumSize(new Dimension(100, 50));
        scrollPane.setPreferredSize(new Dimension(200, 300));
        splitPane.setLeftComponent(scrollPane);
        entryEditor = new ProsodyCorpusEntryEditor(trnContext);
        entryEditor.setMetricIndex( currentMetricIndex );
        if( currentCorpus != null ) {
            currentCorpus.addListener( entryEditor );
        }
        splitPane.setRightComponent(entryEditor);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if(LOAD_COMMAND.equals(cmd)) {
            doLoad();
        } else if(NEW_FOLDER.equals(cmd)) {
            doCreateNewFolder();
        } else if(DELETE.equals(cmd)) {
            doDelete();
        } else if(RENAME.equals(cmd)) {
            doRename();
        } else if(MOVE.equals(cmd)) {
            doMove();
        } else if(SETTINGS.equals(cmd)) {
            doSettings();
        } else if(METRIC_SEARCH.equals(cmd)) {
            doMetricSearch();
        } else if(EXPORT_SYLLABLE_INFO.equals(cmd)) {
            doExportSyllablesInfo();
        }
    }

    private void doExportSyllablesInfo() {
        assert currentCorpus != null;

        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("."));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File outputFolder = fc.getSelectedFile();
            if( !outputFolder.exists() ) {
                if( !outputFolder.mkdir() ) {
                    MessageBox.createError("Unable to create output folder",outputFolder.getPath());
                    return;
                }
            }

            if( !outputFolder.isDirectory() ) {
                MessageBox.createError("Output path is not a folder",outputFolder.getPath());
                return;
            }

            for (CorpusFolder folder : selectedFolders) {
                for (CorpusEntry entry : folder.getEntries()) {
                    try {
                        VerseProcessingUtilities verseProcessingUtilities = new VerseProcessingUtilities(trnContext);
                        HashSet<Treenotation> forceStressed = new HashSet<>();
                        HashSet<Treenotation> forceUnstressed = new HashSet<>();
                        verseProcessingUtilities.collectUserStresses(entry.getMetadata(),forceStressed,forceUnstressed);
                        ArrayList<SyllableInfo> syllableInfo = verseProcessingUtilities.generateSyllableInfo(entry.getMetadata(), forceStressed, forceUnstressed, null);

                        File outputFile = new File(outputFolder,entry.getLabel()+".syll");
                        if( !outputFile.exists() ) {
                            try {
                                if( !outputFile.createNewFile() ) {
                                    MessageBox.createError("Unable to create file",outputFile.getPath());
                                    continue;
                                }
                            } catch (IOException e) {
                                error(e);
                                continue;
                            }
                        }

                        PrintStream out = null;
                        try {
                            out = new PrintStream(outputFile);
                            for (SyllableInfo info : syllableInfo) {
                                out.print(info.toString());
                                out.print(";");
                            }
                        } catch (FileNotFoundException e) {
                            error(e);
                            return;
                        }
                        out.close();
                    } catch (TreetonModelException e) {
                        error(e);
                        return;
                    }
                }
            }
        }



    }

    static Window getWindowForComponent(Component parentComponent)
            throws HeadlessException {
        if (parentComponent == null)
            return JOptionPane.getRootFrame();
        if (parentComponent instanceof Frame || parentComponent instanceof Dialog)
            return (Window)parentComponent;
        return getWindowForComponent(parentComponent.getParent());
    }

    private void doMetricSearch() {
        CorpusFolder targetFolder = chooseTargetFolder("Выберите целевую папку для результатов поиска",null);

        if( targetFolder == null ) {
            return;
        }


        assert currentCorpus != null;

        Container comp = null;
        try {
            comp = new MetricSearchPanel( currentCorpus, currentMetricIndex, selectedFolders, targetFolder );
        } catch (TreetonModelException e) {
            error( e );
            return;
        }

        JDialog dialog;

        assert currentCorpus != null &&
                ( ( selectedEntries.size() == 0 ) && ( selectedFolders.size() > 0 && foldersHaveSameParent( selectedFolders ) ) );

        StringBuilder sb = new StringBuilder();

        for (CorpusFolder selectedFolder : selectedFolders) {
            sb.append(selectedFolder.getLabel()).append(", ");
        }

        String folderNames = sb.substring( 0, sb.length() - 2 );

        Window window = getWindowForComponent(this);
        if (window instanceof Frame) {
            dialog = new JDialog((Frame)window, "Ритмико-метрический поиск по \""+folderNames+"\"", true);
        } else {
            dialog = new JDialog((Dialog)window, "Ритмико-метрический поиск по \""+folderNames+"\"", true);
        }
        dialog.setComponentOrientation(this.getComponentOrientation());
        dialog.setContentPane(comp);
        DialogHandler.getInstance().showDialog(dialog);
    }

    private void doSettings() {
        assert currentCorpus != null;
        Container comp = new CorpusTweakerPanel( currentCorpus );
        JDialog dialog = new JDialog();
        dialog.setModal(true);
        dialog.setContentPane(comp);
        DialogHandler.getInstance().showDialog(dialog);
    }

    class MutableBoolean {
        boolean value;
    }

    private CorpusFolder chooseTargetFolder( String title, MutableBoolean doRemoveFromCurrentLocationFlag ) {
        JCheckBox removeFromCurrentLocationCheckBox = new JCheckBox();

        Frame frame = JOptionPane.getFrameForComponent(this);
        final JDialog dialog = new JDialog( frame, title );
        dialog.setLayout( new BorderLayout() );
        ProsodyCorpusTreeFoldersPanel foldersPanel = new ProsodyCorpusTreeFoldersPanel(currentCorpus);
        final JXTree foldersTree = foldersPanel.getJXTree();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill   = GridBagConstraints.BOTH;
        c.gridheight = 1;
        c.gridwidth  = 1;
        c.weightx = 1;
        c.weighty = 0.9;
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        dialogPanel.add(new JScrollPane(foldersTree),c);
        if( doRemoveFromCurrentLocationFlag != null && selectedFolders.isEmpty() ) {
            JPanel removeFromCurrentLocationPanel = new JPanel(new FlowLayout());
            removeFromCurrentLocationCheckBox.setSelected(true);
            removeFromCurrentLocationPanel.add(new JLabel("Remove entries from current location: "));
            removeFromCurrentLocationPanel.add(removeFromCurrentLocationCheckBox);
            c.gridy = 2;
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.weighty = 0.1;
            dialogPanel.add(removeFromCurrentLocationPanel, c);
            doRemoveFromCurrentLocationFlag.value = true;
        }

        dialog.add(dialogPanel);
        dialog.setModal(true);
        final ArrayList<CorpusFolder> folderContainer = new ArrayList<CorpusFolder>();
        foldersTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                int row = foldersTree.getRowForLocation(me.getX(), me.getY());
                if (row == -1) {
                    foldersTree.clearSelection();
                    return;
                }

                if (me.getClickCount() != 2) {
                    return;
                }

                TreePath tp = foldersTree.getPathForLocation(me.getX(), me.getY());
                if (tp != null) {
                    Object userObject = ((DefaultMutableTreeNode)
                            tp.getLastPathComponent()).getUserObject();
                    assert userObject instanceof CorpusFolder;
                    folderContainer.add((CorpusFolder) userObject);
                    dialog.setVisible(false);
                }
            }
        });

        DialogHandler.getInstance().showDialog(dialog);

        if( folderContainer.isEmpty() ) {
            return null;
        }

        CorpusFolder targetFolder = folderContainer.get(0);
        if( doRemoveFromCurrentLocationFlag != null ) {
            doRemoveFromCurrentLocationFlag.value = removeFromCurrentLocationCheckBox.isSelected();
        }

        return targetFolder;
    }

    private void doMove() {
        assert currentCorpus != null &&
                ( ( selectedEntries.size() > 0 ) ^ ( selectedFolders.size() > 0 && foldersHaveSameParent( selectedFolders ) ) );

        MutableBoolean doRemoveFromCurrentLocationFlag = new MutableBoolean();
        CorpusFolder targetFolder = chooseTargetFolder( "Choose target folder", doRemoveFromCurrentLocationFlag );

        if( targetFolder == null ) {
            return;
        }

        if( selectedEntries.size() > 0 ) {
            ArrayList<CorpusEntry> entriesArray = new ArrayList<CorpusEntry>(selectedEntries);
            ArrayList<CorpusFolder> entriesParentsArray = new ArrayList<CorpusFolder>(selectedEntriesParents);

            for (int i = 0; i < entriesArray.size(); i++) {
                CorpusEntry corpusEntry = entriesArray.get(i);
                CorpusFolder parentFolder = entriesParentsArray.get(i);
                if( parentFolder == targetFolder ) {
                    continue;
                }

                try {
                    currentCorpus.putEntryIntoFolder(corpusEntry,targetFolder);
                    if( doRemoveFromCurrentLocationFlag.value ) {
                        currentCorpus.removeEntryFromFolder(parentFolder,corpusEntry);
                    }
                } catch (CorpusException e) {
                    error(e);
                }
            }
        } else if( selectedFolders.size() > 0 ) {
            ArrayList<CorpusFolder> foldersArray = new ArrayList<CorpusFolder>(selectedFolders);

            for (CorpusFolder folder : foldersArray) {
                CorpusFolder t = targetFolder;

                while( t != null ) {
                    if( t == folder ) {
                        JOptionPane.showMessageDialog(this,"Some of requested movements are impossible!",
                                "Cycle detected", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    t = t.getParentFolder();
                }
            }


            for (CorpusFolder folder : foldersArray) {
                try {
                    currentCorpus.changeFolderParent(folder,targetFolder);
                } catch (CorpusException e) {
                    error(e);
                }
            }
        }
    }

    private void doRename() {
        assert currentCorpus != null && ( selectedEntries.size() + selectedFolders.size() == 1 );

        if( selectedEntries.size() == 1 ) {
            CorpusEntry entry = selectedEntries.iterator().next();
            Frame frame = JOptionPane.getFrameForComponent(this);
            String name = JOptionPane.showInputDialog(frame, "New name:", entry.getLabel());

            if( name == null || name.length() == 0) {
                return;
            }
            try {
                currentCorpus.renameEntry(entry,name);
            } catch (CorpusException e) {
                error(e);
            }
        } else {
            CorpusFolder folder = selectedFolders.iterator().next();
            Frame frame = JOptionPane.getFrameForComponent(this);
            String name = JOptionPane.showInputDialog(frame, "New name:", folder.getLabel());

            if( name == null || name.length() == 0) {
                return;
            }

            try {
                currentCorpus.renameFolder( folder, name );
            } catch (CorpusException e) {
                error(e);
            }
        }

    }

    private void doDelete() {
        assert currentCorpus != null && ( ( selectedEntries.size() > 0 ) ^ ( selectedFolders.size() > 0 ) );

        if( selectedEntries.size() > 0 ) {
            boolean doExistEntriesThatWillBePermanentlyDeleted = false;
            for (CorpusEntry entry : selectedEntries) {
                if( entry.getParentFolders().size() == 1 ) {
                    doExistEntriesThatWillBePermanentlyDeleted = true;
                    break;
                }
            }

            if( doExistEntriesThatWillBePermanentlyDeleted ) {
                Frame frame = JOptionPane.getFrameForComponent(this);
                int res = JOptionPane.showConfirmDialog(frame,
                        "There are entries that will be deleted permanently. Proceed?" );
                if( res != JOptionPane.YES_OPTION ) {
                    return;
                }
            }

            ArrayList<CorpusEntry> entriesArray = new ArrayList<CorpusEntry>(selectedEntries);
            ArrayList<CorpusFolder> entriesParentsArray = new ArrayList<CorpusFolder>(selectedEntriesParents);
            for (int i = 0; i < entriesArray.size(); i++) {
                CorpusEntry corpusEntry = entriesArray.get(i);
                CorpusFolder parentFolder = entriesParentsArray.get(i);

                if( corpusEntry.getParentFolders().size() > 1 ) {
                    try {
                        currentCorpus.removeEntryFromFolder(parentFolder, corpusEntry);
                    } catch (CorpusException e) {
                        error(e);
                    }
                } else {
                    try {
                        currentCorpus.deleteEntry( corpusEntry );
                    } catch (CorpusException e) {
                        error(e);
                    }
                }
            }
        } else if( selectedFolders.size() > 0 ) {
            boolean doExistEntriesThatWillBePermanentlyDeleted = false;
            for (CorpusFolder folder : selectedFolders) {
                for (CorpusEntry entry : folder.getEntries()) {
                    if( entry.getParentFolders().size() == 1 ) {
                        doExistEntriesThatWillBePermanentlyDeleted = true;
                        break;
                    }
                }

                if( doExistEntriesThatWillBePermanentlyDeleted ) {
                    break;
                }
            }

            if( doExistEntriesThatWillBePermanentlyDeleted ) {
                Frame frame = JOptionPane.getFrameForComponent(this);
                int res = JOptionPane.showConfirmDialog(frame,
                        "There are entries that will be deleted permanently. Proceed?" );
                if( res != JOptionPane.YES_OPTION ) {
                    return;
                }
            }

            ArrayList<CorpusFolder> foldersArray = new ArrayList<CorpusFolder>(selectedFolders);

            for (CorpusFolder folder : foldersArray) {
                if( currentCorpus.getFolder(folder.getGuid()) == null ) {
                    continue;
                }

                try {
                    currentCorpus.deleteFolder(folder);
                } catch (CorpusException e) {
                    error(e);
                }
            }
        }
    }

    void doLoad() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("."));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                onClose();

                currentCorpus = new Corpus(fc.getSelectedFile().getPath(), trnContext );
                currentCorpus.load();
                currentMetricIndex = new MetricIndex(currentCorpus);
                treePanel = new ProsodyCorpusTreePanel( currentCorpus );
                treePanel.setEntryTooltipProvider(new ProsodyCorpusTreePanel.EntryTooltipProvider() {
                    @Override
                    public String getTooltip(CorpusEntry entry) {
                        return entryEditor.getMetricInfo(null,entry);
                    }
                });

                initScrollPane(treePanel.getJXTree());
                treePanel.reload();
                treePanel.corpusLabelChanged();
                treePanel.getJXTree().addTreeSelectionListener(new TreeSelectionListener() {
                    public void valueChanged(TreeSelectionEvent e) {
                        selectedFolders.clear();
                        selectedEntries.clear();
                        selectedEntriesParents.clear();

                        TreePath[] selectionPaths = treePanel.getJXTree().getSelectionPaths();

                        if (selectionPaths != null) {
                            for (TreePath selectionPath : selectionPaths) {
                                Object userObject = ((DefaultMutableTreeNode)
                                        selectionPath.getLastPathComponent()).getUserObject();
                                if (userObject instanceof CorpusFolder) {
                                    selectedFolders.add((CorpusFolder) userObject);
                                } else if (userObject instanceof CorpusEntry) {
                                    selectedEntries.add((CorpusEntry) userObject);
                                    Object parentUserObject = ((DefaultMutableTreeNode)selectionPath.getParentPath().getLastPathComponent()).getUserObject();
                                    //Запись обязательно принадлежит какой-то папке
                                    assert parentUserObject instanceof CorpusFolder;
                                    selectedEntriesParents.add((CorpusFolder) parentUserObject);
                                }
                            }
                        }

                        updateButtons();
                    }
                });
                treePanel.getJXTree().addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent me) {
                        doMouseClicked(me);
                    }
                });

                updateButtons();
            } catch (CorpusException e) {
                error(e);
            } catch (TreetonModelException e) {
                error(e);
            } catch (MalformedURLException e) {
                error(e);
            } catch (ContextException e) {
                error(e);
            }
        }
    }

    private void doCreateNewFolder() {
        assert currentCorpus != null && selectedFolders.size() <= 1 && selectedEntries.isEmpty();

        CorpusFolder parentFolder = selectedFolders.isEmpty() ? null : selectedFolders.iterator().next();
        Frame frame = JOptionPane.getFrameForComponent(this);
        String name = JOptionPane.showInputDialog(frame, "Name of the new folder:", "");

        if( name == null || name.length() == 0) {
            return;
        }

        try {
            currentCorpus.createFolder( name, parentFolder );
        } catch (CorpusException e) {
            error(e);
        }
    }

    private void doMouseClicked(MouseEvent me) {
        assert me.getSource() == treePanel.getJXTree();

        int row = treePanel.getJXTree().getRowForLocation(me.getX(), me.getY());
        if (row == -1) {
            treePanel.getJXTree().clearSelection();
            return;
        }

        if (me.getClickCount() != 2) {
            return;
        }

        TreePath tp = treePanel.getJXTree().getPathForLocation(me.getX(), me.getY());
        if (tp != null) {
            Object userObject = ((DefaultMutableTreeNode)
                    tp.getLastPathComponent()).getUserObject();
            if (userObject instanceof CorpusEntry) {
                entryEditor.openEntry((CorpusEntry) userObject );
            }
        }
    }

    public void error(Exception e) {
        e.printStackTrace();
        ExceptionDialog.showExceptionDialog(ProsodyCorpusPanel.this, e);
    }

    public void onClose() {
        if( treePanel != null ) {
            treePanel.onClose();
        }
        if( entryEditor != null && currentCorpus != null ) {
            currentCorpus.removeListener( entryEditor );
            entryEditor.doSave();
        }

        if( currentMetricIndex != null ) {
            currentMetricIndex.close();
        }

        currentCorpus = null;
        currentMetricIndex = null;
    }
}
