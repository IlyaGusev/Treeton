/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public class PtrPtr {
    public Ptr ptr;

    public PtrPtr(Ptr _ptr) {
        setPtr(_ptr);
    }

    public Ptr getPtr() {
        return ptr;
    }

    public void setPtr(Ptr _ptr) {
        ptr = _ptr;
    }
}
