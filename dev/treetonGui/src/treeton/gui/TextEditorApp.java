/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.plastic.theme.SkyBlue;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.GuiConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationXMLNoSchemaImpl;
import treeton.core.config.context.ContextException;
import treeton.core.model.TreetonModelException;
import treeton.gui.labelgen.DefaultLabelGeneratorConfiguration;
import treeton.gui.texteditor.TextEditorPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class TextEditorApp {
    public static void main(String[] args) throws ContextException {
        (new TextEditorApp()).run(args);
    }

    public void run(String[] args) throws ContextException {
        try {
            BasicConfigurator.resetConfiguration();
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.INFO);

            BasicConfiguration.setRootURL(getClass().getResource("/resources/conf/"));
            BasicConfiguration.createInstance();
            ContextConfiguration.registerConfigurationClass(ContextConfigurationXMLNoSchemaImpl.class);
            ContextConfiguration.createInstance();
            GuiConfiguration.createInstance();
            DefaultLabelGeneratorConfiguration.getInstance().configure();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setLookAndFeel();

        JFrame frame = new JFrame("Analysis results");
        TextEditorPanel panel = new TextEditorPanel(null);

        if (args.length > 1) {
            StringTokenizer tokenizer = new StringTokenizer(args[1], ";");
            Set<String> types = new HashSet<String>();

            while (tokenizer.hasMoreTokens()) {
                types.add(tokenizer.nextToken());
            }

            panel.changeTypesSelection(types);
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(true);
        frame.setBounds(0, 0, screenSize.width, screenSize.height);

        frame.setVisible(true);


        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //
        }

        if (args.length > 0) {
            try {
                panel.importXml(new File(args[0]), true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            frame.setTitle(panel.getSourceText());

            if ("/DeleteAfterLaunch".equals(args[args.length - 1])) {
                //noinspection ResultOfMethodCallIgnored
                new File(args[0]).delete();
            }
        }

        if (args.length > 2) {
            Integer focusIntervalStart = Integer.valueOf(args[2]);
            try {
                panel.scrollToPosition(focusIntervalStart);
            } catch (TreetonModelException e) {
                e.printStackTrace();
            }
        }
    }

    protected void setLookAndFeel() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    PlasticLookAndFeel.setMyCurrentTheme(new SkyBlue());
                    UIManager.put("jgoodies.popupDropShadowEnabled", Boolean.TRUE);
                    UIManager.setLookAndFeel(new
                            com.jgoodies.plaf.plastic.PlasticXPLookAndFeel());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void info(String s) {
        System.out.println("Treeton: " + s);
    }

    public void error(String s, Throwable e) {
        System.err.println("Treeton: " + s);
        e.printStackTrace();
    }
}
