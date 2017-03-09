/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PropertyUtil {
    private static String PROCESSOR = "Processor";

    //------------------------------------------------------------------------------
    protected PropertyUtil() {
    }
//------------------------------------------------------------------------------

    /**
     * This method extracts processors defined in properties
     *
     * @param props properties
     * @return returns Hashtable where keys are (String) names of processors
     * and objects are (Integer) processors numbers
     */
    public static synchronized Hashtable getProcessorNames(Properties props) {
        Hashtable ht = new Hashtable();
        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
            String propName = (String) e.nextElement();
            if (propName.startsWith(PROCESSOR)) {
                if (propName.indexOf('.') == -1) {
                    continue;
                }
                String strNumber = propName.substring(PROCESSOR.length(), propName.indexOf('.'));
                int processorNumber;
                try {
                    processorNumber = Integer.parseInt(strNumber);
                    String processorName = props.getProperty(PROCESSOR + String.valueOf(processorNumber) + ".name");
                    if (null == processorName) {
                        System.out.print("ERROR: PropertyUtil.getProcessorNames(...): property file syntax error!");
                        System.out.println(" Undefined processor name for '" + PROCESSOR + strNumber + ".name'");
                        continue;
                    }
                    if (null == ht.get(processorName)) {
                        ht.put(processorName, new Integer(processorNumber));
                    }
                } catch (NumberFormatException nfe) {
                    System.out.print("ERROR: PropertyUtil.getProcessorNames(...): property file syntax error! '" + propName + "'");
                    System.out.println(" Invalid processor number '" + strNumber + "'");
                }
            } // if(propName.startsWith(processorPropStart))
        } // for(Enumeration e = props.propertyNames(); e.hasMoreElements();)
        return ht;
    }
//------------------------------------------------------------------------------

    /**
     * This method checks if processor with given name are marked for load at GATE Server start
     *
     * @param props         properties
     * @param processorName processor name
     * @return true if property ProcessorXXX.load = true
     */
    public static synchronized boolean isLoadProcessor(Properties props, String processorName) {
        Hashtable ht = getProcessorNames(props);
        Integer pNumber = (Integer) ht.get(processorName);
        if (null != pNumber) {
            String startParam = props.getProperty(PROCESSOR + pNumber.intValue() + ".load");
            if (null != startParam) {
                if (startParam.compareToIgnoreCase("true") == 0) {
                    return true;
                }
            }
        }
        return false;
    }
//------------------------------------------------------------------------------

    /**
     * This method extracts modules defined in properties for processor with a given name
     *
     * @param props         properties
     * @param processorName processor name
     * @return Hashtable where keys are (Integer) processor module numbers
     * and objects are (String) names of processor modules
     */
    protected static synchronized Hashtable getProcessorModules(Properties props, String processorName) {
        Integer pNumber = (Integer) getProcessorNames(props).get(processorName);
        if (null != pNumber) {
            Hashtable modulesByName = new Hashtable();
            Hashtable modulesByNumber = new Hashtable();
            String modulePropStart = PROCESSOR + String.valueOf(pNumber) + ".Module";
            for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
                String propName = (String) e.nextElement();
                if (propName.startsWith(modulePropStart)) {
                    if (propName.indexOf('.', modulePropStart.length()) == -1) {
                        continue;
                    }
                    String strNumber = propName.substring(modulePropStart.length(), propName.indexOf('.', modulePropStart.length()));
                    int moduleNumber;
                    try {
                        moduleNumber = Integer.parseInt(strNumber);
                        String moduleName = props.getProperty(modulePropStart + String.valueOf(moduleNumber) + ".name");
                        if (null == moduleName) {
                            System.out.print("ERROR: PropertyUtil.getProcessorModules(...): property file syntax error!");
                            System.out.println(" Undefined module name. Can't find property '" + modulePropStart + strNumber + ".name'");
                            continue;
                        }
                        if (null == modulesByName.get(moduleName)) {
                            if (null == modulesByNumber.get(new Integer(moduleNumber))) {
                                modulesByName.put(moduleName, new Integer(moduleNumber));
                                modulesByNumber.put(new Integer(moduleNumber), moduleName);
                            } else {
                                System.out.print("ERROR: PropertyUtil.getProcessorModules(...): property file syntax error!");
                                System.out.print(" Duplicate definition of modules with the same number!");
                                System.out.print(" '" + modulePropStart + moduleNumber + ".name = " + (String) modulesByNumber.get(new Integer(moduleNumber)) + "'");
                                System.out.print(" & '" + modulePropStart + moduleNumber + ".name = " + moduleName + "'");
                                continue;
                            }
                        } // if(null == modulesByName.get(moduleName))
                    } // try
                    catch (NumberFormatException nfe) {
                        System.out.print("ERROR: PropertyUtil.getProcessorModules(...): property file syntax error! '" + propName + "'");
                        System.out.println(" Invalid module number '" + strNumber + "'");
                    }
                } // if(propName.startsWith(modulePropStart))
            } //for(Enumeration e = props.propertyNames(); e.hasMoreElements();)
            return modulesByNumber;
        } // if(null != pNumber)
        else {
            System.out.println("ERROR: PropertyUtil.getProcessorModules(...): No such processor '" + processorName);
            return null;
        }
    }
//------------------------------------------------------------------------------

    /**
     * This method extracts module names defined in properties for processor with a given name
     * Method is responsible for right order of module names.
     *
     * @param props         properties
     * @param processorName processor name
     * @return Vector which elements are (String) names of processor modules
     */
    public static synchronized Vector getModuleNames(Properties props, String processorName) {
        Hashtable ht = getProcessorModules(props, processorName);
        if (!ht.isEmpty()) {
            Vector moduleNames = new Vector();
            int[] nums = new int[ht.size()];
            Enumeration e = ht.keys();
            for (int i = 0; i < ht.size(); i++) {
                nums[i] = ((Integer) e.nextElement()).intValue();
            }
            Arrays.sort(nums);
            for (int i = 0; i < ht.size(); i++) {
                moduleNames.addElement(ht.get(new Integer(nums[i])));
            }
            return moduleNames;
        } else {
            return null;
        }
    }
//------------------------------------------------------------------------------

    /**
     * This method extracts module class for module with a given name for given processor
     *
     * @param props         properties
     * @param processorName processor name
     * @param moduleName    module name
     * @return module class name as String
     */
    public static synchronized String getModuleClass(Properties props, String processorName, String moduleName) {
        Integer pNumber = (Integer) getProcessorNames(props).get(processorName);
        if (null != pNumber) {
            String moduleClassPropStart = PROCESSOR + String.valueOf(pNumber) + ".Module";
            Hashtable ht = getProcessorModules(props, processorName);
            for (Enumeration e = ht.keys(); e.hasMoreElements(); ) {
                Integer mNumber = (Integer) e.nextElement();
                String mName = (String) ht.get(mNumber);
                if (mName.compareTo(moduleName) == 0) {
                    String moduleClass = props.getProperty(PROCESSOR + String.valueOf(pNumber) + ".Module" + String.valueOf(mNumber) + ".class");
                    if (null == moduleClass) {
                        System.out.print("ERROR: PropertyUtil.getModuleClass(...): ");
                        System.out.print(" Undefined module class for '" + moduleName + "'");
                        System.out.println(" Can't find property '" + PROCESSOR + String.valueOf(pNumber) + ".Module" + String.valueOf(mNumber) + ".class'");
                    } else {
                        return moduleClass;
                    }
                }
            } // for(Enumeration e = ht.keys(); e.hasMoreElements();)
        } // if(null != pNumber)
        return null;
    }
//------------------------------------------------------------------------------

    /**
     * ������ �������� �� ���������� ����� � ���������� �� � ����
     * ���-������� Properties.
     *
     * @param _path ���� � property �����
     * @return ������ Properties
     */
    public static Properties readProperties(String _path)
            throws NullPointerException, IOException {
        InputStream is = Class.class.getResourceAsStream(_path);
        Properties rslt = new Properties();
        rslt.load(is);
        return rslt;
    }

    /**
     * Копирует значение из "исходной" таблицы и "исходного" ключа в
     * "другую" таблицу и "другой" ключ. Если "другой" ключ не указан
     * (равен null), он считается равным "исходному" ключу.
     * <p/>
     * Если "исходный" ключ в "исходной" таблице отсутствует, то и в
     * "другой" таблице "другой" ключ удаляется.
     *
     * @param src    "исходная" таблица
     * @param srcKey ключ в "исходной" таблице
     * @param dst    "другая" таблица
     * @param dstKey "другой" ключ
     */
    public static void copyProperty(Properties src, String srcKey,
                                    Properties dst, String dstKey) {
        if (src != null && dst != null && srcKey != null) {
            dstKey = dstKey != null ? dstKey : srcKey;
            String val = src.getProperty(srcKey);
            if (val != null) {
                dst.setProperty(dstKey, val);
            } else {
                dst.remove(dstKey);
            }
        }
    }

    /**
     * Добавляет все значения из "исходной" таблицы в "другую". Если
     * ключ уже есть в "другой" таблице, значение всё равно будет
     * скопировано.
     *
     * @param src "исходная" таблица
     * @param dst "другая" таблица
     */
    public static void addProperties(Properties src, Properties dst) {
        if (src != null && dst != null) {
            Enumeration keys = src.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String val = src.getProperty(key);
                if (val != null) {
                    dst.setProperty(key, val);
                }
            }
        }
    }

    /**
     * Этот метод сравнивает Proprties по указанному списку ключей.
     *
     * @param p1   первый из сравниваемых набор свойств
     * @param p2   второй из сравниваемых набор свойств
     * @param keys список ключей для сравнения
     * @return &lt; 0 - p1 "меньше" p2<br>
     * &nbsp;&nbsp;0 - значения p1 совпадают с p2<br>
     * &gt; 0 - p1 "больше" p2
     */
    public static int compareProperties(Properties p1, Properties p2,
                                        String[] keys) {
        int gt = 1, eq = 0, lt = -1;
        int rslt = eq;
        int i = 0, n = keys.length;
        String curKey, k1, k2;
        for (; i < n && rslt == eq; i++) {
            curKey = keys[i];
            k1 = p1.getProperty(curKey);
            k2 = p2.getProperty(curKey);
            if (k1 == null) {
                if (k2 == null) {
                    // Если оба значения равны null, то считаем их равными.
                    //
                    // Оператор "continue" можно было не ставить, поскольку
                    // после этого каскада условных опрераторов мы и так
                    // переходим к следующей итерации. Просто, так нагляднее.
                    continue;
                } else {
                    // значение, равное null (k1), считается меньше
                    rslt = lt;
                }
            } else {
                if (k2 == null) {
                    // значение, равное null (k2), считается меньше
                    rslt = gt;
                } else {
                    // Если оба значения не равны null, то сравниваем их.
                    rslt = k1.compareTo(k2);
                }
            }
        }
        return rslt;
    }

    public static boolean areEqual(Properties p1, Properties p2) {
        Iterator it = p1.entrySet().iterator();
        if (p1.size() != p2.size())
            return false;
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            String val = (String) e.getValue();
            String val2 = (String) p2.get(e.getKey());
            if (val == null && val2 != null) {
                return false;
            } else if (!val.equals(val2)) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        Properties p1 = new Properties();
        Properties p2 = new Properties();
        Properties p3 = new Properties();
        Properties p4 = new Properties();
        Object[] a = {p1, p2, p3, p4};
        String keys[] = {"POS", "GEND", "CAS"};

        p1.setProperty("no", "1");
        p1.setProperty("POS", "N");
        p1.setProperty("CAS", "prp");
        p1.setProperty("GEND", "m");

        p2.setProperty("no", "2");
        p2.setProperty("POS", "N");
        p2.setProperty("CAS", "nom");
        p2.setProperty("GEND", "m");

        p3.setProperty("no", "3");
        p3.setProperty("POS", "A");
        p3.setProperty("CAS", "m");

        p4.setProperty("no", "4");
        p4.setProperty("POS", "N");
        p4.setProperty("CAS", "gen");
        p4.setProperty("GEND", "n");

        int i;
        for (i = 0; i < a.length; i++) {
            System.out.print("  " + ((Properties) a[i]).getProperty("no"));
        }
        System.out.println("");

        Arrays.sort(a, new MapComparator(keys));

        for (i = 0; i < a.length; i++) {
            System.out.print("  " + ((Properties) a[i]).getProperty("no"));
        }
        System.out.println("");
    }
}
