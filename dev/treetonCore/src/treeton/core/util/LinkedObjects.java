/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.Iterator;

public class LinkedObjects {
    public static <T> ItemIterator<T> newItemIterator(Item<T> item) {
        return new ItemIterator<T>(item);
    }

    public static <T> Item<T> addItem(T o, Item<T> item) {
        return new Item<T>(o, item);
    }

    public static class Item<T> {
        T o;
        LinkedObjects.Item<T> previous;

        Item(T o, LinkedObjects.Item<T> previous) {
            this.o = o;
            this.previous = previous;
        }

        public T getObject() {
            return o;
        }

        public LinkedObjects.Item<T> getPrevious() {
            return previous;
        }
    }

    public static class ItemIterator<T> implements Iterator<LinkedObjects.Item<T>> {
        private Item<T> cur;

        public ItemIterator(Item<T> item) {
            cur = item;
        }

        public void reset(Item<T> item) {
            cur = item;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return cur != null;
        }

        public LinkedObjects.Item<T> next() {
            Item<T> t = cur;
            cur = cur.previous;
            return t;
        }
    }
}
