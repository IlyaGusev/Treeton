/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

public class ScapeUniLabel {
    private static long label = 0;

    public static long get() {
        return label++;
    }
}
