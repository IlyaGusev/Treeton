/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.api;

import treeton.core.config.context.Context;
import treeton.core.config.context.ContextException;

import java.net.URL;
import java.util.Iterator;

/**
 * Интерфейс отражает модель контекста ресурсов -- пространтсва, в рамках
 * которого существуют ресурсы обработки текста (РОТ) и цепочки ресурсов обработки
 * текста. Каждый РОТ относится к некоторому типу. Типы тоже принадлежат контексту
 * ресурсов. Модели РОТ, типов РОТ и цепочек РОТ описываются следующими интерфейсами:
 * {@link ResourceModel}, {@link ResourceType}, {@link ResourceChainModel}.
 * Контекст ресурсов является абстрактным контекстом, описываемом в {@link Context}.
 * К нему и его содержимому применимы все описанные там правила именования.
 * <p>
 * Каждому контексту соответствует некоторое место в "глобальной" файловой системе.
 * Подразумевается, что все пути, которые будут встречаться в элементах, принадлежащих
 * контексту, являются относительными и отсчитываются от этого места.
 */

public interface ResourcesContext extends Context {

    /**
     * Получение модели типа ресурса обработки текста из данного контекста.
     *
     * @param name   - имя типа ресурса (полное или короткое)
     * @param closed - флаг, определяющий, требуется ли производить поиск в родительских контекстах.
     * @return модель типа ресурса. Если не найден, то null.
     * @throws ContextException
     */

    public ResourceType getResourceType(String name, boolean closed) throws ContextException;

    /**
     * Получение модели ресурса обработки текста из данного контекста.
     *
     * @param name   - имя ресурса (полное или короткое)
     * @param closed - флаг, определяющий, требуется ли производить поиск в родительских контекстах.
     * @return модель ресурса. Если не найден, то null.
     * @throws ContextException
     */

    public ResourceModel getResourceModel(String name, boolean closed) throws ContextException;

    /**
     * Получение модели цепочки ресурсов обработки текста из данного контекста.
     *
     * @param name   - имя цепочки ресурсов (полное или короткое)
     * @param closed - флаг, определяющий, требуется ли производить поиск в родительских контекстах.
     * @return модель цепочки ресурсов. Если не найдена, то null.
     * @throws ContextException
     */

    public ResourceChainModel getResourceChainModel(String name, boolean closed) throws ContextException;

    /**
     * Получение количества ресурсов обработки текста в данном контексте, НЕ включая ресурсы,
     * принадлежащие контекстам-предкам.
     *
     * @return количество ресурсов
     * @throws ContextException
     */

    public int getResourcesCount() throws ContextException;

    /**
     * Получение количества цепочек ресурсов обработки текста в данном контексте, НЕ включая
     * цепочки, принадлежащие контекстам-предкам.
     *
     * @return количество ресурсов
     * @throws ContextException
     */

    public int getResourceChainsCount() throws ContextException;

    /**
     * Получение всех ресурсов обработки текста в данном контексте, НЕ включая
     * ресурсы, принадлежащие контекстам-предкам.
     *
     * @return Итератор, возвращающий полные имена искомых ресурсов. Если 0, то пустой итератор. НЕ null.
     * @throws ContextException
     */

    public Iterator<String> resourcesIterator() throws ContextException;

    /**
     * Получение всех цепочек ресурсов обработки текста в данном контексте, НЕ включая
     * цепочки, принадлежащие контекстам-предкам.
     *
     * @return Итератор, возвращающий полные имена искомых цепочек. Если 0, то пустой итератор. НЕ null.
     * @throws ContextException
     */

    public Iterator<String> resourceChainsIterator() throws ContextException;

    /**
     * Получение основной цепочки ресурсов обработки текста в данном контексте.
     *
     * @return Полное имя искомой цепочки. Null, если таковая отсутствует.
     * @throws ContextException
     */

    public String getMainResourceChain() throws ContextException;

    /**
     * Получение привязки данного контекста к глобальной файловой системе.
     *
     * @return URL, соответствующий контексту.
     * @throws ContextException
     */

    public URL getFolder() throws ContextException;

    /**
     * Получение имени одного (любого) ресурса, относящегося к определенному типу ресурсов
     * обработки текста.
     *
     * @param tp - модель типа ресурса обработки текста
     * @return полное имя искомого ресурса. Если не найден, то null.
     * @throws ContextException
     */

    String findInstanceOfResourceType(ResourceType tp) throws ContextException;
}
