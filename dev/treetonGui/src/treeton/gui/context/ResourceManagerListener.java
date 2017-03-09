/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import treeton.core.config.context.resources.ResourceChain;
import treeton.core.config.context.resources.api.ResourcesContext;

public interface ResourceManagerListener {
    void chainAdded(ResourcesContext context, ResourceChain chain);

    void chainRenamed(ResourcesContext context, ResourceChain chain);

    void chainRemoved(ResourcesContext context, ResourceChain chain);

    void chainStarted(ResourcesContext context, ResourceChain chain);

    void chainFinished(ResourcesContext context, ResourceChain chain);

    void contextSwitched();
}
