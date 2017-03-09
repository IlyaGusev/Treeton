/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model;

/**
 * Абстрактный класс для работы с описаниями типов связей внутри контекста предметной области. Для обеспечения корректной работы
 * в рамках Treeton класс, реализующий описание допустимых типов связей должен быть наследником этого класса.
 * <p>
 * В этом классе определяются два предопределенных типа связи: root и root_path. Первый из них чаще всего используется
 * для вложения одних тринотаций в другие (в большей части задач дополнительно типизировать root-связи не имеет смысла).
 * Второй тип используется в некоторых устаревших частях Treeton.
 */

public abstract class TrnRelationTypeStorage {
    public static final String root_RELATION_name = "root";
    public static final String root_path_RELATION_name = "root_path";
    public static final int root_RELATION = 0;
    public static final int root_path_RELATION = 1;
    /**
     * Количество системных типов
     */

    public static final int numberOfSystemTypes = 2;
    private static RootTrnRelationType root = new RootTrnRelationType();
    private static RootPathTrnRelationType root_path = new RootPathTrnRelationType();

    /**
     * Получить описание типа связи по имени. Сюда включаются системные типы.
     */

    public final TrnRelationType get(String s) throws TreetonModelException {
        if (root_RELATION_name.equals(s)) {
            return root;
        } else if (root_path_RELATION_name.equals(s)) {
            return root_path;
        }
        return _get(s);
    }

    /**
     * Получить описание типа связи по номеру (все типы в системе пронумерованы с нуля). Сюда включаются системные типы.
     */

    public final TrnRelationType get(int i) throws TreetonModelException {
        if (i == root_RELATION) {
            return root;
        } else if (i == root_path_RELATION) {
            return root_path;
        }
        return _get(i);
    }

    /**
     * Получить массив всех типов связей, зарегистрированных в контексте. Массив необязательно должен быть сортированным.
     * Сюда включаются системные типы.
     */

    public final TrnRelationType[] getAllTypes() throws TreetonModelException {
        TrnRelationType[] res = new TrnRelationType[size()];
        res[0] = root;
        res[1] = root_path;
        _fillInTypes(res, numberOfSystemTypes);
        return res;
    }

    /**
     * Получить количество всех типов связей, зарегистрированных в контексте.
     */

    public final int size() throws TreetonModelException {
        return _size() + numberOfSystemTypes;
    }

    /**
     * Этот метод должен возвращать описание типа связи по имени. Системные
     * типы связей при реализации этого метода учитывать не следует.
     *
     * @param s имя типа связи
     * @return описание типа связи
     */

    public abstract TrnRelationType _get(String s) throws TreetonModelException;

    /**
     * Этот метод должен возвращать описание типа связи по номеру. Системные типы связей при реализации этого метода
     * учитывать не следует. Единственное требование -- номера всех типов связей должны образовывать компактное множество
     * чисел и наименьший из номеров должен быть равен numberOfSystemTypes (в текущей реализации - 2).
     *
     * @param i номер типа связи
     * @return описание типа связи
     */

    public abstract TrnRelationType _get(int i) throws TreetonModelException;

    /**
     * Этот метод должен заполнять входной массив всеми типами связей в контексте, начиная с некоторой
     * позиции.  Системные типы связи при реализации этого метода учитывать не следует.
     *
     * @param arr    массив типов связей
     * @param offset позиция, с которой следует начинать заполнение
     */

    public abstract void _fillInTypes(TrnRelationType[] arr, int offset) throws TreetonModelException;

    /**
     * Этот метод должен возвращать общее количество типов связей в контексте. Системные
     * типы связей при реализации этого метода учитывать не следует.
     *
     * @return количество типов связей (исключая системные)
     */

    public abstract int _size() throws TreetonModelException;
}


