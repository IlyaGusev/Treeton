/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Utf8ResourceBundle {
    public static ResourceBundle getBundle(String baseName, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("resources/locale/" + baseName, locale);
        return createUtf8PropertyResourceBundle(bundle);
    }

    private static ResourceBundle createUtf8PropertyResourceBundle(ResourceBundle bundle) {
        if (!(bundle instanceof PropertyResourceBundle)) return bundle;

        return new Utf8PropertyResourceBundle((PropertyResourceBundle) bundle);
    }

    private static class Utf8PropertyResourceBundle extends ResourceBundle {
        PropertyResourceBundle bundle;

        private Utf8PropertyResourceBundle(PropertyResourceBundle bundle) {
            this.bundle = bundle;
        }

        /* (non-Javadoc)
        * @see java.util.ResourceBundle#getKeys()
        */
        public Enumeration<String> getKeys() {
            return bundle.getKeys();
        }

        /* (non-Javadoc)
        * @see java.util.ResourceBundle#handleGetObject(java.lang.String)
        */
        protected Object handleGetObject(String key) {
            String value = (String) bundle.handleGetObject(key);
            try {
                return new String(value.getBytes("ISO-8859-1"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Shouldn't fail - but should we still add logging message?
                return null;
            }
        }

    }
}