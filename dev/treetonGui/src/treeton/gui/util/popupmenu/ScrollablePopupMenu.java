/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util.popupmenu;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ScrollablePopupMenu
        extends JPopupMenu {
    private static final double MENU_SCREEN_HEIGHT_RATIO = 0.5;
    private List<JMenuItem> items;

    public ScrollablePopupMenu() {
        this(null);
    }

    public ScrollablePopupMenu(String label) {
        super(label);

        items = new ArrayList<JMenuItem>();
    }

    private JMenuItem addMenuItem(JMenuItem item) {
        items.add(item);
        return item;
    }

    public JMenuItem add(JMenuItem menuItem) {
        return addMenuItem(super.add(menuItem));
    }

    public void setVisible(boolean b) {
        if (b)
            prepare();
        super.setVisible(b);
    }

    private void prepare() {
        JMenu subMenu = null;
        removeAll();
        int height = 0;
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        for (JMenuItem item : items) {
            if (item instanceof ScrollableJMenu) {
                ((ScrollableJMenu) item).prepare();
            }

            height += item.getPreferredSize().height;
            if (height > dim.height * MENU_SCREEN_HEIGHT_RATIO) {
                JMenu newSubMenu = new JMenu("...");
                prepareMenuItem(subMenu, newSubMenu);
                subMenu = newSubMenu;

                height = 0;
            }

            prepareMenuItem(subMenu, item);
        }
    }

    private void prepareMenuItem(JMenu subMenu, JMenuItem item) {
        if (subMenu == null)
            super.add(item);
        else
            subMenu.add(item);
    }
}
