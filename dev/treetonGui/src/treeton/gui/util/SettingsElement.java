/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

public class SettingsElement {

    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_INTEGER = 2;
    public static final int TYPE_STRING = 3;
    public static final int TYPE_DOUBLE = 4;
    public static final int TYPE_ARRAY = 5;
    public static final int[] typesList = {
            TYPE_BOOLEAN, TYPE_INTEGER, TYPE_STRING, TYPE_DOUBLE, TYPE_ARRAY};
    public static final String defaultArrayDelimiter = ",";
    /**
     * Имя ключа в файле настроек. Нельзя использовать в качестве
     * ключа строку, начинающуюся с префикса "domestic.".
     * Эти ключи используются в служебных целях.
     */
    public String propertyName;
    /**
     * Название в окне настроек
     */
    public String displayName;
    /**
     * Тип элемента (см. TYPE_*)
     */
    public int valueType;
    public String arrayDelimiter;
    public int arrayType;
    /**
     * Объект, хранящий значение элемента.
     * Если значение отсутствует, то null.
     */
    protected Object valueObject;

    public SettingsElement(String pName, int vType) {
        propertyName = pName;
        valueType = vType;
        arrayDelimiter = defaultArrayDelimiter;
        arrayType = TYPE_STRING;
    }

    public String toString() {
        StringBuffer rslt = new StringBuffer();
        if (valueType == TYPE_BOOLEAN ||
                valueType == TYPE_INTEGER ||
                valueType == TYPE_STRING ||
                valueType == TYPE_DOUBLE) {
            if (valueObject != null) {
                rslt.append(valueObject.toString());
            }
        } else if (valueType == TYPE_ARRAY) {
            rslt.append(valueObject != null ?
                    ((Object[]) valueObject).length :
                    0);
            rslt.append(',').append(arrayType);
        }
        return rslt.toString();
    }

    public void parse(String s) {
        try {
            if (TYPE_BOOLEAN == valueType) {
                valueObject = Boolean.valueOf(s);
            } else if (TYPE_INTEGER == valueType) {
                valueObject = Integer.valueOf(s);
            } else if (TYPE_STRING == valueType) {
                valueObject = s;
            } else if (TYPE_DOUBLE == valueType) {
                valueObject = Double.valueOf(s);
            } else if (TYPE_ARRAY == valueType) {
                String[] sa = s.split(",", 2);
                if (sa.length == 2) {
                    int n = Integer.parseInt(sa[0]);
                    int tp = Integer.parseInt(sa[1]);
                    valueObject = (n > 0) ? new SettingsElement[n] : null;
                    arrayType = tp;
                }
            }
        } catch (NumberFormatException e) {
            valueObject = null;
        }
    }

    public boolean hasValue() {
        return (valueObject != null);
    }

    public void setValue(Object val) {
        valueObject = val;
    }

    public Object val() {
        return valueObject;
    }

}
