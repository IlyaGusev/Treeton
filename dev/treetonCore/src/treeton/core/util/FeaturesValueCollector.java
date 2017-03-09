/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.IntMapper;
import treeton.core.TString;
import treeton.core.TreetonFactory;

import java.util.HashMap;
import java.util.Iterator;

public class FeaturesValueCollector {
    HashMap integers = new HashMap();
    HashMap tstrings = new HashMap();
    HashMap intMappers = new HashMap();
    int currentNumber = 0;
    TString searcher = TreetonFactory.newTString(new char[100]);

    public int size() {
        return currentNumber;
    }

    public Object[] toArray() {
        Object[] result = new Object[currentNumber];
        Iterator it = integers.values().iterator();
        while (it.hasNext()) {
            Object[] arr = (Object[]) it.next();
            result[((Integer) arr[0]).intValue()] = arr[1];
        }
        it = tstrings.values().iterator();
        while (it.hasNext()) {
            Object[] arr = (Object[]) it.next();
            result[((Integer) arr[0]).intValue()] = arr[1];
        }
        it = intMappers.values().iterator();
        while (it.hasNext()) {
            Object[] arr = (Object[]) it.next();
            result[((Integer) arr[0]).intValue()] = arr[1];
        }
        return result;
    }

    public IntMapper getValueFor(IntMapper imap) {
        Object[] arr = (Object[]) intMappers.get(imap);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = imap;
            intMappers.put(imap, arr);
        }
        return (IntMapper) arr[1];
    }

    public int getValueIndex(IntMapper imap) {
        Object[] arr = (Object[]) intMappers.get(imap);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = imap;
            intMappers.put(imap, arr);
        }
        return ((Integer) arr[0]).intValue();
    }

    public Integer getValueFor(int n) {
        Integer i = new Integer(n);
        Object[] arr = (Object[]) integers.get(i);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = i;
            integers.put(i, arr);
        }
        return (Integer) arr[1];
    }

    public int getValueIndex(int n) {
        Integer i = new Integer(n);
        Object[] arr = (Object[]) integers.get(i);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = i;
            integers.put(i, arr);
        }
        return ((Integer) arr[0]).intValue();
    }

    public Integer getValueFor(Integer i) {
        Object[] arr = (Object[]) integers.get(i);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = i;
            integers.put(i, arr);
        }
        return (Integer) arr[1];
    }

    public int getValueIndex(Integer i) {
        Object[] arr = (Object[]) integers.get(i);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = i;
            integers.put(i, arr);
        }
        return ((Integer) arr[0]).intValue();
    }

    public TString getValueFor(StringBuffer buf) {
        searcher.slurp(buf);
        Object[] arr = (Object[]) tstrings.get(searcher);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = TreetonFactory.newTString(searcher);
            tstrings.put(arr[1], arr);
        }
        return (TString) arr[1];
    }

    public int getValueIndex(StringBuffer buf) {
        searcher.slurp(buf);
        Object[] arr = (Object[]) tstrings.get(searcher);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = TreetonFactory.newTString(searcher);
            tstrings.put(arr[1], arr);
        }
        return ((Integer) arr[0]).intValue();
    }

    public TString getValueFor(String s) {
        searcher.slurp(s);
        Object[] arr = (Object[]) tstrings.get(searcher);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = TreetonFactory.newTString(searcher);
            tstrings.put(arr[1], arr);
        }
        return (TString) arr[1];
    }

    public int getValueIndex(String s) {
        searcher.slurp(s);
        Object[] arr = (Object[]) tstrings.get(searcher);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = TreetonFactory.newTString(searcher);
            tstrings.put(arr[1], arr);
        }
        return ((Integer) arr[0]).intValue();
    }

    public TString getValueFor(TString s) {
        Object[] arr = (Object[]) tstrings.get(s);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = s;
            tstrings.put(s, arr);
        }
        return (TString) arr[1];
    }


    public int getValueIndex(TString s) {
        Object[] arr = (Object[]) tstrings.get(s);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = s;
            tstrings.put(s, arr);
        }
        return ((Integer) arr[0]).intValue();
    }

    public TString getValueFor(char[] chars, int begin, int count) {
        searcher.slurp(chars, begin, count);
        Object[] arr = (Object[]) tstrings.get(searcher);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = TreetonFactory.newTString(searcher);
            tstrings.put(arr[1], arr);
        }
        return (TString) arr[1];
    }

    public int getValueIndex(char[] chars, int begin, int count) {
        searcher.slurp(chars, begin, count);
        Object[] arr = (Object[]) tstrings.get(searcher);
        if (arr == null) {
            arr = new Object[2];
            arr[0] = new Integer(currentNumber++);
            arr[1] = TreetonFactory.newTString(searcher);
            tstrings.put(arr[1], arr);
        }
        return ((Integer) arr[0]).intValue();
    }

    public TString getValueFor(char[] chars) {
        return getValueFor(chars, 0, chars.length);
    }

    public int getValueIndex(char[] chars) {
        return getValueIndex(chars, 0, chars.length);
    }

    public Object getValueFor(Object o) {
        if (o instanceof String) {
            return getValueFor((String) o);
        } else if (o instanceof TString) {
            return getValueFor((TString) o);
        } else if (o instanceof Integer) {
            return getValueFor((Integer) o);
        } else if (o instanceof IntMapper) {
            return getValueFor((IntMapper) o);
        }
        return null;
    }

    public int getValueIndex(Object o) {
        if (o instanceof TString) {
            return getValueIndex((TString) o);
        } else if (o instanceof String) {
            return getValueIndex((String) o);
        } else if (o instanceof Integer) {
            return getValueIndex((Integer) o);
        } else if (o instanceof IntMapper) {
            return getValueIndex((IntMapper) o);
        }
        return -1;
    }

}
