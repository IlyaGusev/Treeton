/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.gui.GuiResources;
import treeton.gui.TreetonMainFrame;

import javax.swing.*;
import java.awt.*;

public class WorkingDialogControllerEx {
    public static final String title = "Работа...";
    private static final Object LOCK = new Object();
    static JDialog curDialog;
    private static ImageIcon workingIcon;
    private static boolean isShown = false;

    static {
        workingIcon = GuiResources.getImageIcon("working.gif");
    }

    public static void showDialog() {
        showDialog(TreetonMainFrame.getMainFrame());
    }

    public static void showDialog(Frame frame) {
        synchronized (LOCK) {
            if (isShown)
                return;
            isShown = true;
            curDialog = new JDialog(frame, title, true);
            curDialog.setResizable(false);
            curDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            JLabel lbl = new JLabel(workingIcon);
            Container c = curDialog.getContentPane();
            c.setLayout(new BorderLayout());
            c.add(lbl, BorderLayout.CENTER);
            curDialog.pack();
            Dimension d = curDialog.getSize();
            Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
            //Center the window
            curDialog.setLocation((scrSize.width - d.width) / 2,
                    (scrSize.height - d.height) / 2);
        }
        curDialog.setVisible(true);
    }

    public static void hideDialog() {
        synchronized (LOCK) {
            if (!isShown)
                return;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    curDialog.setVisible(false);
                }
            });
            isShown = false;
        }
    }
}
