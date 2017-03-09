/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.plastic.theme.SkyBlue;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.GuiConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationXMLImpl;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.LogListener;
import treeton.gui.labelgen.TreetonRusLabelGeneratorConfiguration;

import javax.swing.*;

public class Starter implements LogListener {
    public static void main(String[] args) throws ContextException {
        (new Starter()).run();
    }

    public void run() throws ContextException {
        try {
            BasicConfiguration.createInstance();
            ContextConfiguration.registerConfigurationClass(ContextConfigurationXMLImpl.class);
            ContextConfiguration.createInstance();
            GuiConfiguration.createInstance();
            TreetonRusLabelGeneratorConfiguration.getInstance().configure();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setLookAndFeel();

        new TreetonMainFrameExtension().extend();
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
