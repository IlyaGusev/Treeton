/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context;

import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.resources.api.ResourcesContextManager;
import treeton.core.config.context.resources.xmlimpl.ResourcesContextManagerXMLImpl;
import treeton.core.config.context.treenotations.TreenotationsContextManager;
import treeton.core.config.context.treenotations.xmlimpl.TreenotationsContextManagerXMLImpl;
import treeton.core.util.xml.XMLConfigurator;

import java.net.URL;

public class ContextConfigurationXMLImpl extends ContextConfiguration {
    XMLConfigurator configurator;
    private TreenotationsContextManager trnsManager;
    private ResourcesContextManager resourcesManager;

    public ContextConfigurationXMLImpl() throws Exception {
        configurator = new XMLConfigurator(
                BasicConfiguration.getInstance().getContextConfigurationURL(),
                "./context.xml",
                "./schema/contextSchema.xsd"
        );
        trnsManager = new TreenotationsContextManagerXMLImpl(configurator.getRootElement(), getRootFolder());
        resourcesManager = new ResourcesContextManagerXMLImpl(configurator.getRootElement(), getRootFolder());
    }

    public TreenotationsContextManager getTreenotationsContextManager() {
        return trnsManager;
    }

    public ResourcesContextManager getResourcesContextManager() {
        return resourcesManager;
    }

    public void shutdown() {
    }

    public URL getRootFolder() {
        return configurator.getRootFolder();
    }
}
