/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.LogListener;
import treeton.core.util.ProgressListener;
import treeton.core.util.sut;
import treeton.gui.util.*;

import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class TreetonMainFrame extends JFrame
        implements ActionListener, ProgressListener, InternalFrameListener, LogListener {
    //public static final String CMD_TOLB    = "TOLB";
    public static final String CMD_STAB = "STAB";
    public static final String CMD_HELP = "HELP";
    public static final String CMD_TYPESDCL = "TYPESDCL";
    public static final String CMD_ABOUT = "ABT";
    public static final String CMD_EXIT = "EXIT";
    public static final String CMD_TEXTEDIT = "TEXTEDIT";

    public static final String MENUNAME_TOOLS = "TOOLS";
    public static final String MENUNAME_HELP = "HELP";
    public static final String MENUNAME_FILE = "FILE";
    public static final String MENUNAME_OPTIONS = "OPTIONS";

    public static final int SPACE_DIALOG_NORTH = 4;
    public static final int SPACE_DIALOG_SOUTH = 4;
    public static final int SPACE_DIALOG_WEST = 4;
    public static final int SPACE_DIALOG_EAST = 4;
    public static final int SPACE_BUTTON_HORZ = 6;
    public static final int SPACE_BUTTON_VERT = 6;
    public static final int SPACE_USUAL_VERT = 6;
    public static final int SPACE_USUAL_HORZ = 6;
    public static final int SPACE_SMALL_VERT = 4;
    public static final int SPACE_SMALL_HORZ = 4;
    public static final int SPACE_DIVIDER_VERT = 6;
    public static final int SPACE_AFTERLABEL = 4;
    public static final int windowUndefLocation = -4321;
    public static String appName = "Treeton";
    protected static TreetonMainFrame mainFrameInstance;
    static Splash splash;
    private final DateFormat df = SimpleDateFormat.getTimeInstance(DateFormat.MEDIUM);
    private final StringBuilder sb = new StringBuilder();
    //protected JToolBar jtbrMain;
    public JDesktopPane dskPanel;
    protected String classFolder = "";
    protected String className = "";
    protected MemoryWatch memWatch;
    protected Thread memThr;
    protected Dimension windowMinSize = new Dimension(0, 0);
    protected Point windowLocation = new Point();
    protected HashMap<String, Image> graphicsProperties;
    MStatusBar statusBar;
    JMenuItem menuItemStatus;
    //JMenuItem menuItemToolbar;
    JMenuItem menuItemSidebar;
    JSplitPane jspMain;
    JMenuBar menuBar;
    List<TreetonInternalFrame> frames = new ArrayList<TreetonInternalFrame>();
    List<Boolean> initialized = new ArrayList<Boolean>();
    String currentStatusString;
    private boolean longWork = false;

    public static boolean mainFrameExists() {
        return mainFrameInstance != null;
    }

    public static TreetonMainFrame getMainFrame() {
        if (mainFrameInstance == null) {
            createMainFrame();
        }
        return mainFrameInstance;
    }

    /*protected void createToolbar() {
     jtbrMain = new JToolBar();
     jtbrMain.setFloatable(false);
     jtbrMain.setOrientation(JToolBar.HORIZONTAL);
     jtbrMain.setRollover(true);
   } */

    public static void createMainFrame() {
        TreetonMainFrame frame = new TreetonMainFrame();
//    frame.setVisible(false);

        splash = new Splash(frame);
        splash.setVisible(true);

        Thread thr = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    //do nothing
                }
                if (splash != null) {
                    splash.setVisible(false);
                    splash = null;
                }
            }
        });
        thr.start();

        frame.constr();
        frame.validate();
    }

    public static void message(String title, String msg) {
        JOptionPane.showMessageDialog(TreetonMainFrame.getMainFrame(),
                msg, title, JOptionPane.WARNING_MESSAGE);
    }

    public static void report(String title, String msg) {
        ReportDialog.showReportDialog(
                TreetonMainFrame.getMainFrame(),
                title, msg);
    }

    public static void setProgressOn(final int min, final int max) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        TreetonMainFrame.getMainFrame().statusBar.setProgressOn(min, max);
                    }
                }
        );
    }

    public static void setProgressOff() {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        setProgressString("");
                        TreetonMainFrame.getMainFrame().statusBar.setProgressOff();
                    }
                }
        );
    }

    public static void setProgressValue(final int val) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        TreetonMainFrame.getMainFrame().statusBar.setProgressValue(val);
                    }
                }
        );
    }

    public static void setProgressString(final String s) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        TreetonMainFrame.getMainFrame().statusBar.setProgressString(s);
                    }
                }
        );
    }

    public void constr() {
        mainFrameInstance = this;
        graphicsProperties = null;

        prepareClassFolderAndName();

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        init();
    }

    protected void prepareClassFolderAndName() {
        String cName = this.getClass().getName();
        StringBuffer sb = new StringBuffer();
        try {
            sb.append(sut.getPathForClass(cName));
        } catch (FileNotFoundException e) {
            sb.setLength(0);
            sb.append(System.getProperties().getProperty("System.user.home"));
        }
        sb.append('/');
        classFolder = sb.toString();
        className = sut.extractLastElement(cName, '.');
    }

    private void init() {
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setTitle(appName);

        WorkingDialog wdlg = WorkingDialog.getInstance(this, this, "");
        wdlg.setVisible(false);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension defSize = new Dimension(
                screenSize.width * 75 / 100,
                screenSize.height * 75 / 100);

        TreetonSessionProperties settings = TreetonSessionProperties.getInstance();
        int left = settings.getInteger("Win.left", windowUndefLocation, true);
        int top = settings.getInteger("Win.top", windowUndefLocation, true);
        int width = settings.getInteger("Win.width", defSize.width, true);
        int height = settings.getInteger("Win.height", defSize.height, true);

        width = Math.max(
                Math.min(width, screenSize.width),
                windowMinSize.width);
        height = Math.max(
                Math.min(height, screenSize.height),
                windowMinSize.height);

        if (left == windowUndefLocation) {
            left = (screenSize.width - width) / 2;
        }
        if (top == windowUndefLocation) {
            top = (screenSize.height - height) / 2;
        }

        if ((left + width) > screenSize.width) {
            left = screenSize.width - width;
        }
        if ((top + height) > screenSize.height) {
            top = screenSize.height - height;
        }

        left = Math.max(left, 0);
        top = Math.max(top, 0);

        this.setResizable(true);
        this.setBounds(left, top, width, height);
        this.setVisible(true);

        int state = settings.getInteger("Win.status", Frame.NORMAL, true);
        if (state == Frame.NORMAL || state == Frame.ICONIFIED || !this.getToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
            this.setExtendedState(Frame.NORMAL);
        } else {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }


        Color c;
        UIDefaults ud = UIManager.getLookAndFeel().getDefaults();
        c = ud.getColor("ProgressBar.selectionBackground");
        UIManager.put("ProgressBar.selectionForeground", c);

        c = ud.getColor("Button.shadow");
        UIManager.put("Table.gridColor", c);

        UIManager.put("Table.font", new Font("Tahoma", 0, 12));
        UIManager.put("TextField.font", new Font("Arial Unicode MS", 0, 12));

        dskPanel = new JDesktopPane();


        statusBar = new MStatusBar();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (graphicsProperties == null) {
                    initGraphicsProperties();
                    try {
                        initComponents();
                    } catch (ContextException e) {
                        throw new RuntimeException("Problem with Context", e);
                    }
                }
            }
        });

    }

    public void addMenuItem(String menuName, JMenuItem item) {
        JMenu menu = null;
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu jMenu = menuBar.getMenu(i);
            if (jMenu.getName().equals(menuName)) {
                menu = jMenu;
                break;
            }
        }
        if (menu == null) {
            menu = new JMenu(menuName);
            menuBar.add(menu);
        }
        menu.add(item);
    }

    public void addMenuSeparator(String menuName) {
        JMenu menu = null;
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu jMenu = menuBar.getMenu(i);
            if (jMenu.getName().equals(menuName)) {
                menu = jMenu;
                break;
            }
        }
        if (menu == null) {
            menu = new JMenu(menuName);
            menuBar.add(menu);
        }
        menu.addSeparator();
    }

    protected void addBasicItems() {
        JMenuItem item = new JMenuItem("Выход");
        item.setActionCommand(CMD_EXIT);
        item.addActionListener(this);
        addMenuItem(MENUNAME_FILE, item);

        /*menuItemToolbar = new JCheckBoxMenuItem("Панель инструментов");
        menuItemToolbar.setActionCommand(CMD_TOLB);
        menuItemToolbar.addActionListener(this);
        menuItemToolbar.setSelected(true);
        addMenuItem("Файл",item);*/

        menuItemStatus = new JCheckBoxMenuItem("Панель состояния");
        menuItemStatus.setActionCommand(CMD_STAB);
        menuItemStatus.addActionListener(this);
        menuItemStatus.setSelected(true);
        addMenuItem(MENUNAME_OPTIONS, menuItemStatus);

        item = new JMenuItem("Справка");
        item.setActionCommand(CMD_HELP);
        item.addActionListener(this);
        addMenuItem(MENUNAME_HELP, item);

        addMenuSeparator(MENUNAME_HELP);

        item = new JMenuItem("О программе...");
        item.setActionCommand(CMD_ABOUT);
        item.addActionListener(this);
        addMenuItem(MENUNAME_HELP, item);
    }

    protected void createMenu() {
        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setName(MENUNAME_FILE);
        JMenu optionMenu = new JMenu("Настройки");
        optionMenu.setName(MENUNAME_OPTIONS);
        JMenu toolsMenu = new JMenu("Утилиты");
        toolsMenu.setName(MENUNAME_TOOLS);
        JMenu helpMenu = new JMenu("Помощь");
        helpMenu.setName(MENUNAME_HELP);

        menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(optionMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        addBasicItems();
        setJMenuBar(menuBar);
    }

    public void paint(Graphics g) {
        super.paint(g);
    }

    private void initComponents() throws ContextException {
        createMenu();
        //createToolbar();

        JPanel contentPane = (JPanel) this.getContentPane();

        contentPane.setLayout(new BorderLayout());
        contentPane.add(dskPanel, BorderLayout.CENTER);
        //contentPane.add(jtbrMain, BorderLayout.PAGE_START);
        contentPane.add(statusBar, BorderLayout.PAGE_END);

        memWatch = new MemoryWatch(statusBar);
        memThr = new Thread(memWatch);
        memThr.start();
    }

    public void addTreetonInternalFrame(TreetonInternalFrame frame) {
        frame.init();
        frames.add(frame);
        initialized.add(true);
        dskPanel.add(frame);
        frame.addInternalFrameListener(this);
    }

    private void initGraphicsProperties() {
        Image im = createImage(10, 10);
        graphicsProperties = new HashMap<String, Image>();

        graphicsProperties.put("service.image", im);
        UIManager.getLookAndFeel().getDefaults();
    }

    public Object getGraphicsProperty(String propName) {
        return graphicsProperties.get(propName);
    }

    protected void fin()
            throws IOException {

        fireCloseAll();

        TreetonSessionProperties settings = TreetonSessionProperties.getInstance();

        settings.put("Win.status", this.getExtendedState());

        this.setExtendedState(Frame.NORMAL);
        Rectangle bounds = this.getBounds();
        settings.put("Win.left", bounds.x);
        settings.put("Win.top", bounds.y);
        settings.put("Win.width", bounds.width);
        settings.put("Win.height", bounds.height);

        settings.put("Dir.last", GuiResources.fileDialogUtil.lastDirectory);

        settings.store();
        System.exit(0);
    }

    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            //
            try {
                fin();
            } catch (IOException x) {
                ExceptionDialog.showExceptionDialog(this, x);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        /*if (CMD_TOLB.equals(action)) {
            if (menuItemToolbar.isSelected()) {
             getContentPane().add(jtbrMain, BorderLayout.PAGE_START);
             getContentPane().validate();
           } else {
             getContentPane().remove(jtbrMain);
             getContentPane().validate();
           }
        } else */
        if (CMD_STAB.equals(action)) {
            if (menuItemStatus.isSelected()) {
                getContentPane().add(statusBar, BorderLayout.PAGE_END);
                getContentPane().validate();
            } else {
                getContentPane().remove(statusBar);
                getContentPane().validate();
            }
        } else if (CMD_EXIT.equals(action)) {
            try {
                fin();
            } catch (IOException x) {
                ExceptionDialog.showExceptionDialog(this, x);
            }
        } else if (CMD_HELP.equals(action)) {
        } else if (CMD_ABOUT.equals(action)) {
            new AboutDialog(this).setVisible(true);
        }
    }

    synchronized void startLongWork() {
        longWork = true;
    }

    synchronized void finishLongWork() {
        longWork = false;
    }

    synchronized boolean isLongWork() {
        return longWork;
    }

    public void activateFrame(TreetonInternalFrame frame, boolean fullSize) {
        if (isDeinitialized(frame)) {
            frame.init();
            initialized.set(frames.indexOf(frame), true);

            dskPanel.add(frame);

        }

        frame.setVisible(true);
        try {
            frame.setIcon(false);
            if (fullSize) {
                frame.setSize(dskPanel.getSize());
            }
        } catch (PropertyVetoException e) {
            ExceptionDialog.showExceptionDialog(this, e);
        }
        frame.toFront();
    }

    private boolean isDeinitialized(TreetonInternalFrame frame) {
        return !initialized.get(frames.indexOf(frame));
    }

    public void internalFrameActivated(InternalFrameEvent e) {
        TreetonInternalFrame frame = (TreetonInternalFrame) e.getInternalFrame();
        frame.activate();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < frames.size(); i++) {
            TreetonInternalFrame other = frames.get(i);

            if (!other.equals(frame)) {
                other.deactivate();
            }
        }
    }

    public void internalFrameClosed(InternalFrameEvent e) {
    }

    public void internalFrameClosing(InternalFrameEvent e) {
        TreetonInternalFrame frame = (TreetonInternalFrame) e.getInternalFrame();
        frame.deinit();
        initialized.set(frames.indexOf(frame), false);
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
        TreetonInternalFrame frame = (TreetonInternalFrame) e.getInternalFrame();
        frame.deactivate();
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
        TreetonInternalFrame frame = (TreetonInternalFrame) e.getInternalFrame();
        frame.activate();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < frames.size(); i++) {
            TreetonInternalFrame other = frames.get(i);

            if (!other.equals(frame)) {
                other.deactivate();
            }
        }
    }

    public void internalFrameIconified(InternalFrameEvent e) {
        TreetonInternalFrame frame = (TreetonInternalFrame) e.getInternalFrame();
        frame.deactivate();
    }

    public void internalFrameOpened(InternalFrameEvent e) {
    }

    public void fireCloseAll() {
        while (!frames.isEmpty()) {
            TreetonInternalFrame frame = frames.get(frames.size() - 1);
            frame.setVisible(false);
            frame.deinit();
            dskPanel.remove(frame);
            frames.remove(frames.size() - 1);
            initialized.remove(initialized.size() - 1);
        }
    }

    public void showMessage(String title, String msg) {
        JOptionPane.showMessageDialog(this, msg, title,
                JOptionPane.WARNING_MESSAGE);
    }

    public void setVisible(boolean b) {
        super.setVisible(b);
        toBack();
    }

    public void progressStarted() {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        statusBar.setProgressOn(0, 1000);
                    }
                }
        );
    }

    public void infiniteProgressStarted() {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        statusBar.setProgressOn(0, 0);
                    }
                }
        );
    }

    public void progressStringChanged(final String s) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        statusBar.setProgressString(s);
                    }
                }
        );
    }

    public void statusStringChanged(final String s) {
        currentStatusString = s;
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        statusBar.setMain(s);
                    }
                }
        );
    }

    public String getStatusString() {
        return currentStatusString;
    }

    public static void setStatusString(final String s) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        TreetonMainFrame.getMainFrame().statusBar.setMain(s);
                    }
                }
        );
    }

    public void progressValueChanged(final double value) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        statusBar.setProgressValue((int) (value * 1000d));
                    }
                }
        );
    }

    public void progressFinished() {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        statusBar.setProgressString("");
                        statusBar.setProgressOff();
                    }
                }
        );
    }

    public void info(String s) {
        statusStringChanged(s);
        sb.setLength(0);
        sb.append(df.format(Calendar.getInstance().getTime())).append(" Treeton: ").append(s);
        System.out.println(sb);
    }

    public void error(String s, Throwable e) {
        sb.setLength(0);
        sb.append(df.format(Calendar.getInstance().getTime())).append("Treeton: ").append(s);
        System.err.println(sb);
        e.printStackTrace();
    }
}
