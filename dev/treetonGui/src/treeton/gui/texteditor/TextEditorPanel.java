/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.texteditor;

import org.jdesktop.swingx.JXHyperlink;
import treeton.core.*;
import treeton.core.config.GuiConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.ResourceChain;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnRelationType;
import treeton.core.model.TrnType;
import treeton.core.util.FileMapper;
import treeton.core.util.xml.XMLParser;
import treeton.gui.GuiResources;
import treeton.gui.TreetonMainFrame;
import treeton.gui.context.CorpusElement;
import treeton.gui.context.CorpusProvider;
import treeton.gui.context.ResourceManagerListener;
import treeton.gui.context.ResourceManagerPanel;
import treeton.gui.labelgen.TrnLabelGenerator;
import treeton.gui.trnedit.*;
import treeton.gui.trnview.TreenotationViewPanelAbstract;
import treeton.gui.trnview.TrnManipulationEvent;
import treeton.gui.trnview.TrnManipulationListener;
import treeton.gui.util.ExceptionDialog;
import treeton.gui.util.ExportDialog;
import treeton.gui.util.ToolBarFactory;
import treeton.gui.util.TypesInfoProvider;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;
import java.util.List;

public class TextEditorPanel extends JPanel
        implements TrnManipulationListener, CaretListener, DocumentListener, ActionListener, ItemListener, ResourceManagerListener, CorpusProvider, HyperlinkListener {


    JTextPane jtxpSource;
    Document docSource;

    JTextPane jtxpAttribs;
    JTextField jtxfUri;

    HashMap<String, TrnLabelGenerator> labelGenerators;
    TreenotationStorage tStorage;

    TreenotationViewPanelAbstract trnView;

    String savedSource;

    ResourceManagerPanel resourceManagerPanel;

    TypesSelectorPanel typesSelectorPanel;
    JButton runButton;
    JButton stopButton;
    JButton removeAllButton;
    JButton exportButton;
    JButton importButton;
    JButton openFileButton;
    JButton saveFileButton;
    ResourcesContext context = null;
    JComboBox domainCombo;
    JComboBox chainCombo;
    HashSet<Treenotation> selection = new HashSet<Treenotation>();
    String fileName = "Unnamed";
    ExportDialog exportDialog;
    ResourceChain currentlyRunningCh = null;
    private boolean unchanged = true;

    public TextEditorPanel(ResourceManagerPanel resourceManagerPanel) {
        this.resourceManagerPanel = resourceManagerPanel;
        try {
            init();
        } catch (MalformedURLException e) {
            ExceptionDialog.showExceptionDialog(null, e);
        } catch (ContextException e) {
            ExceptionDialog.showExceptionDialog(null, e);
        }

        selection.clear();
        jtxpAttribs.setText("");
        refreshButtons();
    }

    private void refreshButtons() {
        if (currentlyRunningCh == null) {
            runButton.setEnabled(chainCombo.getItemCount() > 0);
            stopButton.setEnabled(false);
            exportButton.setEnabled(true);
            importButton.setEnabled(true);
            openFileButton.setEnabled(tStorage.isEmpty());
            saveFileButton.setEnabled(true);

            removeAllButton.setEnabled(true);
            domainCombo.setEnabled(true);
            chainCombo.setEnabled(true);
            jtxpSource.setEditable(tStorage.isEmpty());
            jtxfUri.setEditable(tStorage.isEmpty());
        } else {
            runButton.setEnabled(false);
            stopButton.setEnabled(true);
            exportButton.setEnabled(false);
            importButton.setEnabled(false);
            removeAllButton.setEnabled(false);
            openFileButton.setEnabled(false);
            saveFileButton.setEnabled(false);

            domainCombo.setEnabled(false);
            chainCombo.setEnabled(false);
            jtxpSource.setEditable(false);
            jtxfUri.setEditable(false);
        }
    }

    private void selectContext(ResourcesContext context) throws ContextException {
        this.context = context;

        TreenotationsContext trnsContext = ContextConfiguration.trnsManager().get(ContextUtil.getFullName(context));

        try {
            labelGenerators = GuiConfiguration.getInstance().getAllLabelGenerators(getClass());
        } catch (IllegalAccessException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        } catch (InstantiationException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }

        tStorage = TreetonFactory.newTreenotationStorage(trnsContext);
        try {
            tStorage.setURI(jtxfUri.getText());
        } catch (IllegalAccessException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
        typesSelectorPanel.reFill();
        typesSelectorPanel.selectAll();

        resetTrnView(true);

        jtxpAttribs.setText("");

        refreshChainCombo();
    }

    private void resetTrnView(boolean cursorVisible) {
        try {
            trnView.reset(typesSelectorPanel.getTrnTypes(), null, null, null, tStorage, unchanged ? savedSource : jtxpSource.getText(), 0, labelGenerators);
            if (cursorVisible)
                trnView.setCursorVisible(true);
            trnView.componentResized(null);
            selection.clear();
        } catch (TreetonModelException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
    }

    private void init() throws MalformedURLException, ContextException {
        setLayout(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JSplitPane upper = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JPanel upperLeft = new JPanel();

        upperLeft.setLayout(new GridBagLayout());
        JScrollPane jscr = new JScrollPane();
        jtxpSource = new JTextPane();

        jtxpSource.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "scapeHighlighter");
        AbstractAction searchAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ScapeTemplateHighlighter.highlight(savedSource, tStorage, jtxpSource);
            }
        };
        jtxpSource.getActionMap().put("scapeHighlighter", searchAction);
        jtxpSource.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "removeHighlights");
        jtxpSource.getActionMap().put("removeHighlights", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ScapeTemplateHighlighter.removeHighlight(jtxpSource);
            }
        });

        docSource = jtxpSource.getDocument();
        docSource.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        jscr.getViewport().add(jtxpSource);
        jscr.setPreferredSize(new Dimension(400, 200));

        ToolBarFactory tbf = GuiResources.tbf;
        JToolBar jtbrLeft = new JToolBar(JToolBar.HORIZONTAL);
        jtbrLeft.setFloatable(false);
        jtbrLeft.setRollover(true);
        jtbrLeft.setBorder(BorderFactory.createEmptyBorder());

        runButton = tbf.addToolBarButton(jtbrLeft, "run.gif",
                "RUN", "Начать обработку", this);
        stopButton = tbf.addToolBarButton(jtbrLeft, "stopProcess.gif",
                "STOP", "Остановить обработку", this);
        removeAllButton = tbf.addToolBarButton(jtbrLeft, "close.gif",
                "REMOVEALL", "Удалить разметку", this);
        exportButton = tbf.addToolBarButton(jtbrLeft, "forward.gif",
                "EXPORT", "Сохранить тринотации в файл", this);
        importButton = tbf.addToolBarButton(jtbrLeft, "back.gif",
                "IMPORT", "Загрузить тринотации из файла", this);
        openFileButton = tbf.addToolBarButton(jtbrLeft, "open.gif",
                "OPENFILE", "Загрузить текст из файла", this);
        saveFileButton = tbf.addToolBarButton(jtbrLeft, "save.gif",
                "SAVEFILE", "Сохранить текст в файл", this);

//    JToolBar jtbrRight = new JToolBar(JToolBar.HORIZONTAL);
//    jtbrRight.setFloatable(false);
//    jtbrRight.setRollover(true);
//    jtbrRight.setBorder(BorderFactory.createEmptyBorder());


        /*jtxpSource.addKeyListener(new KeyAdapter() {
          public void keyTyped(KeyEvent e) {
            char ch = e.getKeyChar();
            if (ch == KeyEvent.VK_ENTER &&
                (e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
              try {
                doUpdate();
              } catch (ResourceInstantiationException e1) {
                ExceptionDialog.showExceptionDialog(TextEditorPanel.this,e1);
              }
            }
          }
        });*/

        upperLeft.add(jscr,
                new GridBagConstraints(
                        0, 0, 1, 1, 1.0, 1.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 0), 0, 0));

        JButton searchButton = new JXHyperlink(searchAction);
        searchButton.setText("<html>S<br>e<br>a<br>r<br>c<br>h</html>");

        upperLeft.add(searchButton,
                new GridBagConstraints(
                        1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                        new Insets(0, 1, 0, 1), 9, 0));


        trnView = TreenotationViewPanelAbstract.createTreenotationViewPanel(
                new TrnType[]{}, null, null, null, TreetonFactory.newTreenotationStorage(), "", 0, null);
        trnView.setPreferredSize(new Dimension(400, 200));

        typesSelectorPanel = new TypesSelectorPanel(new TreenotationStorageProvider() {
            public TreenotationStorage getStorage() {
                return tStorage;
            }
        });

        typesSelectorPanel.addListener(new TypesSelectorPanelListener() {
            public void selectionChanged() {
                resetTrnView(false);
            }
        });


        TrnStorageEditor editor = new TrnStorageEditor(
                typesSelectorPanel,
                new TypesInfoProvider() {
                    public Collection<String> getSelectedTypes() {
                        List<String> list = new ArrayList<String>();
                        try {
                            for (TrnRelationType type : tStorage.getRelations().getAllTypes()) {
                                if (type.isRoot())
                                    continue;
                                list.add(type.getName());
                            }
                        } catch (TreetonModelException e) {
                            list.add("Error!!!");
                        }
                        return list;
                    }
                },
                new TrnStorageEditorListener() {
                    public TreenotationStorage getStorage() {
                        return tStorage;
                    }

                    public void storageChanged() {
                        trnView.componentResized(null);
                        jtxpAttribs.setText(getSelectionAsString());
                    }

                    public void attrEditRequest(Treenotation trn) {
                        new IntFeatureMapEditorDialog("Редактирование атрибутов", trn, new IntFeatureMapEditorListener() {
                            private BlackBoard board = TreetonFactory.newBlackBoard(100, false);

                            public void imapEdited(IntFeatureMap source, IntFeatureMap attrs, IntFeatureMap inducedAttrs) {
                                source.removeAll();
                                attrs.fillBlackBoard(board);
                                source.put(board);
                                trnView.componentResized(null);
                                jtxpAttribs.setText(getSelectionAsString());
                            }
                        }).showDialog(JOptionPane.getFrameForComponent(TextEditorPanel.this));
                    }

                    public int getSelectedIntervalStart() {
                        return trnView.getSelectedIntervalStart();
                    }

                    public int getSelectedIntervalEnd() {
                        return trnView.getSelectedIntervalEnd();
                    }

                    public void resetStorageView() {
                        resetTrnView(true);
                    }
                }
        );
        trnView.addMouseListener(editor.createMouseListener(trnView));

        //editor.setCheckCoverageMode(false);

        savedSource = "";

        JSplitPane splitTrn = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        jscr = new JScrollPane();
        jtxpAttribs = new JTextPane();
        jtxpAttribs.setContentType("text/html");
        jtxpAttribs.setEditable(false);
        jtxpAttribs.addHyperlinkListener(this);

        jscr.getViewport().add(jtxpAttribs);
        jscr.setPreferredSize(new Dimension(400, 100));

        JPanel fake = new JPanel();
        fake.setLayout(new GridBagLayout());

        fake.add(jtbrLeft,
                new GridBagConstraints(
                        0, 0, 2, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(4, 4, 4, 4), 0, 0));
        jtxfUri = new JTextField();
        jtxfUri.setPreferredSize(new Dimension(100, jtxfUri.getPreferredSize().height));
        jtxfUri.setMinimumSize(new Dimension(100, jtxfUri.getPreferredSize().height));

        jtxfUri.getDocument().addDocumentListener(this);

        domainCombo = new JComboBox();
        chainCombo = new JComboBox();
        chainCombo.setEditable(false);
        chainCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value == null ? null : ((ResourceChain) value).getName(), index, isSelected, cellHasFocus);    //To change body of overridden methods use File | Settings | File Templates.
            }
        });
        ResourcesContext[] res = ContextConfiguration.resourcesManager().getAllContexts();
        for (ResourcesContext context : res) {
            domainCombo.addItem(context);
        }
        context = ContextConfiguration.resourcesManager().getRootContext();

        domainCombo.setSelectedItem(context);
        selectContext(context);

        if (resourceManagerPanel != null)
            resourceManagerPanel.addListener(this);

        JLabel domainLabel = new JLabel("Домен:");

        fake.add(domainLabel,
                new GridBagConstraints(
                        2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(4, 4, 4, 4), 0, 0));
        fake.add(domainCombo,
                new GridBagConstraints(
                        3, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(4, 4, 4, 4), 0, 0));

        domainCombo.addItemListener(this);

        JLabel chainLabel = new JLabel("Цепочка ресурсов:");

        fake.add(chainLabel,
                new GridBagConstraints(
                        4, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(4, 4, 4, 4), 0, 0));
        fake.add(chainCombo,
                new GridBagConstraints(
                        5, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(4, 4, 4, 4), 0, 0));

        JLabel uriLabel = new JLabel("URI:");
        fake.add(uriLabel,
                new GridBagConstraints(
                        6, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(4, 4, 4, 4), 0, 0));
        fake.add(jtxfUri,
                new GridBagConstraints(
                        7, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        new Insets(4, 4, 4, 4), 0, 0));


        trnView.setBorder(BorderFactory.createLineBorder(Color.gray));
        fake.add(trnView,
                new GridBagConstraints(
                        0, 1, 8, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(4, 4, 4, 0), 0, 0));

        splitTrn.add(fake, JSplitPane.TOP);
        splitTrn.add(jscr, JSplitPane.BOTTOM);
        splitTrn.setResizeWeight(1);

        upper.add(upperLeft, JSplitPane.LEFT);


        upper.add(typesSelectorPanel, JSplitPane.RIGHT);
        upper.setDividerLocation(0.75);

        split.add(upper, JSplitPane.TOP);
        split.add(splitTrn, JSplitPane.BOTTOM);
        split.setDividerLocation(0.5);

        add(split, BorderLayout.CENTER);

        trnView.addTrnManipulationListener(this);
        trnView.addTrnManipulationListener(editor);

        jtxpSource.addCaretListener(this);
        docSource.addDocumentListener(this);
        //sourceChanged();
    }

    private void refreshChainCombo() {
        if (resourceManagerPanel != null) {
            List<ResourceChain> chains = resourceManagerPanel.getAllChains(context);
            ResourceChain selectedChain = (ResourceChain) chainCombo.getSelectedItem();
            chainCombo.removeAllItems();
            for (ResourceChain chain : chains) {
                chainCombo.addItem(chain);
            }
            if (selectedChain != null)
                chainCombo.setSelectedItem(selectedChain);
        }
    }

    public void deinit() {
        if (resourceManagerPanel != null)
            resourceManagerPanel.removeListener(this);
    }

    public void trnClicked(TrnManipulationEvent e) {
        selection.clear();
        while (e.nextSelectionElement()) {
            Treenotation trn = e.getSelectedTrn();
            selection.add(trn);
        }

        jtxpAttribs.setText(getSelectionAsString());
    }

    private String getSelectionAsString() {
        StringBuffer buf = new StringBuffer();
        for (Treenotation trn : selection) {
            buf.append(trn.getUri());
            buf.append(":");
            buf.append(trn.getId());
            buf.append(":");
            buf.append(trn.getHtmlString());
            buf.append("<br>");
        }
        return buf.toString();
    }

    public void caretUpdate(CaretEvent e) {
        Object src = e.getSource();
        if (src == jtxpSource) {
            try {
                if (!unchanged) {
                    int caret = jtxpSource.getCaret().getDot();
                    trnView.setCursorPosition(caret);
                    trnView.scrollToPosition(new Fraction(caret, 1));
                } else {
                    int caret = jtxpSource.getCaret().getDot();
                    caret = recountPos(savedSource, caret);
                    trnView.setCursorPosition(caret);
                    trnView.scrollToPosition(new Fraction(caret, 1));
                }
            } catch (TreetonModelException e1) {
                ExceptionDialog.showExceptionDialog(this, e1);
            }
        }
    }

    private int recountPos(String s, int caret) {
        int res = 0;
        for (int i = 0; i < s.length(); i++) {
            if (res == caret) {
                return i;
            }

            char c = s.charAt(i);
            if (c == '\r') {
                if (i < s.length() - 1) {
                    char c1 = s.charAt(i + 1);
                    if (c1 == '\n') {
                        res++;
                        i++;
                    } else {
                        res++;
                    }
                } else {
                    res++;
                    break;
                }
            } else {
                res++;
            }
        }
        if (res == caret) {
            return s.length();
        } else { //something wrong
            return 0;
        }
    }

    private void sourceChanged() {
        unchanged = false;
    }

    private void uriChanged() {
        if (!tStorage.isEmpty())
            return;
        try {
            tStorage.setURI(jtxfUri.getText());
        } catch (IllegalAccessException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
    }

    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == docSource) {
            sourceChanged();
        } else if (e.getDocument() == jtxfUri.getDocument()) {
            uriChanged();
        }
    }

    public void removeUpdate(DocumentEvent e) {
        if (e.getDocument() == docSource) {
            sourceChanged();
        } else if (e.getDocument() == jtxfUri.getDocument()) {
            uriChanged();
        }
    }

    public void changedUpdate(DocumentEvent e) {
        if (e.getDocument() == docSource) {
            sourceChanged();
        } else if (e.getDocument() == jtxfUri.getDocument()) {
            uriChanged();
        }
    }

    public void actionPerformed(ActionEvent e) {
        try {
            if (e.getActionCommand().equals("RUN")) {
                doRun();
            } else if (e.getActionCommand().equals("STOP")) {
                doStop();
            } else if (e.getActionCommand().equals("REMOVEALL")) {
                selectContext(context);
            } else if (e.getActionCommand().equals("EXPORT")) {
                doExport();
            } else if (e.getActionCommand().equals("IMPORT")) {
                doImport();
            } else if (e.getActionCommand().equals("OPENFILE")) {
                doOpenFile();
            } else if (e.getActionCommand().equals("SAVEFILE")) {
                doSaveFile();
            }
        } catch (Exception e1) {
            ExceptionDialog.showExceptionDialog(TextEditorPanel.this, e1);
        }
        refreshButtons();
    }

    private void doOpenFile() {
        try {
            File fn = GuiResources.fileDialogUtil.openFileDialog(this, "Загрузить текст из файла", null);
            if (fn != null) {
                fileName = fn.getPath();

                savedSource = new String(FileMapper.map2memory(fn.getPath()));

                syncWithSavedSource();
            }
        } catch (Exception e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
    }

    private void syncWithSavedSource() {
        jtxpSource.removeCaretListener(this);
        docSource.removeDocumentListener(this);
        docSource.putProperty(DefaultEditorKit.EndOfLineStringProperty, null);
        jtxpSource.setText(savedSource);
        docSource.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        jtxpSource.addCaretListener(this);
        docSource.addDocumentListener(this);

        unchanged = true;
    }

    private void doSaveFile() {
        try {
            File fn = GuiResources.fileDialogUtil.saveFileDialog(this, "Сохранить текст в файл", fileName, null);
            if (fn != null) {
                FileOutputStream fos = new FileOutputStream(fn);
                fos.write(unchanged ? savedSource.getBytes() : jtxpSource.getText().getBytes());
                fos.close();
            }
        } catch (Exception e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
    }

    private void doStop() {
        if (resourceManagerPanel != null)
            resourceManagerPanel.stopProcess();
    }

    private void doRun() {
        if (tStorage.isEmpty()) {
            resetTrnView(true);
            jtxpAttribs.setText("");
        }

        currentlyRunningCh = (ResourceChain) chainCombo.getSelectedItem();
        if (resourceManagerPanel != null)
            resourceManagerPanel.launch(context, currentlyRunningCh, this);
        refreshButtons();
    }

    private void doExport() {
        exportDialog = new ExportDialog(false);
        exportDialog.setLocationRelativeTo(TreetonMainFrame.getMainFrame());
        exportDialog.setVisible(true);
        if (exportDialog.getFileTarget() != null) {
            try {
                org.w3c.dom.Document doc = tStorage.exportXML();
                XMLParser.serialize(exportDialog.getFileTarget(), doc);
            } catch (ParserConfigurationException e) {
                ExceptionDialog.showExceptionDialog(TextEditorPanel.this, e);
            } catch (IOException e) {
                ExceptionDialog.showExceptionDialog(TextEditorPanel.this, e);
            }
        }
    }

    private void doImport() {
        try {
            File fn = GuiResources.fileDialogUtil.openFileDialog(this, "Выберите файл для импорта", null);
            if (fn != null) {
                importXml(fn, false);
            }
        } catch (Exception e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
    }

    public void importXml(File fn, boolean textFromStorage) throws Exception {
        if (tStorage.isEmpty()) {
            resetTrnView(true);
            jtxpAttribs.setText("");
        }

        FileInputStream fis = new FileInputStream(fn);
        if (jtxfUri.getText().equals("") && tStorage.isEmpty()) {
            tStorage.setURI(null);
        }
        tStorage.importXML(fis);
        jtxfUri.setText(tStorage.getUri());
        fis.close();
        if (textFromStorage) {
            StringBuffer buf = new StringBuffer();
            Token tok = tStorage.firstToken();

            while (tok != null) {
                buf.append(tok.getText());
                tok = tok.getNextToken();
            }

            savedSource = buf.toString();

            syncWithSavedSource();
        }

        resetTrnView(false);

        Object o = tStorage.getFeature("focusIntervalStart");
        if (o != null) {
            trnView.scrollToPosition(new Fraction((Integer) o, 1));
        }
    }

    public void itemStateChanged(ItemEvent e) {
        try {
            if (e.getSource() == domainCombo) {
                ResourcesContext c = (ResourcesContext) domainCombo.getSelectedItem();
                if (c != context) {
                    context = c;

                    selectContext(context);

                    refreshButtons();
                }
            }
        } catch (Exception ex) {
            ExceptionDialog.showExceptionDialog(this, ex);
        }
    }

    public void chainAdded(ResourcesContext context, ResourceChain chain) {
        if (context == this.context) {
            refreshChainCombo();
            refreshButtons();

        }
    }

    public void chainRenamed(ResourcesContext context, ResourceChain chain) {
        if (context == this.context) {
            refreshChainCombo();
            refreshButtons();

        }
    }

    public void chainRemoved(ResourcesContext context, ResourceChain chain) {
        if (context == this.context) {
            refreshChainCombo();
            refreshButtons();

        }
    }

    public void chainStarted(ResourcesContext context, ResourceChain chain) {
    }

    public void chainFinished(ResourcesContext context, ResourceChain chain) {
        if (chain != null && chain == currentlyRunningCh) {
            currentlyRunningCh = null;
            trnView.componentResized(null);
            refreshButtons();
        }
    }

    public void contextSwitched() {
    }

    public Collection<CorpusElement> getCorpusContent() {
        List<CorpusElement> res = new ArrayList<CorpusElement>();
        res.add(new CorpusElement(fileName, unchanged ? savedSource : jtxpSource.getText(), tStorage));
        return res;
    }

    public int contentSize() {
        return 1;
    }

    public void changeTypesSelection(Set<String> types) throws ContextException {
        typesSelectorPanel.deselectAll();
        //typesSelectorPanel.reFill();
        typesSelectorPanel.select(types);

        resetTrnView(true);
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String url = e.getDescription();

            openExternal(url);
        }
    }

    private void openExternal(String url) {
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.browse(URI.create(url));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void scrollToPosition(Integer focusIntervalStart) throws TreetonModelException {
        jtxpSource.setCaretPosition(focusIntervalStart);
        jtxpSource.updateUI();
        trnView.setCursorPosition(focusIntervalStart);
        trnView.scrollToPosition(new Fraction(focusIntervalStart, 1));
    }

    public String getSourceText() {
        return jtxpSource.getText();
    }
}
