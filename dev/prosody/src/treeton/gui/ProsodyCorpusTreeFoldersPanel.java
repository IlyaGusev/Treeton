/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import org.jdesktop.swingx.JXTree;
import treeton.prosody.corpus.Corpus;
import treeton.prosody.corpus.CorpusFolder;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ProsodyCorpusTreeFoldersPanel {
    private JXTree tree = new JXTree();
    private DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
    private Map<CorpusFolder, DefaultMutableTreeNode> foldersToNodes = new HashMap<CorpusFolder, DefaultMutableTreeNode>();
    private Corpus corpus;

    public void reload() {
        model.reload();
    }

    public ProsodyCorpusTreeFoldersPanel(Corpus corpus) {
        this.corpus = corpus;
        importCorpus();
        initGui();
        tree.expandAll();
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {

            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                throw new ExpandVetoException(event);
            }
        });
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

        ArrayList<DefaultMutableTreeNode> childFolderNodes = new ArrayList<DefaultMutableTreeNode>();
        for (CorpusFolder corpusFolder : folder.getChildFolders()) {
            DefaultMutableTreeNode childNode = importFolder( corpusFolder);
            childFolderNodes.add( childNode );
        }

        childFolderNodes.sort(mutableTreeNodeComparator);

        for (DefaultMutableTreeNode folderNode : childFolderNodes) {
            result.add( folderNode );
        }

        return result;
    }


    private void initGui(){
        ToolTipManager.sharedInstance().registerComponent(tree);

        tree.setRootVisible(false);
        tree.expandPath(new TreePath(model.getRoot()));

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

                if( node == null ) {
                    return label;
                }

                label.setIcon( (expanded || leaf) ? getDefaultOpenIcon() : getDefaultClosedIcon() );

                return label;
            }
        });
    }

    public JXTree getJXTree() {
        return tree;
    }

}
