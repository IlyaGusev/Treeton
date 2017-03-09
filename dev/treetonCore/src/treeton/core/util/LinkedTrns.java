/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.Treenotation;

import java.util.Iterator;

public class LinkedTrns {
    public static TrnsIterator newTrnsIterator(Item item) {
        return new TrnsIterator(item);
    }

    public static Item addItem(Treenotation trn, Item item) {
        return new Item(trn, item);
    }

    public static class Item {
        Treenotation trn;
        LinkedTrns.Item previous;

        Item(Treenotation trn, LinkedTrns.Item previous) {
            this.trn = trn;
            this.previous = previous;
        }

        public Item getPrevious() {
            return previous;
        }

        public Treenotation getTrn() {
            return trn;
        }
    }

    public static class TrnsIterator implements Iterator {
        private Item cur;

        private TrnsIterator(Item item) {
            cur = item;
        }

        public void reset(Item item) {
            cur = item;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return cur != null;
        }

        public Object next() {
            Item t = cur;
            cur = cur.previous;
            return t;
        }

        public Treenotation nextTrn() {
            Item t = cur;
            cur = cur.previous;
            return t.trn;
        }

    }

}
