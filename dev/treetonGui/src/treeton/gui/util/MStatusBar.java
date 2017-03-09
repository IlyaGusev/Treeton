/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import treeton.gui.GuiResources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MStatusBar extends JPanel implements ActionListener {
    /**
     * Поле сообщения (самое большое, слева). Потом надо
     * сделать возможность автоматически увеличивать поле
     * при наведении, мыши, если сообщение не помещается,
     */
    protected JLabel jlbMain;
    /**
     * Позиция курсора
     */
    protected JPanel jpnExtra;
    protected JLabel jlbPosition;
    protected JProgressBar jpgProgress;
    /**
     * Информация о символе - код или что-то ещё, чтобы
     * отличать буквы с одинаковым начертанием
     */
    protected JLabel jlbCode;
    /**
     * Режим редактирования (вставка или замена)
     */
    protected JLabel jlbInsMode;
    /**
     * Признак соединения с БД.
     */
    protected JLabel btnConn;
    /**
     * Признак того, что окно сообщений обновилось.
     */
    protected JButton jbtMessage;
    /**
     * Индикатор свободной памяти.
     */
    protected JLabel jlbMemory;
    /**
     * Запустить Garbage collector.
     */
    protected JButton jbtGarbColl;
    int prevTab;
    ImageIcon imgExcpOff;
    ImageIcon imgExcpOn;

    ImageIcon imgConnOff;
    ImageIcon imgConnOn;

    ImageIcon imgGarbColl;

    public MStatusBar() {
        prevTab = -1;
        jlbMain = new JLabel();
        jpnExtra = new JPanel();
        jpnExtra.setLayout(new BorderLayout());
        jlbPosition = new JLabel();
        jpgProgress = new JProgressBar();
        jpgProgress.setOrientation(JProgressBar.HORIZONTAL);
        jpgProgress.setBorderPainted(false);
        jpgProgress.setIndeterminate(false);
        jpnExtra.add(jlbPosition, BorderLayout.CENTER);
        jlbCode = new JLabel();
        jlbInsMode = new JLabel();
        imgExcpOff = GuiResources.getImageIcon("excp-off.gif");
        imgExcpOn = GuiResources.getImageIcon("excp-on.gif");
        imgConnOff = GuiResources.getImageIcon("disconn.gif");
        imgConnOn = GuiResources.getImageIcon("connect.gif");
        imgGarbColl = GuiResources.getImageIcon("recbin.gif");
        jbtMessage = new JButton(imgExcpOff);
        jbtMessage.setMargin(new Insets(0, 0, 0, 0));
        jbtMessage.setFocusable(false);
        jbtMessage.setRolloverEnabled(true);

        btnConn = new JLabel(imgConnOff);
//    btnConn = new JLabel(imgConnOff) {
//	    public JToolTip createToolTip()
//		  {
//			  return new JMultiLineToolTip();
//		  }
//    };
        btnConn.setFocusable(false);

        jlbMemory = new JLabel();

        jlbMemory.setHorizontalAlignment(SwingConstants.CENTER);
        jlbMemory.setToolTipText("Занятая и общая память");

        jbtGarbColl = new JButton(imgGarbColl);
        jbtGarbColl.setMargin(new Insets(0, 0, 0, 0));
        jbtGarbColl.setFocusable(false);
        jbtGarbColl.setRolloverEnabled(true);

        jlbMain.setBorder(BorderFactory.createEtchedBorder());
        jpnExtra.setBorder(BorderFactory.createEtchedBorder());
        jlbCode.setBorder(BorderFactory.createEtchedBorder());
        jlbInsMode.setBorder(BorderFactory.createEtchedBorder());
        jbtMessage.setBorder(BorderFactory.createEtchedBorder());
        btnConn.setBorder(BorderFactory.createEtchedBorder());
        jlbMemory.setBorder(BorderFactory.createEtchedBorder());
        jbtGarbColl.setBorder(BorderFactory.createEtchedBorder());

        this.setLayout(new GridBagLayout());

        this.add(jlbMain,
                new GridBagConstraints(0, 0, 1, 0, 1.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
        this.add(jpnExtra,
                new GridBagConstraints(1, 0, 1, 0, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 2, 0, 0), 0, 0));
        this.add(jlbCode,
                new GridBagConstraints(2, 0, 1, 0, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 2, 0, 0), 0, 0));
        this.add(jlbInsMode,
                new GridBagConstraints(3, 0, 1, 0, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 2, 0, 0), 0, 0));
        this.add(btnConn,
                new GridBagConstraints(4, 0, 1, 0, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 2, 0, 0), 0, 0));
        this.add(jbtMessage,
                new GridBagConstraints(5, 0, 1, 0, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 2, 0, 0), 0, 0));
        this.add(jlbMemory,
                new GridBagConstraints(6, 0, 1, 0, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 2, 0, 0), 0, 0));
        this.add(jbtGarbColl,
                new GridBagConstraints(7, 0, 1, 0, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(0, 2, 0, 0), 0, 0));

        jlbMain.setPreferredSize(new Dimension(50, 22));
        jpnExtra.setPreferredSize(new Dimension(90, 22));
        jlbCode.setPreferredSize(new Dimension(90, 22));
        jlbInsMode.setPreferredSize(new Dimension(40, 22));
        jlbMemory.setPreferredSize(new Dimension(120, 22));

        btnConn.setMinimumSize(new Dimension(22, 22));
        btnConn.setPreferredSize(new Dimension(22, 22));

        jbtMessage.setMinimumSize(new Dimension(22, 22));
        jbtMessage.setPreferredSize(new Dimension(22, 22));
        jbtMessage.addActionListener(this);

        Font oldFont = jlbMemory.getFont();
        Font newFont = new Font(oldFont.getName(),
                oldFont.getStyle() | Font.BOLD,
                oldFont.getSize());
        jlbMemory.setFont(newFont);

        jbtGarbColl.setMinimumSize(new Dimension(22, 22));
        jbtGarbColl.setPreferredSize(new Dimension(22, 22));
        jbtGarbColl.addActionListener(this);

        setConnectTooltip("Нет соединения");

//    this.setBorder(BorderFactory.createLoweredBevelBorder());
//    this.setPreferredSize(new Dimension(10,40));
    }

    public void setMain(String s) {
        jlbMain.setText(new StringBuffer("  ").append(s).toString());
    }

    public void setPosition(String s) {
        jlbPosition.setText(new StringBuffer("  ").append(s).toString());
    }

    public void switchToPosition() {
        jpnExtra.remove(jpgProgress);
        jpnExtra.add(jlbPosition, BorderLayout.CENTER);
    }

    /**
     * Если min == max, то прогрессбар работает в "бесконечном" режиме
     */
    public void setProgressOn(int min, int max) {
        jpnExtra.remove(jlbPosition);
        jpnExtra.add(jpgProgress, BorderLayout.CENTER);
        if (min == max) {
            jpgProgress.setIndeterminate(true);
        } else {
            jpgProgress.setIndeterminate(false);
            jpgProgress.setMinimum(min);
            jpgProgress.setMaximum(max);
            jpgProgress.setValue(min);
        }
        jpnExtra.doLayout();
        jpnExtra.repaint();
    }

    public void setProgressOff() {
        jpgProgress.setIndeterminate(false);
        jpnExtra.remove(jpgProgress);
        jpnExtra.add(jlbPosition, BorderLayout.CENTER);
        jpnExtra.doLayout();
        jpnExtra.repaint();
    }

    public void setProgressValue(int val) {
        jpgProgress.setValue(val);
    }

    public void setProgressString(String s) {
        jpgProgress.setStringPainted(true);
        jpgProgress.setString(s);
    }

    public void setMemory(String s) {
        jlbMemory.setText(new StringBuffer("  ").append(s).toString());
    }

    public void setLog(boolean status) {
        jbtMessage.setIcon(status ? imgExcpOn : imgExcpOff);
        jbtMessage.setToolTipText(status ?
                "Новые сообщения. Нажмите кнопку для просмотра." :
                "Нет новых сообщений.");
    }

    //  <0 - нет иконки
    // ==0 - открыто
    //  >0 - закрыто
    public void setConnect(int status) {
        btnConn.setIcon(
                status == 0 ? imgConnOff :
                        status > 0 ? imgConnOn : null);
    }


    public void setConnectTooltip(String s) {
        btnConn.setToolTipText(s);
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == jbtMessage) {
            for (int i = 0; i < 100; i++) {
                int ai[] = new int[10000];
                ai[10] = 10;
            }
        } else if (source == jbtGarbColl) {
            Runnable runGarbColl = new Runnable() {
                public void run() {
                    System.gc();
                }
            };
            SwingUtilities.invokeLater(runGarbColl);
        }
    }
}
