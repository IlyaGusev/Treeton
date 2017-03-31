/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.metricsearch.andortree;

import org.jdesktop.swingx.JXTree;
import treeton.gui.GuiResources;
import treeton.gui.metricsearch.MetricSearchCriteria;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;

public class AndOrTreePanel<T> {
    private JXTree tree = new JXTree();
    private DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
    private Icon userObjectIcon;
    private Icon operatorIcon;
    private Map<OrOperator<T>,DefaultMutableTreeNode> orNodes = new HashMap<>();
    private Map<AndOperator<T>,DefaultMutableTreeNode> andNodes = new HashMap<>();
    private Map<T,DefaultMutableTreeNode> leaves = new HashMap<>();
    private Set<T> invalidLeaves = new HashSet<>();
    private final AndOperator<T> root;

    public JXTree getTree() {
        return tree;
    }

    public void reload() {
        model.reload();
    }

    public AndOrTreePanel() {
        initGui();
        root = new AndOperator<T>();
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode();
        treeNode.setUserObject(root);
        andNodes.put(root,treeNode);
        model.setRoot( treeNode );
    }

    Comparator<DefaultMutableTreeNode> mutableTreeNodeComparator = (o1, o2) -> {
        boolean firstIsOperator = o1.getUserObject() instanceof AndOperator ||
                o1.getUserObject() instanceof OrOperator;
        boolean secondIsOperator = o1.getUserObject() instanceof AndOperator ||
                o1.getUserObject() instanceof OrOperator;

        if( firstIsOperator == secondIsOperator ) {
            return o1.getUserObject().toString().compareTo(o2.getUserObject().toString());
        } else {
            return firstIsOperator ? -1 : 1;
        }
    };


    private void initGui(){
        ToolTipManager.sharedInstance().registerComponent(tree);

        tree.setRootVisible(true);
        tree.expandPath(new TreePath(model.getRoot()));

        userObjectIcon = GuiResources.getImageIcon("anns.gif");
        operatorIcon = GuiResources.getImageIcon("none.gif");

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                label.setBackground(Color.WHITE);

                if( node == null ) {
                    return label;
                }

                if( node.getUserObject() instanceof AndOperator ) {
                    AndOperator op = (AndOperator) node.getUserObject();
                    label.setText(op.negated ? "^and" : "and");
                    label.setIcon(operatorIcon);

                    if( op.getNumberOfUserObjects() + op.getNumberOfChildOperators() == 0 ) {
                        label.setForeground(Color.PINK);
                    }
                } else if( node.getUserObject() instanceof OrOperator ) {
                    OrOperator op = (OrOperator) node.getUserObject();
                    label.setText(op.negated ? "^or" : "or");
                    label.setIcon(operatorIcon);

                    if( op.getNumberOfUserObjects() + op.getNumberOfChildOperators() == 0 ) {
                        label.setForeground(Color.PINK);
                    }
                } else {
                    label.setIcon(userObjectIcon);

                    //noinspection unchecked
                    if( invalidLeaves.contains( (T) node.getUserObject() ) ) {
                        label.setForeground(Color.PINK);
                    }

                }

                return label;
            }
        });
    }

    public void addAndOperator( OrOperator<T> parentOperator ) {
        DefaultMutableTreeNode orNode = orNodes.get( parentOperator );
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode();
        AndOperator<T> childOperator = new AndOperator<>();
        childNode.setUserObject(childOperator);
        andNodes.put(childOperator,childNode);
        parentOperator.andOperators.add(childOperator);

        int newPlace = addChildNode(orNode,childNode,parentOperator.getNumberOfUserObjects(),parentOperator.getNumberOfUserObjects()+parentOperator.getNumberOfChildOperators() - 1);
        model.nodesWereInserted( orNode,  new int[] {newPlace} );
    }

    public void addOrOperator( AndOperator<T> parentOperator ) {
        DefaultMutableTreeNode andNode = andNodes.get( parentOperator );
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode();
        OrOperator<T> childOperator = new OrOperator<>();
        childNode.setUserObject(childOperator);
        orNodes.put(childOperator,childNode);
        parentOperator.orOperators.add(childOperator);

        int newPlace = addChildNode(andNode,childNode,parentOperator.getNumberOfUserObjects(),parentOperator.getNumberOfUserObjects()+parentOperator.getNumberOfChildOperators() - 1);
        model.nodesWereInserted( andNode,  new int[] {newPlace} );
    }

    @SuppressWarnings("unchecked")
    public void negateOperator(Object userObject) {
        if( userObject instanceof AndOperator ) {
            AndOperator<T> andOperator = (AndOperator<T>) userObject;
            andOperator.negated = !andOperator.negated;
            model.reload(andNodes.get(andOperator));
        } else if( userObject instanceof OrOperator ) {
            OrOperator<T> orOperator = (OrOperator<T>) userObject;
            orOperator.negated = !orOperator.negated;
            model.reload(orNodes.get(orOperator));
        } else {
            assert false;
        }

    }

    @SuppressWarnings("unchecked")
    public void addLeaf(Object parent, T userObject) {
        if( parent instanceof OrOperator ) {
            OrOperator<T> parentOperator = (OrOperator<T>) parent;
            DefaultMutableTreeNode orNode = orNodes.get( parentOperator );
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode();
            childNode.setUserObject(userObject);
            leaves.put(userObject,childNode);
            parentOperator.userObjects.add(userObject);
            int newPlace = addChildNode(orNode,childNode,0,parentOperator.getNumberOfUserObjects() - 1);
            model.nodesWereInserted( orNode,  new int[] {newPlace} );
        } else if( parent instanceof  AndOperator ) {
            AndOperator<T> parentOperator = (AndOperator<T>) parent;
            DefaultMutableTreeNode andNode = andNodes.get( parentOperator );
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode();
            childNode.setUserObject(userObject);
            parentOperator.userObjects.add(userObject);
            leaves.put(userObject,childNode);
            int newPlace = addChildNode(andNode,childNode,0,parentOperator.getNumberOfUserObjects() - 1);
            model.nodesWereInserted( andNode,  new int[] {newPlace} );
        } else {
            assert false;
        }
    }

    public void leafChanged( T userObject, boolean isValid ) {
        DefaultMutableTreeNode leafNode = leaves.get(userObject);
        assert leafNode != null;

        if( isValid ) {
            invalidLeaves.remove(userObject);
        } else {
            invalidLeaves.add(userObject);
        }

        model.reload(leafNode);
    }

    public void remove( Object userObject ) {
        // TODO
    }

    /*DefaultMutableTreeNode folderNode = foldersToNodes.get( folder );
    int leftbound = folder.getChildFolders().size();
    int newPlace = addChildNode( folderNode, importEntry( entry ), leftbound , leftbound + folder.getEntries().size() - 1 );
    model.nodesWereInserted( folderNode,  new int[] {newPlace} );*/
    //for (DefaultMutableTreeNode treeNode : treeNodes) {
    //    model.nodeChanged( treeNode );
    //}
    //DefaultMutableTreeNode treeNode = foldersToNodes.get(folder);
    //model.removeNodeFromParent( treeNode );

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

    public AndOperator<T> getRootOperator() {
        return root;
    }
}
