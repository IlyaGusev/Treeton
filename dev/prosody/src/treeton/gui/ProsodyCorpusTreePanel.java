/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import org.jdesktop.swingx.JXTree;
import treeton.gui.util.MergedIcon;
import treeton.prosody.corpus.Corpus;
import treeton.prosody.corpus.CorpusEntry;
import treeton.prosody.corpus.CorpusFolder;
import treeton.prosody.corpus.CorpusListener;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.*;

public class ProsodyCorpusTreePanel implements CorpusListener {
    private JXTree tree = new JXTree();
    private DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
    private Map<CorpusFolder, DefaultMutableTreeNode> foldersToNodes = new HashMap<CorpusFolder, DefaultMutableTreeNode>();
    private Map<CorpusEntry, Collection<DefaultMutableTreeNode>> entriesToNodes = new HashMap<CorpusEntry, Collection<DefaultMutableTreeNode>>();
    private Corpus corpus;
    private MergedIcon manuallyEditedOpenFolderIcon;
    private MergedIcon manuallyEditedClosedFolderIcon;
    private MergedIcon manuallyEditedEntryIcon;
    private Set<CorpusFolder> manuallyEditedFolders = new HashSet<CorpusFolder>();


    public void reload() {
        model.reload();
    }

    public ProsodyCorpusTreePanel( Corpus corpus ) {
        this.corpus = corpus;
        importCorpus();
        initGui();
        corpus.addListener(this);
    }

    Comparator<DefaultMutableTreeNode> mutableTreeNodeComparator = new Comparator<DefaultMutableTreeNode>() {
        @Override
        public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
            return o1.getUserObject().toString().compareTo(o2.getUserObject().toString());
        }
    };

    private void importCorpus() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        model.setRoot(root);

        ArrayList<DefaultMutableTreeNode> childFolderNodes = new ArrayList<DefaultMutableTreeNode>();
        for (CorpusFolder corpusFolder : corpus.getRootCorpusFolders()) {
            DefaultMutableTreeNode childNode = importFolder( corpusFolder);
            childFolderNodes.add( childNode );
        }

        childFolderNodes.sort(mutableTreeNodeComparator);

        for (DefaultMutableTreeNode folderNode : childFolderNodes) {
            root.add( folderNode );
        }
    }


    private DefaultMutableTreeNode importFolder( CorpusFolder folder ) {
        DefaultMutableTreeNode result = new DefaultMutableTreeNode();
        result.setUserObject( folder );
        foldersToNodes.put( folder, result );

        boolean manualEdit = false;

        ArrayList<DefaultMutableTreeNode> childFolderNodes = new ArrayList<DefaultMutableTreeNode>();
        for (CorpusFolder corpusFolder : folder.getChildFolders()) {
            DefaultMutableTreeNode childNode = importFolder( corpusFolder);
            childFolderNodes.add( childNode );

            if( manuallyEditedFolders.contains( corpusFolder ) ) {
                manualEdit = true;
            }
        }

        ArrayList<DefaultMutableTreeNode> childEntryNodes = new ArrayList<DefaultMutableTreeNode>();
        for (CorpusEntry corpusEntry : folder.getEntries()) {
            DefaultMutableTreeNode childNode = importEntry(corpusEntry);
            childEntryNodes.add( childNode );

            if( corpusEntry.getManualEditionStamp() >= 0 ) {
                manualEdit = true;
            }
        }

        if( manualEdit ) {
            manuallyEditedFolders.add( folder );
        }

        childFolderNodes.sort(mutableTreeNodeComparator);
        childEntryNodes.sort(mutableTreeNodeComparator);

        for (DefaultMutableTreeNode folderNode : childFolderNodes) {
            result.add( folderNode );
        }

        for (DefaultMutableTreeNode entryNode : childEntryNodes) {
            result.add( entryNode );
        }

        return result;
    }

    private DefaultMutableTreeNode importEntry( CorpusEntry entry ) {
        DefaultMutableTreeNode result = new DefaultMutableTreeNode();
        result.setUserObject( entry );

        if( !entriesToNodes.containsKey( entry ) ) {
            entriesToNodes.put( entry, new HashSet<DefaultMutableTreeNode>() );
        }
        entriesToNodes.get( entry ).add(result);

        return result;
    }


    private void initGui(){
        ToolTipManager.sharedInstance().registerComponent(tree);

        tree.setRootVisible(false);
        tree.expandPath(new TreePath(model.getRoot()));

        ToolTipManager.sharedInstance().registerComponent(tree);
        ToolTipManager.sharedInstance().setDismissDelay(ToolTipManager.sharedInstance().getDismissDelay()*4);

        DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();

        ImageIcon img = GuiResources.getImageIcon("pencil.gif");

        manuallyEditedOpenFolderIcon = new MergedIcon( cellRenderer.getDefaultOpenIcon(), img );
        manuallyEditedClosedFolderIcon = new MergedIcon( cellRenderer.getDefaultClosedIcon(), img );
        manuallyEditedEntryIcon = new MergedIcon( cellRenderer.getDefaultLeafIcon(), img );


        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

                if( node == null ) {
                    return label;
                }

                if( node.getUserObject() instanceof CorpusFolder ) {
                    if( manuallyEditedFolders.contains( node.getUserObject() )) {
                        label.setIcon(expanded ? manuallyEditedOpenFolderIcon : manuallyEditedClosedFolderIcon);
                    } else {
                        label.setIcon( (expanded || leaf) ? getDefaultOpenIcon() : getDefaultClosedIcon() );
                    }
                    label.setToolTipText(null);

                } else if( node.getUserObject() instanceof CorpusEntry ) {
                    if( ((CorpusEntry) node.getUserObject()).getManualEditionStamp() >= 0 ) {
                        label.setIcon(manuallyEditedEntryIcon);
                    }
                    if( entryTooltipProvider != null ) {
                        label.setToolTipText(entryTooltipProvider.getTooltip( (CorpusEntry) node.getUserObject() ));
                    } else {
                        label.setToolTipText(null);
                    }
                }


                return label;
            }
        });
    }

    public JXTree getJXTree() {
        return tree;
    }

    @Override
    public void entryCreated(CorpusEntry entry) {
        assert false; // Пока что новые входы через gui не создаем
    }

    @Override
    public void entryDeleted(CorpusEntry entry, Collection<CorpusFolder> parentFolders ) {
        for (DefaultMutableTreeNode node : entriesToNodes.get(entry)) {
            model.removeNodeFromParent( node );
        }

        entriesToNodes.remove(entry);

        for (CorpusFolder folder : parentFolders) {
            handleChildReload(folder);
        }
    }

    @Override
    public void entryNameChanged(CorpusEntry entry) {
        Collection<DefaultMutableTreeNode> treeNodes = entriesToNodes.get( entry );
        for (DefaultMutableTreeNode treeNode : treeNodes) {
            model.removeNodeFromParent( treeNode );
        }

        entriesToNodes.remove(entry);

        for (CorpusFolder folder : entry.getParentFolders()) {
            DefaultMutableTreeNode folderNode = foldersToNodes.get( folder );
            int leftbound = folder.getChildFolders().size();
            int newPlace = addChildNode( folderNode, importEntry( entry ), leftbound , leftbound + folder.getEntries().size() - 1 );
            model.nodesWereInserted( folderNode,  new int[] {newPlace} );
        }
    }

    @Override
    public void entryTextChanged(CorpusEntry entry) {
        for (CorpusFolder corpusFolder : entry.getParentFolders()) {
            handleChildReload(corpusFolder);
        }

        Collection<DefaultMutableTreeNode> treeNodes = entriesToNodes.get( entry );
        for (DefaultMutableTreeNode treeNode : treeNodes) {
            model.nodeChanged( treeNode );
        }
    }

    @Override
    public void entryMetadataManuallyEdited(CorpusEntry entry) {
        for (CorpusFolder corpusFolder : entry.getParentFolders()) {
            handleManualEdit(corpusFolder);
        }

        Collection<DefaultMutableTreeNode> treeNodes = entriesToNodes.get( entry );
        for (DefaultMutableTreeNode treeNode : treeNodes) {
            model.nodeChanged( treeNode );
        }
    }

    private void handleManualEdit(CorpusFolder corpusFolder) {
        manuallyEditedFolders.add(corpusFolder);

        if( corpusFolder.getParentFolder() != null) {
            handleManualEdit( corpusFolder.getParentFolder() );
        }

        DefaultMutableTreeNode treeNode = foldersToNodes.get( corpusFolder );
        model.nodeChanged( treeNode );
    }

    @Override
    public void entryMetadataReloaded(CorpusEntry entry) {
        for (CorpusFolder corpusFolder : entry.getParentFolders()) {
            handleChildReload(corpusFolder);
        }

        Collection<DefaultMutableTreeNode> treeNodes = entriesToNodes.get( entry );
        for (DefaultMutableTreeNode treeNode : treeNodes) {
            model.nodeChanged( treeNode );
        }
    }

    private void handleChildReload(CorpusFolder folder) {
        boolean manualEditDetected = false;
        for (CorpusFolder corpusFolder : folder.getChildFolders()) {
            if( manuallyEditedFolders.contains( corpusFolder ) ) {
                manualEditDetected = true;
            }
        }

        for (CorpusEntry corpusEntry : folder.getEntries()) {
            if( corpusEntry.getManualEditionStamp() >= 0 ) {
                manualEditDetected = true;
            }
        }

        if( manualEditDetected ) {
            manuallyEditedFolders.add(folder);
        } else if( manuallyEditedFolders.contains(folder) ){
            manuallyEditedFolders.remove(folder);
        }

        if( folder.getParentFolder() != null) {
            handleChildReload(folder.getParentFolder());
        }
    }

    @Override
    public void folderCreated(CorpusFolder folder) {
        CorpusFolder parentFolder = folder.getParentFolder();

        DefaultMutableTreeNode parentNode;

        if( parentFolder == null ) {
            parentNode = (DefaultMutableTreeNode) model.getRoot();
        } else {
            parentNode = foldersToNodes.get( parentFolder );
        }

        assert parentNode != null;
        assert folder.getChildFolders().isEmpty() && folder.getEntries().isEmpty();

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode();
        newNode.setUserObject(folder);
        foldersToNodes.put( folder, newNode );

        model.nodesWereInserted(parentNode, new int[]{addChildNode(parentNode, newNode, 0,
                parentFolder == null ? corpus.getRootCorpusFolders().size() - 1 : parentFolder.getChildFolders().size() - 1)});
    }

    private int addChildNode(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode childNode, int leftbound, int rightbound ) {
         if( rightbound == leftbound ) {
            parentNode.insert(childNode,leftbound);

            return leftbound;
        }

        DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) parentNode.getChildAt(leftbound);
        if( mutableTreeNodeComparator.compare( childNode, firstNode ) < 0 ) {
            parentNode.insert( childNode, leftbound );
            return leftbound;
        }

        DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) parentNode.getChildAt(rightbound-1);

        if( mutableTreeNodeComparator.compare( childNode, lastNode ) > 0 ) {
            parentNode.insert(childNode,rightbound);
            return rightbound;
        }

        while( rightbound > leftbound ) {
            int middle = leftbound + ( rightbound - leftbound ) / 2;

            DefaultMutableTreeNode middleNode = (DefaultMutableTreeNode) parentNode.getChildAt(middle);

            int cmp = mutableTreeNodeComparator.compare( childNode, middleNode );

            if( cmp == 0 ) {
                parentNode.insert( childNode, middle );
                return middle;
            } else if( cmp < 0 ) {
                rightbound = middle;
            } else {
                leftbound = middle + 1;
            }
        }

        assert leftbound == rightbound;

        parentNode.insert(childNode,leftbound);
        return leftbound;
    }

    @Override
    public void folderNameChanged(CorpusFolder folder) {
        DefaultMutableTreeNode oldFolderNode = foldersToNodes.get( folder );
        TreeNode[] pathArray = model.getPathToRoot(oldFolderNode);
        TreePath path = new TreePath(pathArray);
        boolean wasExpanded = tree.isExpanded(path);
        boolean wasSelected = tree.isPathSelected(path);

        model.removeNodeFromParent( oldFolderNode );
        foldersToNodes.remove(folder);


        CorpusFolder parentFolder = folder.getParentFolder();
        DefaultMutableTreeNode parentNode;
        if( parentFolder == null ) {
            parentNode = (DefaultMutableTreeNode) model.getRoot();
        } else {
            parentNode = foldersToNodes.get( parentFolder );
        }

        DefaultMutableTreeNode newFolderNode = new DefaultMutableTreeNode( folder );
        foldersToNodes.put( folder, newFolderNode );

        while( oldFolderNode.getChildCount() > 0 ) {
            newFolderNode.add((MutableTreeNode) oldFolderNode.getFirstChild());
        }

        int newPlace = addChildNode( parentNode, newFolderNode, 0, parentFolder == null ?
                corpus.getRootCorpusFolders().size() - 1 : parentFolder.getChildFolders().size() - 1 );
        model.nodesWereInserted( parentNode,  new int[] {newPlace} );

        if( wasExpanded ) {
            pathArray[pathArray.length-1] = newFolderNode;
            tree.expandPath( new TreePath(pathArray) );
        }

        if( wasSelected ) {
            pathArray[pathArray.length-1] = newFolderNode;
            tree.addSelectionPath( new TreePath(pathArray) );
        }
    }

    @Override
    public void folderParentChanged(CorpusFolder folder, CorpusFolder oldParent) {
        DefaultMutableTreeNode folderNode = foldersToNodes.get(folder);
        model.removeNodeFromParent(folderNode);

        CorpusFolder parentFolder = folder.getParentFolder();

        DefaultMutableTreeNode parentNode;

        if( parentFolder == null ) {
            parentNode = (DefaultMutableTreeNode) model.getRoot();
        } else {
            parentNode = foldersToNodes.get( parentFolder );
        }

        assert parentNode != null;

        model.nodesWereInserted(parentNode, new int[]{addChildNode(parentNode, folderNode, 0,
                parentFolder == null ? corpus.getRootCorpusFolders().size() - 1 : parentFolder.getChildFolders().size() - 1)});
    }

    @Override
    public void entryWasPlacedIntoFolder(CorpusEntry entry, CorpusFolder folder) {
        DefaultMutableTreeNode corpusFolderNode = foldersToNodes.get(folder);

        int leftbound = folder.getChildFolders().size();
        int newPlace = addChildNode( corpusFolderNode, importEntry( entry ), leftbound , leftbound + folder.getEntries().size() - 1 );
        model.nodesWereInserted( corpusFolderNode,  new int[] {newPlace} );

        if( entry.getManualEditionStamp() >= 0 ) {
            handleManualEdit( folder );
        }
    }

    @Override
    public void entryWasRemovedFromFolder(CorpusEntry entry, CorpusFolder folder) {
        for (DefaultMutableTreeNode node : entriesToNodes.get(entry)) {
            if( ((DefaultMutableTreeNode)node.getParent()).getUserObject() == folder ) {
                model.removeNodeFromParent( node );
                entriesToNodes.get(entry).remove(node);
                break;
            }
        }

        handleChildReload(folder);
    }

    @Override
    public void folderDeleted(CorpusFolder folder) {
        DefaultMutableTreeNode treeNode = foldersToNodes.get(folder);
        model.removeNodeFromParent( treeNode );
        foldersToNodes.remove( folder );
    }

    @Override
    public void corpusLabelChanged() {
        Component parent = tree.getParent();
        while( parent != null ) {
            if( parent instanceof JInternalFrame ) {
                ((JInternalFrame)parent).setTitle( ((JInternalFrame)parent).getTitle() + ": " + corpus.getCorpusLabel() );
                parent.revalidate();
                break;
            }

            parent = parent.getParent();
        }

    }

    @Override
    public void globalCorpusPropertyChanged(String propertyName) {
        // TODO не факт, что здесь что-то надо делать
    }

    public void onClose() {
        corpus.removeListener( this );
    }

    // TODO scroll, select, find

    EntryTooltipProvider entryTooltipProvider;

    public void setEntryTooltipProvider(EntryTooltipProvider entryTooltipProvider) {
        this.entryTooltipProvider = entryTooltipProvider;
    }

    public interface EntryTooltipProvider {
        String getTooltip( CorpusEntry entry );
    }
}
