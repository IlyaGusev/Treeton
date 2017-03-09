/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.model.TrnType;

public class ScapeOutputItem {
    TrnType type;
    boolean create;
    boolean remove;
    boolean modify;

    ScapeOutputItem(TrnType _type) {
        type = _type;
        create = true;
        remove = false;
        modify = false;
    }

    public ScapeOutputItem(TrnType _type, boolean _create, boolean _remove, boolean _modify) {
        type = _type;
        create = _create;
        remove = _remove;
        modify = _modify;
    }

    ScapeOutputItem(TrnType _type, char[] mask, int len) {
        type = _type;

        if (len > 3)
            len = 3;

        create = false;
        remove = false;
        modify = false;

        for (int i = 0; i < len; i++) {
            if (mask[i] == 'c') {
                create = true;
            } else if (mask[i] == 'r') {
                remove = true;
            } else if (mask[i] == 'm') {
                modify = true;
            }
        }
    }

}
