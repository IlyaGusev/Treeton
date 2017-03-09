/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import org.w3c.dom.Element;

import javax.swing.*;

public interface ElementValidator {
    String isValid(JTable table, Element elem, int column);

    String isValid(JTable table, int row, int column);
}
