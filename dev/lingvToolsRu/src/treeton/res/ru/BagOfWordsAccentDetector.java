/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.TreetonFactory;
import treeton.core.fsm.CharFSM;
import treeton.core.fsm.CharFSMImpl;
import treeton.core.fsm.PackedByteFSM;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

public class BagOfWordsAccentDetector {
    private boolean isPacked = false;
    private CharFSM fsm;
    private ArrayList<byte[]> accentPlaces;
    private int dataByteSize;
    private BitSet klitiksInfo;

    public BagOfWordsAccentDetector() {
        fsm = TreetonFactory.newCharFSM();
        accentPlaces = new ArrayList<>();
        dataByteSize = 0;
        klitiksInfo = new BitSet();
    }

    public BagOfWordsAccentDetector(byte[] data, int from) {
        accentPlaces = new ArrayList<>();

        PackedByteFSM pfsm = new PackedByteFSM();
        from = pfsm.readInFromBytes(data, from);
        fsm = pfsm;
        int nStrings = fsm.getSize();

        for (int i = 0; i < nStrings; i++) {
            int sz = data[from++];
            dataByteSize += sz;

            accentPlaces.add(Arrays.copyOfRange(data, from, from + sz));

            from += sz;
        }

        ByteBuffer buf = ByteBuffer.wrap(data, from, Integer.BYTES);
        int size = buf.getInt();
        from += Integer.BYTES;

        klitiksInfo = BitSet.valueOf(Arrays.copyOfRange(data, from, from + size));

        isPacked = true;
    }

    public void addString(String wordform, byte[] accPlaces, boolean isKlitik) {
        assert (wordform.length() > 0 && wordform.length() < Byte.MAX_VALUE);
        assert (!isPacked);

        wordform = wordform.toLowerCase().replace("ё", "е");

        assert (accPlaces != null && accPlaces.length > 0);

        for (byte accPlace : accPlaces) {
            assert (accPlace >= -1 && accPlace < wordform.length());
        }


        int index = fsm.addString(wordform);
        assert (index >= 0);
        if (index < accentPlaces.size()) {
            byte[] oldAccents = accentPlaces.get(index);

            Arrays.sort(accPlaces);

            int nCommon = 0;

            for (int i = 0, j = 0; i < accPlaces.length && j < oldAccents.length; ) {
                byte ai = accPlaces[i];
                byte aj = oldAccents[j];
                assert (aj >= -1 && aj < wordform.length());

                if (ai < aj) {
                    i++;
                } else if (ai == aj) {
                    nCommon++;
                    i++;
                    j++;
                } else {
                    j++;
                }
            }

            if (nCommon < accPlaces.length) {
                byte[] newAccents = new byte[oldAccents.length + accPlaces.length - nCommon];

                int k = 0;
                int i = 0, j = 0;
                for (; i < accPlaces.length && j < oldAccents.length; ) {
                    byte ai = accPlaces[i];
                    byte aj = oldAccents[j];
                    assert (aj >= -1 && aj < wordform.length());

                    if (ai < aj) {
                        newAccents[k++] = ai;
                        i++;
                    } else if (ai == aj) {
                        i++;
                        j++;
                    } else {
                        newAccents[k++] = aj;
                        j++;
                    }
                }

                while (i < accPlaces.length) {
                    byte ai = accPlaces[i++];
                    newAccents[k++] = ai;
                }

                while (j < oldAccents.length) {
                    byte aj = oldAccents[j++];
                    assert (aj >= -1 && aj < wordform.length());
                    newAccents[k++] = aj;
                }

                accentPlaces.set(index, newAccents);
                dataByteSize -= oldAccents.length;
                dataByteSize += newAccents.length;
            }

            if (isKlitik) {
                klitiksInfo.set(index * 2);
            } else {
                klitiksInfo.set(index * 2 + 1);
            }
        } else {
            Arrays.sort(accPlaces);
            accentPlaces.add(accPlaces);
            dataByteSize += accPlaces.length;

            if (isKlitik) {
                klitiksInfo.set((accentPlaces.size() - 1) * 2);
            } else {
                klitiksInfo.set((accentPlaces.size() - 1) * 2 + 1);
            }
        }
    }

    public byte[] getAccentPlaces(String word) {
        word = word.toLowerCase().replace("ё", "е");

        int i = fsm.get(word);

        if (i >= 0) {
            return accentPlaces.get(i);
        } else {
            return null;
        }
    }

    public KlitikInfo getKlitikInfo(String word) {
        word = word.toLowerCase().replace("ё", "е");

        int i = fsm.get(word);

        if (i >= 0) {
            if (klitiksInfo.get(i * 2)) {
                if (klitiksInfo.get(i * 2 + 1)) {
                    return KlitikInfo.AMBIG;
                } else {
                    return KlitikInfo.YES;
                }
            } else if (klitiksInfo.get(i * 2 + 1)) {
                return KlitikInfo.NO;
            } else {
                throw new AssertionError();
            }
        } else {
            return null;
        }
    }

    public void pack() {
        assert (!isPacked);

        fsm = new PackedByteFSM((CharFSMImpl) fsm);

        isPacked = true;
    }

    public byte[] getByteRepresentation() {
        assert (isPacked);

        PackedByteFSM pfsm = (PackedByteFSM) fsm;

        byte[] klitiksInfoByteArray = klitiksInfo.toByteArray();

        ByteBuffer buf = ByteBuffer.allocate(pfsm.getByteSize() + pfsm.getSize() +
                dataByteSize + Integer.BYTES + klitiksInfoByteArray.length);

        pfsm.appendSelf(buf);
        for (byte[] accPlaces : accentPlaces) {
            assert (accPlaces.length < Byte.MAX_VALUE);
            buf.put((byte) accPlaces.length);
            buf.put(accPlaces);
        }

        buf.putInt(klitiksInfoByteArray.length);
        buf.put(klitiksInfoByteArray);

        return buf.array();
    }

    public enum KlitikInfo {
        YES,
        NO,
        AMBIG
    }
}
