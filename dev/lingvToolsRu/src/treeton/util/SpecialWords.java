/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class SpecialWords {
    public static final String charsetPropertyName = "CHARSET";
    public static final String ATTRIB_PARTS = "_PRTS";
    public static String defaultEncoding = "UTF-8";
    public static String lineTerminator = "\r\n";
    public static String prefixComment = "#";
    public static char compoundDelimeterChar = '+';
    public static String compoundDelimeter = new String(new char[]{compoundDelimeterChar});
    public static String compoundPrefix = "+";
    public static String compoundPartsBegin = "{";
    public static String compoundPartsEnd = "}";
    protected static Properties prsAttrs = null;
    static Pattern compoundPattern = Pattern.compile("\\" + compoundDelimeter);
    HashMap spwords;   // String -> ArrayList(SpecItem*)
    HashMap spwordsCS; // String -> ArrayList(SpecItem*)

    public SpecialWords(String filePath)
            throws IOException {
        buildAttributesTable();
        readDictionary(filePath);
    }

    public static String getAttribsString(String s) {
        String rslt = s;
        if (s.indexOf('=') <= 0) {
            StringTokenizer st = new StringTokenizer(s, " (),.;[]");
            if (st.hasMoreTokens()) {
                StringBuffer sb = new StringBuffer();
                sb.append(Ts.AN_BASE);
                sb.append(SpWordsTrData.eq);
                sb.append(st.nextToken());
                while (st.hasMoreTokens()) {
                    String full = prsAttrs.getProperty(st.nextToken());
                    if (full != null) {
                        sb.append(',');
                        sb.append(full);
                    }
                }
                rslt = sb.toString();
            }
        }
        return rslt;
    }

    public static int[] getStringPartsLens(String s, String delim) {
//    StringTokenizer st = new StringTokenizer(s, delim, true);
        ArrayList parts = new ArrayList();
        int i = 0;
        int lastPartPos = 0;
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (delim.indexOf(c) >= 0) {
                int len = i - lastPartPos;
                parts.add(new Integer(len));
                lastPartPos = i + 1;
            }
        }
        if (i >= lastPartPos) {
            int len = i - lastPartPos;
            parts.add(new Integer(len));
        }

        if (parts.size() == 1 && ((Integer) parts.get(0)).intValue() == 0) {
            // Если строка пустая (по алгоритму состоит из одной
            // части нулевой длины), то в результате вернём
            // пустой массив. То есть, будем считать что такая
            // строка не содержит ни одной части.
            parts.clear();
        }
        int[] rslt = new int[parts.size()];
        i = 0;
        Iterator itr = parts.iterator();
        while (itr.hasNext()) {
            rslt[i++] = ((Integer) itr.next()).intValue();
        }
        return rslt;
    }

    public static String[] getStringParts(String s, String delim) {
        ArrayList parts = new ArrayList();
        int i = 0;
        int lastPartPos = 0;
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (delim.indexOf(c) >= 0) {
                int len = i - lastPartPos;
                String p = i > lastPartPos ?
                        s.substring(lastPartPos, i) :
                        "";
                parts.add(p);
                lastPartPos = i + 1;
            }
        }
        if (i >= lastPartPos) {
            int len = i - lastPartPos;
            String p = i > lastPartPos ?
                    s.substring(lastPartPos, i) :
                    "";
            parts.add(p);
        }

        if (parts.size() == 1 && ((String) parts.get(0)).length() == 0) {
            // Если строка пустая (по алгоритму состоит из одной
            // части нулевой длины), то в результате вернём
            // пустой массив. То есть, будем считать что такая
            // строка не содержит ни одной части.
            parts.clear();
        }
        return (String[]) parts.toArray(new String[parts.size()]);
    }

    public static void main(String[] args) {
        String s = "1+compound+o+++fertsu++";
        getStringPartsLens(s, "+");
        s = "+compound+o+++fertsu++";
        getStringPartsLens(s, "+");
        s = "compoundofertsu";
        getStringPartsLens(s, "+");
        s = "compou+ndofertsu";
        getStringPartsLens(s, "+");
        s = "";
        getStringPartsLens(s, "+");
        s = "h";
        getStringPartsLens(s, "+");
        s = "+";
        getStringPartsLens(s, "+");
    /*
    //String s = "compoundofertsu+";
    StringTokenizer st = new StringTokenizer(s, "=+", true);
    int i = 0;
    while (st.hasMoreTokens()) {
      String t = st.nextToken();
      System.out.println("" + (i++) + ":" + t);
    }
    Matcher matcher = compoundPattern.matcher(s);
    System.out.println(matcher.find() ? "yes" : "no");
    String s2 = matcher.replaceAll("");
    System.out.println("[" + s2 + "]");
    */
    }

    /**
     * Добавляет словарную статью в хэш-таблицу.
     *
     * @param form
     * @param sHypo
     */
    protected void addHypo(String form, String sHypo) {
        HashMap hm = spwords;
        String form2 = form;
        if (form.startsWith("\\")) {
            hm = spwordsCS;
            form2 = form2.substring(1);
        } else {
            form2 = form2.toLowerCase();
        }

        SpecItem si = new SpecItem(form2, sHypo);
        form2 = form2.replaceAll("\\+", "");

        Object o = hm.get(form2);
        ArrayList a = null;
        if (o == null) {
            a = new ArrayList();
            hm.put(form2, a);
        } else {
            a = (ArrayList) o;
        }
        a.add(si);
    }

    /**
     * Возвращает список гипотез в виде ArrayList, каждый
     * элемент которого - Properties. Никогда не возвращает
     * null. Если гипотез нет - возвращает пустой ArrayList.
     *
     * @return ArrayList
     */
    public ArrayList getHypos(String form) {
        ArrayList rslt = new ArrayList();
        Object o = spwords.get(form.toLowerCase());
        if (o != null) {
            ArrayList a = (ArrayList) o;
            Iterator itr = a.iterator();
            while (itr.hasNext()) {
                SpecItem si = (SpecItem) itr.next();
                Properties p = new Properties();
                RusAttributes.setPropertiesFromString(si.val, p);
                if (si.isCompound) {
                    p.setProperty("COMPOUND", "compound");
                    p.put("_PRTS", si);
                }
                if (!p.isEmpty()) {
                    rslt.add(p);
                }
            }
        }
        Object oCS = spwordsCS.get(form);
        if (oCS != null) {
            ArrayList a = (ArrayList) oCS;
            Iterator itr = a.iterator();
            while (itr.hasNext()) {
                SpecItem si = (SpecItem) itr.next();
                Properties p = new Properties();
                RusAttributes.setPropertiesFromString(si.val, p);
                if (si.isCompound) {
                    p.setProperty("COMPOUND", "compound");
                    p.put("_PRTS", si);
                }
                if (!p.isEmpty()) {
                    rslt.add(p);
                }
                if (!p.isEmpty()) {
                    rslt.add(p);
                }
            }
        }
        return rslt;
    }

    /**
     * Открывает файл со словарём и формирует хэш-таблицу.
     *
     * @param filePath
     * @throws IOException
     */
    protected void readDictionary(String filePath)
            throws IOException {
        if (spwords == null)
            spwords = new HashMap();
        else
            spwords.clear();

        if (spwordsCS == null)
            spwordsCS = new HashMap();
        else
            spwordsCS.clear();

        ByteLinesReader br = null;
        System.out.println("specFile: " + filePath);
        try {
            InputStream ist = new FileInputStream(filePath);
            InputStreamReader isr = new InputStreamReader(ist, defaultEncoding);

            int lines = 0;
            int added = 0;
            int comments = 0;

            BufferedReader brd = new BufferedReader(isr);
            String line;
            while ((line = brd.readLine()) != null) {

                lines++;

                if (!line.startsWith(prefixComment)) {
                    line = line.trim();
                    int pcom = line.indexOf(prefixComment);
                    if (pcom > 0) {
                        line = line.substring(0, pcom).trim();
                    }
                    if (line.length() > 0) {
                        int entryLen = 0;
                        int dataPos = -1;
                        int i = 0;
                        boolean insideEntry = true;
                        while (i < line.length()) {
                            char c = line.charAt(i);
                            if (insideEntry) {
                                if (c == ' ' || c == '\t') {
                                    insideEntry = false;
                                    entryLen = i;
                                }
                                i++;
                            } else {
                                if (" ->:=".indexOf(c) >= 0) {
                                    i++;
                                } else {
                                    dataPos = i;
                                    break;
                                }
                            }
                        }

                        if (entryLen > 0 && dataPos > 0) {
                            String entry = line.substring(0, entryLen);
                            String attrs = line.substring(dataPos);
                            if (entry.length() > 0 && attrs.length() > 0) {
                                addHypo(entry, attrs);
                                added++;
                            }
                        }
                    }
                } else {
                    comments++;
                }
            }
            System.out.println(
                    new StringBuffer("Lines: ").append(lines).
                            append("  Added: ").append(added).
                            append("  Comments: ").append(comments));
            brd.close();
            isr.close();
            ist.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildAttributesTable() {
        if (prsAttrs == null)
            prsAttrs = new Properties();
        else
            prsAttrs.clear();
        StringTokenizer st = new StringTokenizer(
                SpWordsTrData.trTable, SpWordsTrData.next);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            int i = s.indexOf(SpWordsTrData.to);
            if (i > 0) {
                prsAttrs.setProperty(s.substring(0, i), s.substring(i + 1));
            } else
                prsAttrs.setProperty(s, "");
        }
    }
}
