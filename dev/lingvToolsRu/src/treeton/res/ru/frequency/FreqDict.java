/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru.frequency;

import java.util.Iterator;

public interface FreqDict {
    /**
     * @return максимальный ipm среди входов частотного словаря с заданными начальной формой и частью речи.
     */
    double getIPM(String base, String pos);

    /**
     * @return максимальный ipm среди входов частотного словаря с заданной начальной формой.
     */
    double getMaxIPM(String base);

    /**
     * @return часть речи входа частотного словаря с максимальным ipm среди входов с заданной начальной формой,
     * или null, если такой начальной формы нет в словаре
     */
    String getMaxIPMPos(String base);

    /**
     * @return итератор по словам, содержащимся в словаре; в имплементациях желательно возвращать
     * итератор, упорядоченный по убыванию частотности
     */
    Iterator<String> wordIterator();
}
