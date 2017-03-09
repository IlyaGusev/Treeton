/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

import java.util.HashSet;

/**
 * <p/>
 * LogicCharSet
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
 * [c^ab] =c or not(a or b) = c or (not a and not b)
 * [a&&bc] = [a] and [b] or [c] = ([a] and [b]) or [c]
 * [a&&[bce]] = a and (b or c or e)
 * <p/>
 * TODO : добавить \ и \p{...}
 * \\,\u1A3F,\x4C,\t,\n,\r,\f,\a,\e   - chars
 * <p/>
 * \\d - digit
 * \\u - uppercase
 * \\l - lowercase
 * \\w - [_\\l\\u\\d] - letters, digits and _
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
 * \p{Latin}
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
 * <p/>
 * <p/>
 * Грамматика :
 * <p/>
 * <anycharset>::=      <single>|[<not>]
 * <not>::=             <disjunction>|^<disjunction>
 * <disjunction>::=     <conjunction><disjunction>|<conjunction>
 * <conjunction>::=     <range>&&<conjunction>|<range>
 * <range>::=           <char>-<char>|<single>
 * <single>::=          <char>|<set>
 * <char>::=            (any unicode char except \.[]()|*+) | \<chartail>
 * <chartail>::=        \|.|[|]|(|)|||*|+||t|n|r|f|a|e|x<hexnumber>
 * <set>::=             .|\<settail>
 * <settail>::=         d|u|l|s|w|p|p{<posixset>}
 * <posixset>::=        lower|upper|ascii|alpha|digit|alnum|punct|graph|print|blank|cntrl|xdigit|space|greek|latin|cyrillic
 */
public class LogicCharSet implements IsLogicSet, Parsable {

    public static char NEGATIVE_CHAR = '^';
    public static char ANY_CHAR = '.';
    public static char EMPTY_CHAR = 0;
    public static char LEFT_PARENTHESIS_CHAR = '[';
    public static char RIGHT_PARENTHESIS_CHAR = ']';
    boolean _isMultiple = false; // for [a*]
    private HashSet<Character> _negative; //for [^abc]
    private HashSet<Character> _positive; //for [abc] (or a|b|c)

    public LogicCharSet() {
        //TODO
    }

    public boolean equals(Object o) {
        //TODO
        return false;
    }

    public LogicCharSet and(IsLogicSet b) // AND operation
    {
        //TODO
        return new LogicCharSet();
    }

    public boolean isMatch(char c) {
        //TODO
        return true;
    }

    public boolean isEmpty()  // $
    {
        // TODO
        return true;
    }

    public LogicCharSet not() /// Отрицание
    {
        //TODO
        return new LogicCharSet();
    }

    public int parse(String s) {
        return -1;
    }

    public String toString() {
        // TODO
        return "";
    }

    public boolean isMember(Object o) {
        return (o instanceof Character) && isMatch((Character) o);
    }
}
