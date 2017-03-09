/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.util.sut;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class RecoderImpl implements Recoder {
    private short[] currentEncoding;
    private ArrayList<Character> currentReverseEncoding;
    private int currentNumber;

    public RecoderImpl() {
        currentEncoding = new short[65536];
        Arrays.fill(currentEncoding, (short) -1);
        currentReverseEncoding = new ArrayList<Character>();
        currentNumber = 0;
    }

    public int getSymbolNumber(char s) {
        short n = currentEncoding[(int) s];
        if (n == -1) {
            n = (short) currentNumber++;
            currentEncoding[(int) s] = n;
            currentReverseEncoding.add(s);
        }
        return n;
    }

    public char getSymbolByNumber(int n) {
        if (n < 0 || n >= currentReverseEncoding.size())
            throw new IllegalArgumentException();
        return currentReverseEncoding.get(n);
    }

    public char[] getReverseEncoding() {
        char[] arr = new char[currentReverseEncoding.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = currentReverseEncoding.get(i);
        }
        return arr;
    }

    public void appendSelf(ByteBuffer buf) {
        buf.putInt(currentReverseEncoding.size());
        for (Object aCurrentReverseEncoding : currentReverseEncoding) {
            Character c = (Character) aCurrentReverseEncoding;
            buf.putChar(c);
        }
    }

    public byte[] getByteRepresentation() {
        byte[] result = new byte[getByteSize()];
        sut.putIntegerInBytes(result, 0, currentReverseEncoding.size());
        for (int i = 0; i < currentReverseEncoding.size(); i++) {
            Character c = currentReverseEncoding.get(i);
            sut.putCharInBytes(result, 2 * i + 4, c);
        }
        return result;
    }

    public int getByteSize() {
        return 4 + 2 * currentReverseEncoding.size();
    }

    public int readInFromBytes(byte[] buf, int start) {
        currentEncoding = new short[65536];
        currentNumber = 0;
        Arrays.fill(currentEncoding, (short) -1);
        int size = sut.getIntegerFromBytes(buf, start);
        start += 4;
        currentReverseEncoding.clear();
        for (int i = 0; i < size; i++) {
            char c = sut.getCharFromBytes(buf, start);
            currentReverseEncoding.add(c);
            currentEncoding[c] = (short) currentNumber++;
            start += 2;
        }
        return start;
    }
}
