/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import java.nio.ByteBuffer;

//todo: добавить valueOf(int)

public interface TString extends java.io.Serializable, Comparable {
    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin);

    public char[] toCharArray();

    public void appendToStringBuffer(StringBuffer buf);

    public char charAt(int index);

    public void slurp(StringBuffer buf);

    public void slurp(String str);

    public void slurp(char[] chars, int begin, int count);

    public int length();

    int getByteSize();

    void appendSelf(ByteBuffer buf);

    int readInFromBytes(byte[] arr, int from);
}
