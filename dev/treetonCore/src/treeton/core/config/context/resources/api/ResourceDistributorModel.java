/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.api;

import treeton.core.config.context.ContextException;

import java.util.List;

/**
 * Интерфейс описывает абстрактный ресурс-распределитель. Подобно цепочке ресурсов, он состоит из
 * звений, отсылающих либо к некоторому ресурсу обработки текста, либо к другой цепочке ресурсов,
 * но они не организованы последовательно, а доступны по строковому ключу (в зависимости от
 * некоторого условия).
 */

public interface ResourceDistributorModel extends ResourceModel {

    /**
     * Получение количества звений цепочки
     *
     * @return количество звений цепочки
     */

    public int size() throws ContextException;

    /**
     * Получение звена по ключу. Ключ представляет
     * абстрактное условие, записанное в виде строки.
     *
     * @param condition условие
     * @return модель звена цепочки
     */

    public ResourceChainNode get(String condition) throws ContextException;

    /**
     * Получение списка всех условий-ключей, поддерживаемых распределителем.
     *
     * @return список условий
     */

    public List<String> listConditions() throws ContextException;
}
