/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.gui.GuiResources;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class ResourcesContextTreeCellRenderer extends DefaultTreeCellRenderer {
    public ResourcesContextTreeCellRenderer() {
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);


        try {
            if (value instanceof ResourcesContext) {
                renderer.setIcon(GuiResources.iconNone);
                renderer.setText(((ResourcesContext) value).getName());
            } else {
                Object[] arr = (Object[]) value;

                renderer.setText(ContextUtil.shortName((String) arr[1]));
                if (arr[0] instanceof ResourcesContext) {
                    ResourcesContext c = (ResourcesContext) arr[0];
                    if (c.getResourceChainModel((String) arr[1], false) != null) {
                        renderer.setIcon(GuiResources.iconResChain);
                    } else {
                        renderer.setIcon(GuiResources.iconNone);
                    }
                } else {
                    ResourcesContext c = (ResourcesContext) arr[2];
                    if (c.getResourceChainModel((String) arr[1], true) != null) {
                        renderer.setIcon(GuiResources.iconResChainRef);
                    } else {
                        renderer.setIcon(GuiResources.iconNoneRef);
                    }
                }
            }
        } catch (ContextException e) {
            throw new RuntimeException("ContextException occured", e);
        }

        return renderer;
    }
}
