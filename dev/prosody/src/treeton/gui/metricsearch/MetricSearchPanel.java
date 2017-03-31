/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.metricsearch;

import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.gui.metricsearch.andortree.AndOperator;
import treeton.gui.metricsearch.andortree.AndOrTreePanel;
import treeton.gui.metricsearch.andortree.OrOperator;
import treeton.prosody.corpus.Corpus;
import treeton.prosody.corpus.CorpusEntry;
import treeton.prosody.corpus.CorpusException;
import treeton.prosody.corpus.CorpusFolder;
import treeton.prosody.metricindex.MetricIndex;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class MetricSearchPanel extends JPanel implements ActionListener, MetricSearchCriteriaPanelListener {
    private final Corpus corpus;
    private final MetricIndex metricIndex;
    private final String add_rhytmic_search_criteria = "ADD_RHYTMIC_SEARCH_CRITERIA";
    private final String add_metainfo_search_criteria = "ADD_METAINFO_SEARCH_CRITERIA";
    private final String add_or_node = "ADD_OR_NODE";
    private final String add_and_node = "ADD_AND_NODE";
    private final String remove_node = "REMOVE_NODE";
    private final String negate_node = "NEGATE_NODE";
    private final String searchCommand = "SEARCH";
    private AndOrTreePanel<MetricSearchCriteria> complexSearchCriteria;
    private MetricSearchCriteriaPanel metricSearchCriteriaPanel;
    private Object lastPathComponent;
    private JSplitPane splitPane;
    private JButton searchButton;
    private Collection<CorpusFolder> selectedFolders;
    private CorpusFolder targetFolder;

    public MetricSearchPanel(Corpus corpus, MetricIndex metricIndex, Collection<CorpusFolder> selectedFolders, CorpusFolder targetFolder) throws TreetonModelException {
        this.corpus = corpus;
        this.metricIndex = metricIndex;
        complexSearchCriteria = new AndOrTreePanel<>();
        this.selectedFolders = selectedFolders;
        this.targetFolder = targetFolder;
        init();

        complexSearchCriteria.getTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                lastPathComponent = null;
                if( e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON3 ) {
                    TreePath path = complexSearchCriteria.getTree().getPathForLocation(e.getX(), e.getY());

                    if( path != null && path.getPathCount() > 0 ) {
                        lastPathComponent = path.getLastPathComponent();
                        Object userObject = ((DefaultMutableTreeNode)lastPathComponent).getUserObject();

                        JPopupMenu popup = new JPopupMenu();

                        JMenuItem menuItem;
                        if( userObject instanceof AndOperator  ||
                                userObject instanceof OrOperator ) {

                            JMenu addCriteriaMenu = new JMenu("Добавить ограничение");
                            JMenuItem subItem = new JMenuItem("ритмико-метрическое");
                            subItem.setActionCommand(add_rhytmic_search_criteria);
                            subItem.addActionListener(MetricSearchPanel.this);
                            addCriteriaMenu.add(subItem);
                            TrnType trnType;
                            try {
                                trnType = corpus.getTrnContext().getType("CorpusElement");
                                int numberOfSystemFeatures = TrnType.getNumberOfSystemFeatures();

                                for( int i = numberOfSystemFeatures; i < trnType.getFeaturesSize(); i++ ) {
                                    subItem = new JMenuItem("на свойство "+trnType.getFeatureNameByIndex(i));
                                    subItem.setActionCommand(add_metainfo_search_criteria+":"+i);
                                    subItem.addActionListener(MetricSearchPanel.this);
                                    addCriteriaMenu.add(subItem);
                                }

                            } catch (TreetonModelException e1) {
                                e1.printStackTrace();
                            }

                            popup.add(addCriteriaMenu);
                        }

                        if( userObject instanceof AndOperator ) {
                            menuItem = new JMenuItem("Добавить or-узел");
                            menuItem.setActionCommand(add_or_node);
                            menuItem.addActionListener(MetricSearchPanel.this);
                            popup.add(menuItem);
                        }

                        if( userObject instanceof OrOperator) {
                            menuItem = new JMenuItem("Добавить and-узел");
                            menuItem.setActionCommand(add_and_node);
                            menuItem.addActionListener(MetricSearchPanel.this);
                            popup.add(menuItem);
                        }

                        menuItem = new JMenuItem("Отрицание");
                        menuItem.setActionCommand(negate_node);
                        menuItem.addActionListener(MetricSearchPanel.this);
                        popup.add(menuItem);

                        if( path.getPathCount() > 1 ) {
                            menuItem = new JMenuItem("Удалить");
                            menuItem.setActionCommand(remove_node);
                            menuItem.addActionListener(MetricSearchPanel.this);
                            popup.add(menuItem);
                        }

                        popup.show(complexSearchCriteria.getTree(), e.getX(), e.getY());
                    }
                } else if( e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 ) {
                    TreePath path = complexSearchCriteria.getTree().getPathForLocation(e.getX(), e.getY());

                    if( path != null && path.getPathCount() > 0 ) {
                        Object userObject = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
                        if( userObject instanceof MetricSearchCriteria ) {
                            splitPane.remove(splitPane.getRightComponent());
                            JPanel fakePanel = new JPanel();
                            fakePanel.setLayout(new BorderLayout());
                            try {
                                metricSearchCriteriaPanel = new MetricSearchCriteriaPanel((MetricSearchCriteria)userObject,MetricSearchPanel.this);
                                fakePanel.add(metricSearchCriteriaPanel,BorderLayout.PAGE_START);
                            } catch (TreetonModelException e1) {
                                e1.printStackTrace();
                            }
                            splitPane.setRightComponent(fakePanel);
                            splitPane.revalidate();
                        }
                    }
                }
            }
        });

        /*complexSearchCriteria.getTree().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath[] selectionPaths = complexSearchCriteria.getTree().getSelectionPaths();
                Object userObject = null;
                if (selectionPaths != null && selectionPaths.length == 1) {
                    TreePath selectionPath = selectionPaths[0];
                    userObject = ((DefaultMutableTreeNode)
                            selectionPath.getLastPathComponent()).getUserObject();
                }

                doNodeSelected( userObject );
            }
        });*/

        validateButtons();
    }

    private void init() throws TreetonModelException {
        this.setLayout(new BorderLayout());
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOneTouchExpandable(true);

        JScrollPane scrollPane = new JScrollPane(complexSearchCriteria.getTree());
        scrollPane.setMinimumSize(new Dimension(300, 200));
        scrollPane.setPreferredSize(new Dimension(600, 400));
        splitPane.setLeftComponent(scrollPane);

        metricSearchCriteriaPanel = new MetricSearchCriteriaPanel(null,this);
        JPanel fakePanel = new JPanel();
        fakePanel.setLayout(new BorderLayout());
        fakePanel.add(metricSearchCriteriaPanel,BorderLayout.PAGE_START);

        splitPane.setRightComponent(fakePanel);
        add(splitPane, BorderLayout.NORTH);

        searchButton = new JButton("Искать");
        searchButton.setActionCommand(searchCommand);
        searchButton.addActionListener(this);

        fakePanel = new JPanel();
        fakePanel.setLayout(new BorderLayout());
        fakePanel.add(searchButton,BorderLayout.EAST);
        fakePanel.add(new JLabel("Целевая папка: "+targetFolder.getLabel()),BorderLayout.WEST);

        add(fakePanel,BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch(command) {
            case searchCommand: {
                try {
                    doSearch();
                } catch (CorpusException e1) {
                    e1.printStackTrace();
                }
                break;
            }
            case add_rhytmic_search_criteria: {
                MainMetricSearchCriteria criteria = new MainMetricSearchCriteria(metricIndex);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastPathComponent;

                if( node == null ) {
                    return;
                }

                complexSearchCriteria.addLeaf(node.getUserObject(), criteria);
                break;
            }
            case add_or_node: {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastPathComponent;

                if( node == null ) {
                    return;
                }
                //noinspection unchecked
                complexSearchCriteria.addOrOperator((AndOperator<MetricSearchCriteria>) node.getUserObject());
                break;
            }
            case add_and_node: {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastPathComponent;

                if( node == null ) {
                    return;
                }
                //noinspection unchecked
                complexSearchCriteria.addAndOperator((OrOperator<MetricSearchCriteria>) node.getUserObject());
                break;
            }
            case remove_node: {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastPathComponent;

                if (node == null) {
                    return;
                }
                complexSearchCriteria.remove(node.getUserObject());
                break;
            }
            case negate_node: {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastPathComponent;

                if (node == null) {
                    return;
                }

                if (node.getUserObject() instanceof MetricSearchCriteria) {
                    MetricSearchCriteria criteria = (MetricSearchCriteria) node.getUserObject();
                    criteria.setNegated(!criteria.isNegated());
                } else {
                    complexSearchCriteria.negateOperator(node.getUserObject());
                }

                break;
            }
            default: {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastPathComponent;

                if( node == null ) {
                    return;
                }

                if( command.startsWith(add_metainfo_search_criteria) ) {
                    int featureIndex = Integer.valueOf(command.substring(command.indexOf(":")+1));
                    TrnType trnType;
                    try {
                        trnType = corpus.getTrnContext().getType("CorpusElement");
                        MetainfoMetricSearchCriteria criteria = new MetainfoMetricSearchCriteria(trnType,featureIndex);

                        complexSearchCriteria.addLeaf(node.getUserObject(), criteria);
                    } catch (TreetonModelException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    assert false;
                }
            }
        }
        complexSearchCriteria.getTree().expandAll();
        validateButtons();
    }

    private void validateButtons() {
        searchButton.setEnabled( isValid( complexSearchCriteria.getRootOperator() ));
    }

    private boolean isValid( AndOperator<MetricSearchCriteria> op ) {
        if( !op.isValid() ) {
            return false;
        }

        ArrayList<MetricSearchCriteria> criteria = new ArrayList<>();

        op.collectUserObjects(criteria);

        for (MetricSearchCriteria metricSearchCriteria : criteria) {
            if( !metricSearchCriteria.isValid() ) {
                return false;
            }
        }

        for (OrOperator<MetricSearchCriteria> orOperator : op) {
            if( !isValid(orOperator) ) {
                return false;
            }
        }

        return true;
    }

    private boolean isValid( OrOperator<MetricSearchCriteria> op ) {
        if( !op.isValid() ) {
            return false;
        }

        ArrayList<MetricSearchCriteria> criteria = new ArrayList<>();

        op.collectUserObjects(criteria);

        for (MetricSearchCriteria metricSearchCriteria : criteria) {
            if( !metricSearchCriteria.isValid() ) {
                return false;
            }
        }

        for (AndOperator<MetricSearchCriteria> andOperator : op) {
            if( !isValid(andOperator) ) {
                return false;
            }
        }

        return true;
    }

    private void doSearch() throws CorpusException {
        HashMap<CorpusEntry,Boolean> searchResult = new HashMap<>();

        for (CorpusFolder folder : selectedFolders) {
            doSearchInFolder( folder, searchResult );
        }


        for (Map.Entry<CorpusEntry, Boolean> entry : searchResult.entrySet()) {
            if( !entry.getValue() ) {
                continue;
            }

            corpus.putEntryIntoFolder( entry.getKey(), targetFolder );
        }
    }

    private void doSearchInFolder( CorpusFolder parentFolder, HashMap<CorpusEntry,Boolean> searchResult ) {
        for (CorpusFolder folder : parentFolder.getChildFolders() ) {
            doSearchInFolder( folder, searchResult );
        }

        for( CorpusEntry corpusEntry : parentFolder.getEntries() ) {
            if( searchResult.containsKey( corpusEntry ) ) {
                continue;
            }

            searchResult.put( corpusEntry, matchSearchCriteria( corpusEntry ) );
        }
    }

    private boolean matchSearchCriteria(CorpusEntry corpusEntry) {
        return match(complexSearchCriteria.getRootOperator(),corpusEntry);
    }

    private boolean match( AndOperator<MetricSearchCriteria> andOp, CorpusEntry corpusEntry) {
        ArrayList<MetricSearchCriteria> atomicCriteria = new ArrayList<>();
        andOp.collectUserObjects(atomicCriteria);

        for (MetricSearchCriteria criteria : atomicCriteria) {
            if( criteria.match(corpusEntry) == criteria.isNegated() ) {
                return false;
            }
        }

        for (OrOperator<MetricSearchCriteria> orOperator : andOp) {
            if( match(orOperator,corpusEntry) == orOperator.isNegated() ) {
                return false;
            }
        }

        return true;
    }

    private boolean match( OrOperator<MetricSearchCriteria> orOp, CorpusEntry corpusEntry) {
        ArrayList<MetricSearchCriteria> atomicCriteria = new ArrayList<>();
        orOp.collectUserObjects(atomicCriteria);

        for (MetricSearchCriteria criteria : atomicCriteria) {
            if( criteria.match(corpusEntry) != criteria.isNegated() ) {
                return true;
            }
        }

        for (AndOperator<MetricSearchCriteria> andOperator : orOp) {
            if( match(andOperator,corpusEntry) != andOperator.isNegated() ) {
                return false;
            }
        }

        return true;
    }


    @Override
    public void metricSearchCriteriaChanged(MetricSearchCriteria criteria) {
        complexSearchCriteria.leafChanged( criteria, criteria.isValid() );

        validateButtons();
    }
}
