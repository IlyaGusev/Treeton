/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.api;

import treeton.core.config.context.ContextElement;
import treeton.core.config.context.ContextException;

import java.util.Map;

/**
 * Интерфейс описывает модель данных, которые необходимы менеджеру ресурсов
 * для создания ресурса обработки текста.
 */

public interface ResourceModel extends ContextElement {

    /**
     * Получение параметров данного ресурса, определенных в рамках контекста.
     *
     * @return отображение с названий параметров на их значения
     */

    public Map<String, Object> getInitialParameters() throws ContextException;

    /**
     * Получение типа данного ресурса
     *
     * @return объект, описывающий модель типа ресурса, к которому относится ресурс.
     * @throws ContextException
     */

    public ResourceType getType() throws ContextException;
}
