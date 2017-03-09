/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.fsm.State;

public abstract class ScapeState implements State, Comparable {
    private static int ID = 0;
    long label;
    private int id;

    ScapeState() {
        label = -1;
        id = ID++;
    }

    public int compareTo(Object o) {
        if (o instanceof ScapeState) {
            ScapeState anotherBinding = (ScapeState) o;
            if (id < anotherBinding.id) {
                return -1;
            } else if (id > anotherBinding.id) {
                return 1;
            }
            return 0;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int getId() {
        return id;
    }
}
