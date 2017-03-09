/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.api;

import treeton.core.config.context.ContextException;

/**
 * Этот интерфейс позволяет работать с системой связанных друг с другом
 * контекстов ресурсов ({@link ResourcesContext}).
 * Он позволяет получать контекст по полному имени, узнавать корневой
 * контекст (единственный контекст, у которого нет родительского. Всегда
 * должен присутствовать), а также получать список всех имеющихся контекстов.
 */

public interface ResourcesContextManager {

    /**
     * Получение контекста по полному имени. Об именах смотри {@link treeton.core.config.context.Context}
     *
     * @param fullName - полное имя контекста
     * @return Модель искомого контекста ресурсов. Если найти не удалось -- null.
     * @throws ContextException
     */

    public ResourcesContext get(String fullName) throws ContextException;

    /**
     * Получение корневого контекста. Он всегда должен быть.
     *
     * @return Корневой контекст.
     * @throws ContextException
     */

    ResourcesContext getRootContext() throws ContextException;

    /**
     * Получение набора всех контекстов, принадлежащих системе.
     *
     * @return массив контекстов
     * @throws ContextException
     */

    ResourcesContext[] getAllContexts() throws ContextException;
}
