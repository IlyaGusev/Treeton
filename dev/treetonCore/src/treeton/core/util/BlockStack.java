/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.BlackBoard;

public class BlockStack {
    private Object[][] blocks;
    private int blockSize;
    private Object[] curBlock;
    private int curBlockNumber;
    private int stackPtr;
    private long position;

    public BlockStack() {
        this(1000);
    }

    public BlockStack(int _blockSize) {
        blockSize = _blockSize;
        blocks = new Object[1][blockSize];
        curBlockNumber = 0;
        curBlock = blocks[0];
        stackPtr = -1;
        position = -1;
    }

    public Object pop() {
        if (stackPtr == -1) {
            throw new RuntimeException("Stack underflow");
        }
        Object r = curBlock[stackPtr];
        curBlock[stackPtr] = null;
        position--;
        stackPtr--;
        if (stackPtr == -1 && curBlockNumber > 0) {
            curBlock = blocks[--curBlockNumber];
            stackPtr = blockSize - 1;
        }
        return r;
    }

    public void push(Object o) {
        stackPtr++;
        position++;
        if (stackPtr == blockSize) {
            stackPtr = 0;
            curBlockNumber++;
            if (curBlockNumber == blocks.length) {
                Object[][] t = new Object[blocks.length + 1][];
                System.arraycopy(blocks, 0, t, 0, blocks.length);
                t[curBlockNumber] = new Object[blockSize];
                blocks = t;
            }
            curBlock = blocks[curBlockNumber];
        }
        curBlock[stackPtr] = o;
    }

    public boolean isEmpty() {
        return stackPtr == -1 ? true : false;
    }

    public long getPosition() {
        return position;
    }

    public void clean() {
        stackPtr = -1;
        position = -1;
        curBlockNumber = 0;
        curBlock = blocks[0];
    }

    public Object[] toArray() {
        Object[] arr = new Object[(int) position + 1];
        int j = 0;
        for (; j < curBlockNumber; j++) {
            System.arraycopy(blocks[j], 0, arr, j * blockSize, blockSize);
        }
        for (int i = 0; i <= stackPtr; i++) {
            arr[j * blockSize + i] = curBlock[i];
        }
        return arr;
    }

    public void fillArray(Object[] arr) {
        int j = 0;
        for (; j < curBlockNumber; j++) {
            System.arraycopy(blocks[j], 0, arr, j * blockSize, blockSize);
        }
        for (int i = 0; i <= stackPtr; i++) {
            arr[j * blockSize + i] = curBlock[i];
        }
    }

    void fillBlackBoard(BlackBoard b) {
        int j = 0;
        for (; j < curBlockNumber; j++) {
            for (int i = 0; i < blockSize; i++) {
                Numerated no = (Numerated) blocks[j][i];
                b.put(no.getNumber(), no);
            }
        }
        for (int i = 0; i <= stackPtr; i++) {
            Numerated no = (Numerated) curBlock[i];
            b.put(no.getNumber(), no);
        }
    }

}
