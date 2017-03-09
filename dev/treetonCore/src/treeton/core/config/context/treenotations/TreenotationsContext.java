/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.treenotations;

import treeton.core.config.context.Context;
import treeton.core.model.*;

/**
 * Этот интерфейс описывает контекст лингвистического процессора, отражающий модель предметной области (контекст модели
 * предметной области). Лингвистический процессор в Treeton -- это преобразователь множества тринотаций. Поэтому в
 * самом первом приближении модель предметной области представляет собой перечисление допустимых типов тринотаций и
 * допустимых типов связей между тринотациями. Кроме этого, в модель входит описание допустимых в рамках каждого типа
 * атрибутов и типов данных, к которым должны относится их значения (см. интерфейс {@link treeton.core.model.TrnType}).
 */

public interface TreenotationsContext extends Context {
    /**
     * Получить объект для работы с типами тринотаций (без типов связей).
     */

    public TrnTypeStorage getTypes() throws TreetonModelException;

    /**
     * Получить описание типа тринотации по имени.
     */

    public TrnType getType(String s) throws TreetonModelException;

    /**
     * Получить описание типа тринотации по номеру (все типы в системе пронумерованы с нуля).
     */

    public TrnType getType(int i) throws TreetonModelException;


    /**
     * Получить описание типа связи по имени.
     */

    public TrnRelationType getRelType(String s) throws TreetonModelException;


    /**
     * Получить описание типа связи по номеру (все типы связей в системе пронумерованы с нуля).
     */

    public TrnRelationType getRelType(int i) throws TreetonModelException;

    /**
     * Получить объект для работы с типами связей (без типов тринотаций).
     */

    public TrnRelationTypeStorage getRelations() throws TreetonModelException;
}
