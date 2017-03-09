/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm.logicset;

/**
 * <p/>
 * <p/>
 * ВАЖНО:
 * если класс соответствует типу Range,
 * т.е. имеется интервал [a,b], то в этом случае
 * интервал ДОЛЖЕН принадлежать
 * ОДНОМУ элементарному множеству (кроме SET_ANY)
 * или не принадлежать никакому (кроме SET_ANY)
 * <p/>
 * Это условие не проверяется, но оно
 * гарантирует корректность работы метода and()
 * Действительно, если любой символ с (c:(c>=a)and(c<=b)) принадлежит эл. м-ву S
 * <p/>
 * тогда and([a,b],A) = [a,b] , если A=S
 * = [] , если A!=S
 * <p/>
 * Это ограничение вызвано тем, что элементарные
 * множества могут и не быть интервалами, например l=[a,z][а,я][alpha,omega]...
 * <p/>
 * <p/>
 * например [б-й]&L = [б-й], но [s-й] - недопустимо,  т.к. [s-й]&L != [s-й]
 * <p/>
 * TODO : добавить проверку допустимости
 */
public class CharInterval implements Parsable// char, set or range
{
    public static final short TYPE_EMPTY = 0;
    public static final short TYPE_CHAR = 1;
    public static final short TYPE_SET = 2;
    public static final short TYPE_RANGE = 3;
    private static final char NULL_CHAR = 0;

    // множества могут и не быть интервалами, например l=[a,z][а,я][α,ω]...
    private static final char SET_ANY = '.'; //любой символ, кроме NULL_CHAR=0
    private static final char SET_DIGITS = 'd';
    private static final char SET_UPPER = 'u';
    private static final char SET_LOWER = 'l';
    //TODO: добавить другие множества

    //String printForm=null;

    public char a;
    public char b;

    private CharInterval(char x, char y) {
        a = x;
        b = y;
    }

    private static CharInterval getSetAndSet(char x, char y) {
        if (x == y) return createSet(x);
        if (x == SET_ANY) return createSet(y);
        if (y == SET_ANY) return createSet(x);

        switch (x) {
            case SET_LOWER:
                switch (y) {
                    case SET_UPPER:
                        return createEmpty();
                    case SET_DIGITS:
                        return createEmpty();
                    default:
                        return createEmpty();
                }
            case SET_UPPER:
                switch (y) {
                    case SET_LOWER:
                        return createEmpty();
                    case SET_DIGITS:
                        return createEmpty();
                    default:
                        return createEmpty();
                }
            case SET_DIGITS:
                switch (y) {
                    case SET_LOWER:
                        return createEmpty();
                    case SET_UPPER:
                        return createEmpty();
                    default:
                        return createEmpty();
                }
            default:
                return createEmpty();
        }
    }

    private static boolean charInSet(char c, char set) {
        switch (set) {
            case SET_ANY:
                return true;
            case SET_LOWER:
                return Character.isLowerCase(c);
            case SET_UPPER:
                return Character.isUpperCase(c);
            case SET_DIGITS:
                return Character.isDigit(c);
            default:
                return false;
        }
    }

    public static CharInterval createChar(char x) {
        return new CharInterval(x, NULL_CHAR);
    }

    public static CharInterval createEmpty() {
        return new CharInterval(NULL_CHAR, NULL_CHAR);
    }

    public static CharInterval createSet(char x) {
        return new CharInterval(NULL_CHAR, x);
    }

    public static CharInterval createRange(char x, char y) {
        if (x > y) return new CharInterval(x, y);
        else if (x < y) return createEmpty();
        else return createChar(x);
    }

    public int getType() {
        if (a == NULL_CHAR) {
            if (b == NULL_CHAR) return TYPE_EMPTY;
            else return TYPE_SET;
        } else {
            if (b == NULL_CHAR) return TYPE_CHAR;
            else return TYPE_RANGE;
        }
    }

    public CharInterval and(CharInterval o) {
        char x = NULL_CHAR, y = NULL_CHAR;
        int type1 = this.getType();
        int type2 = o.getType();
        if ((type1 == TYPE_EMPTY) || (type2 == TYPE_EMPTY)) return createEmpty();
        if (type1 == TYPE_CHAR) {
            char c = this.a;
            if (type2 == TYPE_CHAR)
                return (c == o.a) ? createChar(c) : createEmpty();
            else if (type2 == TYPE_RANGE)
                return ((c >= o.a) && (c <= o.b)) ? createChar(c) : createEmpty();
            else if (type2 == TYPE_SET) return charInSet(c, o.b) ? createChar(c) : createEmpty();
            else return createEmpty();
        } else if (type1 == TYPE_SET) {
            char set = this.b;
            if (type2 == TYPE_CHAR)
                return charInSet(o.a, set) ? createChar(o.a) : createEmpty();
            else if (type2 == TYPE_RANGE)
                return (charInSet(o.a, set) && charInSet(o.b, set)) ? createRange(o.a, o.b) : createEmpty();   // TODO: range & set
            else if (type2 == TYPE_SET) return getSetAndSet(set, o.b);
            else return createEmpty();
        } else if (type1 == TYPE_RANGE) {
            char c1 = this.a, c2 = this.b;
            if (type2 == TYPE_CHAR)
                return ((o.a >= c1) && (o.a <= c2)) ? createChar(o.a) : createEmpty();
            else if (type2 == TYPE_RANGE)
                return createRange((c1 > o.a) ? c1 : o.a, (c2 < o.b) ? c2 : o.b);
            else if (type2 == TYPE_SET)
                return (charInSet(c1, o.b) && charInSet(c2, o.b)) ? createRange(o.a, o.b) : createEmpty();   // TODO: range & set
            else return createEmpty();
        }
        return new CharInterval(x, y);
    }

    public int parse(String s) {
        return -1;
    }
}