/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context;

import treeton.core.config.context.resources.api.ResourcesContext;

/**
 * Интерфейс описывает абстрактный объект, который может принадлежать
 * контексту. Допускается, чтобы в java-памяти объектов, соответствующих
 * одному абстрактному объекту, было больше одного. Но в этом случае для
 * корректной работы системы необходимо, чтобы методы {@link Object#equals}
 * и {@link Object#hashCode} были переопределены корректно (разные варианты
 * одного и того же объекта были бы равны по equals и имели однаковый
 * hashCode).
 *
 * @see Context
 */

public interface ContextElement {
    /**
     * Получение короткого имени данного ресурса (о коротких именах см. {@link treeton.core.config.context.Context}).
     *
     * @return короткое имя ресурса
     */

    public String getName() throws ContextException;

    /**
     * Получение контекста, к которому непосредственно относится ресурс.
     *
     * @return модель контекста ресурсов
     */

    public ResourcesContext getInitialContext() throws ContextException;
}
