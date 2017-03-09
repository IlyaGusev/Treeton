/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;


import treeton.core.util.collector.Collector;
import treeton.core.util.collector.CollectorException;
import treeton.core.util.collector.Mutable;

import java.util.HashSet;

/**
 * <p/>
 * SingleCharLogicSet
 * Ak = a or [P1...Pm^N...Nm] or [P1...Pm^N...Nm*]
 * [ab]=[ab^]
 * a=[a]
 * c is [ab^de] = ((c is a) or (c is b)) and not((c is d) or (c is e))
 * <p/>
 * WARNING not([xxx^abc]) работает не корректно,
 * ввиду того, что not([xxx^abc]) = [abc] or [^xxx]
 * сейчас возвращается только [abc]
 * для [abc] и [^cde] все работает нормально
 * <p/>
 * TODO : все изменения в новом классe LogicCharSet
 * TODO :  представить множество как объединение интервалов на char
 * TODO : добавить операторы && и -    ^ только в начале (сразу после [)
 * [abc] = a or b or c
 * [a-d] = [abcd]
 * [a-cm-o] = [abcmno]
 * [ab]=[a] or [b]
 * [a[^b]] = [a] or not [b]
 * [^ab] = not(a or b) = not a and not b
 * [a&&bc] = [a] and [b] or [c] = ([a] and [b]) or [c]
 * [a&&[bce]] = a and (b or c or e)
 * <p/>
 * TODO : добавить \ и \p{...}
 * \\,\u1A3F,\x4C,\t,\n,\r,\f,\a,\e   - chars
 * <p/>
 * \\d - digit
 * \\u - uppercase
 * \\l - lowercase
 * \\w - letters, digits and _
 * \\s - whitespace [ \t\n\x0B\f\r]
 * \\p - punctuation chars
 * <p/>
 * \p{Lower} = \l
 * \p{Upper} = \\u
 * \p{ASCII} = [\x00-\x7F]
 * \p{Alpha}
 * \p{Digit} = \d
 * \p{Alnum} = [\p{Alpha}\p{Digit}]
 * \p{Punct}
 * \p{Graph} = [\p{Alnum}\p{Punct}]
 * \p{Print} = \p{Graph}
 * \p{Blank} = [ \t] space or tab
 * \p{Cntrl} = [\x00-\x1F\x7F]
 * \p{XDigit} = [a-fA-F\d] hex digit
 * \p{Greek}
 * \p{Cyrillic}
 * ...
 * TODO: написать новую грамматику для языка и
 * функции and not match
 * <p/>
 * TODO : перейти от множества на char к множеству на  int :
 * int>0    int=code(char)
 * int==0   empty set
 * int<0    charset:
 * -1    * any char
 * -2    u uppercase
 * -3    l lowercase
 * -4    # digit
 * -5    ...
 */
public class SingleCharLogicSet implements IsLogicSet, Parsable {
    public static char NEGATIVE_CHAR = '^';
    public static char ANY_CHAR = '.';  //'*';
    public static char EMPTY_CHAR = 0;
    public static char DIGIT_CHAR = '#';
    public static char UPPERCASE_CHAR = 'u';
    public static char LOWERCASE_CHAR = 'l';
    public static char LEFT_PARENTHESIS_CHAR = '[';
    public static char RIGHT_PARENTHESIS_CHAR = ']';
    boolean _isMultiple = false; // for [a*]
    private HashSet<Character> _negative; //for [^abc]
    private HashSet<Character> _positive; //for [abc] (or a|b|c)

    public SingleCharLogicSet() {
        _negative = new HashSet<Character>();
        _positive = new HashSet<Character>();
    }

    public SingleCharLogicSet(char c) {
        _negative = new HashSet<Character>();
        _positive = new HashSet<Character>();
        _positive.add(c);
    }

    public SingleCharLogicSet(String s) {
        _negative = new HashSet<Character>();
        _positive = new HashSet<Character>();
        addElem(s);
    }

    public static SingleCharLogicSet conjunction(SingleCharLogicSet a, SingleCharLogicSet b) // AND operation
    {
        if (a.isAny()) return b; //  1&x=x
        if (b.isAny()) return a; //  x&1=x
        if (a.isEmpty() || b.isEmpty()) return createEmpty(); // 0&x=0
        SingleCharLogicSet res = new SingleCharLogicSet();
        for (char ap : a._positive) // (a|b)&(c|d)=a&c|a&d|b&c|b&d
            for (char bp : b._positive) {
                char rp = _and(ap, bp);
                if (rp != EMPTY_CHAR) res._positive.add(rp);  // a|0=a
            }

        // !(a|b) & !(c|b) =  a|b|c|d
        res._negative.addAll(a._negative);
        res._negative.addAll(b._negative);
        res.normalizeElem();
        return res;
    }

    private static char _and(char a, char b) {
        if (a == b) return a;
        if (a == EMPTY_CHAR) return EMPTY_CHAR;
        if (b == EMPTY_CHAR) return EMPTY_CHAR;
        if (a == ANY_CHAR) return b;
        if (b == ANY_CHAR) return a;
        if ((a == LOWERCASE_CHAR) && (b == UPPERCASE_CHAR)) return EMPTY_CHAR;
        if ((a == UPPERCASE_CHAR) && (b == LOWERCASE_CHAR)) return EMPTY_CHAR;
        if ((a == LOWERCASE_CHAR) && (Character.isLowerCase(b))) return b;
        if ((a == UPPERCASE_CHAR) && (Character.isUpperCase(b))) return b;
        if ((b == LOWERCASE_CHAR) && (Character.isLowerCase(a))) return a;
        if ((b == UPPERCASE_CHAR) && (Character.isUpperCase(a))) return a;
        if ((b == DIGIT_CHAR) && (Character.isDigit(a))) return a;
        if ((a == DIGIT_CHAR) && (Character.isDigit(b))) return b;
        return EMPTY_CHAR;
    }

    private static char _matches(char chr, char pattern) {
        if (pattern == EMPTY_CHAR) return EMPTY_CHAR;
        if (pattern == ANY_CHAR) return chr;
        if ((pattern == LOWERCASE_CHAR) && (Character.isLowerCase(chr))) return chr;
        if ((pattern == UPPERCASE_CHAR) && (Character.isUpperCase(chr))) return chr;
        if ((pattern == DIGIT_CHAR) && (Character.isDigit(chr))) return chr;
        if (chr == pattern) return chr;
        return EMPTY_CHAR;
    }

    public static SingleCharLogicSet createAny() {
        SingleCharLogicSet res = new SingleCharLogicSet();
        res._positive.add(ANY_CHAR);
        return res;
    }

    public static SingleCharLogicSet createEmpty() {
        return new SingleCharLogicSet();
    }

    public boolean isSingle() // single char or group, eg. a or U or *
    {
        return (_negative.size() == 0) && (_positive.size() == 1);
    }

    public boolean equals(Object o) {
        if (o.getClass() != this.getClass()) return false;
        SingleCharLogicSet s = (SingleCharLogicSet) o;
        if ((_negative == null) && (s._negative != null)) return false;
        if ((_negative != null) && (s._negative == null)) return false;
        if ((_positive == null) && (s._positive != null)) return false;
        if ((_positive != null) && (s._positive == null)) return false;
        if ((_positive != null) && (s._positive != null)) {
            if (_positive.size() != s._positive.size()) return false;
            for (Character nc : _positive)
                if (!s._positive.contains((char) nc))
                    return false;
        }

        if ((_negative != null) && (s._negative != null)) {
            if (_negative.size() != s._negative.size()) return false;
            for (Character pc : _negative)
                if (!s._negative.contains((char) pc))
                    return false;
        }
        return true;
    }

    public void addElem(String s) // *^* . Eg. ab^de  means ( c='a' or c='b' ) and not ( c='c' or c='d')
    {
        HashSet<Character> hs = _positive;

        int i = (s.charAt(0) == LEFT_PARENTHESIS_CHAR) ? 1 : 0;  //remove [ ]
        int l = s.length();
        if ((l > 0) && (s.charAt(l - 1) == RIGHT_PARENTHESIS_CHAR)) {
            l--;
            if ((l > 0) && (s.charAt(l - 1) == ANY_CHAR)) {
                l--; // [...*] multiple char
                _isMultiple = true;
            }
        }
        for (; i < l; i++) {
            char c = s.charAt(i);
            if (c == NEGATIVE_CHAR) hs = _negative;
            else hs.add(c);
        }
        if ((_positive.size() == 0) && (_negative.size() > 0))
            _positive.add(ANY_CHAR); // [^a] means [*^a]
        normalizeElem();
    }

    public void setMultiple() {
        _isMultiple = true;
    }

    public boolean isMultiple() {
        return _isMultiple;
    }

    public void clearElem() {
        _positive.clear();
        _negative.clear();
    }

    public void setElem(String s) {
        this.clearElem();
        this.addElem(s);
    }

    public SingleCharLogicSet and(IsLogicSet b) // AND operation
    {
        return conjunction(this, (SingleCharLogicSet) b);
    }

    public boolean isMatch(char c) {

        boolean res = false;
        for (Character p : _positive)
            if (_matches(c, p) != EMPTY_CHAR) {
                res = true;
                break;
            }
        if (!res) return false;
        for (Character p : _negative)
            if (_matches(c, p) != EMPTY_CHAR) {
                return false;
            }
        return true;
    }

    public boolean isEquals(SingleCharLogicSet b) {
        if ((this._positive.size() != b._positive.size())
                || (this._negative.size() != b._negative.size()))
            return false;
        for (Character c : this._positive)
            if (!b._positive.contains(c)) return false;
        for (Character c : this._negative)
            if (!b._negative.contains(c)) return false;
        return true;
    }

    public void normalizeElem() {
        normalizeSet(_positive);
        normalizeSet(_negative);
        // n in neg; if n not in pos then delete n from neg
        HashSet<Character> badNegs = new HashSet<Character>();
        HashSet<Character> badPos = new HashSet<Character>();
        for (Character n : _negative) {
            boolean intersects = false;
            for (Character p : _positive) {
                char c = _and(n, p);
                if (c != EMPTY_CHAR) {
                    if (c == p) badPos.add(c);
                    else intersects = true;
                }
            }
            if (!intersects) badNegs.add(n);
        }
        _negative.removeAll(badNegs);
        _positive.removeAll(badPos);
    }

    private void normalizeSet(HashSet<Character> hs) {
        // _hs = a1|...|an
        // 1|a=1
        // A|u=u
        // a|l=l
        // 0|a=a
        if (hs.contains(ANY_CHAR)) // 1|a=1
        {
            hs.clear();
            hs.add(ANY_CHAR);
            return;
        }
        if (hs.contains(EMPTY_CHAR)) hs.remove(EMPTY_CHAR); // 0|a=a
        if (hs.contains(UPPERCASE_CHAR)) // A|u=u
        {
            HashSet<Character> uppercase = new HashSet<Character>();
            for (Character c : hs)
                if (isTerminalChar(c) && Character.isUpperCase(c)) uppercase.add(c);
            hs.removeAll(uppercase);
        }
        if (hs.contains(LOWERCASE_CHAR)) // a|l=l
        {
            HashSet<Character> lowercase = new HashSet<Character>();
            for (Character c : hs)
                if (isTerminalChar(c) && Character.isLowerCase(c)) lowercase.add(c);
            hs.removeAll(lowercase);
        }
    }

    public boolean isTerminalChar(char c) {
        return (c != ANY_CHAR)
                && (c != EMPTY_CHAR)
                && (c != UPPERCASE_CHAR)
                && (c != DIGIT_CHAR)
                && (c != LOWERCASE_CHAR)
                && (c != NEGATIVE_CHAR)
                && (c != LEFT_PARENTHESIS_CHAR)
                && (c != RIGHT_PARENTHESIS_CHAR);
    }

    public boolean isAny()  // *
    {
        return isSingle() && _positive.contains(ANY_CHAR);
    }

    public boolean isEmpty()  // $
    {
        // "" == "$" == "$^a"
        return _positive.isEmpty() || isSingle() && _positive.contains(EMPTY_CHAR);
    }

    public SingleCharLogicSet not() /// Отрицание
    {

        SingleCharLogicSet re;//=new SingleCharLogicSet();
        if ((_positive.size() > 0) && !((_positive.size() == 1) && (_positive.contains(ANY_CHAR)))) {
            re = new SingleCharLogicSet();
            re._positive.add(ANY_CHAR);
            re._negative = (HashSet<Character>) _positive.clone();
        } else if (_negative.size() > 0) {
            re = new SingleCharLogicSet();
            re._positive = (HashSet<Character>) _negative.clone();
        } else if (_positive.size() > 0) re = createEmpty(); // .
        else re = createAny();
        re.normalizeElem();
        return re;
    }

    public int parse(String s) {
        HashSet<Character> hs = _positive;
        hs.clear();

        if ((s == null) || (s.length() < 1)) return -1;
        if (s.charAt(0) != LEFT_PARENTHESIS_CHAR) {
            hs.add(s.charAt(0));
            _negative.clear();
            return 1;
        } else {

            for (int i = 1; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == RIGHT_PARENTHESIS_CHAR) {
                    if ((_positive.size() == 0) && (_negative.size() > 0))
                        _positive.add(ANY_CHAR); // [^a] means [*^a]
                    normalizeElem();
                    return i + 1; // Normal end
                }
                if (c == NEGATIVE_CHAR) hs = _negative;
                else hs.add(c);
            }
            return -1; // not closed []
        }
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        if (!isSingle()) s.append(LEFT_PARENTHESIS_CHAR);
        if (!((_positive.size() == 1)
                && (_negative.size() > 0)
                && (_positive.contains(ANY_CHAR)))) // [*^a] = [^a]
        {
            for (Character c : _positive) s.append(c);
        }
        if (_negative.size() > 0) {
            s.append(NEGATIVE_CHAR);
            for (Character c : _negative) s.append(c);
        }
        if (!isSingle()) s.append(RIGHT_PARENTHESIS_CHAR);
        return s.toString();
    }

    public boolean isMember(Object o) {
        return (o instanceof Character) && isMatch((Character) o);
    }

    public void makeNotCaseSensitive() {
        HashSet<Character> chars = new HashSet<Character>();
        for (Character c : _positive)
            if (Character.isLetter(c) && c != LOWERCASE_CHAR && c != UPPERCASE_CHAR)
                chars.add(Character.isLowerCase(c) ? Character.toUpperCase(c) : Character.toLowerCase(c));
        _positive.addAll(chars);
        chars.clear();
        for (Character c : _negative)
            if (Character.isLetter(c) && c != LOWERCASE_CHAR && c != UPPERCASE_CHAR)
                chars.add(Character.isLowerCase(c) ? Character.toUpperCase(c) : Character.toLowerCase(c));
        _positive.addAll(chars);
    }

    public static class SingleCharLogicSetCollectable extends Mutable {
        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            SingleCharLogicSet t = (SingleCharLogicSet) o;
            t._negative = (HashSet<Character>) col.get();
            t._positive = (HashSet<Character>) col.get();
            t._isMultiple = (Boolean) col.get();

        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            SingleCharLogicSet t = (SingleCharLogicSet) o;
            col.put(t._negative);
            col.put(t._positive);
            col.put(t._isMultiple);
        }
    }
}
