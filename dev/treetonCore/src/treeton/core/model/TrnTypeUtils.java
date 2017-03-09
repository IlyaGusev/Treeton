/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model;

import treeton.core.TString;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
import treeton.core.fsm.logicset.LogicFSM;
import treeton.core.util.nu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * В этом классе реализованы некоторые операции, связанные с типами тринотаций
 */

public class TrnTypeUtils {

    /**
     * Этот метод осуществляет приведение значения некоторого атрибута к правильному типу данных (в соответствии
     * с описанием типа тринотации). Если приведение невозможно по тем или иным причинам, бросается исключение
     * {@link RuntimeException}.
     * <p>
     * Значение null преобразуется в служебный объект nu.ll (статическое поле ll класса nu)
     * Без изменений остаются объекты nu.ll и nu.other, а также объекты, являющиеся отображениями (реализуют интерфейс
     * {@link java.util.Map}).
     * <p>
     * Приведение значения к нужному типу производится следующим образом: если значение не относится к нужному типу
     * исходно, вызывается метод toString() и делается попытка сконструировать объект нужного типа по этой строке.
     * <p>
     * Если значение атрибута является массивом, то возвращается новый массив, в котором на соответствующих местах
     * стоят результаты приведения элементов исходного массива к нужному типу данных. Это правило действует только на
     * глубину 1 (если элементами исходного массива, в свою очередь, являются массивы метод будет пытаться привести их
     * через toString() как описано выше).
     * <p>
     * Аналогично ситуации с массивами метод ведет себя в случае, если значение атрибута является
     * {@link java.util.Collection} (возвращается {@link ArrayList} с преобразованными элементами).
     * <p>
     * Метод поддерживает следующие типы (может к ним приводить)
     * {@link treeton.core.TString}
     * {@link Integer}
     * {@link Boolean}
     * {@link treeton.core.Treenotation}
     * <p>
     * Последнее используется для ссылок на тринотации. В этом случае метод работает просто как валидатор, т.е.
     * проверяет, является ли значение тринотацией. Если нет, то бросается исключение.
     *
     * @param tp      описание типа тринотации
     * @param feature номер атрибута
     * @param value   значение
     * @return новое значение
     */

    public static Object treatFeatureValue(TrnType tp, int feature, Object value) {
        if (value == null || value == nu.ll)
            return nu.ll;
        if (value == nu.other)
            return nu.other;

        if (value instanceof Map)
            return value;

        Class type;
        try {
            type = tp.getFeatureTypeByIndex(feature);
        } catch (TreetonModelException e) {
            type = null;
        }

        if (type == Integer.class) {
            if (value instanceof Object[]) {
                Object[] narr = new Object[((Object[]) value).length];
                for (int i = 0; i < ((Object[]) value).length; i++) {
                    Object o = ((Object[]) value)[i];
                    if (!(o instanceof LogicFSM) && !(o instanceof Integer)) {
                        narr[i] = Integer.valueOf(o.toString());
                    } else {
                        narr[i] = o;
                    }
                }
                return narr;
            } else if (value instanceof Collection) {
                ArrayList list = new ArrayList();
                for (Object o : (Collection) value) {
                    if (!(o instanceof LogicFSM) && !(o instanceof Integer)) {
                        list.add(Integer.valueOf(o.toString()));
                    } else {
                        list.add(o);
                    }
                }

                return list;
            } else if (value instanceof Integer) {
                return value;
            } else if (!(value instanceof LogicFSM)) {
                return Integer.valueOf(value.toString());
            }
        } else if (type == Boolean.class) {
            if (value instanceof Object[]) {
                Object[] narr = new Object[((Object[]) value).length];
                for (int i = 0; i < ((Object[]) value).length; i++) {
                    Object o = ((Object[]) value)[i];
                    if (!(o instanceof LogicFSM) && !(o instanceof Boolean)) {
                        narr[i] = Boolean.valueOf(o.toString());
                    } else {
                        narr[i] = o;
                    }
                }
                return narr;
            } else if (value instanceof Collection) {
                ArrayList list = new ArrayList();
                for (Object o : (Collection) value) {
                    if (!(o instanceof LogicFSM) && !(o instanceof Integer)) {
                        list.add(Boolean.valueOf(o.toString()));
                    } else {
                        list.add(o);
                    }
                }

                return list;
            } else if (value instanceof Boolean) {
                return value;
            } else if (!(value instanceof LogicFSM)) {
                return Boolean.valueOf(value.toString());
            }
        } else if (type == Treenotation.class) {
            if (value instanceof Object[]) {
                Object[] arr = (Object[]) value;
                for (Object o : arr) {
                    if (!(o instanceof Treenotation)) {
                        try {
                            throw new RuntimeException("Unable to treat one of the values " + value + " of feature " + tp.getFeatureNameByIndex(feature) + " (type of the feature is " + type + ")");
                        } catch (TreetonModelException e) {
                            throw new RuntimeException("Unable to treat one of the values " + value + " of feature " + feature + " (type of the feature is " + type + ")");
                        }
                    }
                }
                return arr;
            } else {
                if (!(value instanceof Treenotation)) {
                    try {
                        throw new RuntimeException("Unable to treat value " + value + " of feature " + tp.getFeatureNameByIndex(feature) + " (type of the feature is " + type + ")");
                    } catch (TreetonModelException e) {
                        throw new RuntimeException("Unable to treat value " + value + " of feature " + feature + " (type of the feature is " + type + ")");
                    }
                }
                return value;
            }
        } else if (type == TString.class) {
            if (value instanceof Collection) {
                Collection col = (Collection) value;
                return treatMulti(col.toArray(new Object[col.size()]));
            }
            if (value instanceof Object[]) {
                return treatMulti((Object[]) value);
            } else if (value instanceof TString) {
                return value;
            } else if (value instanceof LogicFSM) {
                return value;
            } else {
                return TreetonFactory.newTString(value.toString());
            }
        }
        try {
            throw new RuntimeException("Unable to treat value " + value + " of feature " + tp.getFeatureNameByIndex(feature) + " (type of the feature is " + type + ")");
        } catch (TreetonModelException e) {
            throw new RuntimeException("Unable to treat value " + value + " of feature " + feature + " (type of the feature is " + type + ")");
        }
    }

    private static Object[] treatMulti(Object value[]) {
        Object[] narr = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            Object o = value[i];
            if (!(o instanceof TString) && !(o instanceof LogicFSM)) {
                narr[i] = TreetonFactory.newTString(o.toString());
            } else {
                narr[i] = o;
            }
        }
        return narr;
    }

    /**
     * Этот метод осуществляет приведение значения некоторого атрибута к правильному типу данных (в соответствии
     * с описанием типа тринотации). Если приведение невозможно по тем или иным причинам, бросается исключение
     * {@link RuntimeException}. Метод определяет тип атрибута по описанию типа тринотации и вызывает метод
     * {@link treeton.core.model.TrnTypeUtils#treatFeatureValueForGate(Class, int, Object)}
     *
     * @param tp      описание типа тринотации
     * @param feature номер атрибута
     * @param value   значение
     * @return новое значение
     */

    public static Object treatFeatureValueForGate(TrnType tp, int feature, Object value) {
        Class type = null;
        try {
            type = tp.getFeatureTypeByIndex(feature);
        } catch (TreetonModelException e) {
            //do nothing
        }

        return treatFeatureValueForGate(type, feature, value);
    }

    /**
     * Этот метод осуществляет приведение значения некоторого атрибута к определенному типу данных (из набора
     * допустимых). Если приведение невозможно по тем или иным причинам, бросается исключение {@link RuntimeException}.
     * <p>
     * Значение null преобразуется в служебный объект nu.ll (статическое поле ll класса nu)
     * Без изменений остаются объекты nu.ll и nu.other.
     * <p>
     * Приведение значения к нужному типу производится следующим образом: если значение не относится к нужному типу
     * исходно, вызывается метод toString() и делается попытка сконструировать объект нужного типа по этой строке.
     * <p>
     * Если значение атрибута является массивом, то возвращается новый массив, в котором на соответствующих местах
     * стоят результаты приведения элементов исходного массива к нужному типу данных. Аналогично метод ведет себя в
     * случае, если значение атрибута является {@link java.util.Collection} (возвращается {@link ArrayList} с
     * преобразованными элементами).
     * <p>
     * Метод поддерживает следующие типы (может к ним приводить)
     * {@link treeton.core.TString}
     * {@link Integer}
     * {@link Boolean}
     * <p>
     * NB! В первом случае новое значение будет иметь тип {@link java.lang.String}, а не {@link treeton.core.TString}.
     *
     * @param type    тип данных, к которому следует привести значение
     * @param feature номер атрибута
     * @param value   значение
     * @return новое значение
     */

    public static Object treatFeatureValueForGate(Class type, int feature, Object value) {
        if (value == null || value == nu.ll)
            return nu.ll;
        if (value == nu.other)
            return nu.other;
        if (value instanceof Collection) {
            ArrayList list = new ArrayList();
            for (Object o : (Collection) value) {
                list.add(treatFeatureValueForGate(type, feature, o));
            }
            return list;
        } else if (value instanceof Object[]) {
            ArrayList list = new ArrayList();
            Object[] arr = (Object[]) value;
            for (Object anArr : arr) {
                list.add(treatFeatureValueForGate(type, feature, anArr));
            }
            return list;
        }


        if (type == Integer.class) {
            if (value instanceof Integer) {
                return value;
            } else {
                return Integer.valueOf(value.toString());
            }
        } else if (type == TString.class) {
            if (value instanceof String) {
                return value;
            } else {
                return value.toString();
            }
        } else if (type == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else {
                return Boolean.valueOf(value.toString());
            }
        }
        throw new RuntimeException("Unable to treat value " + value + " of feature " + feature + " (type of the feature is " + type + ")");
    }

    // {short}name.length,{char[]}name,{char}autoFill,{char}tokenType,{short}index,{short}nFeatures,
    //         ({short}featureTypeName.length,{char[]}featureType.name,{short}featureName.length,{char[]}featureName),
    //         {short}color.r,{short}color.g,{short}color.b,{short}color.a,

    public static char[] getCharRepresentation(TrnType tp) {
        try {
            StringBuffer buf = new StringBuffer();
            buf.setLength(0);
            buf.append((char) tp.getName().length());
            buf.append(tp.getName());
            buf.append((char) 0); //backwards compatibility
            buf.append((char) (tp.isTokenType() ? 1 : 0));
            buf.append((char) tp.getIndex());
            int n = tp.getFeaturesSize();
            buf.append((char) n);
            Class[] types = tp.getFeatureTypes();
            String[] names = tp.getFeatureNames();
            for (int i = 0; i < n; i++) {
                String tstr = types[i].getName();
                buf.append((char) tstr.length());
                buf.append(tstr);
                tstr = names[i];
                buf.append((char) tstr.length());
                buf.append(tstr);
            }
            buf.append((char) 0); //backwards compatibility
            buf.append((char) 0); //backwards compatibility
            buf.append((char) 0); //backwards compatibility
            buf.append((char) 0); //backwards compatibility
            char[] result = new char[buf.length()];
            buf.getChars(0, buf.length(), result, 0);
            return result;
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error when trying to get char representation of type: " + tp);
        }
    }

    // {short}name.length,{char[]}name,{char}autoFill,{char}tokenType,{short}index,{short}nFeatures,
    //         ({short}featureTypeName.length,{char[]}featureType.name,{short}featureName.length,{char[]}featureName),
    //         {short}color.r,{short}color.g,{short}color.b,{short}color.a,


    public static char[] getFullCharRepresentation(TrnTypeStorage types) {
        try {
            StringBuffer fullBuf = new StringBuffer();

            fullBuf.setLength(0);

            int size = types.size();
            fullBuf.append((char) size);
            TrnType[] all = types.getAllTypes();

            for (TrnType tp : all) {
                fullBuf.append(TrnTypeUtils.getCharRepresentation(tp));
            }
            char[] result = new char[fullBuf.length()];
            fullBuf.getChars(0, fullBuf.length(), result, 0);
            return result;
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error when trying to get char representation of type storage");
        }
    }

}
