/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import treeton.core.IntMapper;
import treeton.core.IntMapperStorage;
import treeton.core.TString;
import treeton.core.TreetonFactory;
import treeton.core.util.sut;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PackedByteFSM extends CharFSMImpl {
    private final static byte FINAL_STATE = 1;
    private final static byte HAS_APPENDIX = 2;
    private final static byte NO_MAPPER = 4;
    private byte[] fsm;
    private int firstFree;
    private IntMapper[] mappers;
    private int maxMapperId;
    private HashMap intMappers;

    public PackedByteFSM() {
    }

    public PackedByteFSM(CharFSMImpl cfsm) {
        Iterator it = cfsm.statesIterator();
        int size = 0;
        nStates = 0;
        while (it.hasNext()) {
            CharFSMState s = (CharFSMState) it.next();
            size++;
            if (s.getIntMapper() != null)
                size += 4;
            if (s.value != -1)
                size += 4;
            if (s.appendix != null) {
                size += 2 + s.appendix.length;
            }
            size += s.size() * 4;
        }
        fsm = new byte[size];
        nStates = cfsm.getNumberOfStates();
        recoder = cfsm.recoder;
        intMappers = new HashMap();
        maxMapperId = -1;
        nStrings = cfsm.getSize();
        firstFree = 0;
        if (fsm.length > 0)
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
        int cur = start;
        fsm[start] = 0;
        cur++;
        if (s.getIntMapper() != null) {
            Integer mapperId = (Integer) intMappers.get(s.getIntMapper());
            if (mapperId == null) {
                maxMapperId++;
                intMappers.put(s.getIntMapper(), (mapperId = new Integer(maxMapperId)));
            }
            sut.putIntegerInBytes(fsm, cur, mapperId.intValue());
            cur += 4;
        } else {
            fsm[start] |= NO_MAPPER;
        }
        if (s.isFinal) {
            fsm[start] |= FINAL_STATE;
            sut.putIntegerInBytes(fsm, cur, s.value);
            cur += 4;
        }
        if (s.appendix != null) {
            fsm[start] |= HAS_APPENDIX;
            sut.putShortInBytes(fsm, cur, (short) s.appendix.length);
            cur += 2;
            int i = 0;
            for (; i < s.appendix.length; i++, cur++) {
                fsm[cur] = (byte) recoder.getSymbolNumber(s.appendix[i]);
            }
        }
        firstFree = cur + s.size() * 4;
        if (s.getIntMapper() != null) {
            Iterator it = s.valueIterator();
            while (it.hasNext()) {
                CharFSMState next = (CharFSMState) it.next();
                sut.putIntegerInBytes(fsm, cur, buildFSM(next));
                cur += 4;
            }
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
        int l;
        byte label;

        cur = 0;
        from = 0;
        length = s.length();

        while (true) {
            start = cur;

            label = fsm[start];
            cur++;
            if ((label & NO_MAPPER) == 0)
                cur += 4;
            if ((label & FINAL_STATE) != 0)
                cur += 4;
            if ((label & HAS_APPENDIX) != 0) {
                l = cur + sut.getShortFromBytes(fsm, cur) + 2;
                cur += 2;
                for (; cur < l && from < length; cur++, from++) {
                    if (recoder.getSymbolByNumber(fsm[cur]) != s.charAt(from))
                        return -1;
                }
                if (cur < l)
                    return -1;
            }
            if (from == length) {
                if ((label & FINAL_STATE) != 0) {
                    if ((label & NO_MAPPER) == 0) {
                        return sut.getIntegerFromBytes(fsm, start + 5);
                    } else {
                        return sut.getIntegerFromBytes(fsm, start + 1);
                    }
                } else {
                    return -1;
                }
            } else {
                if ((label & NO_MAPPER) == 0) {
                    IntMapper m = mappers[sut.getIntegerFromBytes(fsm, start + 1)];
                    snum = recoder.getSymbolNumber(s.charAt(from));
                    int i = m.get(snum);
                    if (i == -1)
                        return -1;
                    cur = sut.getIntegerFromBytes(fsm, cur + i * 4);
                    from++;
                } else {
                    return -1;
                }
            }
        }
    }

    public int get(TString s) {
        if (nStates == 0)
            return -1;

        int cur = 0;
        int from = 0, length;
        int snum;
        int start;
        int l;
        byte label;

        cur = 0;
        from = 0;
        length = s.length();

        while (true) {
            start = cur;

            label = fsm[start];
            cur++;
            if ((label & NO_MAPPER) == 0)
                cur += 4;
            if ((label & FINAL_STATE) != 0)
                cur += 4;
            if ((label & HAS_APPENDIX) != 0) {
                l = cur + sut.getShortFromBytes(fsm, cur) + 2;
                cur += 2;
                for (; cur < l && from < length; cur++, from++) {
                    if (recoder.getSymbolByNumber(fsm[cur]) != s.charAt(from))
                        return -1;
                }
                if (cur < l)
                    return -1;
            }
            if (from == length) {
                if ((label & FINAL_STATE) != 0) {
                    if ((label & NO_MAPPER) == 0) {
                        return sut.getIntegerFromBytes(fsm, start + 5);
                    } else {
                        return sut.getIntegerFromBytes(fsm, start + 1);
                    }
                } else {
                    return -1;
                }
            } else {
                if ((label & NO_MAPPER) == 0) {
                    IntMapper m = mappers[sut.getIntegerFromBytes(fsm, start + 1)];
                    snum = recoder.getSymbolNumber(s.charAt(from));
                    int i = m.get(snum);
                    if (i == -1)
                        return -1;
                    cur = sut.getIntegerFromBytes(fsm, cur + i * 4);
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

    public void appendSelf(ByteBuffer buf) {
        recoder.appendSelf(buf);
        buf.putInt(mappers.length);
        for (int i = 0; i < mappers.length; i++) {
            mappers[i].appendSelf(buf);
        }
        buf.putInt(nStates);
        buf.putInt(nStrings);
        buf.putInt(fsm.length);
        buf.put(fsm);
    }

    public int readInFromBytes(byte[] buf, int from) {
        recoder = TreetonFactory.newRecoder();
        from = recoder.readInFromBytes(buf, from);
        int size = sut.getIntegerFromBytes(buf, from);
        from += 4;
        mappers = new IntMapper[size];
        for (int i = 0; i < mappers.length; i++) {
            mappers[i] = IntMapperStorage.getInstance().getIntMapper(buf, from);
            from += mappers[i].getByteSize();
        }
        nStates = sut.getIntegerFromBytes(buf, from);
        from += 4;
        nStrings = sut.getIntegerFromBytes(buf, from);
        from += 4;
        size = sut.getIntegerFromBytes(buf, from);
        from += 4;
        fsm = new byte[size];
        System.arraycopy(buf, from, fsm, 0, size);
        from += size;
        return from;
    }

    public int getByteSize() {
        int size = recoder.getByteSize() + 4;
        for (int i = 0; i < mappers.length; i++) {
            size += mappers[i].getByteSize();
        }
        size += 4 + 4 + 4 + fsm.length;
        return size;
    }
}
