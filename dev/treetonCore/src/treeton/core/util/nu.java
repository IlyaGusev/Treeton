/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.TString;
import treeton.core.TreetonFactory;

public class nu {
    public static nu ll = new nu();
    public static nu other = new nu();
    public static TString llTString = TreetonFactory.newTString("null");
    public static String llString = "null";
    public static String otherString = "other";

    public String toString() {
        if (this == ll) {
            return llString;
        } else if (this == other) {
            return otherString;
        } else {
            return "";
        }
    }
}
