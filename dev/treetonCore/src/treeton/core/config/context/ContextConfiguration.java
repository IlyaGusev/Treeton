/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context;

import treeton.core.config.context.resources.api.ResourcesContext;
import treeton.core.config.context.resources.api.ResourcesContextManager;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.config.context.treenotations.TreenotationsContextManager;

public abstract class ContextConfiguration {
    private static Class configurationClass;
    private static ContextConfiguration instance;

    public static void createInstance() throws Exception {
        if (configurationClass == null) {
            throw new IllegalStateException("Configuration class must be registered");
        }

        if (instance == null) {
            instance = (ContextConfiguration) configurationClass.newInstance();
        }
    }

    public static void registerConfigurationClass(Class cls) {
        if (!ContextConfiguration.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Configuration class must implement the ContextConfiguration interface");
        }
        configurationClass = cls;
    }

    public static ContextConfiguration getInstance() {
        return instance;
    }

    public static TreenotationsContextManager trnsManager() {
        return getInstance().getTreenotationsContextManager();
    }

    public static ResourcesContextManager resourcesManager() {
        return getInstance().getResourcesContextManager();
    }

    public static ResourcesContext getResourcesContextByTrnContext(TreenotationsContext context) throws ContextException {
        return resourcesManager().get(ContextUtil.getFullName(context));
    }

    public static void tearDown() {
        instance.shutdown();
        instance = null;
    }

    public abstract TreenotationsContextManager getTreenotationsContextManager();

    public abstract ResourcesContextManager getResourcesContextManager();

    public abstract void shutdown();
}
