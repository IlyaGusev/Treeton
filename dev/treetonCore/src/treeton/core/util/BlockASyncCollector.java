/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.concurrent.atomic.AtomicInteger;

public class BlockASyncCollector<E> implements Collector<E> {
    E[][] data;
    AtomicInteger i = new AtomicInteger(0);
    int blockSize;
    int nBlocks;

    public BlockASyncCollector(int nBlocks, int blockSize) {
        this.blockSize = blockSize;
        this.nBlocks = nBlocks;
        data = (E[][]) new Object[nBlocks][];
    }

    public int size() {
        return i.intValue();
    }

    public E get(int index) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return safeGet(index / blockSize, index % blockSize);
    }

    private E safeGet(int bn, int shift) {
        for (; ; ) {
            try {
                E[] o = data[bn];
                if (o != null) {
                    E o1 = o[shift];
                    if (o1 != null)
                        return o1;
                }
            } catch (Exception e) {
                //do nothing
            }
        }
    }


    public void add(E object) {
        if (object == null)
            throw new IllegalArgumentException("nulls are not allowed");

        int index = i.getAndIncrement();
        int blockN = index / blockSize;
        if (data[blockN] == null) {
            synchronized (this) {
                if (data[blockN] == null) {
                    data[blockN] = (E[]) new Object[blockSize];
                }
            }
        }
        data[blockN][index % blockSize] = object;
    }
}
