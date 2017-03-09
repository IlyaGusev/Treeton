/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import java.nio.ByteBuffer;

public interface Recoder {
    public int getSymbolNumber(char s);

    public char getSymbolByNumber(int n);

    public char[] getReverseEncoding();

    void appendSelf(ByteBuffer buf);

    int getByteSize();

    public int readInFromBytes(byte[] buf, int start);

}
