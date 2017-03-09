/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.io.*;

public class FileCutter {
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Usage: java.exe treeton.core.util.FileCutter file_name target_dir");
        }
        File f = new File(args[0]);
        try {
            (new FileCutter()).cut(f, args[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cut(File f, String targetDir) throws IOException {
        Integer partNo = 0;
        String name = f.getName();
        String nameWithoutExt = name.lastIndexOf(".") < 0 ? name : name.substring(0, name.lastIndexOf("."));
        String ext = name.lastIndexOf(".") < 0 ? "" : name.substring(name.lastIndexOf("."));
        InputStream is = new FileInputStream(f);
        byte[] arr = new byte[(int) f.length()];
        if (is.read(arr) > 0) {
            int prev = 0;
            for (int i = 0; i < arr.length; i++) {
                if (i % 100000 == 0) System.out.println(i * 100.0 / arr.length);
                int len = delimeterLocated(arr, i);
                if (len > 0 || i == arr.length - 1)
                    if (i - prev > 64) {
                        OutputStream os = new FileOutputStream(targetDir + "\\" + nameWithoutExt + "_" + partNo.toString() + ext);
                        for (int j = prev; j <= i; j++) {
                            os.write((int) filter(arr[j]));
                        }
                        os.close();
                        prev = i + 1;
                        partNo++;
                    }
            }
        }
    }

    int delimeterLocated(byte[] arr, int i) {
        if (arr[i] == 0x13) {
            return 1;
        }
        return -1;
    }

    byte filter(byte b) {
        if (b < 32 && b >= 0) return 32;
        return b;
    }
}
