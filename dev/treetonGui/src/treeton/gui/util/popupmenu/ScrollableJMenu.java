/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util.popupmenu;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ScrollableJMenu extends JMenu {
    private static final double MENU_SCREEN_HEIGHT_RATIO = 0.5;
    private List<JMenuItem> items;

    public ScrollableJMenu() {
        this(null);
    }

    public ScrollableJMenu(String label) {
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

    public void prepare() {
        JMenu subMenu = null;
        removeAll();
        int height = 0;
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        for (JMenuItem item : items) {
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
