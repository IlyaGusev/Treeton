/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExceptionDialog extends JDialog implements ActionListener {
    private static final String CMD_OK = "OK";
    private static final String CMD_CN = "CN";

    private static final String TEXT_OK = "Send";

    int retValue = JOptionPane.CANCEL_OPTION;

    JTextPane txtHost;

    JScrollPane jsc;
    JButton buttonSend;
    JButton buttonCancel;
    JTextPane textPane;
    JTextArea textArea;


    public ExceptionDialog(Frame parentFrame, Throwable e) throws HeadlessException {
        super(parentFrame, "Error", true);
        jsc = new JScrollPane();
        jsc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream str = new PrintStream(os);
        e.printStackTrace(str);


        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());

        buttonSend = new JButton(TEXT_OK);
        buttonSend.setActionCommand(CMD_OK);
        buttonSend.addActionListener(this);


        int row = 0;


        textPane = new JTextPane();
        textPane.setText(os.toString());
        textPane.setEditable(false);
        int h = textPane.getPreferredSize().height;
        jsc.getViewport().add(textPane);
        textPane.setMinimumSize(new Dimension(540, h));
        textPane.setPreferredSize(new Dimension(1000, 700));
        contentPane.add(jsc,
                new GridBagConstraints(
                        1, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(5, 5, 5, 5), 0, 0));
        setSize(new Dimension(1000, 700));

        row++;
        JLabel label = new JLabel("Comments:");
        contentPane.add(label, new GridBagConstraints(
                1, row, 1, 1, 1.0, 0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 5, 5, 5), 0, 0));
        row++;
        textArea = new JTextArea();
        int j = textArea.getPreferredSize().height;
        textPane.setMinimumSize(new Dimension(540, j));
        textArea.setPreferredSize(new Dimension(540, j));

        contentPane.add(textArea, new GridBagConstraints(1, row, 1, 1, 1.0, 0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        row++;
        contentPane.add(buttonSend,
                new GridBagConstraints(
                        1, row, 1, 1, 1.0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 5, 5, 5), 0, 0));

    }

    static Window getWindowForComponent(Component parentComponent)
            throws HeadlessException {
        if (parentComponent == null)
            return JOptionPane.getRootFrame();
        if (parentComponent instanceof Frame || parentComponent instanceof Dialog)
            return (Window) parentComponent;
        return getWindowForComponent(parentComponent.getParent());
    }

    public static void showExceptionDialog(Component parentComp, Throwable e) {
        Frame frame = JOptionPane.getFrameForComponent(parentComp);
        ExceptionDialog dialog =
                new ExceptionDialog(frame, e);
        dialog.setLocationRelativeTo(frame);

        dialog.setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (CMD_CN.equals(cmd)) {
            retValue = JOptionPane.CANCEL_OPTION;
            setVisible(false);
        }

    }


}
