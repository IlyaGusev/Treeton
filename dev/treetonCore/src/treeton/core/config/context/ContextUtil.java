/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context;

/**
 * Этот класс позволяет производить элементарные операции с контекстами.
 *
 * @see Context
 */

public class ContextUtil {
    /**
     * Определяет, является ли один контекст потомком другого.
     *
     * @param a - контекст потомок
     * @param b - контекст предок
     * @return true, если a является потомком b, false в противном случае.
     * @throws ContextException
     */

    public static boolean inherits(Context a, Context b) throws ContextException {
        while (a != null) {
            if (a.equals(b)) {
                return true;
            }
            a = a.getParent();
        }
        return false;
    }

    /**
     * Определяет полное имя контекста, рекурсивно проходя по предкам.
     *
     * @param c - контекст
     * @return полное имя контекста
     * @throws ContextException
     */

    public static String getFullName(Context c) throws ContextException {
        Context p = c.getParent();
        if (p == null) {
            return c.getName();
        } else {
            return getFullName(p) + "." + c.getName();
        }
    }

    /**
     * Определяет, может ли строка являтся коротким именем контекста или объекта в
     * рамках контекста
     *
     * @param s строка
     * @return true, если строка может являтся коротким именем контекста, иначе false
     */

    public static boolean isWellFormedName(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isJavaIdentifierPart(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Вычисляет полное имя некоторого объекта в рамках контекста.
     *
     * @param c - контекст, в рамках которого вычисляется полное имя.
     * @param s - короткое имя объекта
     * @return полное имя объекта в рамках контекста
     * @throws ContextException
     */

    public static String fullName(Context c, String s) throws ContextException {
        if (s.indexOf(".") != -1)
            return s;
        return getFullName(c) + "." + s;
    }

    /**
     * Вычисляет полное имя некоторого объекта в рамках контекста.
     *
     * @param contextName - имя контекста, в рамках которого вычисляется полное имя.
     * @param s           - короткое имя объекта
     * @return полное имя объекта в рамках контекста
     * @throws ContextException
     */

    public static String fullName(String contextName, String s) throws ContextException {
        if (s.indexOf(".") != -1)
            return s;
        return contextName + "." + s;
    }

    /**
     * Определяет короткое имя объекта по полному имени
     *
     * @param s - полное имя объекта
     * @return короткое имя объекта
     */

    public static String shortName(String s) {
        return s.substring(s.lastIndexOf(".") + 1);
    }
}
