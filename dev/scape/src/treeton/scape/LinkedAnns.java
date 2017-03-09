/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;

import java.util.Iterator;

public class LinkedAnns {
    public static AnnsIterator newAnnsIterator(Item item) {
        return new AnnsIterator(item);
    }

    public static Item addItem(Annotation ann, Item item) {
        return new Item(ann, item);
    }

    public static class Item {
        Annotation ann;
        LinkedAnns.Item previous;

        Item(Annotation ann, LinkedAnns.Item previous) {
            this.ann = ann;
            this.previous = previous;
        }

        public Annotation getAnn() {
            return ann;
        }
    }

    public static class AnnsIterator implements Iterator {
        private Item cur;

        private AnnsIterator(Item item) {
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

        public Annotation nextAnn() {
            Item t = cur;
            cur = cur.previous;
            return t.ann;
        }

    }
}
