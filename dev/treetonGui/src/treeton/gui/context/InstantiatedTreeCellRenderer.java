/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.IgnoredDetector;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceChain;
import treeton.gui.GuiResources;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class InstantiatedTreeCellRenderer extends DefaultTreeCellRenderer {
    ExceptionOccuranceDetector exDetector;
    IgnoredDetector igDetector;

    public InstantiatedTreeCellRenderer(ExceptionOccuranceDetector exDetector, IgnoredDetector igDetector) {
        this.exDetector = exDetector;
        this.igDetector = igDetector;
    }

    public InstantiatedTreeCellRenderer() {
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);


        if (value instanceof InstantiatedResourcesModel) {
            renderer.setIcon(GuiResources.iconNone);
            renderer.setText("");
        } else {
            if (value instanceof ResourceChain) {
                renderer.setText(((ResourceChain) value).getName());
                renderer.setIcon(GuiResources.iconResChain);
            } else if (value instanceof Resource) {
                try {
                    renderer.setText(((Resource) value).getName());
                } catch (ContextException e) {
                    renderer.setText("Exception during getting name");
                }
                boolean ex = exDetector != null && exDetector.exceptionOccured((Resource) value);
                boolean ig = igDetector != null && igDetector.isIgnored((Resource) value);
                if (ex && ig) {
                    renderer.setIcon(GuiResources.iconExcpOnStrike);
                } else if (ex) {
                    renderer.setIcon(GuiResources.iconExcpOn);
                } else if (ig) {
                    renderer.setIcon(GuiResources.iconNoneStrike);
                } else {
                    renderer.setIcon(GuiResources.iconNone);
                }
            }
        }

        return renderer;
    }

    public Icon getClosedIcon() {
        return GuiResources.iconResChain;
    }

    public Icon getOpenIcon() {
        return GuiResources.iconResChain;
    }

    public Icon getDefaultOpenIcon() {
        return GuiResources.iconResChain;
    }

    public Icon getDefaultClosedIcon() {
        return GuiResources.iconResChain;
    }
}
