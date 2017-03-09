/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

import treeton.core.util.sut;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class ByteLinesReader {
    private static int readBufferSize = 2048;
    private static int maxLineSize = 16384;
    private static int tailSize = 4;
    private static int tailPos = readBufferSize - tailSize;

    private InputStream inputStream = null;
    private byte[] currentLine = null;
    private boolean isLineComplete = false;
    private boolean waitLineTerminator = false;
    private int currentLineLength = 0;
    private byte[] buffer = null;
    private int curBufferPosition = 0;
    private byte[] filebuf = null;
    private int fileBufPosition = 0;
    private int fileBufReaded = 0;
    private boolean eofReached = false;
    private boolean lastLineReturned = false;

    public ByteLinesReader(InputStream _fi)
            throws IllegalArgumentException {
        inputStream = _fi;
        if (inputStream != null) {
            currentLine = new byte[maxLineSize];
            buffer = new byte[readBufferSize];
            filebuf = new byte[readBufferSize];
        } else
            throw new IllegalArgumentException();
    }

    public byte[] readLine(int _maxBytes)
            throws UnsupportedEncodingException {
        byte b[] = readLine();
        if (b.length > _maxBytes) {
            byte t[] = new byte[_maxBytes];
            System.arraycopy(b, 0, t, 0, t.length);
            b = t;
        }
        return b;
    }

    public byte[] readLine()
            throws UnsupportedEncodingException {
        if (isLineComplete) {
            byte rslt[] = null;
            if (currentLineLength >= 0 && !lastLineReturned) {
                rslt = new byte[currentLineLength];
                System.arraycopy(currentLine, 0, rslt, 0, currentLineLength);
                currentLineLength = 0;
                isLineComplete = false;
                waitLineTerminator = false;
            }
            if (eofReached && curBufferPosition == 0)
                lastLineReturned = true;
            return rslt;
        } else {
            while (!isLineComplete) {
                int readed = 0;
                if (!eofReached && curBufferPosition < readBufferSize) {
                    readed = readToBuffer(curBufferPosition,
                            readBufferSize - curBufferPosition);
                }
                curBufferPosition += readed;
                int i = sut.findByteForward(buffer, sut.CR_CODE);
                if (i >= 0 && i < curBufferPosition &&
                        i < tailPos && buffer[i + 1] == sut.LF_CODE) { // if found CRLF before tail,
                    // then make current line completed
                    // and return currentLine

                    addToLine(buffer, i);
                    i += 2; // skip CRLF
                    try {
                        sut.moveBytes(buffer, i, curBufferPosition - i, 0);
                    } catch (IndexOutOfBoundsException x) {
                        x.printStackTrace();
                    } catch (NullPointerException x) {
                        x.printStackTrace();
                    }
                    curBufferPosition -= i;
                    isLineComplete = true;
                    return readLine();
                } else {
                    int n = Math.min(tailPos, curBufferPosition);
                    addToLine(buffer, n);
                    try {
                        sut.moveBytes(buffer, n, curBufferPosition - n, 0);
                    } catch (IndexOutOfBoundsException x) {
                        x.printStackTrace();
                    } catch (NullPointerException x) {
                        x.printStackTrace();
                    }
                    curBufferPosition -= n;
                }
                if (eofReached && curBufferPosition == 0) {
                    isLineComplete = true;
                }
            }
            return readLine();
        }
    }

    protected int addToLine(byte[] _b, int _len) {
        int newLen = 0;
        if (!waitLineTerminator) {
            newLen = Math.min(maxLineSize - currentLineLength, _len);
            if (newLen > 0) {
                System.arraycopy(_b, 0, currentLine, currentLineLength, newLen);
                currentLineLength += newLen;
            }
            waitLineTerminator = !(currentLineLength < maxLineSize);
        }
        return newLen;
    }

    protected int readToBuffer(int _start, int _len) {
        int copied = 0; // used as rslt
        if (_len > 0 && (fileBufPosition < fileBufReaded || !eofReached)) {
            int tLen = Math.min(_len, fileBufReaded - fileBufPosition);
            if (tLen > 0) {
                System.arraycopy(filebuf, fileBufPosition, buffer, _start, tLen);
                fileBufPosition += tLen;
                copied += tLen;
            }
            if (fileBufPosition == fileBufReaded && !eofReached) {
                try {
                    fileBufReaded = inputStream.read(filebuf);
                    eofReached = (fileBufReaded <= 0);
                } catch (IOException x) {
                    eofReached = true;
                }
                fileBufPosition = 0;
            }
            copied += readToBuffer(_start + tLen, _len - tLen);
        }
        return copied;
    }

    public void close() {
        currentLine = null;
        buffer = null;
        filebuf = null;
    }
}
