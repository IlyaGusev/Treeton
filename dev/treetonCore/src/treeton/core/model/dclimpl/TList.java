/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

public class TList {
    TListEntry first = null;
    TListEntry last = null;

    public void concat(TList l) {
        if (first == null) {
            first = l.first;
            last = l.last;
        } else {
            if (l.first != null) {
                last.next = l.first;
                last = l.last;
            }
        }
    }

    public void add(TListEntry e) {
        if (first == null) {
            first = e;
            last = e;
        } else {
            last.next = e;
            last = e;
        }
    }

    /**
     * Returns an array containing all of the elements in this list
     * in the correct order.
     *
     * @return an array containing all of the elements in this list
     * in the correct order.
     */
    public Object[] toArray() {
        int size = 0;
        TListEntry cur = first;
        while (cur != null) {
            size++;
            cur = cur.next;
        }
        Object[] result = new Object[size];
        cur = first;
        int i = 0;
        while (cur != null) {
            result[i++] = cur.o;
            cur = cur.next;
        }
        return result;
    }

    /**
     * Returns an array containing all of the elements in this list in the
     * correct order; the runtime type of the returned array is that of the
     * specified array.  If the list fits in the specified array, it is
     * returned therein.  Otherwise, a new array is allocated with the runtime
     * type of the specified array and the size of this list.<p>
     * <p/>
     * If the list fits in the specified array with room to spare (i.e., the
     * array has more elements than the list), the element in the array
     * immediately following the end of the collection is set to
     * <tt>null</tt>.  This is useful in determining the length of the list
     * <i>only</i> if the caller knows that the list does not contain any
     * <tt>null</tt> elements.
     *
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list.
     * @throws ArrayStoreException if the runtime type of a is not a supertype
     *                             of the runtime type of every element in this list.
     */
    public Object[] toArray(Object a[]) {
        int size = 0;
        TListEntry cur = first;
        while (cur != null) {
            size++;
            cur = cur.next;
        }
        if (a.length < size)
            a = (Object[]) java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), size);
        cur = first;
        int i = 0;
        while (cur != null) {
            a[i++] = cur.o;
            cur = cur.next;
        }

        if (a.length > size)
            a[size] = null;

        return a;
    }

    public int size() {
        int size = 0;
        TListEntry cur = first;
        while (cur != null) {
            size++;
            cur = cur.next;
        }
        return size;
    }
}
