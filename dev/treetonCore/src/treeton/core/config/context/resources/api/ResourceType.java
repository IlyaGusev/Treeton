/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.api;

import treeton.core.config.context.ContextElement;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.WrongParametersException;

/**
 * Интерфейс, описывающий тип ресурса обработки текста. Тип ресурса обработки
 * описывает то, какими могут быть ресурсы, к нему относящиеся. Все ресурсы относящиеся
 * к одному типу должны быть объектами одного java-класса. Этот класс фиксируется в
 * типе ресурса. Кроме того, в типе ресурса содержится описание того, какие у
 * ресурсов этого типа могут быть параметры. Это описание называется сигнатурой ресурса.
 * Модель сигнатуры ресурса описывается в {@link ResourceSignature}.
 */

public interface ResourceType extends ContextElement {

    /**
     * Получение java-класса, к которому должны относится все ресурсы
     * данного типа.
     *
     * @return java-класс, соответствующий данному типу ресурсов
     * @throws ClassNotFoundException
     */

    public Class getResourceClass() throws ContextException;

    /**
     * Получение сигнатуры ресурса данного типа
     *
     * @return модель сигнатуры ресурса
     * @throws WrongParametersException
     */

    public ResourceSignature getSignature() throws WrongParametersException;
}
