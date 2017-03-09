/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

public class Ptr {
    public static long nPtr = 0;
    protected StringBuffer rootString = null;
    protected char[] rootArray = null;
    protected Ptr root;
    protected int p = 0;
    //------ for testing
    String tstCurStr = "";
    //------
    boolean tstIsNull = true;

    public Ptr() {
        set(); // make Ptr = null
    }

    public Ptr(char[] _arr) {
        nPtr++;
        setMembers(null, _arr, null, 0);
    }

    public Ptr(int _len) {
        nPtr++;
        setMembers((_len > 0) ? new StringBuffer(_len) : new StringBuffer(),
                null, null, 0);
        memset(this, sut.CNULL);
        ptrChanged(); // после memset пришлось повторить
    }

    public Ptr(String _s) {
        nPtr++;
        setMembers((_s != null) ? new StringBuffer(_s) : null, null, null, 0);
    }

    public Ptr(StringBuffer _sb) {
        nPtr++;
        setMembers(_sb, null, null, 0);
    }

    public Ptr(Ptr _p) {
        nPtr++;
        set(_p);
    }

    public static int strlen(Ptr _p) {
        return ((_p != null) ? _p.len() : 0);
    }

    public static int strlen(String _s) {
        return ((_s != null) ? strlen(new Ptr(_s)) : 0);
    }

    public static int strlen(char[] _c) {
        return ((_c != null) ? strlen(new Ptr(_c)) : 0);
    }

    public static Ptr strcat(Ptr _dst, Ptr _src) {
        if (_dst != null) _dst.cat(_src);
        return _dst;
    }

    public static Ptr strcat(Ptr _dst, String _src) {
        if (_dst != null) _dst.cat(new Ptr(_src));
        return _dst;
    }

    public static char[] strcat(char _dst[], Ptr _src) {
        if (_dst != null) {
            (new Ptr(_dst)).cat(_src);
        }
        return _dst;
    }

    public static char[] strcat(char _dst[], char[] _src) {
        if (_dst != null) {
            (new Ptr(_dst)).cat(new Ptr(_src));
        }
        return _dst;
    }

    public static char[] strcat(char _dst[], String _src) {
        if (_dst != null) {
            (new Ptr(_dst)).cat(new Ptr(_src));
        }
        return _dst;
    }

    public static int strcmp(Ptr _s1, Ptr _s2)
            throws IllegalArgumentException {
        if (_s1 != null && _s2 != null)
            return _s1.cmp(_s2);
        else
            throw new IllegalArgumentException();
    }

    public static int strcmp(String _s1, Ptr _s2)
            throws IllegalArgumentException {
        return strcmp(new Ptr(_s1), _s2);
    }

    public static int strcmp(Ptr _s1, String _s2)
            throws IllegalArgumentException {
        return strcmp(_s1, new Ptr(_s2));
    }

    public static int strcmp(String _s1, String _s2)
            throws IllegalArgumentException {
        return strcmp(new Ptr(_s1), new Ptr(_s2));
    }

    public static int strcmp(char[] _s1, char[] _s2)
            throws IllegalArgumentException {
        return strcmp(new Ptr(_s1), new Ptr(_s2));
    }

    public static int strcmp(char[] _s1, String _s2)
            throws IllegalArgumentException {
        return strcmp(new Ptr(_s1), new Ptr(_s2));
    }

    public static int strcmp(String _s1, char[] _s2)
            throws IllegalArgumentException {
        return strcmp(new Ptr(_s1), new Ptr(_s2));
    }

    public static int strcmp(char[] _s1, Ptr _s2)
            throws IllegalArgumentException {
        return strcmp(new Ptr(_s1), _s2);
    }

    public static int strncmp(Ptr _s1, Ptr _s2, int _n)
            throws IllegalArgumentException {
        if (_s1 != null && _s2 != null)
            return _s1.ncmp(_s2, _n);
        else
            throw new IllegalArgumentException();
    }

    public static int strncmp(String _s1, Ptr _s2, int _n)
            throws IllegalArgumentException {
        if (_s1 != null && _s2 != null)
            return (new Ptr(_s1)).ncmp(_s2, _n);
        else
            throw new IllegalArgumentException();
    }

    public static int strncmp(Ptr _s1, String _s2, int _n)
            throws IllegalArgumentException {
        if (_s1 != null && _s2 != null)
            return _s1.ncmp(new Ptr(_s2), _n);
        else
            throw new IllegalArgumentException();
    }

    public static int strncmp(char[] _s1, String _s2, int _n)
            throws IllegalArgumentException {
        if (_s1 != null && _s2 != null)
            return (new Ptr(_s1)).ncmp(new Ptr(_s2), _n);
        else
            throw new IllegalArgumentException();
    }

    public static Ptr strcpy(Ptr _dst, Ptr _src) {
        if (_dst != null)
            _dst.cpy(_src);
        return _dst;
    }

    public static void strcpy(char[] _dst, char[] _src) {
        new Ptr(_dst).cpy(new Ptr(_src));
    }

    public static Ptr strcpy(Ptr _dst, String _src) {
        if (_dst != null)
            _dst.cpy(new Ptr(_src));
        return _dst;
    }

    public static Ptr strcpy(char[] _dst, String _src) {
        Ptr d = null;
        if (_dst != null) {
            d = new Ptr(_dst);
            d.cpy(new Ptr(_src));
        }
        return d;
    }

    public static Ptr strcpy(char[] _dst, Ptr _src) {
        Ptr d = null;
        if (_dst != null) {
            d = new Ptr(_dst);
            d.cpy(_src);
        }
        return d;
    }

    public static Ptr strcpy(Ptr _dst, char[] _src) {
        if (_dst != null)
            _dst.cpy(new Ptr(_src));
        return _dst;
    }

    public static Ptr strncpy(Ptr _dst, Ptr _src, int _n) {
        if (_dst != null && _n > 0)
            _dst.ncpy(_src, _n);
        return _dst;
    }

    public static Ptr strncpy(char[] _dst, Ptr _src, int _n) {
        Ptr d = null;
        if (_dst != null) {
            d = new Ptr(_dst);
            d.ncpy(_src, _n);
        }
        return d;
    }

    public static Ptr strncpy(char[] _dst, char[] _src, int _n) {
        return strncpy(_dst, new Ptr(_src), _n);
    }

    public static Ptr strstr(Ptr _s1, Ptr _s2) {
        Ptr rslt = null;
        if (isGood(_s1) && isGood(_s2)) {
            int i = _s1.toString().indexOf(_s2.toString());
            if (i >= 0) {
                rslt = (new Ptr(_s1)).add(i);
            }
        }
        return rslt;
    }

    public static Ptr strstr(Ptr _s1, String _s2) {
        Ptr rslt = null;
        if (isGood(_s1) && _s2 != null)
            rslt = strstr(_s1, new Ptr(_s2));
        return rslt;
    }

    public static Ptr strstr(char[] _s1, char[] _s2) {
        return strstr(new Ptr(_s1), new Ptr(_s2));
    }

    public static Ptr strstr(String _s1, Ptr _s2) {
        return strstr(new Ptr(_s1), _s2);
    }

    public static Ptr strstr(String _s1, String _s2) {
        return strstr(new Ptr(_s1), new Ptr(_s2));
    }

    public static Ptr strstr(Ptr _s1, char[] _s2) {
        return strstr(_s1, new Ptr(_s2));
    }

    public static Ptr strstr(char[] _s1, Ptr _s2) {
        return strstr(new Ptr(_s1), _s2);
    }

    public static Ptr strstr(char[] _s1, String _s2) {
        return strstr(new Ptr(_s1), new Ptr(_s2));
    }

    public static Ptr strstr(String _s1, char[] _s2) {
        return strstr(new Ptr(_s1), new Ptr(_s2));
    }

    public static Ptr strset(Ptr _p, char _c) {
        if (isGood(_p))
            _p.setChars(_c);
        return _p;
    }

    public static Ptr memset(Ptr _p, char _c) {
        Ptr rslt = null;
        if (isGood(_p)) {
            int n;
            if (_p.dataInBuffer())
                n = _p.getBuffer().length() - _p.p;
            else
                n = _p.getArray().length - _p.p;
            if (n >= 0)
                rslt = memset(_p, _c, n);
        }
        return rslt;
    }

    public static Ptr memset(Ptr _p, char _c, int _n) {
        if (isGood(_p)) {
            if (_p.dataInBuffer()) {
                StringBuffer b = _p.getBuffer();
                int n = Math.max(0, Math.min(_n, b.length())) + _p.p;
                for (int i = _p.p; i < n; i++)
                    b.setCharAt(i, _c);
            } else {
                char[] ca = _p.getArray();
                int n = Math.max(0, Math.min(_n, ca.length)) + _p.p;
                for (int i = _p.p; i < n; i++)
                    ca[i] = _c;
            }
            _p.ptrChanged();
        }
        return _p;
    }

    // clang functions

    public static Ptr strchr(String _p, char _c) {
        return strchr(new Ptr(_p), _c);
    }

    public static Ptr strchr(Ptr _p, char _c) {
        Ptr rslt = null;
        if (isGood(_p)) {
            int i = _p.findCharSafe(_c);
            if (i >= 0)
                if (i >= 0)
                    rslt = (new Ptr(_p)).add(i);
        }
        return rslt;
    }

    public static Ptr strchr(char[] _s, char _c) {
        return strchr(new Ptr(_s), _c);
    }

    public static Ptr malloc(int _len) {
        return new Ptr(_len);
    }

    public static boolean isGood(Ptr _p) {
        return (_p != null && _p.isGood());
    }

    public static int atoi(Ptr _s) {
        return (atoi((new Ptr(_s)).toString()));
    }

    public static int atoi(char[] _s) {
        return (atoi((new Ptr(_s)).toString()));
    }

    public static int atoi(String _s) {
        int rslt = 0;
        if (_s != null) {
            try {
                rslt = Integer.parseInt(_s);
            } catch (NumberFormatException x) {
                // do nothing - just return default value (0)
            }
        }
        return rslt;
    }

    public static Ptr strcpycat(Ptr _dst, Ptr _src1, Ptr _src2) {
        strcpy(_dst, _src1);
        strcat(_dst, _src2);
        return _dst;
    }

    public static Ptr strcpycat(char[] _dst, Ptr _src1, Ptr _src2) {
        Ptr d = null;
        if (_dst != null) {
            d = new Ptr(_dst);
            strcpycat(d, _src1, _src2);
        }
        return d;
    }

    public static Ptr strcpycat(char[] _dst, char[] _src1, String _src2) {
        return strcpycat(_dst, new Ptr(_src1), new Ptr(_src2));
    }

    public static Ptr strcpycat(char[] _dst, String _src1, char[] _src2) {
        return strcpycat(_dst, new Ptr(_src1), new Ptr(_src2));
    }

    public static Ptr strcpycat(Ptr _dst, String _src1, char[] _src2) {
        return strcpycat(_dst, new Ptr(_src1), new Ptr(_src2));
    }

    public static Ptr strcpycat(char[] _dst, String _src1, String _src2) {
        return strcpycat(_dst, new Ptr(_src1), new Ptr(_src2));
    }

    public static Ptr strcpycat(char[] _dst, char[] _src1, Ptr _src2) {
        return strcpycat(_dst, new Ptr(_src1), _src2);
    }

    public static boolean isNull(Ptr _p) {
        boolean rslt = true;
        if (_p != null)
            rslt = _p.isNull();
        return rslt;
    }

    public Ptr set(Ptr _p) {
        Ptr tRoot;
        int tP;
        if (_p != null) {
            tRoot = _p.getRoot();
            tP = _p.p;
        } else {
            tRoot = null;
            tP = 0;
        }
        setMembers(null, null, tRoot, tP);
        return this;
    }

    public Ptr set() {
        return set(null);
    }

    protected void setMembers(StringBuffer _rootString, char[] _rootArray,
                              Ptr _root, int _p) {
        rootString = _rootString;
        rootArray = _rootArray;
        root = _root;
        p = _p;

        ptrChanged();
    }

    public boolean isRoot() {
        return (root == null);
    }

    public boolean isNull() {
        boolean rslt = true;
        Ptr rt = getRoot();
        if (rt != null)
            rslt = (rt.getBuffer() == null && rt.getArray() == null);
        return rslt;
    }

    public String toStringX()
            throws StringIndexOutOfBoundsException, NullPointerException {
        String rslt;
        int i = findChar(sut.CNULL);
        if (i < 0)
            rslt = toStringFull();
        else {
            if (dataInBuffer())
                rslt = getBuffer().substring(p, p + i);
            else {
                char[] ca = getArray();
                rslt = new String(ca, p, i);
            }
        }
        return rslt;
    }

    public String toString() {
        String rslt = "";
        try {
            rslt = toStringX();
        } catch (StringIndexOutOfBoundsException x) {
            // do nothing - just return void string
        } catch (NullPointerException x) {
            // do nothing - just return void string
        }
        return rslt;
    }

    public String toStringFull() {
        String rslt = null;
        try {
            if (dataInBuffer())
                rslt = getBuffer().substring(p);
            else {
                char[] ca = getArray();
                rslt = new String(ca, p, ca.length - p);
            }
        } catch (StringIndexOutOfBoundsException x1) {
            // do nothing - just return null
        } catch (NullPointerException x2) {
            // do nothing - just return null
        }
        return rslt;
    }

    public int findCharSafe(char _c) {
        if (isGood()) {
            int i = p;
            char cur;
            if (dataInBuffer()) {
                StringBuffer sb = getBuffer();
                while (i < sb.length()) {
                    cur = sb.charAt(i);
                    if (cur == sut.CNULL) {
                        return -1;
                    } else {
                        if (cur == _c)
                            return i - p;
                        else
                            i++;
                    }
                }
            } else if (dataInArray()) {
                char[] ca = getArray();
                while (i < ca.length) {
                    cur = ca[i];
                    if (cur == sut.CNULL) {
                        return -1;
                    } else {
                        if (cur == _c)
                            return i - p;
                        else
                            i++;
                    }
                }
            }
        }
        return -1;
    }

    public int findChar(char _c) {
        if (isGood()) {
            int i = p;
            if (dataInBuffer()) {
                StringBuffer sb = getBuffer();
                while (i < sb.length())
                    if (sb.charAt(i) == _c)
                        return i - p;
                    else
                        i++;
            } else if (dataInArray()) {
                char[] ca = getArray();
                while (i < ca.length)
                    if (ca[i] == _c)
                        return i - p;
                    else
                        i++;
            }
        }
        return -1;
    }

    // возвращает true если данные в StringBuffer
    // и false если данные в char[]
    public boolean dataInBuffer() {
        Ptr root = getRoot();
        return (root.rootString != null);
    }

    // возвращает true если данные в char[]
    // и false если данные в StringBuffer
    public boolean dataInArray() {
        Ptr root = getRoot();
        return (root.rootArray != null);
    }

    public StringBuffer getBuffer() {
        return (isRoot() ? rootString : root.rootString);
    }

    protected char[] getArray() {
        return (isRoot() ? rootArray : root.rootArray);
    }

    public boolean sameData(Ptr _p) {
        return (
                (dataInBuffer() && _p.dataInBuffer() && getBuffer() == _p.getBuffer()) ||
                        (dataInArray() && _p.dataInArray() && getArray() == _p.getArray()));
    }

    protected Ptr getRoot() {
        return isRoot() ? this : this.root;
    }

    public char charAt()
            throws IndexOutOfBoundsException {
        return charAt(0);
    }

    public char charAt(int _i)
            throws IndexOutOfBoundsException {
        int truePos = p + _i;
        if (dataInBuffer()) {
            StringBuffer b = getBuffer();
            if (truePos < b.length() && truePos >= 0)
                return b.charAt(truePos);
            else
                return sut.CNULL;
        } else {
            char[] ca = getArray();
            if (truePos < ca.length && truePos >= 0)
                return ca[truePos];
            else
                return sut.CNULL;
        }
    }

    public Ptr setCharAt(char _c)
            throws IndexOutOfBoundsException {
        return setCharAt(0, _c);
    }

    public Ptr setCharAt(int _i, char _c)
            throws IndexOutOfBoundsException {
        int truePos = p + _i;
        if (dataInBuffer()) {
            StringBuffer b = getBuffer();
            if (truePos >= 0) {
                if (truePos >= b.length())
                    b.setLength(truePos + 1);
                b.setCharAt(truePos, _c);
            }
        } else {
            char[] ca = getArray();
            if (truePos < ca.length && truePos >= 0)
                ca[truePos] = _c;
        }
        ptrChanged();
        return this;
    }

    public boolean equals(Ptr _other)
            throws IllegalArgumentException {
        return (delta(_other) == 0);
    }

    // ++ operator
    public Ptr inc() {
        p++;
        ptrChanged();
        return this;
    }

    // -- operator
    public Ptr dec() {
        p--;
        ptrChanged();
        return this;
    }

    // += operator
    public Ptr inc(int _d) {
        p += _d;
        ptrChanged();
        return this;
    }

    // -= operator
    public Ptr dec(int _d) {
        p -= _d;
        ptrChanged();
        return this;
    }

    // + operator (returns new Ptr)
    public Ptr add(int _d) {
        return (new Ptr(this)).inc(_d);
    }

    // + operator (returns new Ptr)
    public Ptr sub(int _d) {
        return (new Ptr(this)).dec(_d);
    }

    public boolean isGood() {
        boolean rslt = false;
        if (!isNull()) {
            int n = -1;
            boolean inBuffer = false;
            if (dataInBuffer()) {
                StringBuffer sb = getBuffer();
                if (sb != null) n = sb.length();
                inBuffer = true;
            } else if (dataInArray()) {
                char[] ca = getArray();
                if (ca != null) n = ca.length;
            }
            rslt = (n >= 0 && p >= 0 && (inBuffer || p < n));
        }
        return rslt;
    }

    public int delta(char[] _c)
            throws IllegalArgumentException {
        return delta(new Ptr(_c));
    }

    public int delta(Ptr _p)
            throws IllegalArgumentException {
        if (!isGood(_p))
            throw new IllegalArgumentException();
        int rslt;
        if (getRoot() == _p.getRoot() || sameData(_p)) {
            rslt = p - _p.p;
        } else {
            throw new IllegalArgumentException();
        }
        return rslt;
    }

    // strcpy
    public Ptr cpy(Ptr _src) {
        if (isGood(_src)) cpy(_src.toString());
        return this;
    }

    public Ptr cpy(String _src) {
        if (isGood() && _src != null) {
            int len = p + _src.length();
            if (dataInBuffer()) {
                StringBuffer b = getBuffer();
                b.replace(p, len, _src); // "replace" extends buffer if neccessary
                if (b.length() > len)
                    b.setCharAt(len, sut.CNULL);
            } else {
                char[] ca = getArray();
                len = Math.min(ca.length, len);
                int n = len - p;
                if (n > 0)
                    _src.getChars(0, n, ca, p);
                if (ca.length > len)
                    ca[len] = sut.CNULL;
            }
        }
        ptrChanged();
        return this;
    }

    // strncpy
    public Ptr ncpy(Ptr _src, int _n) {
        if (isGood(_src)) ncpy(_src.toString(), _n);
        return this;
    }

    public Ptr ncpy(String _src, int _n) {
        if (isGood() && _src != null) {
            String s = _src.substring(0, _n);
            int len = p + s.length();
            if (dataInBuffer()) {
                StringBuffer b = getBuffer();
                b.replace(p, len, s); // "replace" extends buffer if neccessary
                if (b.length() > len)
                    b.setCharAt(len, sut.CNULL);
            } else {
                char[] ca = getArray();
                len = Math.min(ca.length, len);
                int n = len - p;
                if (n > 0)
                    s.getChars(0, n, ca, p);
                if (ca.length > len)
                    setCharAt(len, sut.CNULL);
            }
        }
        ptrChanged();
        return this;
    }

    // strcmp
    public int cmp(Ptr _p) {
        return (toString().compareTo(_p.toString()));
    }

    // strncmp
    public int ncmp(Ptr _p, int _maxLen) {
        String thisStr = toString();
        String thatStr = _p.toString();
        int n = Math.min(_maxLen, Math.min(thisStr.length(), thatStr.length()));
        return (thisStr.substring(0, n).compareTo(thatStr.substring(0, n)));
    }

    // strcat
    public Ptr cat(Ptr _src) {
        if (isGood() && isGood(_src)) {
            String s = _src.toString();
            int i = findChar(sut.CNULL);
            if (dataInBuffer()) {
                StringBuffer b = getBuffer();
                if (i < 0)
                    b.append(s);
                else {
                    int len = p + i + s.length();
                    b.replace(p + i, len, s); // "replace" extends buffer if neccessary
                    if (b.length() > len)
                        b.setCharAt(len, sut.CNULL);
                }
            } else {
                //char[] ca = getArray();
                // добавить строку можно только если в массиве есть символ СNULL
                // это связано с тем, что нельзя расширять границу массива, а если
                // символа CNULL в массиве нет, то строка равна всей последовательности
                // символв массива
                if (i >= 0) {
                    (new Ptr(this)).inc(i).cpy(s);
                }
            }
        }
        ptrChanged();
        return this;
    }

    // strlen
    public int len() {
        int i = findChar(sut.CNULL);
        int n = dataInBuffer() ? getBuffer().length() : getArray().length;
        return ((i < 0) ? Math.max(0, n - p) : i);
    }

    public boolean eq(Ptr _other) {
        return equals(_other);
    }

    public boolean ne(Ptr _other) {
        return !equals(_other);
    }

    // strset
    public Ptr setChars(char _c) {
        int i = p;
        if (dataInBuffer()) {
            StringBuffer b = getBuffer();
            while (i < b.length() && b.charAt(i) != sut.CNULL) {
                b.setCharAt(i, _c);
                i++;
            }
        } else {
            char[] ca = getArray();
            while (i < ca.length && ca[i] != sut.CNULL) {
                ca[i] = _c;
                i++;
            }
        }
        ptrChanged();
        return this;
    }

    public void ptrChanged() {
//    tstCurStr = toString();
//    if (!isRoot() && getRoot() != null) getRoot().ptrChanged();
    }
}
