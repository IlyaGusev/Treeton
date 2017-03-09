/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

public class GUIUtil {

    public static final String btnNameOk = "Ok";
    public static final String btnNameCn = "Прекратить";
    public static final String btnNameAp = "Применить";

    public static void setAllSizes(JComponent c, Dimension d) {
        c.setMinimumSize(d);
        c.setMaximumSize(d);
        c.setPreferredSize(d);
        c.setSize(d);
    }

    public static void setAllSizes(JComponent c, int w, int h) {
        setAllSizes(c, new Dimension(w, h));
    }

    public static Icon getIconForType(int iconType) {
        switch (iconType) {
            case JOptionPane.ERROR_MESSAGE:
                return UIManager.getIcon("OptionPane.errorIcon");
            case JOptionPane.INFORMATION_MESSAGE:
                return UIManager.getIcon("OptionPane.informationIcon");
            case JOptionPane.WARNING_MESSAGE:
                return UIManager.getIcon("OptionPane.warningIcon");
            case JOptionPane.QUESTION_MESSAGE:
                return UIManager.getIcon("OptionPane.questionIcon");
        }
        return null;
    }

    public static Color stringToColor(String strColor) {
        return stringToColor(strColor, null);
    }

    public static Color stringToColor(String strColor, Color defVal) {
        Color rslt = defVal;
        try {
            String[] cc = strColor.split(",");
            if (cc.length == 3) {
                int r = Integer.parseInt(cc[0]);
                int g = Integer.parseInt(cc[1]);
                int b = Integer.parseInt(cc[2]);
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                rslt = new Color(r, g, b);
            }
        } catch (NumberFormatException x) {
        } catch (NullPointerException x) {
        }
        return rslt;
    }

    public static String colorToString(Color color)
            throws NullPointerException {
        StringBuffer rslt = new StringBuffer();
        if (color != null) {
            rslt.append(color.getRed()).append(',').
                    append(color.getGreen()).append(',').
                    append(color.getBlue());
        } else {
            throw new NullPointerException();
        }
        return rslt.toString();
    }

    public static String colorToStringSafe(Color color) {
        String rslt = "";
        try {
            rslt = colorToString(color);
        } catch (NullPointerException x) {
        }
        return rslt;
    }

    public static Image makeColorTransparent(Image im,
                                             final Color color) {
        ImageFilter filter = new RGBImageFilter() {
            // Alpha bits are set to opaque, regardless of what they
            // might have been already.
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent, but
                    // preserve the other information about the color
                    // of the pixel.
                    return 0x00FFFFFF & rgb;
                } else {
                    // leave the pixel untouched
                    return rgb;
                }
            }
        }; // end of inner class

        // Setup to use transparency filter
        ImageProducer ip = new FilteredImageSource(
                im.getSource(), filter);

        // Pull the old image thru this filter and create a new one
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    /**
     * Создаёт массив кнопок
     */
//  public void addToolBarButtons(Object[][] btnData,
//      JToolBar tb, ActionListener listener,
//      Dimension separatorSize)
//      throws NullPointerException {
//    for (int i=0; i<btnData.length; i++) {
//      tb.getActionMap().get().
//
//    }
//  }


}
