/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import com.sun.tools.javac.Main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Javac {
    public static int compile(File srcDir, File outDir, String encoding) throws IOException {
        List args = new ArrayList();
        args.add("-sourcepath");
        args.add(srcDir.getAbsolutePath());
        if (null != encoding && !encoding.equals("")) {
            args.add("-encoding");
            //args.add("UTF-8");
            args.add(encoding);
        }
        args.add("-d");
        args.add(outDir.getAbsolutePath());
        args.addAll(findAllJavaFiles(srcDir, null));
        int res = Main.compile((String[]) args.toArray(new String[args.size()]));
        return res;
    }

    private static List findAllJavaFiles(File srcDir, List fileNames) throws IOException {
        if (fileNames == null) fileNames = new ArrayList();
        File[] fList = srcDir.listFiles();
        for (int i = 0; i < fList.length; i++) {
            File file = fList[i];
            if (file.isFile() && file.getName().endsWith(".java")) {
                fileNames.add(file.getCanonicalPath());
                continue;
            }
            if (file.isDirectory()) fileNames.addAll(findAllJavaFiles(file, null));
        }
        return fileNames;
    }
}
