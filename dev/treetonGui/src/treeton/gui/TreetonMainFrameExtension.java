/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.ResourceUtils;
import treeton.gui.context.ResourceManagerPanel;
import treeton.gui.texteditor.TextEditorPanel;
import treeton.gui.util.ExceptionDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class TreetonMainFrameExtension {
    private static final String LOGGER_CONFIGURATION_FILE = "logger.config";
    protected ResourceManagerPanel resourcePanel;
    private TreetonInternalFrame resourcesFrame;
    private TreetonInternalFrame textEditorFrame;

    public void extend() {
        if (new File(LOGGER_CONFIGURATION_FILE).exists()) {
            System.out.println("Using logging configuration from " + new File(LOGGER_CONFIGURATION_FILE).getPath());
            PropertyConfigurator.configure(LOGGER_CONFIGURATION_FILE);
        } else {
            System.out.println("Working according to default logging policy");
            BasicConfigurator.resetConfiguration();
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.INFO);
        }

        final TreetonMainFrame treetonMainFrame = TreetonMainFrame.getMainFrame();

        resourcesFrame = new TreetonInternalFrame("Менеджер ресурсов",
                true, false, true, true) {

            public void deinit() {
                resourcePanel = null;
            }

            public void init() {
                getContentPane().setLayout(new BorderLayout());
                resourcePanel = createResourceManagerPanel();
                getContentPane().add(resourcePanel, BorderLayout.CENTER);
                try {
                    resourcePanel.init(treetonMainFrame, false);
                } catch (ContextException e) {
                    ExceptionDialog.showExceptionDialog(this, e);
                }
                pack();
            }

            public void activate() {
            }

            public void deactivate() {
            }
        };

        treetonMainFrame.addTreetonInternalFrame(resourcesFrame);

        textEditorFrame = new TreetonInternalFrame("Обработка текста",
                true, true, true, true) {

            public void deinit() {
            }

            public void init() {
                getContentPane().setLayout(new BorderLayout());
                getContentPane().add(new TextEditorPanel(resourcePanel), BorderLayout.CENTER);
                pack();
            }

            public void activate() {
            }

            public void deactivate() {
            }
        };
        treetonMainFrame.addTreetonInternalFrame(textEditorFrame);

        JMenuItem item = new JMenuItem("Менеджер ресурсов");
        item.getAccessibleContext().setAccessibleDescription("Менеджер ресурсов");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                treetonMainFrame.activateFrame(resourcesFrame, false);
            }
        });
        treetonMainFrame.addMenuItem(TreetonMainFrame.MENUNAME_TOOLS, item);
        item = new JMenuItem("Обработка текста");
        item.getAccessibleContext().setAccessibleDescription("Обработка текста");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                treetonMainFrame.activateFrame(textEditorFrame, false);
            }
        });
        treetonMainFrame.addMenuItem(TreetonMainFrame.MENUNAME_TOOLS, item);
    }

    protected ResourceManagerPanel createResourceManagerPanel() {
        return new ResourceManagerPanel(new ResourceUtils(), ContextConfiguration.trnsManager(), ContextConfiguration.resourcesManager());
    }
}