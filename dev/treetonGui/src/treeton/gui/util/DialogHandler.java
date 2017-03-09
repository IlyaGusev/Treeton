/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Данный класс используется во всех случаях, когда нужно о чем-либо проинформировать пользователя
 */
public class DialogHandler {
    private static final Logger logger = Logger.getLogger(DialogHandler.class);
    private static boolean MAILING_ENABLED = true;
    private static DialogHandler instance = null;
    /**
     * Родительское окно для диалогов.
     * Если его не задать, то при переключении в ОС на окно нашего приложения,
     * в котором произошла ошибка, ошибка всплывать не будет.
     */
    private JFrame parentFrame = null;

    private DialogHandler() {
    }

    synchronized public static DialogHandler getInstance() {
        if (instance == null)
            instance = new DialogHandler();

        return instance;
    }

    protected Dimension getDefaultSize(Window window) {
        window.pack();
        Dimension size = window.getPreferredSize();

        /* Вот тут бы и вернуть size, но он может оказаться слишком большим */

        int screenWidth = (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth());
        int screenHeight = (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getHeight());

        if (size.width > screenWidth)
            size.width = (int) Math.round(screenWidth / 1.3);

        if (size.height > screenHeight)
            size.height = (int) Math.round(screenHeight / 1.3);

        return size;
    }

    public void error(String message) {
        try {
            MessageBox box = MessageBox.createError(message, null);
            showDialog(box);
            mail(message, box.getOptionValue());
        } catch (Exception e) {
            logger.error(message);
        }
    }

    private boolean mail(String message, int optionValue) {
        return false;
    }

    public void error(String message, Exception exception) {
        try {
            String errorMessageText = message + " - " + exception.getMessage();
            MessageBox box = MessageBox.createError(errorMessageText, "");
            showDialog(box);
            mail(errorMessageText + "\n", box.getOptionValue());
        } catch (Exception e) {
            logger.error(message, exception);
        }
    }

    public void error(Exception exception) {
        try {
            String errorMessageText = exception.getMessage();
            MessageBox box = MessageBox.createError(errorMessageText, "");
            showDialog(box);
            mail(errorMessageText + "\n", box.getOptionValue());
        } catch (Exception e) {
            logger.error("", exception);
        }
    }

    public void warning(String message) {
        try {
            MessageBox box = MessageBox.createWarning(message, null);
            showDialog(box);
            mail(message, box.getOptionValue());
        } catch (Exception e) {
            logger.warn(message);
        }
    }


    public int questionYesNo(String message, boolean cancelButton) {
        try {
            MessageBox messageBox = MessageBox.createQuestion(message, null, cancelButton);
            showDialog(messageBox);
            return messageBox.getOptionValue();
        } catch (Exception e) {
            return -1;
        }
    }

    private Throwable getCause(Throwable e) {
        return e.getCause() == null ? e : getCause(e.getCause());
    }

    public JFrame getParentFrame() {
        return parentFrame;
    }

    public void setParentFrame(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    public void info(String message) {
        try {
            showDialog(MessageBox.createMessage(message, null));
        } catch (Exception e) {
            logger.info(message);
        }
    }

    public void showDialog(JDialog dialog) {
        try {
            dialog.setSize(getDefaultSize(dialog));
            dialog.setLocation(SwingUtils.getWindowPositionFromScreenSize(dialog.getSize()));
            dialog.setVisible(true);
        } catch (Exception e) {
            logger.warn(e);
        }
    }
}