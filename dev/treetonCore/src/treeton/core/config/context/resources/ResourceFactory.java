/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.api.ResourceChainModel;
import treeton.core.config.context.resources.api.ResourceModel;
import treeton.core.config.context.resources.api.ResourceSignature;
import treeton.core.config.context.resources.api.ResourcesContext;

import java.util.Map;

/**
 * Интерфейс фабрики ресурсов -- объекта, позволяющего создавать конкретные экземпляры ресурсов обработки текстов
 * и их цепочек.
 */


public interface ResourceFactory {
    /**
     * Создание ресурса, исходя из его модели.
     *
     * @return экземпляр ресурса
     */

    public Resource createResource(ResourceModel model) throws ContextException;

    /**
     * Создание ресурса, исходя из его имени и контекста ресурсов.
     *
     * @return экземпляр ресурса
     */

    public Resource createResource(ResourcesContext context, String name) throws ContextException;


    /**
     * Создание цепочки ресурсов, исходя из ее модели.
     *
     * @return экземпляр ресурса
     */
    public ResourceChain createResourceChain(ResourceChainModel model) throws ContextException;

    /**
     * Создание цепочки ресурсов, исходя из ее имени и контекста ресурсов.
     *
     * @return экземпляр ресурса
     */
    public ResourceChain createResourceChain(ResourcesContext context, String name) throws ContextException;

    /**
     * Приведение заданного отображения с параметрами в соответствие с сигнатурой ресурса.
     * Делается попытка привести все значения параметров к тем типам, которые указаны в сигнатуре.
     *
     * @throws WrongParametersException возникает в случае, если не удается привести тот или иной
     *                                  параметр к соответствующему типу или не хватает какого-то обязательного параметра.
     */
    public void validateParams(ResourceSignature signature, Map<String, Object> params) throws WrongParametersException;
}
