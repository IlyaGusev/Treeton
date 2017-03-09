/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ReportDialog extends JDialog implements ActionListener {

    /**
     * команда кнопки "Закрыть"
     */
    private static final String CMD_CLO = "CLO";

    private String message;

    public ReportDialog(Frame frame, Component parentComp,
                        String title, String message) {
        super(frame,
                title != null ? title : "...",
                false);
        this.message = message;
        init();
        pack();
        setLocationRelativeTo(parentComp);
        setResizable(true);
    }

    public static void showReportDialog(Component parentComp,
                                        String title, String message) {
        Frame frame = JOptionPane.getFrameForComponent(parentComp);
        ReportDialog dialog =
                new ReportDialog(frame, parentComp, title, message);
        dialog.setVisible(true);
    }

    public void init() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());

        JButton btnClose = new JButton(" Закрыть ");
        btnClose.setActionCommand(CMD_CLO);
        btnClose.addActionListener(this);

        JScrollPane jscMess = new JScrollPane(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JTextPane jtxMess = new JTextPane();
        jtxMess.setEditable(false);
        jscMess.getViewport().add(jtxMess);
        jtxMess.setText(message);

        jscMess.setMinimumSize(new Dimension(350, 100));
        jscMess.setPreferredSize(new Dimension(1000, 600));
        contentPane.add(jscMess,
                new GridBagConstraints(
                        0, 0, 1, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(6, 6, 4, 6), 0, 0));

        JPanel line = new JPanel();
        line.setBackground(Color.black);
        line.setMaximumSize(new Dimension(500, 1));
        line.setPreferredSize(new Dimension(1, 1));
        line.setMinimumSize(new Dimension(1, 1));
        contentPane.add(line,
                new GridBagConstraints(
                        0, 1, 1, 1, 1.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        new Insets(0, 6, 0, 6), 0, 0));

        contentPane.add(btnClose,
                new GridBagConstraints(
                        0, 2, 1, 1, 1.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(4, 6, 0, 6), 0, 0));
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (CMD_CLO.equals(cmd)) {
            setVisible(false);
        }
    }

}
