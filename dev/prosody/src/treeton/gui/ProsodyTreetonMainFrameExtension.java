/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.gui.util.ExceptionDialog;

import javax.swing.*;
import java.awt.*;

public class ProsodyTreetonMainFrameExtension extends treeton.gui.TreetonMainFrameExtension {
    protected TreetonInternalFrame prosodyCorpusFrame;
    protected ProsodyCorpusPanel prosodyCorpusPanel;

    public void extend() {
        super.extend();
        final TreetonMainFrame treetonMainFrame =  TreetonMainFrame.getMainFrame();

        prosodyCorpusFrame = new TreetonInternalFrame("Редактор стиховедческого корпуса",
                true, true, true, true) {

            public void deinit() {
                if( prosodyCorpusPanel != null ) {
                    prosodyCorpusPanel.onClose();
                    getContentPane().remove(prosodyCorpusPanel);
                    prosodyCorpusPanel = null;
                    prosodyCorpusFrame.setTitle("Редактор стиховедческого корпуса");
                }
            }

            public void init() {
                getContentPane().setLayout(new BorderLayout());
                try {
                    TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");

                    getContentPane().add( prosodyCorpusPanel = new ProsodyCorpusPanel(trnContext) );
                } catch (Exception e) {
                    ExceptionDialog.showExceptionDialog(this,e);
                }
                pack();
            }

            public void activate() {
            }

            public void deactivate() {
            }
        };

        treetonMainFrame.addTreetonInternalFrame( prosodyCorpusFrame );

        JMenuItem item = new JMenuItem("Редактировать стиховедческий корпус");
        item.getAccessibleContext().setAccessibleDescription("Редактировать стиховедческий корпус");
        item.addActionListener(e -> treetonMainFrame.activateFrame( prosodyCorpusFrame,false));
        treetonMainFrame.addMenuItem(TreetonMainFrame.MENUNAME_TOOLS,item);
    }
}
