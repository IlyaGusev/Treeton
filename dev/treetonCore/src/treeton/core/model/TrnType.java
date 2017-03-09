/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model;

import treeton.core.TString;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Каждая тринотация в системе Treeton относится к некоторому типу. У типа есть имя и уникальный идентификатор. Все
 * тринотации, относящиеся к одному типу, могут иметь атрибуты из одного и того же набора. Считается, что в рамках
 * одного типа тринотаций каждый атрибут определенного типа может принимать значения, относящиеся к определенному типу
 * данных. Данный класс является абстракцией для декларативного описания типа тринотаций. Обращаясь к объекту такого
 * класса (т.е. к конкретному типу тринотации) можно получить информацию о допустимых в рамках этого типа атрибутах и
 * типах данных для их значений. Далее в тексте вместо "тип данных для значений атрибута" говорится просто "тип
 * атрибута". Все атрибуты в рамках типа занумерованы с нуля. В рамках самого класса задается набор предопределенных
 * атрибутов (допустимых в рамках любого типа тринотаций):
 * <p>
 * string - строковый атрибут, обозначающий текстовый фрагмент, покрываемый тринотацией
 * length - числовой атрибут, обозначающий длину тринотации (в символах)
 * start - числовой атрибут, обозначающий сдвиг начала тринотации по отношению к началу текста (в символах)
 * end - числовой атрибут, обозначающий сдвиг конца тринотации по отношению к началу текста (в символах)
 * orthm - строковый атрибут, обозначающий символьную маску (редко используемый атавизм).
 * <p>
 * Во многих частях тритона работа с этими атрибутами носит специфический характер (те или иные действия
 * переопределяются специально для этих атрибутов)
 * <p>
 * Наследники данного класса должны реализовывать ряд абстрактных методов, которые описаны ниже. Это позволяет
 * настраивать тритон на различные  декларативные описания.
 * <p>
 * В Treeton существует одна реализация этого класса, в рамках которой типы тринотаций описываются в текстовых файлах
 * на специальном формальном языке (dcl-формат). См. {@link treeton.core.model.dclimpl.TrnTypeDclImpl},
 * {@link treeton.core.model.dclimpl.TrnTypeStorageDclImpl}
 */


public abstract class TrnType implements Comparable<TrnType> {
    public static final String string_FEATURE_name = "string";
    public static final String length_FEATURE_name = "length";
    public static final String orthm_FEATURE_name = "orthm";
    public static final String start_FEATURE_name = "start";
    public static final String end_FEATURE_name = "end";
    public static final int string_FEATURE = 0;
    public static final int length_FEATURE = 1;
    public static final int orthm_FEATURE = 2;
    public static final int start_FEATURE = 3;
    public static final int end_FEATURE = 4;
    private static Map<String, Integer> featuresMap;
    private static String[] featuresArr;
    private static Class[] typesArr;

    static {
        featuresMap = new HashMap<String, Integer>();
        featuresMap.put(string_FEATURE_name, string_FEATURE);
        featuresMap.put(length_FEATURE_name, length_FEATURE);
        featuresMap.put(orthm_FEATURE_name, orthm_FEATURE);
        featuresMap.put(start_FEATURE_name, start_FEATURE);
        featuresMap.put(end_FEATURE_name, end_FEATURE);

        featuresArr = new String[]{string_FEATURE_name, length_FEATURE_name, orthm_FEATURE_name,
                start_FEATURE_name, end_FEATURE_name};
        typesArr = new Class[]{TString.class, Integer.class, TString.class, Integer.class, Integer.class};
    }

    /**
     * Получение количества системных атрибутов
     *
     * @return количество системных атрибутов
     */

    public static int getNumberOfSystemFeatures() {
        return featuresArr.length;
    }

    /**
     * Получение набора системных атрибутов
     *
     * @return набор системных атрибутов
     */

    public static Collection<String> getSystemFeatures() {
        return Arrays.asList(featuresArr);
    }

    /**
     * Получение набора типов системных атрибутов
     *
     * @return набор типов системных атрибутов (в порядке, соответствующем порядку возвращения самих атрибутов
     * методом {@link TrnType#getSystemFeatures()}
     */

    public static Collection<Class> getSystemFeaturesTypes() {
        return Arrays.asList(typesArr);
    }

    /**
     * Этот метод определяет, является ли атрибут системным
     *
     * @param s имя атрибута
     * @return true, если является
     */

    public static boolean isSystemFeature(String s) {
        return featuresMap.containsKey(s);
    }

    /**
     * Получение объекта для работы с типами тринотаций, к которому относится данный тип.
     *
     * @return хранилище типов тринотаций
     */

    public abstract TrnTypeStorage getStorage() throws TreetonModelException;

    /**
     * Узнать номер атрибута в рамках данного тринотации типа по имени атрибута. Сюда включаются системные атрибуты.
     *
     * @param t имя атрибута
     * @return номер
     */

    public final int getFeatureIndex(String t) throws TreetonModelException {
        Integer res = featuresMap.get(t);
        if (res != null) {
            return res;
        }
        return _getFeatureIndex(t);
    }


    /**
     * Узнать тип атрибута в рамках данного типа тринотации по имени атрибута. Сюда включаются системные атрибуты.
     *
     * @param t имя атрибута
     * @return тип
     */

    public final Class getFeatureType(String t) throws TreetonModelException {
        Integer res = featuresMap.get(t);
        if (res != null) {
            return typesArr[res];
        }
        return _getFeatureType(t);
    }

    /**
     * Узнать имя атрибута в рамках данного типа тринотации по номеру атрибута. Сюда включаются системные атрибуты.
     *
     * @param i номер атрибута
     * @return имя атрибута
     */

    public final String getFeatureNameByIndex(int i) throws TreetonModelException {
        if (i < 0) {
            return null;
        }
        if (i < featuresArr.length) {
            return featuresArr[i];
        }
        return _getFeatureNameByIndex(i);
    }

    /**
     * Узнать тип атрибута в рамках данного типа тринотации по номеру атрибута. Сюда включаются системные атрибуты.
     *
     * @param i номер атрибута
     * @return номер
     */

    public final Class getFeatureTypeByIndex(int i) throws TreetonModelException {
        if (i < 0) {
            return null;
        }
        if (i < typesArr.length) {
            return typesArr[i];
        }
        return _getFeatureTypeByIndex(i);
    }

    /**
     * Узнать количество атрибутов для данного типа тринотации (включая системные)
     *
     * @return количество атрибутов
     */

    public final int getFeaturesSize() throws TreetonModelException {
        return featuresArr.length + _getFeaturesSize();
    }

    /**
     * Получить массив всех атрибутов для данного типа тринотации (включая системные)
     *
     * @return массив атрибутов
     */

    public final String[] getFeatureNames() throws TreetonModelException {
        String[] res = new String[getFeaturesSize()];
        System.arraycopy(featuresArr, 0, res, 0, featuresArr.length);
        fillFeatureNames(res, featuresArr.length);
        return res;
    }

    /**
     * Получить массив всех типов атрибутов для данного типа тринотации (включая системные)
     *
     * @return массив типов атрибутов (порядок соответствует порядку, в котором возвращаются сами атрибуты методом
     * {@link TrnType#getFeatureNames()} )
     */

    public final Class[] getFeatureTypes() throws TreetonModelException {
        Class[] res = new Class[getFeaturesSize()];
        System.arraycopy(typesArr, 0, res, 0, typesArr.length);
        fillFeatureTypes(res, typesArr.length);
        return res;
    }

    /**
     * Этот метод должен возвращать имя типа
     */

    public abstract String getName() throws TreetonModelException;

    /**
     * Особое понятие в Тритоне -- это тринотации-токены. Про них сказано здесь
     * {@link treeton.core.TreenotationStorage}, {@link treeton.core.Treenotation}, {@link treeton.core.Token}
     * Этот метод определяет, является ли данный тип типом тринотаций-токенов или нет.
     */

    public abstract boolean isTokenType() throws TreetonModelException;

    /**
     * Этот метод должен возвращать индекс типа -- число, уникально идентифицирующее тип. Вместе индексы всех типов
     * в контексте должны образовывать компактное множество чисел, начинающееся с 0
     *
     * @return номер типа
     */
    public abstract int getIndex() throws TreetonModelException;

    /**
     * Этот метод должен возвращать номер атрибута в рамках данного типа тринотаций по имени атрибута. Системные
     * атрибуты при реализации этого метода учитывать не следует. Единственное требование -- номера всех атрибутов
     * должны образовывать компактное множество чисел и наименьший из номеров должен быть равен
     * {@link treeton.core.model.TrnType#getNumberOfSystemFeatures()}.
     *
     * @param t имя атрибута
     * @return номер
     */

    protected abstract int _getFeatureIndex(String t) throws TreetonModelException;

    /**
     * Этот метод должен возвращать тип атрибута в рамках данного типа тринотаций по имени атрибута. Системные
     * атрибуты при реализации этого метода учитывать не следует.
     *
     * @param t имя атрибута
     * @return тип атрибута
     */

    protected abstract Class _getFeatureType(String t) throws TreetonModelException;

    /**
     * Этот метод должен возвращать имя атрибута в рамках данного типа тринотаций по номеру атрибута. Системные
     * атрибуты при реализации этого метода учитывать не следует.
     *
     * @param i номер атрибута
     * @return имя атрибута
     */

    protected abstract String _getFeatureNameByIndex(int i) throws TreetonModelException;

    /**
     * Этот метод должен возвращать тип атрибута в рамках данного типа тринотаций по номеру атрибута. Системные
     * атрибуты при реализации этого метода учитывать не следует.
     *
     * @param i номер атрибута
     * @return тип атрибута
     */

    protected abstract Class _getFeatureTypeByIndex(int i) throws TreetonModelException;

    /**
     * Этот метод должен возвращать общее количество атрибутов для данного типа тринотаций. Системные
     * атрибуты при реализации этого метода учитывать не следует.
     *
     * @return количество атрибутов (исключая системные)
     */

    protected abstract int _getFeaturesSize() throws TreetonModelException;

    /**
     * Этот метод должен заполнять входной массив всеми атрибутами для данного типа тринотации, начиная с некоторой
     * позиции.  Системные атрибуты при реализации этого метода учитывать не следует.
     *
     * @param arr    массив атрибутов
     * @param offset позиция, с которой следует начинать заполнение
     */

    protected abstract void fillFeatureNames(String[] arr, int offset) throws TreetonModelException;

    /**
     * Этот метод должен заполнять входной массив всеми типами атрибутов для данного типа тринотации, начиная с
     * некоторой позиции.  Системные атрибуты при реализации этого метода учитывать не следует. Порядок типов атрибутов
     * должен соответствовать порядку, в котором производит заполнение метод
     * {@link TrnType#fillFeatureNames(String[], int)}.
     *
     * @param arr    массив атрибутов
     * @param offset позиция, с которой следует начинать заполнение
     */

    protected abstract void fillFeatureTypes(Class[] arr, int offset) throws TreetonModelException;

    /**
     * Сравнение производится по имени
     */

    public int compareTo(TrnType t) {
        try {
            return getName().compareTo(t.getName());
        } catch (TreetonModelException e) {
            throw new RuntimeException("Exception when trying to compare TrnTypes", e);
        }
    }

    /**
     * Два типа равны, если они принадлежат одному хранилищу типов тринотаций и их имена равны
     */

    public boolean equals(Object obj) {
        if (!(obj instanceof TrnType))
            return false;
        try {
            return getStorage().equals(((TrnType) obj).getStorage())
                    && getName().equals(((TrnType) obj).getName());
        } catch (TreetonModelException e) {
            throw new RuntimeException("Problem with TreetonModel");
        }
    }

    /**
     * Хэшкод считается по имени и хранилищу типов тринотаций
     */

    public int hashCode() {
        try {
            return getStorage().hashCode() + 31 * getName().hashCode();
        } catch (TreetonModelException e) {
            throw new RuntimeException("Problem with TreetonModel");
        }
    }
}

