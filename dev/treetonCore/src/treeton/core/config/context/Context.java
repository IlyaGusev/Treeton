/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context;

import java.util.Iterator;

/**
 * Интерфейс описывает абстрактное понятие контекста -- некоторого пространства,
 * в рамках которого могут существовать разного рода объекты (см. {@link ContextElement}).
 * Контекст может принадлежать родительскому контексту (максимум одному). У
 * контекстов есть имена. На имя контекста накладывается ограничение: все символы
 * имени должны удовлетворять методу {@link Character#isJavaIdentifierPart(char)}
 * (такую проверку осуществляет метод {@link ContextUtil#isWellFormedName(String)}).
 * Различаются полные и короткие имена контекстов. Полное имя контекста -- это
 * строка, составленная из коротких имен его предков и его самого, записанных через
 * точку. Сам контекст возвращает короткое имя, а его полное имя можно получить
 * с помощью метода {@link ContextUtil#getFullName(Context)}.
 * <p>
 * Подразумевается, что все объекты, которые будут помещаться в контекст, так же будут
 * иметь правильно составленные короткие имена. В этом случае можно говорить о полных
 * именах объектов в рамках контекста. Получить полное имя объекта по короткому можно
 * с помощью метода {@link ContextUtil#fullName(Context, String)}
 * <p>
 * Базовые операции над контекстами описываются в классе {@link ContextUtil}.
 * <p>
 * Допускается, чтобы в java-памяти объектов, соответствующих одному контексту, было
 * больше одного. Но в этом случае для корректной работы системы необходимо, чтобы
 * методы {@link Object#equals} и {@link Object#hashCode} были переопределены корректно
 * (разные варианты одного и того же контекста были бы равны по equals и имели однаковый
 * hashCode).
 */

public interface Context {

    /**
     * Получение родительского контекста
     *
     * @return родительский контекст. В случае, когда такового нет, null.
     * @throws ContextException
     */

    public Context getParent() throws ContextException;

    /**
     * Получение имени контекста
     *
     * @return короткое имя контекста (не содержит точек)
     * @throws ContextException
     */
    public String getName() throws ContextException;

    /**
     * Получение информации о количестве контекстов-детей
     *
     * @return количество контекстов-детей
     * @throws ContextException
     */

    int getChildCount() throws ContextException;

    /**
     * Получение всех контекстов-детей данного контекста
     *
     * @return итератор из контекстов
     * @throws ContextException
     */

    Iterator<Context> childIterator() throws ContextException;
}
