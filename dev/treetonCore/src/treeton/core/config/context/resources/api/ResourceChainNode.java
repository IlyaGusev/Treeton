/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.api;

import treeton.core.config.context.ContextException;

/**
 * Интерфейс описывает звено цепочки ресурсов (см. {@link ResourceChainModel}).
 */

public interface ResourceChainNode {

    /**
     * Получение модели ресурса (или цепочки ресурсов), к которому отсылает звено.
     *
     * @return Модель ресурса или цепочки ресурсов
     * @throws ContextException
     */

    public ResourceModel getResourceModel() throws ContextException;

    /**
     * Получение информации о том, надо ли передавать параметры цепочки ресурсов,
     * которой принадлежит звено, самому звену.
     *
     * @return true если надо, false в противном случае.
     */
    public boolean passParameters() throws ContextException;
}
