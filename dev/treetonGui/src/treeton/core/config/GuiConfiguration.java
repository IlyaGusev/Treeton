/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config;

import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.gui.labelgen.TrnLabelGenerator;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GuiConfiguration {
    private static GuiConfiguration instance;

    Map<Class, Map<String, Class>> componentClass2generatorClass = new HashMap<Class, Map<String, Class>>();

    public static void createInstance() {
        if (instance == null) {
            instance = new GuiConfiguration();
        }
    }

    public static GuiConfiguration getInstance() {
        return instance;
    }

    public TrnLabelGenerator createTrnLabelGenerator(Component c, TrnType t) throws IllegalAccessException, InstantiationException {
        Map<String, Class> mp = componentClass2generatorClass.get(c.getClass());
        if (mp == null) {
            return null;
        }

        Class cls = null;
        try {
            cls = mp.get(t.getName());
        } catch (TreetonModelException e) {
            //do nothing
        }

        if (cls == null) {
            return null;
        }

        return (TrnLabelGenerator) cls.newInstance();
    }

    public void registerLabelGenerator(Class componentClass, String typeName, Class labelGeneratorClass) {
        Map<String, Class> mp = componentClass2generatorClass.get(componentClass);
        if (mp == null) {
            mp = new HashMap<String, Class>();
            componentClass2generatorClass.put(componentClass, mp);
        }


        mp.put(typeName, labelGeneratorClass);
    }

    public Color getColorForType(String name) {
        return new Color(
                (name.hashCode() & 15) * 8 + 128,
                ((name.hashCode() >> 4) & 15) * 8 + 128,
                ((name.hashCode() >> 8) & 15) * 8 + 128
        );
    }

    public HashMap<String, TrnLabelGenerator> getAllLabelGenerators(Class component) throws IllegalAccessException, InstantiationException {
        HashMap<String, TrnLabelGenerator> result = new HashMap<String, TrnLabelGenerator>();

        Map<String, Class> map = componentClass2generatorClass.get(component);
        if (map != null) {
            for (Map.Entry<String, Class> e : map.entrySet()) {
                result.put(e.getKey(), (TrnLabelGenerator) e.getValue().newInstance());
            }
        }

        return result;
    }
}
