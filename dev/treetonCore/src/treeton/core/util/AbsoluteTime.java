/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.util.concurrent.atomic.AtomicInteger;

public class AbsoluteTime {
    private static AtomicInteger t = new AtomicInteger(0);

    public static int tick() {
        return t.getAndIncrement();
    }
}
