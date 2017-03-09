/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import java.awt.*;
import java.awt.event.MouseEvent;

public class SwingUtils {
    private static final Color BUTTON_NORMAL_BACKGROUND_COLOR = new Color(225, 225, 225);
    private static final Color TABLE_NORMAL_BACKGROUND_COLOR = new Color(255, 255, 255);

    public static Frame getFrame(Component component) {
        if (component == null)
            return null;
        if (component instanceof Frame)
            return (Frame) component;
        return getFrame(component.getParent());
    }

    public static Point getComponentCentre(Component component) {
        Point location = component.getLocationOnScreen();
        Dimension size = component.getSize();
        return new Point(location.x + size.width / 2, location.y + size.height / 2);
    }

    public static Color getButtonNormalBackgroundColor() {
        return BUTTON_NORMAL_BACKGROUND_COLOR;
    }

    public static Color getTableNormalBackgroundColor() {
//        return UIManager.getColor("Table.focusCellForeground");
        return Color.WHITE;
    }

    /**
     * Получение размера окна, которое составляет указанную часть от текущего разрешения экрана
     *
     * @param ratio размер окна относительно размера экрана
     * @return размера окна
     */
    public static Dimension getWindowSizeFromScreenSize(double ratio) {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        return new Dimension((int) Math.round(dim.getWidth() * ratio), (int) Math.round(dim.getHeight() * ratio));
    }

    /**
     * Получение позиции окна, которое составляет указанную часть от текущего разрешения экрана и располагается по центру
     *
     * @param windowSize размер окна
     * @return размера окна
     */
    public static Point getWindowPositionFromScreenSize(Dimension windowSize) {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        return new Point((int) Math.round((dim.getWidth() - windowSize.getWidth()) / 2), (int) Math.round((dim.getHeight() - windowSize.getHeight()) / 2));
    }

    /*
    * Генерирует симпатичный цвет по имени объекта. Всегда один и тот же
    * для одного и того же имени.
    * */

    public static Color genColorForName(String name) {
        if (name == null) {
            return Color.WHITE;
        }
        return new Color(
                (name.hashCode() & 15) * 8 + 128,
                ((name.hashCode() >> 4) & 15) * 8 + 128,
                ((name.hashCode() >> 8) & 15) * 8 + 128
        );
    }

    public static Color genColorForNameBright(String name) {
        return SwingUtils.brighter(SwingUtils.genColorForName(name), 0.8);
    }

    /*
    * Смешивает цвета
    * */

    public static Color mixColors(Color baseColor, Color addColor) {
        if (addColor == null && baseColor == null) return null;
        if (addColor == null) return baseColor;
        if (baseColor == null) return addColor;
        int red = (baseColor.getRed() + addColor.getRed()) / 2;
        int green = (baseColor.getGreen() + addColor.getGreen()) / 2;
        int blue = (baseColor.getBlue() + addColor.getBlue()) / 2;
        int alpha = (baseColor.getAlpha() + addColor.getAlpha()) / 2;
        return new Color(red, green, blue, alpha);
    }

    public static Point getPosition(MouseEvent e) {
        return new Point(e.getComponent().getLocationOnScreen().x + e.getX(), e.getComponent().getLocationOnScreen().y + e.getY());
    }

    public static Color brighter(Color source, double factor) {
        int r = source.getRed();
        int g = source.getGreen();
        int b = source.getBlue();

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
        int i = (int) (1.0 / (1.0 - factor));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i);
        }
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;

        return new Color(Math.min((int) (r / factor), 255),
                Math.min((int) (g / factor), 255),
                Math.min((int) (b / factor), 255));
    }


}
