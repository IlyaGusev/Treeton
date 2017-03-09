/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.attreditor;

public class TableElement {
    String pName;
    Object pValue;
    Boolean pMultiple;
    String pType;
    Boolean isParent;
    Boolean isEditable;
    Boolean isOptional;

    /**
     * @param pName
     * @param pValue
     * @param pMultiple
     * @param pType
     */
    public TableElement(String pName, Object pValue, Boolean pMultiple, String pType, Boolean isPar, Boolean isEditable, Boolean isOptional) {
        super();
        this.isOptional = isOptional;
        this.pName = pName;
        this.pValue = pValue;
        this.pMultiple = pMultiple;
        this.pType = pType;
        this.isParent = isPar;
        this.isEditable = isEditable;
    }

    public TableElement() {
        super();
    }
}
