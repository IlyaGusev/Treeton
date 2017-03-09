/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import treeton.core.util.Utf8ResourceBundle;
import treeton.core.util.xml.XMLConfigurator;
import treeton.core.util.xml.XMLParser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Синглтон, обеспечивающий статическую конфигурацию Treeton. Статическая конфигурация -- это набор параметров, влияющих
 * на поведение системы, которые задаются перед запуском всей java-машины и не меняются в процессе ее работы. В нее
 * входят такие параметры как locale (язык интерфейса), путь к папке с описанием контекстов, e-mail-ы администраторов.
 */

public class BasicConfiguration {
    private static BasicConfiguration instance;
    private static URL rootURL = null;
    private static Class configurationClass = BasicConfiguration.class;
    XMLConfigurator configurator;

    public BasicConfiguration() throws Exception {
        if (rootURL != null) {
            configurator = new XMLConfigurator(rootURL, "./core.xml", null);
        } else {
            configurator = new XMLConfigurator("./core.xml", null);
        }
    }

    public static void setRootURL(URL rootURL) {
        BasicConfiguration.rootURL = rootURL;
    }

    public static void createInstance() throws Exception {
        if (instance == null) {
            instance = (BasicConfiguration) configurationClass.newInstance();
        }
    }

    public static BasicConfiguration getInstance() {
        return instance;
    }

    public static String localize(String bundleName, String s) {
        ResourceBundle bundle;
        bundle = Utf8ResourceBundle.getBundle(bundleName, new Locale(getInstance().getLocale(), "", ""));
        return bundle.getString(s);
    }

    public static URL getResource(String s) {
        return BasicConfiguration.class.getResource("/resources" + s);
    }

    public static void registerConfigurationClass(Class cls) {
        if (!BasicConfiguration.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Configuration class must extend the BasicConfiguration class");
        }
        configurationClass = cls;
    }

    public static URL getResourceDir(String s) throws MalformedURLException {
        URL url = getResource(s);
        String s1 = url.toString();
        return new URL(s1.substring(0, s1.length() - s.length() + 1));
    }

    public static void tearDown() {
        instance = null;
    }

    protected XMLConfigurator getConfigurator() {
        return configurator;
    }

    public URL getRootFolder() {
        return configurator.getRootFolder();
    }

    String getLocale() {
        NodeList l = configurator.getRootElement().getElementsByTagName("LOCALE");
        if (l == null || l.getLength() == 0) {
            return "en";
        } else {
            return ((Text) XMLParser.getFirstChild(l.item(0))).getData();
        }
    }

    protected Element getContextConfigurationElement() {
        NodeList l = configurator.getRootElement().getElementsByTagName("CONTEXT_CONFIGURATION");
        if (l == null || l.getLength() == 0)
            return null;
        return (Element) l.item(0);
    }

    public URL getContextConfigurationURL() throws MalformedURLException {
        NodeList l = getContextConfigurationElement().getElementsByTagName("URL");
        if (l == null || l.getLength() == 0)
            return null;
        return new URL(configurator.getRootFolder(), ((Text) XMLParser.getFirstChild(l.item(0))).getData());
    }

    public String[] getAdminMails() {
        ArrayList<String> res = new ArrayList<String>();
        NodeList l = configurator.getRootElement().getElementsByTagName("ADMIN_MAIL");
        if (l != null) {
            for (int i = 0; i < l.getLength(); i++) {
                res.add(((Text) XMLParser.getFirstChild(l.item(i))).getData());
            }
        }
        return res.toArray(new String[0]);
    }
}
