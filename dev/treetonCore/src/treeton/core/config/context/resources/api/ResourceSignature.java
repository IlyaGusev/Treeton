/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.api;

import treeton.core.config.context.ContextException;

/**
 * Интерфейс описывает сигнатуру типа ресурса обработки текста. Работая
 * с сигнатурой, можно узнавать ее состав последовательно проходя по всем
 * описаниям параметров. Предоставляется также возможность получения описания
 * параметра с конкретным именем.
 */

public interface ResourceSignature extends Iterable<ParamDescription> {

    /**
     * Получение описания параметра с определенным именем.
     *
     * @param name - имя параметра
     * @return описание параметра
     */

    public ParamDescription getParamDescription(String name) throws ContextException;
}
