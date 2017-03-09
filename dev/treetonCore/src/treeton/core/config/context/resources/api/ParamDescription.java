/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources.api;

/**
 * Этот класс позволяет описывать один параметр ресурса обработки текста. У параметра
 * есть имя, тип (число, строка и т.п.), характеристика множественности и характеристика
 * опциональности. Все значения параметра у ресурса должен соответствовать типу параметра.
 * Если параметр множественный, то ресурс может иметь много значений этого параметра
 * одновременно. Если параметр опциональный, то ресурс может не иметь ни одного значения
 * этого параметра.
 */

public class ParamDescription {
    String name;
    boolean manyValued;
    boolean optional;
    Class type;

    public ParamDescription(String name, Class type, boolean manyValued, boolean optional) {
        this.name = name;
        this.manyValued = manyValued;
        this.type = type;
        this.optional = optional;
    }

    /**
     * Получение имени параметра
     *
     * @return имя параметра
     */

    public String getName() {
        return name;
    }

    /**
     * Извлечение информации о том, является ли параметр множественным.
     *
     * @return true если множественный, false в противном случае.
     */
    public boolean isManyValued() {
        return manyValued;
    }

    /**
     * Извлечение информации о том, является ли параметр не обязательным.
     *
     * @return true если не обязательный, false в противном случае.
     */

    public boolean isOptional() {
        return optional;
    }

    /**
     * Получение типа параметра
     *
     * @return java-class, соответствующий типу параметра
     */

    public Class getType() {
        return type;
    }

    public String toString() {
        String tp;
        if (type == Integer.class) {
            tp = "Integer";
        } else if (type == Long.class) {
            tp = "Long";
        } else if (type == Boolean.class) {
            tp = "Boolean";
        } else if (type == String.class) {
            tp = "String";
        } else {
            tp = "?";
        }

        if (manyValued) {
            tp = "List<" + tp + ">";
        }

        return "[" + (optional ? "optional " : "") + tp + " " + name + "]";
    }
}