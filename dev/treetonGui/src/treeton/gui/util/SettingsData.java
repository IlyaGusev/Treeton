/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import java.util.*;

public class SettingsData {
    public static final String domesticPrefix = "domestic.";
    public static final String listPrefix = domesticPrefix + "all";

    protected Hashtable elements;

    public SettingsData() {
        elements = new Hashtable();
    }

    public static SettingsData createSettingsArray(Properties props) {
        SettingsData rslt = new SettingsData();
        rslt.fromProperties(props);
        return rslt;
    }

    public void fromProperties(Properties props) {
        String namesAndTypes = props.getProperty(listPrefix);
        elements.clear();
        if (namesAndTypes != null) {
            StringTokenizer st = new StringTokenizer(namesAndTypes, ",");
            while (st.hasMoreTokens()) {
                String elmItem = st.nextToken();
                int i = elmItem.indexOf('(');
                if (i > 0) {
                    int j = elmItem.indexOf(')', i + 2);
                    if (j > 0) {
                        String name = elmItem.substring(0, i).trim();
                        if (name.length() > 0) {
                            String strType = elmItem.substring(i + 1, j);
                            int type = Integer.parseInt(strType.trim());
                            if (Arrays.binarySearch(SettingsElement.typesList, type) >= 0) {
                                SettingsElement elm = new SettingsElement(name, type);
                                String sVal = props.getProperty(name);
                                elm.parse(sVal);
                                put(elm);
                                if (elm.valueType == SettingsElement.TYPE_ARRAY) {
                                    getArrayFromProperties(name, props);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public Properties toProperties(Properties props) {
        props.clear();
        StringBuffer all = new StringBuffer();
        String curDelim = "";
        for (Enumeration en = elements.keys(); en.hasMoreElements(); ) {
            String key = (String) en.nextElement();
            SettingsElement elm = (SettingsElement) elements.get(key);
            if (elm.hasValue() || elm.valueType == SettingsElement.TYPE_ARRAY) {
                props.setProperty(elm.propertyName, elm.toString());
            }
            if (elm.valueType == SettingsElement.TYPE_ARRAY) {
                putArrayToProperties(elm.propertyName, props);
            }
            all.append(curDelim).append(elm.propertyName).
                    append('(').append(elm.valueType).append(')');
            curDelim = ", ";
        }
        if (all.length() > 0) {
            props.setProperty(listPrefix, all.toString());
        }
        return props;
    }

    public SettingsElement put(SettingsElement elm) {
        elements.put(elm.propertyName, elm);
        return elm;
    }

    public SettingsElement put(String name, boolean v) {
        SettingsElement elm = get(name);
        if (elm == null) {
            elm = new SettingsElement(name, SettingsElement.TYPE_BOOLEAN);
            put(elm);
        }
        elm.valueObject = new Boolean(v);
        return elm;
    }

    public SettingsElement put(String name, int v) {
        SettingsElement elm = get(name);
        if (elm == null) {
            elm = new SettingsElement(name, SettingsElement.TYPE_INTEGER);
            put(elm);
        }
        elm.valueObject = new Integer(v);
        return elm;
    }

    public SettingsElement put(String name, String v) {
        SettingsElement elm = get(name);
        if (elm == null) {
            elm = new SettingsElement(name, SettingsElement.TYPE_STRING);
            put(elm);
        }
        elm.valueObject = v;
        return elm;
    }

    public SettingsElement put(String name, double v) {
        SettingsElement elm = get(name);
        if (elm == null) {
            elm = new SettingsElement(name, SettingsElement.TYPE_DOUBLE);
            put(elm);
        }
        elm.valueObject = new Double(v);
        return elm;
    }

    public SettingsElement put(String name, Object[] v) {
        SettingsElement elm = get(name);
        if (elm == null) {
            elm = new SettingsElement(name, SettingsElement.TYPE_ARRAY);
            put(elm);
        }
        elm.valueObject = v;
        return elm;
    }

    public SettingsElement get(String name) {
        return (SettingsElement) elements.get(name);
    }

    public boolean getBoolean(String name, boolean defaultValue,
                              boolean create) {
        boolean rslt = defaultValue;
        try {
            SettingsElement elm = get(name);
            if (elm == null && create) {
                elm = put(name, defaultValue);
            }
            rslt = ((Boolean) elm.val()).booleanValue();
        } catch (NullPointerException x) {
        } catch (ClassCastException x) {
        }
        return rslt;
    }

    public int getInteger(String name, int defaultValue,
                          boolean create) {
        int rslt = defaultValue;
        try {
            SettingsElement elm = get(name);
            if (elm == null && create) {
                elm = put(name, defaultValue);
            }
            rslt = ((Integer) elm.val()).intValue();
        } catch (NullPointerException x) {
        } catch (ClassCastException x) {
        }
        return rslt;
    }

    public String getString(String name, String defaultValue,
                            boolean create) {
        String rslt = defaultValue;
        try {
            SettingsElement elm = get(name);
            if (elm == null && create) {
                elm = put(name, defaultValue);
            }
            rslt = (String) elm.val();
        } catch (NullPointerException x) {
        } catch (ClassCastException x) {
        }
        if (rslt == null) {
            rslt = defaultValue;
        }
        return rslt;
    }

    public double getDouble(String name, double defaultValue,
                            boolean create) {
        double rslt = defaultValue;
        try {
            SettingsElement elm = get(name);
            if (elm == null && create) {
                elm = put(name, defaultValue);
            }
            rslt = ((Double) elm.val()).doubleValue();
        } catch (NullPointerException x) {
        } catch (ClassCastException x) {
        }
        return rslt;
    }

    public Object getArray(String name,
                           boolean create) {
        Object[] rslt = null;
        try {
            SettingsElement elm = get(name);
            if (elm == null && create) {
                elm = put(new SettingsElement(name,
                        SettingsElement.TYPE_ARRAY));
            }
            rslt = (Object[]) elm.val();
        } catch (NullPointerException x) {
        } catch (ClassCastException x) {
        }
        return rslt;
    }

    public void getArrayFromProperties(String pName, Properties prs) {
        StringBuffer sb = new StringBuffer(pName);
        SettingsElement elm = get(pName);
        if (elm != null &&
                elm.valueType == SettingsElement.TYPE_ARRAY) {
            SettingsElement[] elms = (SettingsElement[]) elm.val();
            if (elms != null && elms.length > 0) {
                int n = elms.length;
                sb.append('.');
                int prefixLen = sb.length();
                for (int i = 0; i < n; i++) {
                    sb.setLength(prefixLen);
                    sb.append(i);
                    elms[i] = new SettingsElement(null, elm.arrayType);
                    elms[i].parse(prs.getProperty(sb.toString()));
                }
            }
        }
    }

    public void putArrayToProperties(String pName, Properties prs) {
        StringBuffer sb = new StringBuffer(pName);
        SettingsElement elm = get(pName);
        if (elm != null &&
                elm.valueType == SettingsElement.TYPE_ARRAY) {
            SettingsElement[] elms = (SettingsElement[]) elm.val();
            if (elms != null && elms.length > 0) {
                int n = elms.length;
                sb.append('.');
                int prefixLen = sb.length();
                for (int i = 0; i < n; i++) {
                    SettingsElement c = elms[i];
                    sb.setLength(prefixLen);
                    sb.append(i);
                    prs.setProperty(sb.toString(), c.toString());
                }
            }
        }
    }
}
