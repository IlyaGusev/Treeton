/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model;

/**
 * Интерфейс для работы с описаниями типов тринотаций внутри контекста предметной области.
 */

public interface TrnTypeStorage {
    /**
     * Получить описание типа тринотации по имени.
     */

    public TrnType get(String s) throws TreetonModelException;

    /**
     * Получить описание типа тринотации по номеру (все типы внутри контекста пронумерованы с нуля).
     */

    public TrnType get(int i) throws TreetonModelException;

    /**
     * Получить массив всех типов тринотаций, зарегистрированных в контексте. Массив необязательно должен быть сортированным.
     */

    public TrnType[] getAllTypes() throws TreetonModelException;

    /**
     * Получить количество всех типов тринотаций, зарегистрированных в контексте.
     */

    public int size() throws TreetonModelException;
}
