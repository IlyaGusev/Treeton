/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import java.io.*;
import java.net.URL;

public class FileMapper {
    public static byte[] map2bytes(File f) throws IOException {
        InputStream is = new FileInputStream(f);
        byte[] buf = new byte[(int) f.length()];
        int len = is.read(buf, 0, buf.length);
        is.close();
        return buf;
    }

    public static byte[] map2bytes(String filename) throws IOException {
        File f = new File(filename);
        InputStream is = new FileInputStream(f);
        byte[] buf = new byte[(int) f.length()];
        int len = is.read(buf, 0, buf.length);
        is.close();
        return buf;
    }

    public static char[] map2memory(File f, String encoding) throws IOException {
        InputStream is = new FileInputStream(f);
        InputStreamReader rd = new InputStreamReader(is, encoding);
        char[] buf = new char[(int) f.length()];
        int len = rd.read(buf, 0, buf.length);
        is.close();

        char[] res = new char[len];
        System.arraycopy(buf, 0, res, 0, len);

        return res;
    }

    public static char[] map2memory(URL url, String encoding) throws IOException {
        InputStream is = url.openStream();
        InputStreamReader rd = new InputStreamReader(is, encoding);

        StringBuffer buf = new StringBuffer();

        char[] arr = new char[1000];

        int len;
        while ((len = rd.read(arr, 0, arr.length)) > 0) {
            buf.append(arr, 0, len);
        }

        is.close();

        return buf.toString().toCharArray();
    }

    public static char[] map2memory(String filename, String encoding) throws IOException {
        File f = new File(filename);
        InputStream is = !f.exists() ? FileMapper.class.getResourceAsStream(filename) : new FileInputStream(f);
        InputStreamReader rd = new InputStreamReader(is, encoding);
        char[] buf = new char[(int) f.length()];
        int len = rd.read(buf, 0, buf.length);
        is.close();

        char[] res = new char[len];
        System.arraycopy(buf, 0, res, 0, len);

        return res;
    }

    public static char[] map2memory(String filename) throws IOException {
        File f = new File(filename);
        InputStream is = new FileInputStream(f);
        InputStreamReader rd = new InputStreamReader(is);
        char[] buf = new char[(int) f.length()];
        int len = rd.read(buf, 0, buf.length);
        is.close();

        char[] res = new char[len];
        System.arraycopy(buf, 0, res, 0, len);

        return res;
    }

    public static char[] getBinaryChars(String filename) throws IOException {
        File f = new File(filename);
        InputStream is = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(is);
        int len = (int) f.length() / 2;
        char[] result = new char[len];
        for (int i = 0; i < len; i++) {
            int ch1 = bis.read();
            int ch2 = bis.read();
            result[i] = (char) ((ch1 << 8) + (ch2 << 0));
        }
        bis.close();
        is.close();
        return result;
    }

    public static char[] getBinaryChars(File f) throws IOException {
        InputStream is = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(is);
        int len = (int) f.length() / 2;
        char[] result = new char[len];
        for (int i = 0; i < len; i++) {
            int ch1 = bis.read();
            int ch2 = bis.read();
            result[i] = (char) ((ch1 << 8) + (ch2 << 0));
        }
        bis.close();
        is.close();
        return result;
    }

    public static void putBinaryChars(String filename, char[] array) throws IOException {
        OutputStream out = new FileOutputStream(filename);
        BufferedOutputStream buf = new BufferedOutputStream(out);

        for (int i = 0; i < array.length; i++) {
            char v = array[i];
            buf.write((v >>> 8) & 0xFF);
            buf.write((v >>> 0) & 0xFF);
        }
        buf.close();
        out.close();
    }
}
