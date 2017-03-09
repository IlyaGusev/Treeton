/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

public interface CheckListListener {
    // индекс изменившегося элемента
    // -1 - произошло групповое изменение
    public void checkChanged(JCheckList object, int itemIndex);
}
