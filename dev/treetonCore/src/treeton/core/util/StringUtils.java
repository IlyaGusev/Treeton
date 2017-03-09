/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.TString;
import treeton.core.TreetonFactory;
import treeton.core.scape.ParseException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// class with methods for string and bytes processing

public class StringUtils {
    public static final char CNULL = '\u0000';
    public final static String LANG_UNDEFINED = "undefined";
    protected static final char TEMPLATE_U = 'u';
    protected static final char TEMPLATE_L = 'l';
    protected static final char TEMPLATE_A = '*';
    private static final MutableInteger mi = new MutableInteger();
    private static final String templateException =
            "Word does not correspond template ";
    private static final StringBuffer tb = new StringBuffer();
    private static final int ORTH_UNDEF = 0;
    private static final int ORTH_UPPER = 1;
    private static final int ORTH_UPPERIFORCE = 2;
    private static final int ORTH_LOWER = 3;
    private static final int ORTH_ALLCAPS = 4;
    private static final int ORTH_MIXEDCAPS = 5;
    private static final String[] ortharr = new String[]{"undef", "upperInitial", "upperInitial", "lowercase", "allCaps", "mixedCaps"};
    private static final String[] kindarr = new String[]{"word", "complex"};
    private static final HashMap<String, Matcher> builtAlready = new HashMap<String, Matcher>();
    public static char hexDigits[] = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static int HEXBASE = 16;
    public static byte CR_CODE = 0x0D;
    public static byte LF_CODE = 0x0A;
    public static String NOTHING = "";
    public static String TWOSINGLEQUOTES = "''";
    public static char BACKSLASH_SYMB = '\\';
    public static char LF_SYMB = 'n';
    public static char CR_SYMB = 'r';
    public static int BACKSLASH_CODE = 0x5C;
    public static char SPACE = ' ';
    public static byte SPACE_CODE = 0x20;
    public static char FIG_BRACKET_OPENED = '{';
    public static char FIG_BRACKET_CLOSED = '}';
    public static String TOKEN_DELIMITERS = " \t\n\r\f";
    public static int STRINGBUFFER_SIZE = 50;
    public static HashMap<Character, String> translitTableLat = new HashMap<Character, String>();
    public static HashMap<Character, String> translitTable = new HashMap<Character, String>();
    static String[] langArr = new String[]{LANG_UNDEFINED, "lat", "cyr", "mix"};
    private static char[][] booleans = new char[][]{new char[]{'t', 'r', 'u', 'e'}, new char[]{'f', 'a', 'l', 's', 'e'}};
    private static String vowelLetters = "аеёиоуыэюя";
    private static char primaryAccent = '\u0301';
    private static char secondaryAccent = '\u0300';
    private static char[] primaryCodes = {
            '\u0492', '\u0494', '\u0496', '\u0498', '\u049A',
            '\u049C', '\u049E', '\u04A0', '\u04A2', '\u04A4'
    };
    private static char[] secondaryCodes = {
            '\u0493', '\u0495', '\u0497', '\u0499', '\u049B',
            '\u049D', '\u049F', '\u04A1', '\u04A3', '\u04A5'
    };

    static {
        translitTableLat.put('a', "а");
        translitTableLat.put('b', "б");
        translitTableLat.put('c', "ц");
        translitTableLat.put('d', "д");
        translitTableLat.put('e', "е");
        translitTableLat.put('f', "ф");
        translitTableLat.put('g', "г");
        translitTableLat.put('h', "х");
        translitTableLat.put('i', "и");
        translitTableLat.put('j', "дж");
        translitTableLat.put('k', "к");
        translitTableLat.put('l', "л");
        translitTableLat.put('m', "м");
        translitTableLat.put('n', "н");
        translitTableLat.put('o', "о");
        translitTableLat.put('p', "п");
        translitTableLat.put('q', "к");
        translitTableLat.put('r', "р");
        translitTableLat.put('s', "с");
        translitTableLat.put('t', "т");
        translitTableLat.put('u', "у");
        translitTableLat.put('v', "в");
        translitTableLat.put('w', "в");
        translitTableLat.put('x', "кс");
        translitTableLat.put('y', "й");
        translitTableLat.put('z', "з");
        translitTableLat.put('A', "А");
        translitTableLat.put('B', "Б");
        translitTableLat.put('C', "Ц");
        translitTableLat.put('D', "Д");
        translitTableLat.put('E', "Е");
        translitTableLat.put('F', "Ф");
        translitTableLat.put('G', "Г");
        translitTableLat.put('H', "Х");
        translitTableLat.put('I', "И");
        translitTableLat.put('J', "Дж");
        translitTableLat.put('K', "К");
        translitTableLat.put('L', "Л");
        translitTableLat.put('M', "М");
        translitTableLat.put('N', "Н");
        translitTableLat.put('O', "О");
        translitTableLat.put('P', "П");
        translitTableLat.put('Q', "К");
        translitTableLat.put('R', "Р");
        translitTableLat.put('S', "С");
        translitTableLat.put('T', "Т");
        translitTableLat.put('U', "У");
        translitTableLat.put('V', "В");
        translitTableLat.put('W', "В");
        translitTableLat.put('X', "Кс");
        translitTableLat.put('Y', "Й");
        translitTableLat.put('Z', "З");
    }

    static {
        translitTable.put('А', "A");
        translitTable.put('Б', "B");
        translitTable.put('В', "V");
        translitTable.put('Г', "G");
        translitTable.put('Д', "D");
        translitTable.put('Е', "E");
        translitTable.put('Ё', "E");
        translitTable.put('Ж', "Zh");
        translitTable.put('З', "Z");
        translitTable.put('И', "I");
        translitTable.put('Й', "I");
        translitTable.put('К', "K");
        translitTable.put('Л', "L");
        translitTable.put('М', "M");
        translitTable.put('Н', "N");
        translitTable.put('О', "O");
        translitTable.put('П', "P");
        translitTable.put('Р', "R");
        translitTable.put('С', "S");
        translitTable.put('Т', "Ts");
        translitTable.put('У', "U");
        translitTable.put('Ф', "F");
        translitTable.put('Х', "H");
        translitTable.put('Ц', "C");
        translitTable.put('Ч', "Ch");
        translitTable.put('Ш', "Sh");
        translitTable.put('Щ', "Sh");
        translitTable.put('Ъ', "'");
        translitTable.put('Ы', "Y");
        translitTable.put('Ь', "'");
        translitTable.put('Э', "E");
        translitTable.put('Ю', "Yu");
        translitTable.put('Я', "Ya");
        translitTable.put('а', "a");
        translitTable.put('б', "b");
        translitTable.put('в', "v");
        translitTable.put('г', "g");
        translitTable.put('д', "d");
        translitTable.put('е', "e");
        translitTable.put('ё', "e");
        translitTable.put('ж', "zh");
        translitTable.put('з', "z");
        translitTable.put('и', "i");
        translitTable.put('й', "i");
        translitTable.put('к', "k");
        translitTable.put('л', "l");
        translitTable.put('м', "m");
        translitTable.put('н', "n");
        translitTable.put('о', "o");
        translitTable.put('п', "p");
        translitTable.put('р', "r");
        translitTable.put('с', "s");
        translitTable.put('т', "t");
        translitTable.put('у', "u");
        translitTable.put('ф', "f");
        translitTable.put('х', "h");
        translitTable.put('ц', "ts");
        translitTable.put('ч', "ch");
        translitTable.put('ш', "sh");
        translitTable.put('щ', "sh");
        translitTable.put('ъ', "'");
        translitTable.put('ы', "y");
        translitTable.put('ь', "'");
        translitTable.put('э', "e");
        translitTable.put('ю', "yu");
        translitTable.put('я', "ya");
    }

    // make all single quotes repeated twice (for text in sql queries)
    public static String makeQuoteTwice(String _s) {
        StringBuffer rslt = null;
        String delim = NOTHING;
        if (_s != null) {
            rslt = new StringBuffer(_s.length());
            StringTokenizer st = new StringTokenizer(_s, "'");
            while (st.hasMoreTokens()) {
                rslt.append(delim).append(st.nextToken());
                delim = TWOSINGLEQUOTES;
            }
        }
        return (rslt != null) ? rslt.toString() : null;
    }

    // returns slashed string with byte value in "c" format
    // example: \\ - for one backslash, \x6A, \x07 - for another bytes
    public static String byteToSlashed(byte _b) {
        StringBuffer rslt = new StringBuffer(6);
        rslt.append(BACKSLASH_SYMB);
        if (_b == BACKSLASH_CODE)
            rslt.append(BACKSLASH_SYMB);
        else {
            int val = (int) _b & 0x00FF;
            rslt.append('x').append(hexDigits[val >> 4]).append(hexDigits[val & 0x0F]);
        }
        return rslt.toString();
    }

    // see next method [slashedTobyteEx(String)]
    public static int[] slashedTobyte(String _s, int _maxCodeLen) {
        return slashedTobyte(_s, 0, _maxCodeLen);
    }

    public static int[] slashedTobyte(String _s, int _pos, int _maxCodeLen) {
        int[] rslt = null;
        try {
            rslt = slashedTobyteEx(_s, _pos, _maxCodeLen);
        } catch (NumberFormatException x) {
            // do nothing - just return null
        } catch (NullPointerException x) {
            // do nothing - just return null
        }
        return rslt;
    }

    // returns byte value (-128..127), corresponding slashed string in "c" format
    // formats supported: \\ and \xHH or \xH
    // return: array of two int ([0] - value, [1] - slashed code length)
    // _maxCodeLen - max number of digits
    public static int[] slashedTobyteEx(String _s, int _pos, int _maxCodeLen)
            throws NullPointerException, NumberFormatException {
        int[] rslt = null;
        if (_s == null)
            throw new NullPointerException();
        int n = _s.length() - _pos;
        if (n < 2 || _s.charAt(_pos) != BACKSLASH_SYMB)
            throw new NumberFormatException();
        if (_s.charAt(_pos + 1) == BACKSLASH_SYMB) {
            rslt = new int[2];
            rslt[0] = BACKSLASH_CODE;
            rslt[1] = 2;
        } else if (_s.charAt(_pos + 1) == CR_SYMB) {
            rslt = new int[2];
            rslt[0] = CR_CODE;
            rslt[1] = 2;
        } else if (_s.charAt(_pos + 1) == LF_SYMB) {
            rslt = new int[2];
            rslt[0] = LF_CODE;
            rslt[1] = 2;
        } else {
            if (Character.toUpperCase(_s.charAt(_pos + 1)) == 'X') {
                int maxPos;
                if (_maxCodeLen > 0)
                    maxPos = Math.min(_s.length() - 1, _pos + _maxCodeLen + 1);
                else
                    maxPos = _s.length() - 1;
                int val = findCharForward(hexDigits, _s.charAt(_pos + 2));
                if (val < 0)
                    throw new NumberFormatException();
                int i = _pos + 3;
                for (i = _pos + 3; i <= maxPos; i++) {
                    int nextDigit = findCharForward(hexDigits, _s.charAt(i));
                    if (nextDigit >= 0)
                        val = val * 16 + nextDigit;
                    else
                        break;
                }
                rslt = new int[2];
                rslt[0] = val;
                rslt[1] = i - _pos;
            } else {
                throw new NumberFormatException();
            }
        }
        return rslt;
    }

    public static int findByteForward(byte[] _where, byte _what) {
        return findByteForward(_where, 0, _what);
    }

    public static int findByteForward(byte[] _where, int _from, byte _what) {
        if (_where != null && _from < _where.length) {
            for (int i = _from; i < _where.length; i++)
                if (_where[i] == _what) return i;
        }
        return -1;
    }

    public static int findCharForward(char[] _where, char _what) {
        return findCharForward(_where, 0, _what);
    }

    public static int findCharForward(char[] _where, int _from, char _what) {
        if (_where != null && _from < _where.length) {
            for (int i = _from; i < _where.length; i++)
                if (_where[i] == _what) return i;
        }
        return -1;
    }

    public static void moveBytes(byte[] _what, int _start, int _length,
                                 int _to) throws IndexOutOfBoundsException, NullPointerException {
        if (_what == null) throw new NullPointerException();
        if (_start < _what.length && _to < _what.length &&
                _start >= 0 && _to >= 0) {
            if (_start < _to) // move forward (from last element to first)
            {
                if ((_to + _length) > _what.length)
                    _length = _what.length - _to;
                int i = _start + _length - 1;
                int j = _to + _length - 1;
                while (i >= _start)
                    _what[j--] = _what[i--];
            } else if (_start > _to) // move backward (from first element to last)
            {
                if ((_start + _length) > _what.length)
                    _length = _what.length - _start;
                int i = _start;
                int j = _to;
                int finish = _start + _length - 1;
                while (i <= finish)
                    _what[j++] = _what[i++];
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public static String byteArraytoString(byte[] _b) {
        String rslt = "null";
        if (_b != null) {
            rslt = "[ ";
            int n = _b.length - 1;
            for (int i = 0; i < n; i++) {
                rslt += "" + (int) _b[i] + ", ";
            }
            rslt += "" + (int) _b[n];
            rslt += " ]";
        }
        return rslt;
    }

    public static String intArraytoString(int[] _b) {
        String rslt = "null";
        if (_b != null) {
            rslt = "[ ";
            int n = _b.length - 1;
            for (int i = 0; i < n; i++) {
                rslt += "" + _b[i] + ", ";
            }
            rslt += "" + _b[n];
            rslt += " ]";
        }
        return rslt;
    }

    public static Vector stringToVector(String _s, String _delim)
            throws NullPointerException {
        return (Vector) stringToAbstractList(new Vector<String>(), _s, _delim);
    }

    public static Vector stringToVector(String _s) throws NullPointerException {
        return stringToVector(_s, TOKEN_DELIMITERS);
    }

    public static ArrayList stringToArrayList(String _s, String _delim)
            throws NullPointerException {
        return (ArrayList) stringToAbstractList(new ArrayList<String>(), _s, _delim);
    }

    public static ArrayList stringToArrayList(String _s)
            throws NullPointerException {
        return stringToArrayList(_s, TOKEN_DELIMITERS);
    }

    public static AbstractList<String> stringToAbstractList(AbstractList<String> _arr,
                                                            String _s, String _delim) throws NullPointerException {
        StringTokenizer st = new StringTokenizer(_s, _delim);
        while (st.hasMoreTokens()) {
            _arr.add(st.nextToken());
        }
        return _arr;
    }

    public static String listToString(AbstractList _v, String _delim) {
        StringBuffer rslt = new StringBuffer(STRINGBUFFER_SIZE);
        boolean isFirst = true;
        if (_v != null) {
            for (Iterator e = _v.iterator(); e.hasNext(); ) {
                if (isFirst)
                    isFirst = false;
                else
                    rslt.append(_delim);
                rslt.append(e.next());
            }
        }
        return rslt.toString();
    }

    /**
     * Только для совместимости со старыми версиями.
     * Полностью идентичен методу listToString.
     *
     * @param _v
     * @param _delim
     * @return
     * @see #listToString
     * @deprecated Лучше использовать метод listToString.
     */
    public static String vectorToString(AbstractList _v, String _delim) {
        return listToString(_v, _delim);
    }

    public static boolean isLetterBasicLatin(char _c) {
        return (
                Character.UnicodeBlock.of(_c) == Character.UnicodeBlock.BASIC_LATIN &&
                        Character.isLetter(_c));
    }

    public static boolean isLetterCyrillic(char _c) {
        return (
                Character.UnicodeBlock.of(_c) == Character.UnicodeBlock.CYRILLIC &&
                        Character.isLetter(_c));
    }

    public static boolean isLatinLetterOrDigit(char _c) {
        return (isLetterBasicLatin(_c) || Character.isDigit(_c));
    }

    public static String toFirstUpper(String _s) {
        StringBuffer rslt = new StringBuffer();
        if (_s.length() > 0) {
            rslt.append(_s.substring(0, 1).toUpperCase()).
                    append(_s.substring(1).toLowerCase());
        }
        return rslt.toString();
    }

    public static Ptr findFreeOr(Ptr _p) {
        Ptr rslt = null;
        int p = findFreeOr(_p.toString(), 0);
        if (p >= 0)
            rslt = _p.add(p);
        return rslt;
    }

    public static int findFreeOr(String _pattern, int _pos) {
        int bl = 0;
        boolean inside = false;
        int i = _pos;
        while (i < _pattern.length()) {
            char c = _pattern.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '\'') {
                inside = !inside;
            } else if (!inside) {
                if (c == '[' || c == '(' || c == '{') {
                    bl++;
                } else if (c == ']' || c == ')' || c == '}') {
                    bl--;
                } else if (bl == 0 && c == '|') {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    // возвращает позицию закрывающей круглой скобки ')'
    // или -1 если она не найдена
    public static Ptr findMatchingRoundBracket(Ptr _p) {
        Ptr rslt = null;
        int p = findMatchingRoundBracket(_p.toString(), 0);
        if (p >= 0)
            rslt = _p.add(p);
        return rslt;
    }

    public static int findMatchingRoundBracket(String _pattern) {
        return findMatchingRoundBracket(_pattern, 0);
    }

    public static int findMatchingRoundBracket(String _pattern, int _pos) {
        return findMatchingBracket(_pattern, _pos, '(', ')');
    }

    // возвращает позицию закрывающей квадратной скобки ']'
    // или -1 если она не найдена
    public static Ptr findMatchingSquareBracket(Ptr _p) {
        Ptr rslt = null;
        int p = findMatchingSquareBracket(_p.toString(), 0);
        if (p >= 0)
            rslt = _p.add(p);
        return rslt;
    }

    public static int findMatchingSquareBracket(String _pattern) {
        return findMatchingSquareBracket(_pattern, 0);
    }

    public static int findMatchingSquareBracket(String _pattern, int _pos) {
        return findMatchingBracket(_pattern, _pos, '[', ']');
    }

    // возвращает позицию закрывающей фигурной скобки '}'
    // или -1 если она не найдена
    public static Ptr findMatchingFigureBracket(Ptr _p) {
        Ptr rslt = null;
        int p = findMatchingFigureBracket(_p.toString(), 0);
        if (p >= 0)
            rslt = _p.add(p);
        return rslt;
    }

    public static int findMatchingFigureBracket(String _pattern) {
        return findMatchingFigureBracket(_pattern, 0);
    }

    public static int findMatchingFigureBracket(String _pattern, int _pos) {
        return findMatchingBracket(_pattern, _pos, '{', '}');
    }

    public static int findMatchingBracket(String _pattern, int _pos, char _obr, char _cbr) {
        int bl = 0;
        boolean inside = false;
        int i = _pos;
        while (i < _pattern.length()) {
            char c = _pattern.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '\'') {
                inside = !inside;
            } else if (!inside) {
                if (c == _obr) {
                    bl++;
                } else if (bl == 1 && c == _cbr) {
                    return i;
                } else if (c == _cbr) {
                    bl--;
                }
            }
            i++;
        }
        return -1;
    }

    public static boolean ends(String _word, Object[] _ends) {
        boolean rslt = false;
        if (_word != null && _ends != null) {
            for (int i = 0; i < _ends.length; i++) {
                try {
                    if (_word.endsWith((String) (_ends[i]))) {
                        rslt = true;
                        break;
                    }
                } catch (NullPointerException x) {
                    // do nothing - the method just return false
                }
            }
        }
        return rslt;
    }

    public static boolean ends(String _word, String _ends) {
        return ends(_word, stringToVector(_ends));
    }

    public static boolean ends(String _word, Vector _ends) {
        return ends(_word, _ends.toArray());
    }

    public static boolean ends(Ptr _word, Vector _ends) {
        return ends(_word.toString(), _ends.toArray());
    }

    public static boolean ends(Ptr _word, String _ends) {
        return ends(_word.toString(), stringToVector(_ends));
    }

    public static boolean ends(char[] _word, String _ends) {
        return ends(new Ptr(_word), stringToVector(_ends));
    }

    public static boolean ends(Ptr _word, Ptr _ends) {
        return ends(_word.toString(), _ends.toString());
    }

    // copies String into char[]
    public static void getChars(char[] _in, String _out) {
        int len = Math.min(_in.length, _out.length());
        if (len > 0)
            _out.getChars(0, len, _in, 0);
    }

    // copies String into char[]
    public static void getChars(char[] _in, StringBuffer _out) {
        int len = Math.min(_in.length, _out.length());
        if (len > 0)
            _out.getChars(0, len, _in, 0);
    }

    // copies String into char[] and fills array tail with '\u0000'
    public static void getCharsNull(char[] _in, String _out) {
        getChars(_in, _out);
        for (int i = _out.length(); i < _in.length; i++)
            _in[i] = CNULL;
    }

    // copies String into char[] and fills array tail with '\u0000'
    public static void getCharsNull(char[] _in, StringBuffer _out) {
        getChars(_in, _out);
        for (int i = _out.length(); i < _in.length; i++)
            _in[i] = CNULL;
    }

    /**
     * Возвращает файл, содержащий класс. Если класс находится просто на
     * диске, возвращается полный путь. Если класс находится в архивах
     * jar или zip, результат имеет следующий формат.
     * <p/>
     * {путь к архиву}{символ '!'}{путь к классу внутри архива}
     * <p/>
     * Путь к классу внутри архива разделяется символами '/'.
     * <p/>
     * <b>Примеры:</b>
     * <p/>
     * <i>Вызов:</i> getFileForClass("com.pkname.SomeClass");<br>
     * <i>Результат:</i> D:/Project/classes/com/pkname/SomeClass.class
     * <p/>
     * <i>Вызов:</i> getFileForClass("java.util.regex.Pattern");<br>
     * <i>Результат:</i> D:/jdk/141/jre/lib/rt.jar!/java/util/regex/Pattern.class
     * <p/>
     * <i>Вызов:</i> getFileForClass("pkjar.ClassInJar");<br>
     * <i>Результат:</i> D:/Project/lib/jarname.jar!/pkjar/ClassInJar.class
     */
    public static String getFileForClass(String className)
            throws FileNotFoundException {
        int i;
        StringBuffer buf = new StringBuffer(1024);
        if (!className.startsWith("/")) {
            buf.append('/');
        }
        buf.append(className);
        for (i = 0; i < buf.length(); i++) {
            if (buf.charAt(i) == '.') {
                buf.setCharAt(i, '/');
            }
        }
        buf.append(".class");

        java.net.URL classURL =
                StringUtils.class.getResource(buf.toString());

        if (classURL == null) {
            throw new FileNotFoundException();
        }

        String filePath = classURL.getFile();
        if (filePath.toLowerCase().startsWith("file:")) {
            filePath = filePath.substring(5);
        }
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        return filePath;
    }

    public static String getPathForClass(String className)
            throws FileNotFoundException {
        String classFile = getFileForClass(className);
        int len = classFile.length();

        int lastSlash = -1;
        if (len > 0) {

            // Если класс находится в jar или zip архиве, то после
            // имени архива и перед путём к классу внутри архива
            // ставится восклицательный знак.

            // last - завершение имени физического файла
            int last = classFile.indexOf('!');
            if (last != 0) {
                if (last < 0) {
                    last = len - 1;
                } else {
                    // last > 0
                    last--;
                }
                lastSlash = classFile.lastIndexOf('/', last - 1);
            }
            if (lastSlash < 0) {
                lastSlash = 0;
            }
        }
        if (lastSlash <= 0) {
            throw new FileNotFoundException();
        }
        return classFile.substring(0, lastSlash);
    }

    public static String getFolder(String _fileName) {
        return getFolder(_fileName, false);
    }

    public static String getFolder(String _fileName,
                                   boolean _needLastSlash) {
        int from = 0, to = 0;
        if (_fileName.length() > 0) {
            boolean hasDrive = (_fileName.indexOf(":") > 0);
            if (hasDrive) {
                if (_fileName.charAt(0) == '/') {
                    from = 1;
                }
            }
            int lastSlash = _fileName.lastIndexOf('/');
            if (lastSlash < 0) {
                to = hasDrive ? _fileName.length() : from;
            } else {
                to = lastSlash;
            }
        }
        return _fileName.substring(from, to) +
                (_needLastSlash ? "/" : "");
    }

    public static String extractLastElement(String source, char delim) {
        int from = source.lastIndexOf(delim);
        if (from < 0) {
            from = 0;
        } else {
            from++;
        }
        return source.substring(from);
    }

    public static String getPropertiesFileForClass(Class<? extends Object> c) {
        String className = c.getName();
        StringBuffer sb = new StringBuffer();
        try {
            sb.append(sut.getPathForClass(className));
        } catch (FileNotFoundException e) {
            sb.setLength(0);
            sb.append(System.getProperties().getProperty("System.user.home"));
        }
        sb.append('/').
                append(sut.extractLastElement(className, '.')).
                append(".properties");
        return sb.toString();
    }

    public static boolean equalsWithNull(String one, String two) {
        return (one == null) ? (two == null) : one.equals(two);
    }

    public static String getSafeString(String s) {
        return s != null ? s : new String();
    }

    public static int getSafeLength(String s) {
        return s != null ? s.length() : 0;
    }

    public static int parseIntSafe(String src, int defaultValue) {
        int rslt = defaultValue;
        if (src != null) {
            try {
                rslt = Integer.parseInt(src);
            } catch (NumberFormatException x) {
                rslt = defaultValue;
            }
        }
        return rslt;
    }

    public static String replaceSimple(Pattern ptrn, String source, String with) {
        String rslt = source;
        Matcher m = ptrn.matcher(source);
        if (m.find()) {
            rslt = m.replaceAll(with);
        }
        return rslt;
    }

    public static void checkEndOfStream(char s[], int pl, int endpl) throws ParseException {
        if (pl > endpl)
            throw new ParseException("unexpected end of input stream", null, s, pl, endpl);
    }

    public static int skipSpaces(char s[], int pl, int endpl) {
        while (pl <= endpl && (s[pl] == ' ' || s[pl] == '\t')) pl++;
        return pl;
    }

    public static int skipNumber(char s[], int pl, int endpl) {
        while (pl <= endpl && (s[pl] >= '0' && s[pl] <= '9')) pl++;
        return pl;
    }

    public static int skipDouble(char[] s, int pl, int endpl) {
        while (pl <= endpl && (s[pl] >= '0' && s[pl] <= '9') || s[pl] == '.' || s[pl] == '-' || s[pl] == '*') pl++;
        return pl;
    }

    public static int skipBoolean(char s[], int pl, int endpl) {
        int n;
        if ((n = checkDelims(s, pl, endpl, booleans)) != -1) {
            return pl + booleans[n].length;
        }
        return pl;
    }

    public static int skipVarValue(char s[], int pl, int endpl) {
        if (pl > endpl) return pl;
        if (s[pl] == '"') {
            pl++;
            while (pl <= endpl) {
                if (s[pl] == '\\') {
                    pl++;
                } else if (s[pl] == '"') {
                    return pl + 1;
                }

                pl++;
            }
            return pl;
        }
        while (pl <= endpl && (s[pl] >= 'a' && s[pl] <= 'z' || s[pl] >= 'A' && s[pl] <= 'Z' || s[pl] == '\'' || s[pl] >= '0' && s[pl] <= '9' || s[pl] == '_' || s[pl] == '*' || s[pl] == '#' || s[pl] == '@' ||
                (java.lang.Character.UnicodeBlock.CYRILLIC ==
                        java.lang.Character.UnicodeBlock.of(s[pl]))))
            pl++;
        return pl;
    }

    public static int skipVarValueWithDotAndBrackets(char s[], int pl, int endpl) {
        if (pl > endpl) return pl;
        if (s[pl] == '"') {
            pl++;
            while (pl <= endpl) {
                if (s[pl] == '"')
                    return pl + 1;
                pl++;
            }
            return pl;
        }
        while (pl <= endpl && (s[pl] >= 'a' && s[pl] <= 'z' || s[pl] >= 'A' && s[pl] <= 'Z' || s[pl] >= '0' && s[pl] <= '9' || s[pl] == '_' || s[pl] == '*' || s[pl] == '.' || s[pl] == '[' || s[pl] == ']' || s[pl] == '@' ||
                (java.lang.Character.UnicodeBlock.CYRILLIC ==
                        java.lang.Character.UnicodeBlock.of(s[pl]))))
            pl++;
        return pl;
    }

    public static int skipVarName(char s[], int pl, int endpl) {
        while (pl <= endpl && (s[pl] >= 'a' && s[pl] <= 'z' || s[pl] >= 'A' && s[pl] <= 'Z' || s[pl] == '_' || s[pl] == '*' || s[pl] >= '0' && s[pl] <= '9' || s[pl] == '@' || s[pl] == '$' ||
                (java.lang.Character.UnicodeBlock.CYRILLIC ==
                        java.lang.Character.UnicodeBlock.of(s[pl]))))
            pl++;
        return pl;
    }

    private static byte skipUTF8Char(RandomAccessFile file) throws IOException {
        byte b = file.readByte();
        if ((b & 0x80) == 0) {
            return b;
        } else {
            if ((b & 0x20) == 0) {
                file.readByte();
            } else if ((b & 0x10) == 0) {
                file.readByte();
                file.readByte();
            } else if ((b & 0x8) == 0) {
                file.readByte();
                file.readByte();
                file.readByte();
            }
            return (byte) 0xFF;
        }
    }

    public static int skipSpacesEndls(char s[], int pl, int endpl) {
        while (pl <= endpl && (s[pl] == ' ' || s[pl] == '\n' || s[pl] == '\r' || s[pl] == '\t')) pl++;
        if (pl >= endpl)
            return pl;
        if (s[pl] == '/') {
            int pl1;
            if (s[pl + 1] == '*') {
                for (pl1 = pl + 2; pl1 <= endpl; pl1++) {
                    if (s[pl1] == '*' && pl1 < endpl && s[pl1 + 1] == '/') {
                        return skipSpacesEndls(s, pl1 + 2, endpl);
                    }
                }
                return pl1;
            } else if (s[pl + 1] == '/') {
                for (pl1 = pl + 2; pl1 <= endpl; pl1++) {
                    if (s[pl1] == '\n') {
                        return skipSpacesEndls(s, pl1 + 1, endpl);
                    }
                }
                return pl1;
            }
        }
        return pl;
    }

    public static List<URL> retrieveURLsFromFolder(File f, String[] extensions) throws MalformedURLException {
        ArrayList<URL> res = new ArrayList<URL>();
        ExtensionFilter filter = new ExtensionFilter(extensions);
        if (!f.isDirectory()) {
            if (filter.accept(f)) {
                res.add(f.toURL());
            }
            return res;
        }
        retrieveURLsFromFolder(f, res, filter);
        return res;
    }

    private static void retrieveURLsFromFolder(File f, ArrayList<URL> res, ExtensionFilter filter) throws MalformedURLException {
        if (!f.isDirectory()) {
            res.add(f.toURL());
            return;
        }
        File[] arr = f.listFiles(filter);
        for (File inner : arr) {
            retrieveURLsFromFolder(inner, res, filter);
        }
    }

    public static void skipTillByte(RandomAccessFile file, byte[] delims) throws IOException {
        //delim can contain only characters from the 0x0 - 0x7F range

        FileChannel ch = file.getChannel();

        byte c = 0;
        while (ch.position() < ch.size()) {
            c = skipUTF8Char(file);
            if (c == 0xFF) {
                continue;
            }
            for (byte aDelim : delims) {
                if (c == aDelim) {
                    ch.position(ch.position() - 1);
                    return;
                }
            }

            if (c == '/') {
                if (ch.position() >= ch.size()) {
                    return;
                }

                c = file.readByte();

                if (c == '*') {
                    while (ch.position() < ch.size()) {
                        c = file.readByte();
                        if (c == '*') {
                            if (ch.position() >= ch.size()) {
                                return;
                            }
                            c = file.readByte();
                            if (c == '/') {
                                skipTillByte(file, delims);
                                return;
                            }
                        }
                    }
                    return;
                } else if (c == '/') {
                    while (ch.position() < ch.size()) {
                        c = file.readByte();
                        if (c == '\n') {
                            skipTillByte(file, delims);
                            return;
                        }
                    }
                    return;
                } else {
                    ch.position(ch.position() - 1);
                }
            }
        }
    }

    public static void skipSpacesEndls(RandomAccessFile file) throws IOException {
        FileChannel ch = file.getChannel();

        byte c = 0;
        while (ch.position() < ch.size()) {
            c = file.readByte();
            if (!(c == ' ' || c == '\n' || c == '\r' || c == '\t')) {
                break;
            }
        }

        if (ch.position() >= ch.size()) {
            return;
        }

        if (c == '/') {
            if (ch.position() >= ch.size()) {
                return;
            }

            c = file.readByte();

            if (c == '*') {
                while (ch.position() < ch.size()) {
                    c = file.readByte();
                    if (c == '*') {
                        if (ch.position() >= ch.size()) {
                            return;
                        }
                        c = file.readByte();
                        if (c == '/') {
                            skipSpacesEndls(file);
                            return;
                        }
                    }
                }
            } else if (c == '/') {
                while (ch.position() < ch.size()) {
                    c = file.readByte();
                    if (c == '\n') {
                        skipSpacesEndls(file);
                        return;
                    }
                }
            } else {
                ch.position(ch.position() - 2);
            }
        } else {
            ch.position(ch.position() - 1);
        }
    }

    public static String extractString(char s[], int from, int length) {
        //возвращает строку, извлеченную из заданного отрезка массива с учетом
        //того, что все вхождения вида '\с' заменяются на 'c'.
        StringBuffer buf = new StringBuffer();

        for (int i = from; i < from + length; i++) {
            if (s[i] == '\\') {
                buf.append(s[++i]);
            } else {
                buf.append(s[i]);
            }
        }

        return buf.toString();
    }

    public static TString extractTString(char s[], int from, int length) {
        //возвращает строку, извлеченную из заданного отрезка массива с учетом
        //того, что все вхождения вида '\с' заменяются на 'c'.
        StringBuffer buf = new StringBuffer();

        for (int i = from; i < from + length; i++) {
            if (s[i] == '\\') {
                buf.append(s[++i]);
            } else {
                buf.append(s[i]);
            }
        }

        return TreetonFactory.newTString(buf.toString());
    }

    public static int skipVarValueName(char s[], int pl, int endpl) {
        if (pl > endpl) return pl;
        if (s[pl] == '"') {
            pl++;
            while (pl <= endpl) {
                if (s[pl] == '"')
                    return pl + 1;
                pl++;
            }
            return pl;
        }
        while (pl <= endpl && (s[pl] >= 'a' && s[pl] <= 'z' || s[pl] >= 'A' && s[pl] <= 'Z' || s[pl] == '\'' || s[pl] >= '0' && s[pl] <= '9' || s[pl] == '.' || s[pl] == '#' || s[pl] == '@' || s[pl] == '$' ||
                (java.lang.Character.UnicodeBlock.CYRILLIC ==
                        java.lang.Character.UnicodeBlock.of(s[pl])) || s[pl] == '_' || s[pl] == '*' || s[pl] == '.'))
            pl++;
        return pl;
    }

    public static int readInString(char[] s, int pl, int endpl, char src[]) throws ParseException {
        int i, j;
        for (i = pl, j = 0; i <= endpl && j < src.length; i++, j++) {
            if (s[i] != src[j]) {
                throw new ParseException(
                        "wrong symbol " + (new String(s, i, 1)) + ", was expecting " + (new String(src, j, 1)), null,
                        s, pl, endpl);
            }
        }
        if (i > endpl && j < src.length)
            throw new ParseException("unexpected end of input stream, was expecting " + (new String(src, j, 1)), null,
                    s, pl, endpl);
        return i;
    }

    public static int skipBracesContent(char[] s, int pl, int endpl) throws ParseException {
        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }
        pl++;
        while (pl <= endpl && s[pl] != '}') {
            pl++;
        }
        if (pl > endpl) {
            throw new ParseException("unexpected end of input stream, was expecting '}'", null, s, pl, endpl);
        }
        return pl + 1;
    }

    public static int readInInteger(char[] s, int pl, int endpl, MutableInteger result) throws ParseException {
        synchronized (tb) {
            pl = skipSpacesEndls(s, pl, endpl);
            tb.setLength(0);
            while (pl < endpl) {
                char c = s[pl];
                if (Character.isDigit(c) || c == '-') {
                    tb.append(c);
                } else {
                    break;
                }
                pl++;
            }
            if (tb.length() == 0) {
                throw new ParseException(
                        "unable to read integer", null,
                        s, pl, endpl);
            }
            try {
                result.value = Integer.parseInt(tb.toString());
            } catch (NumberFormatException e) {
                throw new ParseException(
                        "unable to read integer: " + e.getMessage(), null,
                        s, pl, endpl);
            }
            return pl;
        }
    }

    public static int readInIntegerArray(char[] s, int pl, int endpl, ArrayList<Integer> result) throws ParseException {
        synchronized (mi) {
            result.clear();
            pl = skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] != '{') {
                throw new ParseException("Missing \'{\'", null, s, pl, endpl);
            }
            pl++;
            while (true) {
                pl = skipSpacesEndls(s, pl, endpl);
                if (s[pl] == '}') {
                    pl++;
                    break;
                } else {
                    pl = readInInteger(s, pl, endpl, mi);
                    result.add(new Integer(mi.value));
                    pl = skipSpacesEndls(s, pl, endpl);
                    sut.checkEndOfStream(s, pl, endpl);
                    if (s[pl] == ',') {
                        pl++;
                    } else if (s[pl] == '}') {
                        pl++;
                        break;
                    } else {
                        throw new ParseException("Missing \',\' or \'}\'", null, s, pl, endpl);
                    }
                }

            }
            return pl;
        }
    }

    public static int readInIntegerAssignment(char[] s, int pl, int endpl, char[] varName, MutableInteger result) throws ParseException {
        pl = skipSpacesEndls(s, pl, endpl);
        pl = readInString(s, pl, endpl, varName);
        pl = skipSpacesEndls(s, pl, endpl);
        checkEndOfStream(s, pl, endpl);
        if (s[pl] != '=') {
            throw new ParseException(
                    "wrong symbol " + s[pl] + ", was expecting =", null,
                    s, pl, endpl);
        }
        pl++;
        pl = readInInteger(s, pl, endpl, result);
        pl = skipSpacesEndls(s, pl, endpl);
        checkEndOfStream(s, pl, endpl);
        if (s[pl] != ';') {
            throw new ParseException("Missing \';\'", null, s, pl, endpl);
        }
        return pl;
    }

    public static int readInIntegerArrayAssignment(char[] s, int pl, int endpl, char[] varName, ArrayList<Integer> result) throws ParseException {
        pl = skipSpacesEndls(s, pl, endpl);
        pl = readInString(s, pl, endpl, varName);
        pl = skipSpacesEndls(s, pl, endpl);
        checkEndOfStream(s, pl, endpl);
        if (s[pl] != '=') {
            throw new ParseException(
                    "wrong symbol " + s[pl] + ", was expecting =", null,
                    s, pl, endpl);
        }
        pl++;
        pl = readInIntegerArray(s, pl, endpl, result);
        pl = skipSpacesEndls(s, pl, endpl);
        checkEndOfStream(s, pl, endpl);
        if (s[pl] != ';') {
            throw new ParseException("Missing \';\'", null, s, pl, endpl);
        }
        return pl;
    }

    public static int checkDelims(char[] s, int pl, int endpl, char delims[][]) {
        if (pl > endpl)
            return -1;
        int maxi = -1;
        int maxLen = 0;
        for (int i = 0; i < delims.length; i++) {
            if (s[pl] == delims[i][0]) {
                int pl1 = pl + 1;
                int j = 1;
                while (pl1 <= endpl && j < delims[i].length) {
                    if (delims[i][j] != s[pl1])
                        break;
                    j++;
                    pl1++;
                }
                if (j == delims[i].length) {
                    if (delims[i].length > maxLen) {
                        maxi = i;
                        maxLen = delims[i].length;
                    }
                }
            }
        }
        return maxi;
    }

    public static int checkDelims(RandomAccessFile file, byte delims[][]) throws IOException {
        FileChannel ch = file.getChannel();
        if (ch.position() >= ch.size())
            return -1;
        int maxi = -1;
        int maxLen = 0;
        long pos = ch.position();
        for (int i = 0; i < delims.length; i++) {
            byte c = file.readByte();
            if (c == delims[i][0]) {
                int j = 1;
                while (ch.position() < ch.size() && j < delims[i].length) {
                    if (delims[i][j] != file.readByte()) {
                        break;
                    }
                    j++;
                }
                ch.position(pos);
                if (j == delims[i].length) {
                    if (delims[i].length > maxLen) {
                        maxi = i;
                        maxLen = delims[i].length;
                    }
                }
            }
        }
        return maxi;
    }

    public static int skipSpaces(byte s[], int pl, int endpl) {
        while (pl <= endpl && (s[pl] == ' ' || s[pl] == '\t')) pl++;
        return pl;
    }

    public static int skipNumber(byte s[], int pl, int endpl) {
        while (pl <= endpl && (s[pl] >= '0' && s[pl] <= '9')) pl++;
        return pl;
    }

    public static int skipBoolean(byte s[], int pl, int endpl) {
        if (pl <= endpl && (s[pl] == 'T' || s[pl] == 'F'))
            pl++;
        return pl;
    }

    public static int skipVarValue(byte s[], int pl, int endpl) {
        if (pl > endpl) return pl;
        if (s[pl] == '"') {
            pl++;
            while (pl <= endpl) {
                if (s[pl] == '"')
                    return pl + 1;
                pl++;
            }
            return pl;
        }
        while (pl <= endpl && (s[pl] >= 'a' && s[pl] <= 'z' || s[pl] >= 'A' && s[pl] <= 'Z' || s[pl] == '\'' || s[pl] >= '0' && s[pl] <= '9' || s[pl] == '_' || s[pl] == '*' || s[pl] == '.' || s[pl] == '#' ||
                (s[pl] & 0xFF) >= 224 || (s[pl] & 0xFF) >= 192 && (s[pl] & 0xFF) <= 223))
            pl++;
        return pl;
    }

    public static int skipVarName(byte s[], int pl, int endpl) {
        while (pl <= endpl && (s[pl] >= 'a' && s[pl] <= 'z' || s[pl] >= 'A' && s[pl] <= 'Z' || s[pl] == '_' || s[pl] == '*' || s[pl] >= '0' && s[pl] <= '9' || s[pl] == '@'))
            pl++;
        return pl;
    }

    public static int skipSpacesEndls(byte s[], int pl, int endpl) {
        while (pl <= endpl && (s[pl] == ' ' || s[pl] == '\n' || s[pl] == '\r' || s[pl] == '\t')) pl++;
        if (pl >= endpl)
            return pl;
        if (s[pl] == '/') {
            int pl1;
            if (s[pl + 1] == '*') {
                for (pl1 = pl + 2; pl1 <= endpl; pl1++) {
                    if (s[pl1] == '*' && pl1 < endpl && s[pl1 + 1] == '/') {
                        return skipSpacesEndls(s, pl1 + 2, endpl);
                    }
                }
                return pl1;
            } else if (s[pl + 1] == '/') {
                for (pl1 = pl + 2; pl1 <= endpl; pl1++) {
                    if (s[pl1] == '\n') {
                        return skipSpacesEndls(s, pl1 + 1, endpl);
                    }
                }
                return pl1;
            }
        }
        return pl;
    }

    public static int skipVarValueName(byte s[], int pl, int endpl) {
        while (pl <= endpl && (s[pl] >= 'a' && s[pl] <= 'z' || s[pl] >= 'A' && s[pl] <= 'Z' || s[pl] == '\'' || s[pl] >= '0' && s[pl] <= '9' || s[pl] == '.' || s[pl] == '#' ||
                (s[pl] & 0xFF) >= 224 || (s[pl] & 0xFF) >= 192 && (s[pl] & 0xFF) <= 223 || s[pl] == '_' || s[pl] == '*' || s[pl] == '.'))
            pl++;
        return pl;
    }

    public static int readInString(byte[] s, int pl, int endpl, byte src[]) throws ParseException, UnsupportedEncodingException {
        int i, j;
        for (i = pl, j = 0; i <= endpl && j < src.length; i++, j++) {
            if (s[i] != src[j]) {
                throw new ParseException(
                        "wrong symbol " + (new String(s, i, 1, "Cp1251")) + ", was expecting " + (new String(src, j, 1, "Cp1251")), null,
                        s, pl, endpl);
            }
        }
        if (i > endpl && j < src.length)
            throw new ParseException("unexpected end of input stream, was expecting " + (new String(src, j, 1, "Cp1251")), null,
                    s, pl, endpl);
        return i;
    }

    public static String forSQL(Object o) {
        if (o == null || o == nu.ll) {
            return "NULL";
        }
        return "N'" + o.toString().replaceAll("'", "''") + "'";
    }

    public static void putIntegerInChars(char[] arr, int start, int value) {
        arr[start] = (char) (value >> 16);
        arr[start + 1] = (char) (value & 0x0000FFFF);
    }

    public static void append2ByteBuffer(ByteBuffer buf, int value, int dim) {
        if (dim == 4) {
            putIntegerInBytes(buf, value);
        } else if (dim == 2) {
            putShortInBytes(buf, (short) value);
        } else {
            buf.put((byte) value);
        }
    }

    public static void putInBytes(byte[] arr, int start, int value, int dim) {
        if (dim == 4) {
            putIntegerInBytes(arr, start, value);
        } else if (dim == 2) {
            putShortInBytes(arr, start, (short) value);
        } else {
            arr[start] = (byte) value;
        }
    }

    /*public static Iterator<String> unambiguousTemplateIterator(Iterator<Treenotation> it, int template_feature) {

     Treenotation trn = it.next();
     TString s = (TString) trn.get(template_feature);
     String res;
     if (s==null) {
       res = "*";
     } else {
       res = s.toString();
     }

     //вход u*,*,uu*,l-u
     //выход uu*,u[^u]*,l-u,[^u^l]*


     return null;
   } */

    public static int getFromBytes(byte[] arr, int start, int dim) {
        if (dim == 4) {
            return getIntegerFromBytes(arr, start);
        } else if (dim == 2) {
            return getShortFromBytes(arr, start);
        } else {
            return arr[start];
        }
    }

    public static void putIntegerInBytes(byte[] arr, int start, int value) {
        arr[start] = (byte) ((value >> 24) & 0x000000FF);
        arr[start + 1] = (byte) ((value >> 16) & 0x000000FF);
        arr[start + 2] = (byte) ((value >> 8) & 0x000000FF);
        arr[start + 3] = (byte) ((value >> 0) & 0x000000FF);
    }

    public static void putIntegerInBytes(ByteBuffer buf, int value) {
        buf.put((byte) ((value >> 24) & 0x000000FF));
        buf.put((byte) ((value >> 16) & 0x000000FF));
        buf.put((byte) ((value >> 8) & 0x000000FF));
        buf.put((byte) ((value >> 0) & 0x000000FF));
    }

    public static int getIntegerFromBytes(byte[] arr, int start) {
        int result = 0;
        result |= (((int) arr[start]) & 0x000000FF) << 24;
        result |= (((int) arr[start + 1]) & 0x000000FF) << 16;
        result |= (((int) arr[start + 2]) & 0x000000FF) << 8;
        result |= (((int) arr[start + 3]) & 0x000000FF) << 0;
        return result;
    }

    public static void putShortInBytes(byte[] arr, int start, short value) {
        arr[start] = (byte) ((value >> 8) & 0x00FF);
        arr[start + 1] = (byte) ((value >> 0) & 0x00FF);
    }

    public static void putShortInBytes(ByteBuffer buf, short value) {
        buf.put((byte) ((value >> 8) & 0x00FF));
        buf.put((byte) ((value >> 0) & 0x00FF));
    }

    public static short getShortFromBytes(byte[] arr, int start) {
        short result = 0;
        result |= (((short) arr[start]) & 0x00FF) << 8;
        result |= (((short) arr[start + 1]) & 0x00FF) << 0;
        return result;
    }

    public static void putCharInBytes(byte[] arr, int start, char value) {
        arr[start] = (byte) ((value >> 8) & 0x00FF);
        arr[start + 1] = (byte) ((value >> 0) & 0x00FF);
    }

    public static char getCharFromBytes(byte[] arr, int start) {
        char result = 0;
        result |= (((char) arr[start]) & 0x00FF) << 8;
        result |= (((char) arr[start + 1]) & 0x00FF) << 0;
        return result;
    }

    public static char[] integerToChars(int value) {
        char[] arr = new char[2];
        arr[0] = (char) (value >> 16);
        arr[1] = (char) (value & 0x0000FFFF);
        return arr;
    }

    public static char[] longToChars(long value) {
        char[] arr = new char[4];
        arr[0] = (char) ((value >> 48) & 0x000000000000FFFF);
        arr[1] = (char) ((value >> 32) & 0x000000000000FFFF);
        arr[2] = (char) ((value >> 16) & 0x000000000000FFFF);
        arr[3] = (char) ((value >> 0) & 0x000000000000FFFF);
        return arr;
    }

    public static long getLongFromChars(char[] arr, int start) {
        long result = 0L;
        result |= ((long) arr[start]) << 48;
        result |= ((long) arr[start + 1]) << 32;
        result |= ((long) arr[start + 2]) << 16;
        result |= ((long) arr[start + 3]) << 0;
        return result;
    }

    public static int getIntegerFromChars(char[] arr, int start) {
        int result = arr[start + 1];
        result |= arr[start] << 16;
        return result;
    }

    public static void putString(ByteBuffer buf, String str) {
        buf.putInt(str != null ? str.length() : -1);
        if (str != null) {
            for (int i = 0; i < str.length(); i++) {
                buf.putChar(str.charAt(i));
            }
        }
    }

    public static String readInStringFromBytes(byte[] arr, int from) {
        int length = getIntegerFromBytes(arr, from);
        if (length == -1) {
            return null;
        }
        from += 4;
        char[] data = new char[length];
        for (int i = 0; i < length; i++) {
            data[i] = getCharFromBytes(arr, from);
            from += 2;
        }
        return new String(data);
    }

    public static String addFileToPath(String path, String filename) {
        if (path.charAt(path.length() - 1) == '/') {
            return path + filename;
        } else {
            return path + '/' + filename;
        }
    }

    public static boolean matchTemplate(String word,
                                        String template) throws IllegalArgumentException {
        boolean rslt = true;
        String templateNorm = template.toLowerCase();
        int nw = word.length();
        int nt = template.length();
        if (nt == 0)
            return false;
        char lastTemplSymbol = TEMPLATE_A;
        for (int i = 0; i < nw; i++) {
            char c = word.charAt(i);
            if (i < nt) {
                lastTemplSymbol = templateNorm.charAt(i);
            }
            if (lastTemplSymbol == TEMPLATE_U) {
                if (Character.isLetter(c)) {
                    if (!Character.isUpperCase(c)) {
                        rslt = false;
                        break;
                    }
                } else {
                    rslt = false;
                    break;
                }
            } else if (lastTemplSymbol == TEMPLATE_L) {
                if (Character.isLetter(c)) {
                    if (!Character.isLowerCase(c)) {
                        rslt = false;
                        break;
                    }
                } else {
                    rslt = false;
                    break;
                }
            } else if (lastTemplSymbol != TEMPLATE_A) {
                if (c != lastTemplSymbol) {
                    rslt = false;
                    break;
                }
            }
        }
        return rslt;
    }

    public static String applyTemplate(String word, String template) throws IllegalArgumentException {
        String res = applyTemplateIfPossible(word, template);
        if (res == null)
            throw new IllegalArgumentException(templateException + template + ": " + word);
        return res;
    }

    public static String applyTemplateIfPossible(String word, String template) {
        StringBuffer rslt = new StringBuffer();
        String templateNorm = template.toLowerCase();
        int nw = word.length();
        int nt = template.length();
        char lastTemplSymbol = TEMPLATE_A;
        for (int i = 0; i < nw; i++) {
            char c = word.charAt(i);
            if (i < nt) {
                lastTemplSymbol = templateNorm.charAt(i);
            }
            if (lastTemplSymbol == TEMPLATE_U) {
                if (Character.isLetter(c)) {
                    c = Character.toUpperCase(c);
                } else {
                    return null;
                }
            } else if (lastTemplSymbol == TEMPLATE_L) {
                if (Character.isLetter(c)) {
                    c = Character.toLowerCase(c);
                } else {
                    return null;
                }
            } else if (lastTemplSymbol == TEMPLATE_A) {
                if (Character.isLetter(c)) {
                    c = Character.toLowerCase(c);
                }
            } else {
                if (c != lastTemplSymbol) {
                    return null;
                }
            }
            rslt.append(c);
        }
        return rslt.toString();
    }

    public static String getProbableTemplate(String word) {   //by pjalybin 27.10.05
        // changed 07.11.05
        //not checked yet
        StringBuffer result = new StringBuffer();
        int wordLength = word.length();

        for (int i = 0; i < wordLength; i++) {
            char newTemplateChar = word.charAt(i);
            if (Character.isUpperCase(newTemplateChar))
                newTemplateChar = TEMPLATE_U;
            else if (Character.isLowerCase(newTemplateChar))
                newTemplateChar = TEMPLATE_A;
            result.append(newTemplateChar);
        }


        while ((result.length() >= 2)) {
            char last = result.charAt(result.length() - 1);
            char prelast = result.charAt(result.length() - 2);
            if (last == prelast) {
                result.deleteCharAt(result.length() - 1);
            } else {
                break;
            }
        }
        return result.toString();
    }

    public static String convertFromUTF8ForRusMorph(String str) {
        synchronized (tb) {
            tb.setLength(0);
            int n = str.length();
            char prevChar = 0;
            int prevIndex = -1;
            for (int i = 0; i < n; i++) {
                char c = str.charAt(i);
                if (c == primaryAccent) {
                    if (prevIndex >= 0) {
                        tb.append(primaryCodes[prevIndex]);
                    } else {
                        tb.append(c);
                    }
                    prevIndex = -1;
                } else if (c == secondaryAccent) {
                    if (prevIndex >= 0) {
                        tb.append(secondaryCodes[prevIndex]);
                    } else {
                        tb.append(c);
                    }
                    prevIndex = -1;
                } else {
                    if (prevIndex >= 0) {
                        if (prevChar == 'ё') {
                            tb.append("\u0496");
                        } else {
                            tb.append(prevChar);
                        }
                        prevIndex = -1;
                    }
                    prevIndex = vowelLetters.indexOf(c);
                    if (prevIndex >= 0) {
                        prevChar = c;
                    } else {
                        tb.append(c);
                    }
                }
            }
            if (prevIndex >= 0) {
                if (prevChar == 'ё') {
                    tb.append("\u0496");
                } else {
                    tb.append(prevChar);
                }
            }
            return tb.toString();
        }
    }

    public static char[] convertFromUTF8ForRusMorphIntoCharArray(String str) {
        synchronized (tb) {
            tb.setLength(0);
            int n = str.length();
            char prevChar = 0;
            int prevIndex = -1;
            for (int i = 0; i < n; i++) {
                char c = str.charAt(i);
                if (c == primaryAccent) {
                    if (prevIndex >= 0) {
                        tb.append(primaryCodes[prevIndex]);
                    } else {
                        tb.append(c);
                    }
                    prevIndex = -1;
                } else if (c == secondaryAccent) {
                    if (prevIndex >= 0) {
                        tb.append(secondaryCodes[prevIndex]);
                    } else {
                        tb.append(c);
                    }
                    prevIndex = -1;
                } else {
                    if (prevIndex >= 0) {
                        if (prevChar == 'ё') {
                            tb.append("\u0496");
                        } else {
                            tb.append(prevChar);
                        }
                        prevIndex = -1;
                    }
                    prevIndex = vowelLetters.indexOf(c);
                    if (prevIndex >= 0) {
                        prevChar = c;
                    } else {
                        tb.append(c);
                    }
                }
            }
            if (prevIndex >= 0) {
                if (prevChar == 'ё') {
                    tb.append("\u0496");
                } else {
                    tb.append(prevChar);
                }
            }

            char[] res = new char[tb.length()];
            tb.getChars(0, tb.length(), res, 0);
            return res;
        }
    }

    public static String detectOrth(String s) {
        int len = s.length();
        int state = ORTH_UNDEF;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (state == ORTH_UNDEF) {
                if (Character.isUpperCase(c)) {
                    if (i == 0) {
                        state = ORTH_UPPER;
                    } else {
                        state = ORTH_MIXEDCAPS;
                        break;
                    }
                } else {
                    if (Character.isLowerCase(c)) {
                        state = ORTH_LOWER;
                    }
                }
            } else if (state == ORTH_LOWER) {
                if (Character.isUpperCase(s.charAt(i))) {
                    state = ORTH_MIXEDCAPS;
                    break;
                }
            } else if (state == ORTH_UPPER) {
                if (Character.isUpperCase(s.charAt(i))) {
                    state = ORTH_ALLCAPS;
                } else {
                    state = ORTH_UPPERIFORCE;
                }
            } else if (state == ORTH_UPPERIFORCE) {
                if (Character.isUpperCase(s.charAt(i))) {
                    state = ORTH_MIXEDCAPS;
                    break;
                }
            } else { //if (state == ORTH_ALLCAPS)
                if (Character.isLowerCase(s.charAt(i))) {
                    state = ORTH_MIXEDCAPS;
                    break;
                }
            }
        }
        return ortharr[state];
    }

    public static String detectKind(String s) {
        int len = s.length();
        int state = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (!Character.isLetter(c)) {
                state = 1;
                break;
            }
        }
        return kindarr[state];
    }

    public static void appendCharsToByteBuffer(ByteBuffer buf, char[] chars) {
        for (int i = 0; i < chars.length; i++) {
            buf.putChar(chars[i]);
        }
    }

    public static char[] getCharArrayFromByteBuffer(ByteBuffer buf, int count) {
        char[] arr = new char[count];
        for (int i = 0; i < count; i++) {
            arr[i] = buf.getChar();
        }
        return arr;
    }

    public static char[] getCharArrayFromBytes(byte[] buf, int from, int count) {
        char[] arr = new char[count];
        for (int i = 0; i < count; i++) {
            arr[i] = getCharFromBytes(buf, from);
            from += 2;
        }
        return arr;
    }

    public static final String reverse(String s) {
        if (s.length() < 2) {
            return s;
        }
        char[] rev = s.toCharArray();
        char t;
        for (int i = 0, j = rev.length - 1; i < j; i++, j--) {
            t = rev[i];
            rev[i] = rev[j];
            rev[j] = t;
        }
        return new String(rev);
    }

    public static String detectLang(TString s) {
        int n = s.length();
        int rslt = 0; // 0 - undefined, 1 - lat, 2 - rus, 3 - mix
        for (int i = 0; i < n && rslt != 3; i++) {
            char c = s.charAt(i);
            Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
            int tp = Character.getType(c);
            if (
                    tp == Character.DASH_PUNCTUATION ||
                            tp == Character.CONNECTOR_PUNCTUATION ||
                            tp == Character.OTHER_PUNCTUATION
                    ) {
                // continue
            } else if (ub == Character.UnicodeBlock.CYRILLIC) {
                rslt |= 2;
            } else if (ub == Character.UnicodeBlock.BASIC_LATIN) {
                rslt |= 1;
            } else if (ub == Character.UnicodeBlock.LATIN_1_SUPPLEMENT) {
                rslt |= 1;
            } else if (ub == Character.UnicodeBlock.LATIN_EXTENDED_A) {
                rslt |= 1;
            } else if (ub == Character.UnicodeBlock.LATIN_EXTENDED_B) {
                rslt |= 1;
            } else {
                rslt = 0;
                break;
            }
        }
        return langArr[rslt];
    }

    public static String detectLang(String s) {
        int n = s.length();
        int rslt = 0; // 0 - undefined, 1 - lat, 2 - rus, 3 - mix
        for (int i = 0; i < n && rslt != 3; i++) {
            char c = s.charAt(i);
            Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
            int tp = Character.getType(c);
            if (
                    tp == Character.DASH_PUNCTUATION ||
                            tp == Character.CONNECTOR_PUNCTUATION ||
                            tp == Character.OTHER_PUNCTUATION
                    ) {
                // continue
            } else if (ub == Character.UnicodeBlock.CYRILLIC) {
                rslt |= 2;
            } else if (ub == Character.UnicodeBlock.BASIC_LATIN) {
                rslt |= 1;
            } else if (ub == Character.UnicodeBlock.LATIN_1_SUPPLEMENT) {
                rslt |= 1;
            } else if (ub == Character.UnicodeBlock.LATIN_EXTENDED_A) {
                rslt |= 1;
            } else if (ub == Character.UnicodeBlock.LATIN_EXTENDED_B) {
                rslt |= 1;
            } else {
                rslt = 0;
                break;
            }
        }
        return langArr[rslt];
    }

    public static String cyr2translit(String text) {
        StringBuffer res = new StringBuffer();
        int l = text.length();
        for (int i = 0; i < l; i++) {
            char c = text.charAt(i);
            String s = translitTable.get(c);
            if (s != null) {
                res.append(s);
            } else {
                res.append(c);
            }
        }
        return res.toString();
    }

    public static String lat2translitcyr(String text) {
        StringBuffer res = new StringBuffer();
        int l = text.length();
        for (int i = 0; i < l; i++) {
            char c = text.charAt(i);
            String s = translitTableLat.get(c);
            if (s != null) {
                res.append(s);
            } else {
                res.append(c);
            }
        }
        return res.toString();
    }

    public static String setLength(String s, int len) {
        if (s == null || s.length() == len) return s;
        if (len <= 0) return "";
        if (len < s.length()) return s.substring(0, len);
        StringBuffer spaces = new StringBuffer();
        for (int i = s.length(); i < len; i++) spaces.append(' ');
        return s + spaces.toString();
    }

    public static String getExceptionStack(Throwable e) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream str = new PrintStream(os);
        e.printStackTrace(str);
        return os.toString();
    }

    public static boolean matchRegexp(String regexp, String source) {
        synchronized (builtAlready) {
            Matcher m = builtAlready.get(regexp);
            if (m == null) {
                Pattern p = Pattern.compile(regexp);
                m = p.matcher("");
                builtAlready.put(regexp, m);
            }
            m.reset(source);
            return m.matches();
        }
    }

    static String readString(ByteBuffer buf) {
        int len = buf.getInt();
        byte[] cb = new byte[len];
        buf.get(cb);
        return new String(cb);
    }

    public static void appendInsertingBreaks(StringBuffer buf, String s, int averageStringLength, String breakString) {
        int cnt = 0;
        for (int i = 0; i < s.length(); i++, cnt++) {
            char c = s.charAt(i);
            if (cnt >= averageStringLength && c == ' ') {
                buf.append(breakString);
                cnt = 0;
            } else {
                buf.append(c);
            }
        }
    }

    private static class ExtensionFilter implements FileFilter {
        String[] extensions;

        public ExtensionFilter(String[] extensions) {
            this.extensions = extensions;
        }

        public boolean accept(File pathname) {
            if (pathname.isDirectory())
                return true;
            for (int i = 0; i < extensions.length; i++) {
                if (pathname.getPath().endsWith("." + extensions[i]))
                    return true;
            }
            return false;
        }
    }
}

