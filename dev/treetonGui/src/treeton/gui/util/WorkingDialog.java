/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.gui.GuiResources;

import javax.swing.*;
import java.awt.*;

public class WorkingDialog extends JDialog {
    public static final String title = "Работа...";
    public static Frame frame;
    // TODO разобраться с WD
    private static WorkingDialog workingDialog;
    private static ImageIcon workingIcon;

    static {
        workingIcon = GuiResources.getImageIcon("working.gif");
    }

    private String msg;

    private WorkingDialog() {
        super(frame, title, true);
    }

    public static WorkingDialog getInstance(Frame _frame, Component parentComp, String msg) {
        if (workingDialog == null || frame != _frame) {
            frame = _frame;
            workingDialog = new WorkingDialog();
            workingDialog.setResizable(false);
            workingDialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            init();
            workingDialog.pack();
        }
        workingDialog.msg = msg;
        workingDialog.setLocationRelativeTo(parentComp);
        return workingDialog;
    }

    public static WorkingDialog getInstance() {
        return workingDialog;
    }

    private static void init() {
        JLabel lbl = new JLabel(workingIcon);
        Container c = workingDialog.getContentPane();
        c.setLayout(new BorderLayout());
        c.add(lbl, BorderLayout.CENTER);
    }

    public void show() {
        Dimension d = getSize();
        Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
        //Center the window
        setLocation((scrSize.width - d.width) / 2,
                (scrSize.height - d.height) / 2);
        super.show();
    }


}
