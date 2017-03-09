/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import java.awt.*;

public class ColorUtils {
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
}
