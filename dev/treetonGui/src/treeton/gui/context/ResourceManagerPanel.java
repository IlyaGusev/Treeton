/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import org.apache.log4j.Logger;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.*;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.config.context.resources.api.ResourcesContextManager;
import treeton.core.config.context.resources.def.DefaultResourceChainModel;
import treeton.core.config.context.treenotations.TreenotationsContextManager;
import treeton.core.util.ProgressListener;
import treeton.gui.GuiResources;
import treeton.gui.attreditor.AttrPanel;
import treeton.gui.util.ExceptionDialog;
import treeton.gui.util.ReportDialog;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.List;

public class ResourceManagerPanel extends JPanel implements
        ItemListener,
        ActionListener, TreeSelectionListener, TreeModelListener,
        LogListener, ExceptionOccuranceDetector,
        IgnoredDetector, MouseListener {
    private static final Logger logger = Logger.getLogger(ResourceManagerPanel.class);
    static boolean treeton = false;
    private static int MODE_INIT = 0;
    private static int MODE_REINIT = 1;
    private static int MODE_RUN = 2;
    TreenotationsContextManager treenotationsContextManager;
    ResourcesContextManager resourcesContextManager;
    ResourcesContext context;
    JTree domainTree;
    ResourcesContextModel treeModel;
    JComboBox domainCombo;
    JButton transferButton;
    JToolBar instanceTreeToolBar;
    JTree instanceTree;
    InstantiatedResourcesModel instanceModel;

    //JComboBox corpusCombo;
    JSplitPane split;
    AttrPanel attributesPanel;
    JCheckBox printResultCheckBox;
    JButton removeButton;
    JButton shiftUpButton;
    JButton shiftDownButton;
    JButton reinitButton;
    JButton runButton;
    JButton stopButton;
    JButton commentButton;
    JLabel logLabel;
    Map<ResourcesContext, InstantiatedResourcesModel> domains2model = new HashMap<ResourcesContext, InstantiatedResourcesModel>();
    ProgressListener pListener;
    Resource res;
    ResourceChain chain;
    CorpusProvider provider;
    ProcessingThread workingThread;
    ArrayList<Object> toAdd = new ArrayList<Object>();
    HashMap<Resource, Map<String, Object>> res2params = new HashMap<Resource, Map<String, Object>>();
    HashSet<Resource> ignored = new HashSet<Resource>();
    boolean stopping = false;
    boolean firstTime = true;
    ArrayList<ResourceManagerListener> listeners = new ArrayList<ResourceManagerListener>();
    private ResourceFactory factory;
    private CorpusProvider registeredCorpusProvider;
    private Set<Resource> exceptionOccured = new HashSet<Resource>();

    public ResourceManagerPanel(ResourceFactory factory, TreenotationsContextManager treenotationsContextManager, ResourcesContextManager resourcesContextManager) {
        this.factory = factory;
        this.treenotationsContextManager = treenotationsContextManager;
        this.resourcesContextManager = resourcesContextManager;
    }

    public void registerCorpusProvider(CorpusProvider provider) {
        registeredCorpusProvider = provider;
    }

    private void disableAll() {
        //domainTree.setEnabled(false);
        info("");
        domainTree.removeTreeSelectionListener(this);
        domainTree.removeMouseListener(this);
        domainCombo.setEnabled(false);
        domainCombo.removeItemListener(this);
        transferButton.setEnabled(false);
        transferButton.removeActionListener(this);
        instanceTreeToolBar.setEnabled(false);
        int c = instanceTreeToolBar.getComponentCount();
        for (int i = 0; i < c; i++) {
            Component comp = instanceTreeToolBar.getComponentAtIndex(i);
            comp.setEnabled(false);
        }
        //instanceTree.setEnabled(false);
        instanceTree.removeTreeSelectionListener(this);
        attributesPanel.setEnabled(false);
        //split.setEnabled(false);
        //corpusCombo.setEnabled(false);
        //corpusCombo.removeItemListener(this);
    }

    private void enableAll() throws ContextException {
        //domainTree.setEnabled(true);
        info("");
        domainTree.addTreeSelectionListener(this);
        domainTree.addMouseListener(this);
        domainCombo.setEnabled(true);
        domainCombo.addItemListener(this);
        transferButton.setEnabled(true);
        transferButton.addActionListener(this);
        instanceTreeToolBar.setEnabled(true);
        int c = instanceTreeToolBar.getComponentCount();
        for (int i = 0; i < c; i++) {
            Component comp = instanceTreeToolBar.getComponentAtIndex(i);
            comp.setEnabled(true);
        }

        //instanceTree.setEnabled(true);
        instanceTree.addTreeSelectionListener(this);
        //attributesPanel.setEnabled(true);
        //split.setEnabled(true);
        //corpusCombo.setEnabled(true);
        //corpusCombo.addItemListener(this);

        refreshButtons();
    }
    /*HashMap<Resource,Map<String,Object>> initParams = new HashMap<Resource,Map<String,Object>>();*/

    public void init(ProgressListener pListener, boolean enableRunButton) throws ContextException {
        this.pListener = pListener;
        initGuiComponents(enableRunButton);
    }

    public void setPListener(ProgressListener pListener) {
        this.pListener = pListener;
    }

    private void initGuiComponents(boolean enableRunButton) throws ContextException {
        setLayout(new GridBagLayout());
        ResourcesContext rootContext = resourcesContextManager.getRootContext();
        treeModel = createResourcesContextModel(rootContext);
        treeModel.setAncestorsOnly(true);
        treeModel.setTargetContext(context = rootContext);
        domainTree = new JTree(treeModel);
        ToolTipManager.sharedInstance().registerComponent(domainTree);
        domainTree.setCellRenderer(createResourcesContextTreeCellRenderer());
        JScrollPane leftScroll = new JScrollPane(domainTree);

        transferButton = new JButton(">>");
        transferButton.setActionCommand("TRANSFER");

        add(leftScroll,
                new GridBagConstraints(
                        0, 0, 1, 4, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(2, 2, 2, 1), 0, 0));

        JPanel fake1 = new JPanel();
        JPanel fake2 = new JPanel();

        add(fake1,
                new GridBagConstraints(
                        1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));

        add(transferButton,
                new GridBagConstraints(
                        1, 1, 1, 2, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(10, 10, 10, 10), 0, 0));

        add(fake2,
                new GridBagConstraints(
                        1, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));

        domainCombo = new JComboBox();
        ResourcesContext[] dom = resourcesContextManager.getAllContexts();
        for (ResourcesContext domain : dom) {
            domainCombo.addItem(domain);
        }
        domainCombo.setSelectedItem(context);

        JLabel domainLabel = new JLabel("Домен:");

        JPanel right1 = new JPanel();
        right1.setLayout(new GridBagLayout());

        right1.add(domainLabel,
                new GridBagConstraints(
                        0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                        new Insets(2, 2, 2, 1), 0, 0));
        right1.add(domainCombo,
                new GridBagConstraints(
                        1, 0, 1, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(2, 0, 2, 2), 0, 0));

        add(right1,
                new GridBagConstraints(
                        2, 0, 1, 1, 1.0, 0.0,
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        new Insets(1, 2, 2, 2), 0, 0));

        instanceTreeToolBar = new JToolBar();
        instanceTreeToolBar.setFloatable(false);
        instanceTreeToolBar.setRollover(true);
        instanceTreeToolBar.setBorder(BorderFactory.createEmptyBorder());

        try {
            GuiResources.tbf.addToolBarButton(instanceTreeToolBar,
                    "addChain.gif", "ADD_CHAIN", "Добавить цепочку", this);
            removeButton = GuiResources.tbf.addToolBarButton(instanceTreeToolBar,
                    "delrow.gif", "REMOVE", "Удалить", this);
            reinitButton = GuiResources.tbf.addToolBarButton(instanceTreeToolBar,
                    "refresh.gif", "REINIT", "Реинициализировать", this);
            shiftDownButton = GuiResources.tbf.addToolBarButton(instanceTreeToolBar,
                    "down.gif", "SHIFT_DOWN", "Сдвинуть вниз", this);
            shiftUpButton = GuiResources.tbf.addToolBarButton(instanceTreeToolBar,
                    "up.gif", "SHIFT_UP", "Сдвинуть вверх", this);
            if (enableRunButton) {
                runButton = GuiResources.tbf.addToolBarButton(instanceTreeToolBar,
                        "run.gif", "RUN", "Запустить", this);
            }
            stopButton = GuiResources.tbf.addToolBarButton(instanceTreeToolBar,
                    "stopProcess.gif", "STOP", "Остановить", this);
            commentButton = GuiResources.tbf.addToolBarButton(instanceTreeToolBar,
                    "commentrow.gif", "COMMENT", "Блокировать/Разблокировать", this);
        } catch (MalformedURLException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }

        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new GridBagLayout());

        toolbarPanel.add(instanceTreeToolBar,
                new GridBagConstraints(
                        0, 0, 1, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(2, 2, 2, 2), 0, 0));

        JLabel lbl = new JLabel("Используемые ресурсы");

        toolbarPanel.add(lbl,
                new GridBagConstraints(
                        1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                        new Insets(2, 8, 2, 8), 0, 0));


        instanceModel = new InstantiatedResourcesModel();
        instanceModel.addTreeModelListener(this);
        instanceTree = new JTree(instanceModel) {
            public String getToolTipText(MouseEvent e) {
                Object o;
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    o = path.getLastPathComponent();
                    if (o instanceof Resource && !(o instanceof ResourceChain)) {
                        Resource res = (Resource) o;
                        Map<String, Object> params = res.getInitialParameters();
                        return generateToolTip(res, params, "");
                    } else {
                        return "";
                    }
                }
                return "";
            }

            public boolean isPathEditable(TreePath path) {
                return path.getLastPathComponent() instanceof ResourceChain;
            }
        };
        instanceTree.setEditable(true);
        ToolTipManager.sharedInstance().registerComponent(instanceTree);

        instanceTree.setCellRenderer(new InstantiatedTreeCellRenderer(this, this));
        instanceTree.setCellEditor(new InstantiatedTreeCellEditor(instanceTree, (DefaultTreeCellRenderer) instanceTree.getCellRenderer()));

        domains2model.put(context, instanceModel);

        JScrollPane rightScroll = new JScrollPane(instanceTree);

        JPanel right2 = new JPanel();

        right2.setLayout(new GridBagLayout());

        right2.add(toolbarPanel,
                new GridBagConstraints(
                        0, 0, 1, 1, 1.0, 0.0,
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        new Insets(2, 2, 1, 2), 0, 0));
        right2.add(rightScroll,
                new GridBagConstraints(
                        0, 1, 1, 1, 1.0, 1.0,
                        GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                        new Insets(0, 2, 2, 2), 0, 0));

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.add(right2, JSplitPane.TOP);

        attributesPanel = new AttrPanel();
        attributesPanel.init(null, null);
        //attributesPanel.setEnabled(false);
        attributesPanel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                TreePath[] arr = instanceTree.getSelectionPaths();
                if (arr != null) {
                    if (arr.length == 1) {
                        Object o = arr[0].getLastPathComponent();
                        if (o instanceof Resource && !(o instanceof ResourceChain)) {
                            Resource res = (Resource) o;

                            res2params.put(res, attributesPanel.getData());
                        }
                    }
                }
            }
        });

        split.add(attributesPanel, JSplitPane.BOTTOM);

        add(split,
                new GridBagConstraints(
                        2, 1, 1, 2, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(2, 2, 2, 2), 0, 0));


        //corpusCombo = new JComboBox();

        JPanel cmbPanel = new JPanel();
        cmbPanel.setLayout(new GridBagLayout());
        /*lbl = new JLabel("Выбранный корпус:");

  cmbPanel.add(lbl,
  new GridBagConstraints(
  0, 0, 1, 1, 0.0, 0.0,
  GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
  new Insets(2, 2, 2, 1), 0, 0));
  cmbPanel.add(corpusCombo,
  new GridBagConstraints(
  1, 0, 1, 1, 1.0, 0.0,
  GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
  new Insets(2, 0, 2, 2), 0, 0));*/
        lbl = new JLabel("Печать результата:");
        cmbPanel.add(lbl,
                new GridBagConstraints(
                        0, 0, 3, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                        new Insets(2, 2, 2, 1), 0, 0));
        printResultCheckBox = new JCheckBox();
        printResultCheckBox.setSelected(false);

        cmbPanel.add(printResultCheckBox,
                new GridBagConstraints(
                        3, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                        new Insets(2, 0, 2, 2), 0, 0));

        add(cmbPanel,
                new GridBagConstraints(
                        2, 3, 1, 1, 1.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        new Insets(2, 2, 2, 2), 0, 0));

        logLabel = new JLabel(" ");
        logLabel.setBorder(BorderFactory.createEtchedBorder());
        add(logLabel,
                new GridBagConstraints(
                        0, 4, 3, 1, 1.0, 0.0,
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        new Insets(2, 2, 2, 2), 0, 0));

        enableAll();
    }

    protected ResourcesContextModel createResourcesContextModel(ResourcesContext rootContext) {
        return new ResourcesContextModel(rootContext);
    }

    protected ResourcesContextTreeCellRenderer createResourcesContextTreeCellRenderer() {
        return new ResourcesContextTreeCellRenderer();
    }

    private String generateToolTip(Resource res, Map<String, Object> params, String comments) {
        StringBuffer buf = new StringBuffer();
        buf.append("<html><b>");
        buf.append(safeGetName(res));
        buf.append("</b><br>");
        buf.append(comments);
        buf.append("<i>");
        try {
            buf.append(res.getType().getName());
        } catch (ContextException e) {
            buf.append("Exception!!!");
        }
        buf.append("</i>");
        if (exceptionOccured.contains(res)) {
            buf.append("<p><font color=\"#FF0000\">");
            buf.append("Во время последней инициализации произошла ошибка!<br>Этот ресурс может работать некорректно");
            buf.append("</font></p>");
        }
        appendParams(params, buf, "Параметры:");
        buf.append("</html>");
        return buf.toString();
    }

    private void appendParams(Map<String, Object> params, StringBuffer buf, String title) {
        if (params.size() > 0) {
            buf.append("<br><b>");
            buf.append(title);
            buf.append("</b><br>");
            for (Map.Entry<String, Object> en : params.entrySet()) {
                buf.append("&nbsp;&nbsp;");
                buf.append(en.getKey());
                buf.append("&nbsp;=&nbsp;");
                if (en.getValue() == null) {
                    buf.append("null");
                } else {
                    if (en.getValue() instanceof List) {
                        buf.append("[<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                        List<Object> l = (List<Object>) en.getValue();
                        for (int i = 0; i < l.size(); i++) {
                            Object o = l.get(i);
                            if (o == null) {
                                buf.append("null");
                            } else {
                                buf.append(o.toString());
                            }
                            if (i < l.size() - 1) {
                                buf.append(",<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                            }
                        }
                        buf.append("<br>&nbsp;&nbsp;]");
                    } else {
                        buf.append(en.getValue().toString());
                    }
                }
                buf.append("<br>");
            }
        }
    }

    public void itemStateChanged(ItemEvent e) {
        try {
            if (e.getSource() == domainCombo) {
                ResourcesContext d = (ResourcesContext) domainCombo.getSelectedItem();
                if (!d.equals(context)) {
                    context = d;
                    treeModel.setTargetContext(context);
                    domainTree.setSelectionPath(null);
                    domainTree.updateUI();
                    if (treeModel.getChildCount(d) == 0) {
                        domainTree.makeVisible(new TreePath(treeModel.pathForObject(d)));
                    } else {
                        domainTree.makeVisible(new TreePath(treeModel.pathForObject(treeModel.getChild(d, 0))));
                    }

                    if (instanceModel != null) {
                        instanceModel.removeTreeModelListener(this);
                    }
                    instanceModel = domains2model.get(context);
                    if (instanceModel == null) {
                        instanceModel = new InstantiatedResourcesModel();
                        domains2model.put(context, instanceModel);
                    }
                    instanceModel.addTreeModelListener(this);
                    instanceTree.setModel(instanceModel);
                    instanceTree.setSelectionPath(null);
                    instanceTree.updateUI();
                    refreshButtons();
                    notifySwitchDomain();
                }
            }
        } catch (Exception ex) {
            ExceptionDialog.showExceptionDialog(this, ex);
        }
    }

    public boolean exceptionOccured(Resource res) {
        return exceptionOccured.contains(res);
    }

    /*public void corpusesChanged() {
     Object o = corpusCombo.getSelectedItem();
     corpusCombo.getModel().removeListDataListener(corpusCombo);
     corpusCombo.removeAllItems();
     corpusCombo.getModel().addListDataListener(corpusCombo);
     for (Corpus c : plugin.getCorpuses()) {
       corpusCombo.addItem(new CorpusWrapper(c));
     }
     if (o != null)
       corpusCombo.setSelectedItem(o);

     if (!stopButton.isEnabled()) {
       refreshButtons();
     }
   } */

    public boolean isIgnored(Resource res) {
        return ignored.contains(res);
    }

    public void deinit() {
        if (stopButton != null) {
            if (stopButton.isEnabled()) {
                synchronized (this) {
                    if ((res != null || chain != null) && !stopping) {
                        stopping = true;
                        if (res != null) {
                            ResourceManagerPanel.this.info("Производится попытка корректно остановить ресурс " + safeGetName(res));
                            res.startStopping();
                        } else {
                            chain.startStopping();
                        }
                    }
                }
                try {
                    Thread.sleep(1500);
                    synchronized (ResourceManagerPanel.this) {
                        if (
                                res != null && isWorking(res) ||
                                        chain != null && isWorking(chain)
                                ) {
                            workingThread.stopSelf();
                            if (res != null) {
                                ResourceManagerPanel.this.info("Работа ресурса " + safeGetName(res) + " была некорректно остановлена");
                                res.recoverAfterProcessTermination();
                                res = null;
                            } else {
                                ResourceManagerPanel.this.info("Работа цепочки ресурсов " + safeGetName(chain) + " была некорректно остановлена");
                                chain.recoverAfterProcessTermination();
                                chain.removeLogListener(ResourceManagerPanel.this);
                                notifyFinishChain(context, chain);
                                chain = null;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    //do nothing
                } catch (Exception e) {
                    ExceptionDialog.showExceptionDialog(this, e);
                } finally {
                    synchronized (ResourceManagerPanel.this) {
                        stopping = false;
                    }
                }
            }
            for (Resource res : res2params.keySet()) {
                ResourceManagerPanel.this.info("Удаляется ресурс " + safeGetName(res));
                res.deInitialize();
                ResourceManagerPanel.this.info("Готово");
            }
            stopButton = null;
        }
    }

    private String safeGetName(Resource res) {
        try {
            return res.getName();
        } catch (ContextException e) {
            return "Exception during getting name";
        }
    }

    private boolean isWorking(Resource res) {
        return res.getStatus() == ResourceStatus.INITIALIZING || res.getStatus() == ResourceStatus.WORKING || res.getStatus() == ResourceStatus.STOPPING;
    }

    public void logMemoryStat() {
        long total = Runtime.getRuntime().totalMemory();
        long used = total - Runtime.getRuntime().freeMemory();
        info(Long.toString(used >> 20) + "Mb / " + Long.toString(total >> 20) + "Mb");
    }

    public void treeNodesChanged(TreeModelEvent e) {
        if (e.getSource() == instanceModel) {
            if (e.getTreePath().getLastPathComponent() instanceof ResourceChain) {
                notifyRenameChain(context, (ResourceChain) e.getTreePath().getLastPathComponent());
            }
        }
    }

    public void treeNodesInserted(TreeModelEvent e) {
    }

    public void treeNodesRemoved(TreeModelEvent e) {
    }

    public void treeStructureChanged(TreeModelEvent e) {
    }

    public void stopProcess() {
        doStop();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public boolean isPrintResultEnabled() {
        return printResultCheckBox.isSelected();
    }

    private boolean isReady(Resource res) {
        return res.getStatus() == ResourceStatus.READY_TO_WORK;
    }

    public boolean launch(ResourcesContext dom, Resource res, CorpusProvider provider) {
        if (res == null || dom == null)
            return false;
        InstantiatedResourcesModel m = domains2model.get(dom);
        if (m == null) {
            throw new IllegalArgumentException("Wrong domain: " + dom);
        }

        if (res instanceof ResourceChain) {
            chain = (ResourceChain) res;
        } else {
            chain = new ResourceChain(factory);
            chain.setResourceModel(new DefaultResourceChainModel("__internal__", dom));
            chain.addResource(res);
        }
        this.provider = provider;

        doRun();
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            if (e.getActionCommand().equals("TRANSFER")) {
                disableAll();
                stopButton.setEnabled(true);
                (workingThread = new ProcessingThread(domainTree.getSelectionPaths(), MODE_INIT)).start();
                /*} else if (e.getActionCommand().equals("RUN")) {
                doRun();*/
            } else if (e.getActionCommand().equals("STOP")) {
                doStop();
            } else if (e.getActionCommand().equals("ADD_CHAIN")) {
                ResourceChain chain = new ResourceChain(factory);
                chain.setResourceModel(new DefaultResourceChainModel("New_Resource_Chain", context));
                chain.initialize(treenotationsContextManager.get(ContextUtil.getFullName(context)));
                chain.setIgnoredDetector(this);
                instanceModel.addResourceChain(chain, instanceModel.getChildCount(instanceModel));
                instanceTree.updateUI();
                instanceTree.expandPath(new TreePath(instanceModel.pathForObject(chain)));
                notifyAddChain(context, chain);
            } else if (e.getActionCommand().equals("SHIFT_DOWN")) {
                TreePath[] arr = instanceTree.getSelectionPaths();
                if (arr != null) {
                    Arrays.sort(arr, new Comparator<TreePath>() {
                        public int compare(TreePath o1, TreePath o2) {
                            return instanceTree.getRowForPath(o1) - instanceTree.getRowForPath(o2);
                        }
                    });
                    for (TreePath treePath : arr) {
                        if (!instanceModel.mayBeShiftedDown(treePath))
                            return;
                    }
                    for (int j = arr.length - 1; j >= 0; j--) {
                        instanceModel.shiftObjectDown(arr[j]);
                    }
                }
                instanceTree.setSelectionPath(null);
                if (arr != null) {
                    for (TreePath treePath : arr) {
                        TreePath newPath = new TreePath(instanceModel.pathForObject(treePath.getLastPathComponent()));
                        instanceTree.addSelectionPath(newPath);
                        instanceTree.expandPath(newPath);
                    }
                }
                instanceTree.updateUI();
            } else if (e.getActionCommand().equals("SHIFT_UP")) {
                TreePath[] arr = instanceTree.getSelectionPaths();
                if (arr != null) {
                    Arrays.sort(arr, new Comparator<TreePath>() {
                        public int compare(TreePath o1, TreePath o2) {
                            return instanceTree.getRowForPath(o1) - instanceTree.getRowForPath(o2);
                        }
                    });
                    for (TreePath treePath : arr) {
                        if (!instanceModel.mayBeShiftedUp(treePath))
                            return;
                    }
                    for (TreePath treePath : arr) {
                        instanceModel.shiftObjectUp(treePath);
                    }
                }
                instanceTree.setSelectionPath(null);
                if (arr != null) {
                    for (TreePath treePath : arr) {
                        TreePath newPath = new TreePath(instanceModel.pathForObject(treePath.getLastPathComponent()));
                        instanceTree.addSelectionPath(newPath);
                        instanceTree.expandPath(newPath);
                    }
                }
                instanceTree.updateUI();
            } else if (e.getActionCommand().equals("REINIT")) {
                disableAll();
                stopButton.setEnabled(true);
                (workingThread = new ProcessingThread(instanceTree.getSelectionPaths(), MODE_REINIT)).start();
            } else if (e.getActionCommand().equals("COMMENT")) {
                HashSet<Resource> resources = new HashSet<Resource>();
                for (TreePath treePath : instanceTree.getSelectionPaths()) {
                    Object o = treePath.getLastPathComponent();
                    if (o instanceof ResourceChain) {
                        ResourceChain ch = (ResourceChain) o;
                        for (int i = 0; i < ch.getNumberOfResources(); i++) {
                            resources.add(ch.getResource(i));
                        }
                    } else if (o instanceof Resource) {
                        resources.add((Resource) o);
                    }
                }
                for (Resource r : resources) {
                    if (ignored.contains(r)) {
                        ignored.remove(r);
                    } else {
                        ignored.add(r);
                    }
                }
                instanceTree.updateUI();
            } else if (e.getActionCommand().equals("RUN")) {
                launchCurrent(registeredCorpusProvider);
            } else if (e.getActionCommand().equals("REMOVE")) {
                TreePath[] arr = instanceTree.getSelectionPaths();
                if (arr != null) {
                    Arrays.sort(arr, new Comparator<TreePath>() {
                        public int compare(TreePath o1, TreePath o2) {
                            return instanceTree.getRowForPath(o1) - instanceTree.getRowForPath(o2);
                        }
                    });
                    for (TreePath p : arr) {
                        Object o = p.getLastPathComponent();
                        if (o instanceof ResourceChain) {
                            ResourceChain ch = (ResourceChain) o;

                            for (int i = 0; i < ch.getNumberOfResources(); i++) {
                                Resource res = ch.getResource(i);
                                res2params.remove(res);
                                exceptionOccured.remove(res);
                            }

                            ch.deInitialize();

                            ResourceManagerPanel.this.info("Удаляется цепочка ресурсов " + safeGetName(ch));
                            instanceModel.removeChain(ch);
                            notifyRemoveChain(context, ch);
                            ResourceManagerPanel.this.info("Готово");
                        } else if (o instanceof Resource) {
                            Resource res = (Resource) o;
                            ResourceManagerPanel.this.info("Удаляется ресурс " + safeGetName(res));
                            if (res2params.containsKey(res)) {
                                res.deInitialize();
                                res2params.remove(res);
                                exceptionOccured.remove(res);
                            }
                            instanceModel.removeResource(res);
                            ResourceManagerPanel.this.logMemoryStat();
                            ResourceManagerPanel.this.info("Готово");
                        }
                    }
                    instanceTree.updateUI();
                }
            }
        } catch (Exception ex) {
            ExceptionDialog.showExceptionDialog(this, ex);
        }
    }

    private void doRun() {
        disableAll();
        stopButton.setEnabled(true);
        (workingThread = new ProcessingThread(instanceTree.getSelectionPaths(), MODE_RUN)).start();
        notifyStartChain(context, chain);
        /*try {
         workingThread.join();
       } catch (InterruptedException e) {
         notifyFinishChain(context,chain);
       } */
    }

    private void doStop() {
        synchronized (this) {
            if ((res != null || chain != null) && !stopping) {
                stopping = true;
                if (res != null) {
                    ResourceManagerPanel.this.info("Производится попытка корректно остановить ресурс " + safeGetName(res));
                    res.startStopping();
                } else {
                    chain.startStopping();
                }
            }
        }
        Runnable rnb = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1500);
                    synchronized (ResourceManagerPanel.this) {
                        if (
                                res != null && isWorking(res) ||
                                        chain != null && isWorking(chain)
                                ) {
                            workingThread.stopSelf();
                            if (res != null) {
                                ResourceManagerPanel.this.info("Работа ресурса " + safeGetName(res) + " была некорректно остановлена");
                                res.recoverAfterProcessTermination();
                                if (workingThread.mode == MODE_REINIT)
                                    exceptionOccured.add(res);
                                res = null;
                            } else {
                                ResourceManagerPanel.this.info("Работа цепочки ресурсов " + safeGetName(chain) + " была некорректно остановлена");
                                chain.recoverAfterProcessTermination();
                                chain.removeLogListener(ResourceManagerPanel.this);
                                notifyFinishChain(context, chain);
                                chain = null;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    //do nothing
                } catch (Exception e) {
                    ExceptionDialog.showExceptionDialog(ResourceManagerPanel.this, e);
                } finally {
                    synchronized (ResourceManagerPanel.this) {
                        stopping = false;
                    }
                }
            }
        };
        (new Thread(rnb)).start();
    }

    public void valueChanged(TreeSelectionEvent e) {
        try {
            refreshButtons();
        } catch (Exception ex) {
            ExceptionDialog.showExceptionDialog(this, ex);
        }
    }

    public void refreshButtons() throws ContextException {
        boolean found = false;
        TreePath[] arr = domainTree.getSelectionPaths();
        if (arr != null) {
            Arrays.sort(arr, new Comparator<TreePath>() {
                public int compare(TreePath o1, TreePath o2) {
                    return instanceTree.getRowForPath(o1) - instanceTree.getRowForPath(o2);
                }
            });

            for (TreePath treePath : arr) {
                if (treePath.getLastPathComponent() instanceof Object[]) {
                    Object[] elem = (Object[]) treePath.getLastPathComponent();
                    if (elem.length > 2) {
                        found = true;
                        break;
                    }
                } else {
                    found = true;
                    break;
                }
            }
        }
        if (arr != null && arr.length > 0 && !found) {
            transferButton.setEnabled(true);
        } else {
            transferButton.setEnabled(false);
        }

        arr = instanceTree.getSelectionPaths();
        attributesPanel.init(null, null);
        attributesPanel.setEnabled(false);
        if (arr != null && arr.length == 1) {
            Object o = arr[0].getLastPathComponent();
            if (o instanceof Resource && !(o instanceof ResourceChain)) {
                Resource res = (Resource) o;
                try {
                    attributesPanel.init(res.getType().getSignature(), res2params.get(res));
                    attributesPanel.setEnabled(true);
                } catch (Exception e1) {
                    ExceptionDialog.showExceptionDialog(this, e1);
                }
            }
        }

        if (runButton != null) {
            if (registeredCorpusProvider.contentSize() > 0 && arr != null && arr.length == 1 && arr[0].getLastPathComponent() != instanceModel) {
                runButton.setEnabled(true);
            } else {
                runButton.setEnabled(false);
            }
        }

        if (stopButton != null)
            stopButton.setEnabled(false);

        if (arr != null && (arr.length > 1 || arr.length == 1 && arr[0].getLastPathComponent() != instanceModel)) {
            reinitButton.setEnabled(true);
            removeButton.setEnabled(true);
            commentButton.setEnabled(true);
        } else {
            reinitButton.setEnabled(false);
            removeButton.setEnabled(false);
            commentButton.setEnabled(false);
        }

        if (arr != null) {
            boolean ok = true;
            for (TreePath treePath : arr) {
                if (!instanceModel.mayBeShiftedDown(treePath)) {
                    ok = false;
                    break;
                }
            }
            shiftDownButton.setEnabled(ok);

            ok = true;
            for (TreePath treePath : arr) {
                if (!instanceModel.mayBeShiftedUp(treePath)) {
                    ok = false;
                    break;
                }
            }

            shiftUpButton.setEnabled(ok);
        } else {
            shiftDownButton.setEnabled(false);
            shiftUpButton.setEnabled(false);
        }

        arr = instanceTree.getSelectionPaths();
        attributesPanel.init(null, null);
        attributesPanel.setEnabled(false);
        if (arr != null && arr.length == 1) {
            Object o = arr[0].getLastPathComponent();
            if (o instanceof Resource && !(o instanceof ResourceChain)) {
                Resource res = (Resource) o;
                try {
                    attributesPanel.init(res.getType().getSignature(), res2params.get(res));
                    attributesPanel.setEnabled(true);
                } catch (Exception e1) {
                    ExceptionDialog.showExceptionDialog(this, e1);
                }
            }
        }
    }

    public void paint(Graphics g) {
        super.paint(g);
        if (firstTime) {
            pListener.statusStringChanged("");
            pListener.progressFinished();
            firstTime = false;
            split.setDividerLocation(0.7);
        }
    }

    public void info(String s) {
        logger.info(s);

        logLabel.setForeground(Color.BLACK);
        if (s == null || s.length() == 0) {
            logLabel.setText(" ");
        } else {
            System.out.println("Treeton: " + s);
            logLabel.setText(s);
        }
    }

    public void error(String s, Throwable e) {
        logger.error(s);

        logLabel.setForeground(Color.RED);
        if (s == null || s.length() == 0) {
            logLabel.setText(" ");
        } else {
            System.err.println("Treeton: " + s);
            logLabel.setText(s);
        }
    }

    /*class CorpusWrapper {
     Corpus corp;

     public CorpusWrapper(Corpus corp) {
       this.corp = corp;
     }

     public String toString() {
       return corp.getName();
     }

     public boolean equals(Object obj) {
       if (obj==null)
         return false;
       return corp == ((CorpusWrapper)obj).corp;
     }
   }

   class RunAction extends AbstractAction {
     RunAction(){
       super("Run");
     }

     public void actionPerformed(ActionEvent e){
       if (runButton.isEnabled()) {
         doRun();
       }
     }
   }


   class CollectGarbageAction extends AbstractAction {
     CollectGarbageAction(){
       super("Collect garbage");
     }

     public void actionPerformed(ActionEvent e){
       System.gc();
       try {
         Thread.sleep(500);
       } catch (InterruptedException e1) {
         //do nothing
       }
       ResourceLoaderPanel.this.logMemoryStat();
     }
   }

   class StopAction extends AbstractAction {
     StopAction(){
       super("Stop");
     }

     public void actionPerformed(ActionEvent e){
       if (stopButton!=null && stopButton.isEnabled()) {
         doStop();
       }
     }
   } */

    public void addListener(ResourceManagerListener listener) {
        for (int i = 0; i < listeners.size(); i++) {
            ResourceManagerListener l = listeners.get(i);
            if (l == listener)
                return;
        }
        listeners.add(listener);
    }

    public void removeListener(ResourceManagerListener listener) {
        listeners.remove(listener);
    }

    private void notifyAddChain(ResourcesContext d, ResourceChain ch) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).chainAdded(d, ch);
        }

    }

    private void notifySwitchDomain() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).contextSwitched();
        }
    }

    private void notifyRenameChain(ResourcesContext d, ResourceChain ch) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).chainRenamed(d, ch);
        }
    }

    private void notifyRemoveChain(ResourcesContext d, ResourceChain ch) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).chainRemoved(d, ch);
        }
    }

    private void notifyStartChain(ResourcesContext d, ResourceChain ch) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).chainStarted(d, ch);
        }
    }

    private void notifyFinishChain(ResourcesContext d, ResourceChain ch) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).chainFinished(d, ch);
        }
    }

    public List<ResourceChain> getAllChains(ResourcesContext dom) {
        ArrayList<ResourceChain> res = new ArrayList<ResourceChain>();
        InstantiatedResourcesModel m = domains2model.get(dom);
        if (m != null) {
            int len = m.getChildCount(m.getRoot());
            for (int i = 0; i < len; i++) {
                Object o = m.getChild(m.getRoot(), i);
                if (o instanceof ResourceChain) {
                    res.add((ResourceChain) o);
                }
            }
        }
        return res;
    }

    private Resource getSelectedInstantiatedResource() {
        TreePath selectionPath = instanceTree.getSelectionPath();
        if (selectionPath == null)
            return null;
        Object o = selectionPath.getLastPathComponent();
        if (o == instanceModel) {
            return null;
        } else {
            return (Resource) o;
        }
    }

    public boolean launchCurrent(CorpusProvider provider) {
        return launch(context, getSelectedInstantiatedResource(), provider);
    }

    public boolean isRunning() {
        return stopButton.isEnabled();
    }

    public ResourcesContext getContext() {
        return context;
    }

    private class ProcessingThread extends Thread implements Thread.UncaughtExceptionHandler {
        TreePath[] arr;
        int mode;

        ProcessingThread(TreePath[] arr, int mode) {
            this.arr = arr;
            this.mode = mode;
            setUncaughtExceptionHandler(this);
        }

        public void uncaughtException(Thread t, Throwable e) {
            ResourceManagerPanel.this.error("Uncaught exception!!!", e);
            System.gc();
        }

        public void run() {
            if (mode == MODE_INIT) {
                try {
                    for (TreePath treePath : arr) {
                        if (treePath.getLastPathComponent() instanceof Object[]) {
                            Object[] elem = (Object[]) treePath.getLastPathComponent();
                            if (elem.length == 2) {
                                ResourcesContext localContext = (ResourcesContext) elem[0];
                                if (localContext.getResourceChainModel((String) elem[1], false) != null) {
                                    synchronized (ResourceManagerPanel.this) {
                                        chain = factory.createResourceChain(context, (String) elem[1]);
                                        chain.setIgnoredDetector(ResourceManagerPanel.this);
                                    }
                                    ResourceManagerPanel.this.info("Загружается цепочка ресурсов " + safeGetName(chain));
                                    chain.addLogListener(ResourceManagerPanel.this);
                                    chain.setProgressListener(pListener);
                                    chain.initialize(treenotationsContextManager.get(ContextUtil.getFullName(context)));
                                    chain.setProgressListener(null);
                                    chain.removeLogListener(ResourceManagerPanel.this);
                                    synchronized (ResourceManagerPanel.this) {
                                        if (isReady(chain)) {
                                            int n = chain.getNumberOfResources();
                                            for (int i = 0; i < n; i++) {
                                                Resource res = chain.getResource(i);
                                                HashMap<String, Object> copy = new HashMap<String, Object>();
                                                copy.putAll(res.getInitialParameters());

                                                res2params.put(res, copy);
                                            }
                                            toAdd.add(chain);
                                            ResourceManagerPanel.this.info("Готово");
                                        }
                                    }
                                } else if (localContext.getResourceModel((String) elem[1], false) != null) {
                                    synchronized (ResourceManagerPanel.this) {
                                        res = factory.createResource(context, (String) elem[1]);
                                    }
                                    long time = System.currentTimeMillis();
                                    ResourceManagerPanel.this.info("Загружается ресурс " + safeGetName(res));
                                    res.setProgressListener(pListener);
                                    res.initialize(treenotationsContextManager.get(ContextUtil.getFullName(context)));
                                    res.setProgressListener(null);
                                    synchronized (ResourceManagerPanel.this) {
                                        if (isReady(res)) {
                                            HashMap<String, Object> copy = new HashMap<String, Object>();
                                            copy.putAll(res.getInitialParameters());

                                            res2params.put(res, copy);
                                            toAdd.add(res);
                                            ResourceManagerPanel.this.logMemoryStat();
                                            ResourceManagerPanel.this.info("Готово. " + Double.toString((System.currentTimeMillis() - time) / 1000.0) + "s");
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    if (chain != null)
                        chain.removeLogListener(ResourceManagerPanel.this);
                    ExceptionDialog.showExceptionDialog(ResourceManagerPanel.this, e);
                } finally {
                    pListener.statusStringChanged("");
                    pListener.progressFinished();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            synchronized (ResourceManagerPanel.this) {
                                for (Object o : toAdd) {
                                    if (o instanceof ResourceChain) {
                                        instanceModel.addResourceChain((ResourceChain) o, instanceModel.getChildCount(instanceModel));
                                        notifyAddChain(context, (ResourceChain) o);
                                    } else if (o instanceof Resource) {
                                        instanceModel.addResource((Resource) o, instanceModel.getChildCount(instanceModel));
                                    }
                                    instanceTree.expandPath(new TreePath(instanceModel.pathForObject(o)));
                                }
                                toAdd.clear();
                                res = null;
                                chain = null;
                            }
                            try {
                                enableAll();
                            } catch (ContextException e) {
                                ExceptionDialog.showExceptionDialog(ResourceManagerPanel.this, e);
                            }
                            instanceTree.updateUI();
                        }
                    });
                }
            } else if (mode == MODE_REINIT) {
                try {
                    HashSet<Resource> resources = new HashSet<Resource>();
                    for (TreePath treePath : arr) {
                        Object o = treePath.getLastPathComponent();
                        if (o instanceof ResourceChain) {
                            ResourceChain ch = (ResourceChain) o;
                            for (int i = 0; i < ch.getNumberOfResources(); i++) {
                                resources.add(ch.getResource(i));
                            }
                        } else if (o instanceof Resource) {
                            resources.add((Resource) o);
                        }
                    }

                    for (Resource r : resources) {
                        res = r;
                        ResourceManagerPanel.this.info("Реинициализируется ресурс " + safeGetName(res));
                        long time = System.currentTimeMillis();
                        res.setProgressListener(pListener);
                        Map<String, Object> initialParameters = res2params.get(res);
                        res.getInitialParameters().clear();
                        res.getInitialParameters().putAll(initialParameters);
                        res.initialize(treenotationsContextManager.get(ContextUtil.getFullName(context)));

                        res.setProgressListener(null);
                        synchronized (ResourceManagerPanel.this) {
                            if (isReady(res)) {
                                exceptionOccured.remove(res);
                                ResourceManagerPanel.this.logMemoryStat();
                                ResourceManagerPanel.this.info("Готово. " + Double.toString((System.currentTimeMillis() - time) / 1000.0) + "s");
                            }
                        }
                    }
                } catch (Throwable e) {
                    if (res != null)
                        exceptionOccured.add(res);
                    ExceptionDialog.showExceptionDialog(ResourceManagerPanel.this, e);
                } finally {
                    pListener.statusStringChanged("");
                    pListener.progressFinished();
                    res = null;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                enableAll();
                            } catch (ContextException e) {
                                ExceptionDialog.showExceptionDialog(ResourceManagerPanel.this, e);
                            }
                            instanceTree.updateUI();
                        }
                    });
                }
            } else if (mode == MODE_RUN && chain != null) {
                try {
                    for (CorpusElement element : provider.getCorpusContent()) {
                        String docName = element.getDocName();
                        String docContent = element.getText();
                        TextMarkingStorage storage = element.getStorage();

                        ResourceManagerPanel.this.info("Запускается цепочка ресурсов " + safeGetName(chain) + " для документа " + docName);
                        chain.addLogListener(ResourceManagerPanel.this);
                        chain.setProgressListener(pListener);
                        String result;
                        result = chain.execute(docContent, storage, new HashMap<String, Object>());
                        chain.setProgressListener(null);
                        chain.removeLogListener(ResourceManagerPanel.this);
                        element.setResult(result);
                        ResourceManagerPanel.this.info("Готово");
                        if (isPrintResultEnabled()) {
                            ResourceManagerPanel.this.info("Результат запуска:");
                            if (result == null)
                                System.out.println("null");
                            else {
                                System.out.println(result);
                                ReportDialog.showReportDialog(ResourceManagerPanel.this, "Результат запуска", result);
                            }
                        }
                    }
                } catch (Throwable e) {
                    if (chain != null)
                        chain.removeLogListener(ResourceManagerPanel.this);
                    ExceptionDialog.showExceptionDialog(ResourceManagerPanel.this, e);
                } finally {
                    pListener.statusStringChanged("");
                    pListener.progressFinished();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            synchronized (ResourceManagerPanel.this) {
                                notifyFinishChain(context, chain);
                                chain = null;
                                provider = null;
                            }
                            try {
                                enableAll();
                            } catch (ContextException e) {
                                ExceptionDialog.showExceptionDialog(ResourceManagerPanel.this, e);
                            }
                            instanceTree.updateUI();
                        }
                    });
                }
            }
        }


        public void stopSelf() {
            this.stop();
        }
    }
}
