/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import treeton.core.util.IdProvider;

public class CombinationsControllerBitArrayImpl implements CombinationsController {
    private static final int START_CAPACITY = 64;

    int dim;
    int[] sizes;
    long[] dsizes;
    byte[] data;

    public CombinationsControllerBitArrayImpl(int dim) {
        this.dim = dim;
        sizes = new int[dim];
        dsizes = new long[dim];
        int dataLength = 1;

        for (int i = 0; i < sizes.length; i++) {
            sizes[i] = START_CAPACITY;
            dsizes[i] = i > 0 ? dsizes[i - 1] * START_CAPACITY : 1;
            dataLength *= START_CAPACITY;
        }

        data = new byte[dataLength / 8];
    }

    public CombinationsControllerBitArrayImpl(int[] sizes) {
        this.dim = sizes.length;
        this.sizes = new int[dim];
        this.dsizes = new long[dim];
        int dataLength = 1;

        for (int i = 0; i < this.sizes.length; i++) {
            this.sizes[i] = sizes[i];
            dsizes[i] = i > 0 ? dsizes[i - 1] * sizes[i - 1] : 1;
            dataLength *= sizes[i];
        }

        data = new byte[dataLength / 8];
    }

    public boolean combinationIsMarked(boolean markIfNot, IdProvider... ids) {
        assert ids.length == dim;

        long position = 0;
        long blocksize = 0;
        long newblocksize = 0;
        long newDataLength = -1;
        for (int i = 0; i < ids.length; i++) {
            int val = ids[i].getId();
            int currentSize = sizes[i];
            if (val >= currentSize) {
                if (newDataLength == -1) {
                    blocksize = i == dim - 1 ? data.length : (dsizes[i] * sizes[i] / 8);
                    sizes[i] = (Math.max(currentSize * 3 / 2, val + 1) / 8) * 8 + 8;
                    newblocksize = i == dim - 1 ? data.length : (dsizes[i] * sizes[i] / 8);
                    newDataLength = dsizes[i] * sizes[i];
                    position += val * dsizes[i];
                    continue;
                } else {
                    sizes[i] = (Math.max(currentSize * 3 / 2, val + 1) / 8) * 8 + 8;
                }
            }

            if (newDataLength != -1) {
                dsizes[i] = dsizes[i - 1] * sizes[i - 1];
                newDataLength *= sizes[i];
            }

            position += val * dsizes[i];
        }

        //8 8      |      8 16
        //1 8      |      1 8

        //8 8      |      16 8
        //1 8      |      1  16


        if (newDataLength != -1) { //resizing is needed
            byte[] newdata = new byte[(int) (newDataLength / 8)];

            int srcoffs = 0;
            int destoffs = 0;
            while (srcoffs < data.length) {
                System.arraycopy(data, srcoffs, newdata, destoffs, (int) blocksize);
                srcoffs += blocksize;
                destoffs += newblocksize;
            }

            data = newdata;
        }


        int pos = (int) (position / 8);
        int mask = 1 << position % 8;
        int val = data[pos] & mask;
        if (val > 0) {
            return true;
        } else {
            if (markIfNot) {
                data[pos] |= mask;
            }

            return false;
        }
    }

    public int[] getSizes() {
        return sizes;
    }

    public void unmark(IdProvider... ids) {
        assert ids.length == dim;

        int position = 0;
        for (int i = 0; i < ids.length; i++) {
            int val = ids[i].getId();
            position += val * dsizes[i];
        }

        int pos = position / 8;
        int mask = 255 ^ (1 << position % 8);
        data[pos] &= mask;
    }
}