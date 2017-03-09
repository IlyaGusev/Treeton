/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import treeton.core.*;
import treeton.core.util.MutableInteger;
import treeton.core.util.NumeratedObject;
import treeton.core.util.sut;

import java.util.HashMap;
import java.util.Iterator;

public class CharFSMImpl implements FSM, CharFSM {
    static char _tmpArr[] = new char[2048];
    private static BlackBoard localBoard = TreetonFactory.newBlackBoard(100, false);
    int nStates;
    int nStrings;
    Recoder recoder;
    private CharFSMState firstState;
    private CharFSMState firstInStateList;
    private CharFSMState lastInStateList;
    private int nullIndex;

    CharFSMImpl() { //Only for PackedCharFSM
    }

    public CharFSMImpl(Recoder _recoder) {
        nStates = 0;
        nStrings = 0;
        nullIndex = -1;
        firstState = null;
        firstInStateList = null;
        lastInStateList = null;
        recoder = _recoder;
    }

    public State getStartState() {
        return firstState;
    }

    public int changeValue(String s, int newValue) {
        if (s == null) {
            if (nullIndex != -1)
                return nullIndex;
            return -1;
        }

        if (firstState == null)
            return -1;
        synchronized (_tmpArr) {
            char[] tmpArr;
            if (s.length() <= _tmpArr.length) {
                s.getChars(0, s.length(), _tmpArr, 0);
                tmpArr = _tmpArr;
            } else {
                tmpArr = s.toCharArray();
            }

            CharFSMState cur = firstState;
            int from = 0, length = s.length();

            while (true) {
                if (cur.appendix != null) {
                    int i = 0;
                    for (; i < cur.appendix.length && from < length; i++, from++) {
                        if (cur.appendix[i] != tmpArr[from])
                            return -1;
                    }
                    if (i < cur.appendix.length)
                        return -1;
                }
                if (from == length) {
                    if (cur.isFinal) {
                        cur.value = newValue;
                        return cur.value;
                    } else {
                        return -1;
                    }
                } else {
                    int snum = recoder.getSymbolNumber(tmpArr[from]);
                    CharFSMState next = (CharFSMState) cur.get(snum);
                    if (next == null)
                        return -1;
                    from++;
                    cur = next;
                }
            }
        }
    }

    public int changeValue(TString s, int newValue) {
        if (s == null) {
            if (nullIndex != -1)
                return nullIndex;
            return -1;
        }

        if (firstState == null)
            return -1;

        CharFSMState cur = firstState;
        int from = 0, length = s.length();
        char[] value = ((TStringImpl) s).value;

        while (true) {
            if (cur.appendix != null) {
                int i = 0;
                for (; i < cur.appendix.length && from < length; i++, from++) {
                    if (cur.appendix[i] != value[from])
                        return -1;
                }
                if (i < cur.appendix.length)
                    return -1;
            }
            if (from == length) {
                if (cur.isFinal) {
                    cur.value = newValue;
                    return cur.value;
                } else {
                    return -1;
                }
            } else {
                int snum = recoder.getSymbolNumber(value[from]);
                CharFSMState next = (CharFSMState) cur.get(snum);
                if (next == null)
                    return -1;
                from++;
                cur = next;
            }
        }
    }

    public int get(String s) {
        return get(s, null);
    }

    public int get(String s, MutableInteger usedLength) {
        if (s == null) {
            if (nullIndex != -1)
                return nullIndex;
            return -1;
        }

        if (firstState == null)
            return -1;
        synchronized (_tmpArr) {
            char[] tmpArr;
            if (s.length() <= _tmpArr.length) {
                s.getChars(0, s.length(), _tmpArr, 0);
                tmpArr = _tmpArr;
            } else {
                tmpArr = s.toCharArray();
            }

            CharFSMState cur = firstState;
            int from = 0, length = s.length();

            while (true) {
                if (cur.appendix != null) {
                    int i = 0;
                    for (; i < cur.appendix.length && from < length; i++, from++) {
                        if (cur.appendix[i] != tmpArr[from])
                            return -1;
                    }
                    if (i < cur.appendix.length)
                        return -1;
                }
                if (from == length) {
                    if (cur.isFinal) {
                        if (usedLength != null) {
                            usedLength.value = length;
                        }

                        return cur.value;
                    } else
                        return -1;
                } else {
                    int snum = recoder.getSymbolNumber(tmpArr[from]);
                    CharFSMState next = (CharFSMState) cur.get(snum);
                    if (next == null) {
                        if (cur.isFinal && usedLength != null) {
                            usedLength.value = from;
                            return cur.value;
                        }
                        return -1;
                    }
                    from++;
                    cur = next;
                }
            }
        }
    }

    public int get(TString s) {
        if (s == null) {
            if (nullIndex != -1)
                return nullIndex;
            return -1;
        }

        if (firstState == null)
            return -1;

        CharFSMState cur = firstState;
        int from = 0, length = s.length();
        char[] value = ((TStringImpl) s).value;

        while (true) {
            if (cur.appendix != null) {
                int i = 0;
                for (; i < cur.appendix.length && from < length; i++, from++) {
                    if (cur.appendix[i] != value[from])
                        return -1;
                }
                if (i < cur.appendix.length)
                    return -1;
            }
            if (from == length) {
                if (cur.isFinal)
                    return cur.value;
                else
                    return -1;
            } else {
                int snum = recoder.getSymbolNumber(value[from]);
                CharFSMState next = (CharFSMState) cur.get(snum);
                if (next == null)
                    return -1;
                from++;
                cur = next;
            }
        }
    }

    public CharFSMState addCharArray(char[] arr, int from, int length) {
        CharFSMState cur = firstState;

        while (true) {
            if (cur.appendix == null && cur.size() == 0) {
                if (!cur.isFinal) {
                    if (length != from) {
                        cur.appendix = new char[length - from];
                        System.arraycopy(arr, from, cur.appendix, 0, length - from);
                    }
                    cur.finalizeState();
                    return cur;
                } else {
                    if (length == from) {
                        return cur;
                    }
                    int snum = recoder.getSymbolNumber(arr[from]);
                    CharFSMState next = (CharFSMState) addState();
                    cur.put(snum, next);
                    from++;
                    cur = next;
                }
            } else if (cur.size() == 0) {
                int i = 0;
                int d = from;
                for (; i < cur.appendix.length && d < length; i++) {
                    if (arr[d] != cur.appendix[i])
                        break;
                    d++;
                }
                if (i == cur.appendix.length) {
                    if (d != length) {
                        int snum = recoder.getSymbolNumber(arr[d]);
                        CharFSMState next = (CharFSMState) addState();
                        cur.put(snum, next);
                        from = d + 1;
                        cur = next;
                    } else {
                        return cur;
                    }
                } else if (d == length) {
                    int snum = recoder.getSymbolNumber(cur.appendix[i]);
                    CharFSMState next = (CharFSMState) addState();
                    next.addCharArray(cur.appendix, i + 1, cur.appendix.length);
                    next.value = cur.value;
                    next.isFinal = true;
                    cur.value = -1;
                    cur.finalizeState();
                    cur.put(snum, next);
                    if (i != 0) {
                        cur.appendix = new char[i];
                        System.arraycopy(arr, from, cur.appendix, 0, i);
                    } else {
                        cur.appendix = null;
                    }
                    return cur;
                } else {
                    int snum1 = recoder.getSymbolNumber(arr[d]);
                    CharFSMState next1 = (CharFSMState) addState();
                    int snum2 = recoder.getSymbolNumber(cur.appendix[i]);
                    CharFSMState next2 = (CharFSMState) addState();

                    next2.addCharArray(cur.appendix, i + 1, cur.appendix.length);
                    next2.value = cur.value;
                    next2.isFinal = true;
                    cur.isFinal = false;
                    cur.value = -1;
                    cur.put(snum1, next1);
                    cur.put(snum2, next2);
                    if (i != 0) {
                        cur.appendix = new char[i];
                        System.arraycopy(arr, from, cur.appendix, 0, i);
                    } else {
                        cur.appendix = null;
                    }
                    from = d + 1;
                    cur = next1;
                }
            } else {
                if (cur.appendix == null) {
                    if (from == length) {
                        cur.finalizeState();
                        return cur;
                    }
                    int snum = recoder.getSymbolNumber(arr[from]);
                    CharFSMState next = (CharFSMState) cur.get(snum);
                    if (next == null) {
                        next = (CharFSMState) addState();
                        cur.put(snum, next);
                    }
                    from++;
                    cur = next;
                } else {
                    int i = 0;
                    int d = from;
                    for (; i < cur.appendix.length && d < length; i++) {
                        if (arr[d] != cur.appendix[i])
                            break;
                        d++;
                    }
                    if (i == cur.appendix.length) {
                        if (d != length) {
                            int snum = recoder.getSymbolNumber(arr[d]);
                            CharFSMState next = (CharFSMState) cur.get(snum);
                            if (next == null) {
                                next = (CharFSMState) addState();
                                cur.put(snum, next);
                            }
                            from = d + 1;
                            cur = next;
                        } else {
                            cur.finalizeState();
                            return cur;
                        }
                    } else if (d == length) {
                        int snum = recoder.getSymbolNumber(cur.appendix[i]);
                        CharFSMState next = (CharFSMState) addState();
                        next.addCharArray(cur.appendix, i + 1, cur.appendix.length);
                        next.importFrom(cur);
                        next.value = cur.value;
                        next.isFinal = cur.isFinal;
                        cur.value = -1;
                        cur.finalizeState();
                        cur.put(snum, next);
                        if (i != 0) {
                            cur.appendix = new char[i];
                            System.arraycopy(arr, from, cur.appendix, 0, i);
                        } else {
                            cur.appendix = null;
                        }
                        return cur;
                    } else {
                        int snum1 = recoder.getSymbolNumber(arr[d]);
                        CharFSMState next1 = (CharFSMState) addState();
                        int snum2 = recoder.getSymbolNumber(cur.appendix[i]);
                        CharFSMState next2 = (CharFSMState) addState();

                        next2.addCharArray(cur.appendix, i + 1, cur.appendix.length);
                        next2.importFrom(cur);
                        next2.value = cur.value;
                        next2.isFinal = cur.isFinal;
                        cur.value = -1;
                        cur.isFinal = false;
                        cur.put(snum1, next1);
                        cur.put(snum2, next2);
                        if (i != 0) {
                            cur.appendix = new char[i];
                            System.arraycopy(arr, from, cur.appendix, 0, i);
                        } else {
                            cur.appendix = null;
                        }
                        from = d + 1;
                        cur = next1;
                    }
                }
            }
        }
    }

    public Iterator statesIterator() {
        return new StatesIterator();
    }

    public int getNumberOfStates() {
        return nStates;
    }

    public State addState() {
        CharFSMState s = new CharFSMState(recoder, 4);
        if (lastInStateList == null)
            firstInStateList = s;
        else
            lastInStateList.nextInStateList = s;
        lastInStateList = s;
        s.setIndex(nStates++);
        return s;
    }

    public int addString(String s) {
        if (s == null) {
            if (nullIndex == -1) {
                nullIndex = nStrings++;
            }
            return nullIndex;
        }
        CharFSMState t;
        synchronized (_tmpArr) {
            char[] tmpArr;
            if (s.length() <= _tmpArr.length) {
                s.getChars(0, s.length(), _tmpArr, 0);
                tmpArr = _tmpArr;
            } else {
                tmpArr = s.toCharArray();
            }
            if (firstState == null)
                firstState = (CharFSMState) addState();
            t = addCharArray(tmpArr, 0, s.length());
        }
        if (t.value == -1)
            t.value = nStrings++;
        return t.value;
    }

    public int addString(TString s) {
        if (s == null) {
            if (nullIndex == -1) {
                nullIndex = nStrings++;
            }
            return nullIndex;
        }

        if (firstState == null)
            firstState = (CharFSMState) addState();
        CharFSMState t = addCharArray(((TStringImpl) s).value, 0, s.length());
        if (t.value == -1)
            t.value = nStrings++;
        return t.value;
    }

    public int getSize() {
        return nStrings;
    }

    public char[] getCharRepresentation() {
        CharFSMState cur = firstInStateList;
        int size = 0;

        while (cur != null) {
            size += 2; //int cur.index
            size += 2; //int cur.value
            size += 1; //boolean cur.isFinal

            size += 1; //int cur.appendix.length
            size += cur.appendix == null ? 0 : cur.appendix.length;

            size += 1; //cur.size()
            size += cur.size() * 3; //short key, int value
            cur = cur.nextInStateList;
        }

        char[] reverseEncoding = recoder.getReverseEncoding();
        size += 1;//short reverseEncoding.length
        size += reverseEncoding.length;
        size += 2; //int nullIndex
        size += 2; //int firstState (I mean index)
        size += 2; //int nStates

        char[] arr = new char[size];

        int ptr = 0;
        arr[ptr++] = (char) reverseEncoding.length;
        System.arraycopy(reverseEncoding, 0, arr, ptr, reverseEncoding.length);
        ptr += reverseEncoding.length;
        sut.putIntegerInChars(arr, ptr, nullIndex);
        ptr += 2;
        sut.putIntegerInChars(arr, ptr, firstState.index);
        ptr += 2;
        sut.putIntegerInChars(arr, ptr, nStates);
        ptr += 2;

        cur = firstInStateList;

        while (cur != null) {
            sut.putIntegerInChars(arr, ptr, cur.index);
            ptr += 2;
            sut.putIntegerInChars(arr, ptr, cur.value);
            ptr += 2;
            arr[ptr++] = (char) (cur.isFinal ? 1 : 0);

            if (cur.appendix != null) {
                arr[ptr++] = (char) cur.appendix.length;
                System.arraycopy(cur.appendix, 0, arr, ptr, cur.appendix.length);
                ptr += cur.appendix.length;
            } else {
                arr[ptr++] = (char) 0;
            }
            arr[ptr++] = (char) cur.size();
            Iterator it = cur.numeratedObjectIterator();
            while (it.hasNext()) {
                NumeratedObject no = (NumeratedObject) it.next();
                arr[ptr++] = (char) no.n;
                sut.putIntegerInChars(arr, ptr, ((CharFSMState) no.o).index);
                ptr += 2;
            }
            cur = cur.nextInStateList;
        }
        return arr;
    }

    public int readInFromChars(char[] arr, int from) {
        synchronized (localBoard) {
            if (firstState != null)
                throw new RuntimeException("Trying to readInFromChars into CharFSM that is not empty");
            int ptr = from;
            int reverseEncLen = arr[ptr++];
            char[] reverseEncoding = new char[reverseEncLen];
            System.arraycopy(arr, ptr, reverseEncoding, 0, reverseEncLen);
            ptr += reverseEncLen;

            nullIndex = sut.getIntegerFromChars(arr, ptr);
            ptr += 2;
            int firstStateIndex = sut.getIntegerFromChars(arr, ptr);
            ptr += 2;
            nStates = sut.getIntegerFromChars(arr, ptr);
            ptr += 2;

            HashMap statesByIndex = new HashMap();

            for (int i = 0; i < nStates; i++) {
                CharFSMState state = new CharFSMState(recoder);
                if (lastInStateList == null)
                    firstInStateList = state;
                else
                    lastInStateList.nextInStateList = state;
                lastInStateList = state;
                state.index = sut.getIntegerFromChars(arr, ptr);
                statesByIndex.put(new Integer(state.index), state);
                ptr += 2;
                state.value = sut.getIntegerFromChars(arr, ptr);
                ptr += 2;
                if (state.value != -1)
                    nStrings++;
                state.isFinal = arr[ptr++] == 0 ? false : true;

                int appendixLen = arr[ptr++];
                if (appendixLen == 0) {
                    state.appendix = null;
                } else {
                    state.appendix = new char[appendixLen];
                    System.arraycopy(arr, ptr, state.appendix, 0, appendixLen);
                    ptr += appendixLen;
                }
                int size = arr[ptr++];
                for (int j = 0; j < size; j++) {
                    int t = arr[ptr++];
                    char c = reverseEncoding[t];
                    int feature = recoder.getSymbolNumber(c);
                    Integer value = new Integer(sut.getIntegerFromChars(arr, ptr));
                    ptr += 2;
                    localBoard.put(feature, value);
                }
                state.put(localBoard);
            }
            firstState = (CharFSMState) statesByIndex.get(new Integer(firstStateIndex));

            Iterator it = statesIterator();
            while (it.hasNext()) {
                CharFSMState s = (CharFSMState) it.next();
                int sz = s.size();
                for (int i = 0; i < sz; i++) {
                    Integer value = (Integer) s.getByIndex(i);
                    s.putByIndex(i, statesByIndex.get(value));
                }
            }
            return ptr;
        }
    }

    private class StatesIterator implements Iterator {
        CharFSMState s;

        StatesIterator() {
            s = firstInStateList;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return s != null;
        }

        public Object next() {
            CharFSMState t = s;
            s = s.nextInStateList;
            return t;
        }
    }
}
