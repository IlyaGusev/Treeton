/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import treeton.core.util.sut;
import treeton.gui.util.SettingsData;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TreetonSessionProperties extends SettingsData {
    private static TreetonSessionProperties instance = null;
    private String propertiesFilePath = "";

    private TreetonSessionProperties() {
    }

    public static TreetonSessionProperties getInstance() {
        if (instance == null) {
            instance = new TreetonSessionProperties();
            instance.getProperties();
        }
        return instance;
    }

    private void getProperties() {
        //TODO: разобраться с путями на предмет пробелов в названиях директорий и т.п.
        //TODO: 07.07.05 (Старостин)

        propertiesFilePath = sut.getPropertiesFileForClass(this.getClass()).replaceAll("%20", " ");

        Properties props = new Properties();
        try {
            InputStream is = new FileInputStream(propertiesFilePath);
            props.load(is);
            fromProperties(props);
        } catch (IOException e) {
            System.out.print("Can not load properties.\nFile: ");
            System.out.println((propertiesFilePath != null) ?
                    propertiesFilePath : "<null>");
        }
    }

    public void store() {
        try {
            toProperties(new Properties()).store(new FileOutputStream(propertiesFilePath), "");
        } catch (IOException e) {
            // do nothing
        }
    }
}
