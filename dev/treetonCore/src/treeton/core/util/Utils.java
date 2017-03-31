/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static boolean smartEquals(Object o1, Object o2) {
        return o1 == null && o2 == null || !(o1 == null || o2 == null) && o1.equals(o2);
    }

    public static String memoryState() {
        return "Memory: " + Long.toString((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20) + "/" +
                Long.toString(Runtime.getRuntime().totalMemory() >> 20) + "m";
    }

    /**
     * Returns list of files of given directory according to given filter
     * recursively or not.
     */
    public static List<File> listFiles(File directory, FilenameFilter filter,
                                       boolean recurse) {
        ArrayList<File> files = new ArrayList<File>();
        File[] entries = directory.listFiles();

        if (entries != null) {
            for (File entry : entries) {
                if (filter == null || filter.accept(directory, entry.getName())) {
                    files.add(entry);
                }
                if (recurse && entry.isDirectory()) {
                    files.addAll(listFiles(entry, filter, recurse));
                }
            }
        }
        return files;
    }
}
