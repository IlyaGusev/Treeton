/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import treeton.core.IntMapper;
import treeton.core.TString;
import treeton.core.TStringImpl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PackedCharFSM extends CharFSMImpl {
    private final static int FINAL_STATE = 1 << 16;
    private final static int HAS_APPENDIX = 2 << 16;
    private int[] fsm;
    private int firstFree;
    private IntMapper[] mappers;
    private int maxMapperId;
    private HashMap intMappers;

    public PackedCharFSM(CharFSMImpl cfsm) {
        Iterator it = cfsm.statesIterator();
        int size = 0;
        nStates = 0;
        while (it.hasNext()) {
            CharFSMState s = (CharFSMState) it.next();
            size += 2;
            if (s.appendix != null)
                size += s.appendix.length;
            size += s.size();
            if (s.value != -1)
                size++;
        }
        fsm = new int[size];
        nStates = cfsm.getNumberOfStates();
        recoder = cfsm.recoder;
        intMappers = new HashMap();
        maxMapperId = -1;
        nStrings = cfsm.getSize();
        firstFree = 0;
        buildFSM((CharFSMState) cfsm.getStartState());
        mappers = new IntMapper[maxMapperId + 1];
        it = intMappers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            mappers[((Integer) e.getValue()).intValue()] = (IntMapper) e.getKey();
        }
        intMappers = null;
    }

    private int buildFSM(CharFSMState s) {
        int start = firstFree;
        int cur = start + 2;
        fsm[start] = 0;
        Integer mapperId = (Integer) intMappers.get(s.getIntMapper());
        if (mapperId == null) {
            maxMapperId++;
            intMappers.put(s.getIntMapper(), (mapperId = new Integer(maxMapperId)));
        }
        fsm[start + 1] = mapperId.intValue();
        if (s.isFinal) {
            fsm[start] |= FINAL_STATE;
            fsm[start + 2] = s.value;
            cur++;
        }
        if (s.appendix != null) {
            fsm[start] |= HAS_APPENDIX;
            int i = 0;
            for (; i < s.appendix.length; i++, cur++) {
                fsm[cur] = s.appendix[i];
            }
            fsm[start] |= i;
        }
        firstFree = cur + s.size();
        Iterator it = s.valueIterator();
        while (it.hasNext()) {
            CharFSMState next = (CharFSMState) it.next();
            fsm[cur++] = buildFSM(next);
        }
        return start;
    }


    public int get(String s) {
        if (nStates == 0)
            return -1;

        int cur = 0;
        int from = 0, length;
        int snum;
        int start;
        IntMapper m;
        int l;
        int label;

        cur = 0;
        from = 0;
        length = s.length();

        while (true) {
            start = cur;
            cur += 2;
            label = fsm[start];
            if ((label & FINAL_STATE) != 0)
                cur++;
            if ((label & HAS_APPENDIX) != 0) {
                l = cur + (label & 0x0000FFFF);
                for (; cur < l && from < length; cur++, from++) {
                    if (fsm[cur] != s.charAt(from))
                        return -1;
                }
                if (cur < l)
                    return -1;
            }
            if (from == length) {
                if ((label & FINAL_STATE) != 0)
                    return fsm[start + 2];
                else
                    return -1;
            } else {
                m = mappers[fsm[start + 1]];
                if (m != null) {
                    snum = recoder.getSymbolNumber(s.charAt(from));
                    int i = m.get(snum);
                    if (i == -1)
                        return -1;
                    cur = fsm[cur + i];
                    from++;
                } else {
                    return -1;
                }
            }
        }
    }

    public int get(TString str) {
        TStringImpl s = (TStringImpl) str;
        if (nStates == 0)
            return -1;

        int cur = 0;
        int from = 0, length;
        int snum;
        int start;
        IntMapper m;
        int l;
        int label;

        cur = 0;
        from = 0;
        length = s.length();

        while (true) {
            start = cur;
            cur += 2;
            label = fsm[start];
            if ((label & FINAL_STATE) != 0)
                cur++;
            if ((label & HAS_APPENDIX) != 0) {
                l = cur + (label & 0x0000FFFF);
                for (; cur < l && from < length; cur++, from++) {
                    if (fsm[cur] != s.value[from])
                        return -1;
                }
                if (cur < l)
                    return -1;
            }
            if (from == length) {
                if ((label & FINAL_STATE) != 0)
                    return fsm[start + 2];
                else
                    return -1;
            } else {
                m = mappers[fsm[start + 1]];
                if (m != null) {
                    snum = recoder.getSymbolNumber(s.value[from]);
                    int i = m.get(snum);
                    if (i == -1)
                        return -1;
                    cur = fsm[cur + i];
                    from++;
                } else {
                    return -1;
                }
            }
        }
    }

    public CharFSMState addCharArray(char[] arr, int from, int length) {
        return null;
    }

    public Iterator statesIterator() {
        return null;
    }

    public int getNumberOfStates() {
        return -1;
    }

    public State addState() {
        return null;
    }

    public int addString(String s) {
        return 0;
    }

    public int addString(TString s) {
        return 0;
    }

    public char[] getCharRepresentation() {
        return new char[0];
    }

    public int readInFromChars(char[] arr, int from) {
        return 0;
    }

    public int get(char[] value) {
        if (nStates == 0)
            return -1;

        int cur = 0;
        int from = 0, length;
        int snum;
        int start;
        IntMapper m;
        int l;
        int label;

        cur = 0;
        from = 0;
        length = value.length;

        while (true) {
            start = cur;
            cur += 2;
            label = fsm[start];
            if ((label & FINAL_STATE) != 0)
                cur++;
            if ((label & HAS_APPENDIX) != 0) {
                l = cur + (label & 0x0000FFFF);
                for (; cur < l && from < length; cur++, from++) {
                    if (fsm[cur] != value[from])
                        return -1;
                }
                if (cur < l)
                    return -1;
            }
            if (from == length) {
                if ((label & FINAL_STATE) != 0)
                    return fsm[start + 2];
                else
                    return -1;
            } else {
                m = mappers[fsm[start + 1]];
                if (m != null) {
                    snum = recoder.getSymbolNumber(value[from]);
                    int i = m.get(snum);
                    if (i == -1)
                        return -1;
                    cur = fsm[cur + i];
                    from++;
                } else {
                    return -1;
                }
            }
        }
    }

}
