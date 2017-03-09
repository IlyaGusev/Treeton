/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public class Utils {
    public static boolean smartEquals(Object o1, Object o2) {
        return o1 == null && o2 == null || !(o1 == null || o2 == null) && o1.equals(o2);
    }

    public static String memoryState() {
        return "Memory: " + Long.toString((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20) + "/" +
                Long.toString(Runtime.getRuntime().totalMemory() >> 20) + "m";
    }
}
