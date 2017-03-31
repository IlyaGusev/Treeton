/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import treeton.core.config.BasicConfiguration;
import treeton.core.config.GuiConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationProsodyImpl;
import treeton.core.config.context.ContextException;
import treeton.gui.util.ExceptionDialog;

public class ProsodyStarter extends Starter {
    ProsodyTreetonMainFrameExtension frameExtension;

    public ProsodyStarter(ProsodyTreetonMainFrameExtension treetonMainFrameExtension) {
        frameExtension = treetonMainFrameExtension;
    }

    public void run() throws ContextException {
        try {
            BasicConfiguration.createInstance();
            ContextConfiguration.registerConfigurationClass(ContextConfigurationProsodyImpl.class);
            ContextConfiguration.createInstance();
            GuiConfiguration.createInstance();
        } catch (Exception e) {
            ExceptionDialog.showExceptionDialog(null,e);
        }

        setLookAndFeel();

        frameExtension.extend();
     }

    public static void main(String [] args) throws ContextException {
      (new ProsodyStarter(new ProsodyTreetonMainFrameExtension())).run();
    }
}
