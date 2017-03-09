/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import treeton.core.Token;
import treeton.core.TreenotationStorage;
import treeton.core.TreetonFactory;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.GuiConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationXMLNoSchemaImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TrnType;
import treeton.gui.labelgen.DefaultLabelGeneratorConfiguration;
import treeton.gui.labelgen.TreetonRusLabelGeneratorConfiguration;
import treeton.gui.labelgen.TrnLabelGenerator;
import treeton.gui.texteditor.TextEditorPanel;
import treeton.gui.trnview.TreenotationViewPanelAbstract;
import treeton.gui.util.ExceptionDialog;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;

public class TrnViewTest {
    public static void main(String[] args) throws Exception {
        new TrnViewTest().run(args);
    }

    public void run(String[] args) throws Exception, FileNotFoundException {
        try {
            BasicConfigurator.resetConfiguration();
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.INFO);

//            BasicConfiguration.setRootURL(getClass().getResource("/resources/conf/"));
            BasicConfiguration.createInstance();
            ContextConfiguration.registerConfigurationClass(ContextConfigurationXMLNoSchemaImpl.class);
            ContextConfiguration.createInstance();
            GuiConfiguration.createInstance();
            DefaultLabelGeneratorConfiguration.getInstance().configure();
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Test");

        TreenotationsContext context = ContextConfiguration.trnsManager().get("Common.Russian");
        TreenotationStorage storage = TreetonFactory.newTreenotationStorage(context);
        storage.importXML(new FileInputStream("trnview_test1.xml"));

        Iterator<Token> it = storage.tokenIterator();
        StringBuilder buf = new StringBuilder();
        while (it.hasNext()) {
            buf.append(it.next().getText());
        }

        HashMap<String, TrnLabelGenerator> labelGenerators = null;
        TreetonRusLabelGeneratorConfiguration.getInstance().configure();
        try {

            labelGenerators = GuiConfiguration.getInstance().getAllLabelGenerators(TextEditorPanel.class);
        } catch (IllegalAccessException e) {
            ExceptionDialog.showExceptionDialog(frame, e);
        } catch (InstantiationException e) {
            ExceptionDialog.showExceptionDialog(frame, e);
        }
        TreenotationViewPanelAbstract panel = TreenotationViewPanelAbstract.createTreenotationViewPanel(new TrnType[]{context.getType("Gramm"), context.getType("Token"), context.getType("System")},
                null, null, null, storage, buf.toString(), 0, labelGenerators);
        panel.setCursorVisible(true);
        panel.setCursorPosition(21);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(true);
        frame.setBounds(0, 0, screenSize.width, screenSize.height);

        frame.setVisible(true);
    }

}
