/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.gui.GuiResources;
import treeton.gui.TreetonMainFrame;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class ExportDialog extends JDialog
        implements ActionListener, CaretListener {
    private static final String CMD_OK = "OK"; // ok
    private static final String CMD_CN = "CN"; // cancel
    private static final String CMD_ST = "ST"; // select target
    private static Insets buttonInsets = new Insets(2, 2, 2, 2);
    JButton jbtnOk;
    JTextField jtxfTarget;
    JTextField jtxfUri;
    File fileTarget;
    boolean askUri;

    public ExportDialog(boolean askUri) {
        super(TreetonMainFrame.getMainFrame(), "Экспорт тринотаций", true);
        fileTarget = null;
        this.askUri = askUri;
        init();
        pack();
        setResizable(false);
        refreshButtons();
    }

    private void init() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());

        jbtnOk = new JButton("ОК");
        jbtnOk.setActionCommand(CMD_OK);
        jbtnOk.addActionListener(this);
        JButton btnCn = new JButton("Отмена");
        btnCn.setActionCommand(CMD_CN);
        btnCn.addActionListener(this);

        int row = 0;
        JLabel label;
        JButton button;

        int txtWidth = 170;

        label = new JLabel("Файл:");
        jtxfTarget = new JTextField();
        int h = jtxfTarget.getPreferredSize().height;
        jtxfTarget.setMinimumSize(new Dimension(txtWidth, h));
        jtxfTarget.setPreferredSize(new Dimension(txtWidth, h));
        jtxfTarget.setEditable(false);
        button = new JButton(GuiResources.iconFolderGreen) {
            public Insets getInsets() {
                return buttonInsets;
            }
        };
        button.setActionCommand(CMD_ST);
        button.addActionListener(this);
        button.setToolTipText("Выбрать файл для экспорта");
        contentPane.add(label,
                new GridBagConstraints(
                        0, row, 1, 1, 1.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE,
                        new Insets(6, 6, 4, 4), 0, 0));
        contentPane.add(jtxfTarget,
                new GridBagConstraints(
                        1, row, 1, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(6, 0, 4, 2), 0, 0));
        contentPane.add(button,
                new GridBagConstraints(
                        2, row, 1, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(6, 0, 4, 6), 0, 0));
        row++;

        if (askUri) {
            label = new JLabel("URI:");
            jtxfUri = new JTextField();
            h = jtxfUri.getPreferredSize().height;
            jtxfUri.setMinimumSize(new Dimension(txtWidth, h));
            jtxfUri.setPreferredSize(new Dimension(txtWidth, h));
            jtxfUri.addCaretListener(this);
            contentPane.add(label,
                    new GridBagConstraints(
                            0, row, 1, 1, 1.0, 0.0,
                            GridBagConstraints.EAST, GridBagConstraints.NONE,
                            new Insets(4, 6, 4, 4), 0, 0));
            contentPane.add(jtxfUri,
                    new GridBagConstraints(
                            1, row, 1, 1, 1.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                            new Insets(4, 0, 4, 0), 0, 0));
            row++;
        }

        Divider line = new Divider(Divider.HORZ);
        contentPane.add(line,
                new GridBagConstraints(
                        0, row, 3, 1, 1.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        new Insets(0, 6, 0, 6), 0, 0));
        row++;

        JPanel buttons = new JPanel();
        GridLayout grid = new GridLayout(1, 2);
        grid.setHgap(6);
        buttons.setLayout(grid);
        buttons.add(jbtnOk);
        buttons.add(btnCn);
        contentPane.add(buttons,
                new GridBagConstraints(
                        0, row, 3, 1, 1.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE,
                        new Insets(6, 6, 6, 6), 0, 0));

        updateTargetTextField();
        refreshButtons();
    }


    private void doOk() {
        setVisible(false);
    }

    private void refreshButtons() {
        if (fileTarget != null) {
            if (askUri) {
                try {
                    new URI(jtxfUri.getText());
                    jbtnOk.setEnabled(true);
                } catch (URISyntaxException e) {
                    jbtnOk.setEnabled(false);
                }
            } else {
                jbtnOk.setEnabled(true);
            }
        } else {
            jbtnOk.setEnabled(false);
        }
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (CMD_OK.equals(cmd)) {
            doOk();
        } else if (CMD_ST.equals(cmd)) {
            File fn = GuiResources.fileDialogUtil.saveFileDialog((JPanel) TreetonMainFrame.getMainFrame().getContentPane(), "Выберите файл для экспорта", null, null);
            if (fn != null) {
                fileTarget = fn;
            }
            updateTargetTextField();
            refreshButtons();
        } else if (CMD_CN.equals(cmd)) {
            setVisible(false);
        }
    }

    private void updateTargetTextField() {
        String fname = "Выберите файл";
        String tooltip = null;
        Color c = Color.RED;
        if (fileTarget != null) {
            fname = fileTarget.getName();
            c = Color.BLACK;
            tooltip = fileTarget.getAbsolutePath();
        }
        jtxfTarget.setText(fname);
        jtxfTarget.setToolTipText(tooltip);
        jtxfTarget.setForeground(c);
    }

    public void caretUpdate(CaretEvent e) {
        refreshButtons();
    }

    public File getFileTarget() {
        return fileTarget;
    }
}
