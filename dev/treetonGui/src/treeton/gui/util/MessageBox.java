/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Диалоги для показа сообщений.
 */
public class MessageBox
        extends JDialog {
    private static final Logger logger = Logger.getLogger(MessageBox.class);
    /* Из-за того, что java не может сама кнопки в JOptionPane локализовать, приходится делать это здесь */
    private static final String MSG_OK_OPTION = "Послать";
    /**
     * Кнопка ОК
     */
    public static final String[] DEFAULT_OPTION = {MSG_OK_OPTION};
    private static final String MSG_YES_OPTION = "Да";
    private static final String MSG_NO_OPTION = "Нет";
    /**
     * Кнопки Да, Нет
     */
    public static final String[] YES_NO_OPTION = {MSG_YES_OPTION, MSG_NO_OPTION};
    private static final String MSG_CANCEL_OPTION = "Отмена";
    /**
     * Кнопки Да, Нет, Отмена
     */
    public static final String[] YES_NO_CANCEL_OPTION = {MSG_YES_OPTION, MSG_NO_OPTION, MSG_CANCEL_OPTION};
    /**
     * Кнопки ОК, Отмена
     */
    public static final String[] OK_CANCEL_OPTION = {MSG_OK_OPTION, MSG_CANCEL_OPTION};
    /**
     * разделитель строк на текущей платформе
     */
    private static final String LF = System.getProperty("line.separator");
    /**
     * Код нажатой кнопки
     */
    private int value;

    /**
     * В качестве родителя всегда берется текущий фрейм
     */
    private MessageBox() {
        super(DialogHandler.getInstance().getParentFrame(), true);
    }

    private static void writeToLog(String plainText, String errorInfo, int type) {
        if (type == JOptionPane.ERROR_MESSAGE) {
            logger.error("Error message from " + createLogMessage(plainText, errorInfo));
        } else if (type == JOptionPane.WARNING_MESSAGE) {
            logger.warn("Warning message from " + createLogMessage(plainText, errorInfo));
        } else if (logger.isInfoEnabled()) {
            logger.info("Message from " + createLogMessage(plainText, errorInfo));
            //logger.info(LogUtils.traceFull(4));
        }
    }

    private static String createLogMessage(String plainText, String errorInfo) {
        StackTraceElement src = getFirstExtCall();
        return src.toString() + "\nmessage text:" + plainText + ((errorInfo != null) ? "\ndescription:" + errorInfo : "");
    }

    private static JPanel createDetailsPanel(String errorInfo) {
        JPanel detailsPane = new JPanel();
        final JTextArea detailsTextArea = new JTextArea(splitWords(errorInfo, 100));
        detailsTextArea.setEditable(false);

        JScrollPane detailsScrollPane = new JScrollPane(detailsTextArea);
        Dimension preferredSize = detailsScrollPane.getPreferredSize();
        Dimension maxSize = SwingUtils.getWindowSizeFromScreenSize(0.7);
        detailsScrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width, maxSize.width), Math.min(preferredSize.height, maxSize.height)));

        detailsPane.add(detailsScrollPane);
        return detailsPane;
    }

    /**
     * @return элемент стека - первый вызов метода не из этого класса
     */
    private static StackTraceElement getFirstExtCall() {
        StackTraceElement[] st = new Throwable().getStackTrace();
        StackTraceElement src = st[0];
        int i;
        for (i = 0; i < st.length; i++)//ищем первый вызов метода не из этого класса
            if (st[i].getClassName() != MessageBox.class.getName())
                break;
        if (i < st.length)
            src = st[i];
        return src;
    }

    /**
     * Разбивает текст на строки.
     * Упрощенный вариант: отступ не задается.
     *
     * @param src        исходный текст
     * @param line_width ширина строки в символах
     * @return текст, разбитый на строки
     */
    private static String splitWords(String src, int line_width) {
        if (src == null)
            return "";
        //доработано для сохранения переносов строк
        String[] arr = src.split("\\n+");
        StringBuffer text = new StringBuffer();
        for (String anArr : arr)
            text.append(splitWords(anArr, line_width, 0));
        return text.toString();
    }

    /**
     * Разбивает текст на строки.
     *
     * @param src        исходный текст
     * @param line_width ширина строки в символах
     * @param indent     отступ слева (кол-во символов)
     * @return текст, разбитый на строки
     */
    private static String splitWords(String src, int line_width, int indent) {
        //  // текст преобразуется в массив, пробелы между словами удаляются
        String[] arr = src.split("\\s+");
        StringBuffer text = new StringBuffer();
        String s_indent = "";
        // отступ
        for (int i = 0; i < indent; i++)
            s_indent += " ";
        int i = 0;
        // пока не кончились слова...
        while (i < arr.length) {
            StringBuffer line = new StringBuffer();
            // пока строка не превышает нужную длину...
            do {
                line.append(arr[i]);
                line.append(' ');
                i++;
            }
            while (i < arr.length && line.length() < line_width);
            text.append(s_indent);
            text.append(line.toString().trim());// убираем лишний пробел справа
            text.append(LF);
        }
        return text.toString();
    }

    /**
     * Создает диалог с текстовым сообщением с заданными иконкой и кнопками.
     *
     * @param errorString Дополнительная информация, обычно стек или ответ от AM.
     * @param type        Тип диалога с сообщением (ошибка, инфо или вопрос), задаётся константами JOptionPane. Определяет иконку.
     * @param buttons     Кнопки в диалоге (ОК, Отмена, Да или Нет), задаются константами JOptionPane
     * @return диалог с текстовым сообщением с заданными иконкой и кнопками.
     */
    private static MessageBox createGenericMessageBox(String plainErrorMessageText, String errorString, int type, int buttons) {
        writeToLog(plainErrorMessageText, errorString, type);

        int msgWidth = 100;//todo: можно высчитывать его как-нить по размеру шрифта?
        String msg = splitWords(plainErrorMessageText, msgWidth);

        //добавляем кнопку "Дополнительно"
        JPanel details = null;
        if (errorString != null)
            details = createDetailsPanel(errorString);

        Object[] options = null;
        switch (buttons) {
            case JOptionPane.DEFAULT_OPTION:
                options = DEFAULT_OPTION;
                break;
            case JOptionPane.YES_NO_OPTION:
                options = YES_NO_OPTION;
                break;
            case JOptionPane.YES_NO_CANCEL_OPTION:
                options = YES_NO_CANCEL_OPTION;
                break;
            case JOptionPane.OK_CANCEL_OPTION:
                options = OK_CANCEL_OPTION;
                break;

            default:// непонятно что пришло в buttons, тогда ничего не делаем
        }

        final JOptionPane pane = new JOptionPane(msg, type, buttons, null, options, null);

        MessageBox dialog = new MessageBox();
        dialog.init(pane, details, options);
        return dialog;
    }

    /**
     * Создает диалог - информационное сообщение с кнопкой OK
     *
     * @param errorMessageText Текст сообщения - строка-идентификатор ресурсов.
     * @param errorInfo        Дополнительная информация, обычно стек или ответ от AM.
     * @return диалог с текстовым сообщением с заданными иконкой и кнопками.
     */
    public static MessageBox createMessage(String errorMessageText, String errorInfo) {
        return createGenericMessageBox(errorMessageText, errorInfo, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION);
    }

    public static MessageBox createMessageWithDetails(String plainErrorMessageText, String errorString, int type, int buttons) {
        return createGenericMessageBox(plainErrorMessageText, errorString, type, buttons);
    }

    /**
     * Создает диалог с предупреждением с кнопкой OK
     *
     * @param errorMessageText Текст сообщения - строка-идентификатор ресурсов.
     * @param errorInfo        Дополнительная информация, обычно стек или ответ от AM.
     * @return диалог с текстовым сообщением с заданными иконкой и кнопками.
     */
    public static MessageBox createWarning(String errorMessageText, String errorInfo) {
        return createGenericMessageBox(errorMessageText, errorInfo, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION);
    }

    /**
     * Создает диалог - сообщение об ошибке с кнопкой OK
     *
     * @param errorMessageText Текст сообщения - строка-идентификатор ресурсов.
     * @param errorInfo        Дополнительная информация, обычно стек или ответ от AM.
     * @return диалог с текстовым сообщением с заданными иконкой и кнопками.
     */
    public static MessageBox createError(String errorMessageText, String errorInfo) {
        return createGenericMessageBox(errorMessageText, errorInfo, JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION);
    }

    /**
     * Создает диалог с вопросом с кнопками YES, NO и CANCEL(если cancelButton == true)
     *
     * @param errorMessageText Текст сообщения - строка-идентификатор ресурсов.
     * @param errorInfo        Дополнительная информация, обычно стек или ответ от AM.
     * @param cancelButton     показывать ли кнопку CANCEL
     * @return диалог с текстовым сообщением с заданными иконкой и кнопками.
     */
    public static MessageBox createQuestion(String errorMessageText, String errorInfo, boolean cancelButton) {
        return createGenericMessageBox(errorMessageText, errorInfo, JOptionPane.QUESTION_MESSAGE, cancelButton ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION);
    }

    /**
     * Инициализация диалога
     *
     * @param pane    диалоговая панель с сообщением и кнопками
     * @param det     панель подробностей
     * @param options
     */
    private void init(final JOptionPane pane, JPanel det, final Object[] options) {
        value = -1;
        getContentPane().add(pane, BorderLayout.CENTER);
        if (det != null)
            getContentPane().add(det, BorderLayout.SOUTH);
        setResizable(false);
        // регистрируем обработчик событий, которые генерируются при изменении состояния JOptionPane
        pane.addPropertyChangeListener("value", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Object selectedOption = pane.getValue();
                //получаем код нажатой кнопки
                if (selectedOption instanceof Integer)
                    value = (Integer) selectedOption;
                else
                    for (int i = 0; i < options.length; i++)
                        if (selectedOption == options[i])
                            value = i;
                pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                setVisible(false);
            }
        });
        pack();
    }

    /**
     * @return -1 если диалог был закрыт, иначе код нажатой кнопки, определяется константами из JOptionPane
     */
    public int getOptionValue() {
        return value;
    }
}
