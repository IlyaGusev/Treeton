/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

import treeton.core.TString;
import treeton.core.fsm.*;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.BlockStack;
import treeton.core.util.HalfSegment;
import treeton.core.util.nu;
import treeton.core.util.sut;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ScapeExpression implements FSM { //TODO: избавиться от статики
    public static final int LTdir_L = 0;
    public static final int LTdir_R = 1;
    public static final int LTdir_ANY = 2;
    public static final int LTdir_DENIED = 3;
    public static final String OPSYMBOLS = "!=><&|?:p+-$.";
    public static char L_INDEXBRACKET = '[';
    public static char R_INDEXBRACKET = ']';

    public static OperatorType LT_PLUS = new OperatorType("+", LTdir_DENIED, 2, 5);
    public static OperatorType LT_MINUS = new OperatorType("-", LTdir_DENIED, 2, 5);
    public static OperatorType LT_NOTEQ = new OperatorType("!=", LTdir_DENIED, 2, 4);
    public static OperatorType LT_EQUAL = new OperatorType("==", LTdir_DENIED, 2, 4);
    public static OperatorType LT_LESS = new OperatorType("<", LTdir_DENIED, 2, 4);
    public static OperatorType LT_MORE = new OperatorType(">", LTdir_DENIED, 2, 4);
    public static OperatorType LT_EQLESS = new OperatorType("<=", LTdir_DENIED, 2, 4);
    public static OperatorType LT_EQMORE = new OperatorType(">=", LTdir_DENIED, 2, 4);
    public static OperatorType LT_AND = new OperatorType("&&", LTdir_R, 2, 3);
    public static OperatorType LT_OR = new OperatorType("||", LTdir_R, 2, 2);
    public static OperatorType LT_PENALTY = new OperatorType("=p=", LTdir_DENIED, 2, 1);
    public static OperatorType LT_QUESTION = new OperatorType("?", LTdir_R, 2, 0);
    public static OperatorType LT_DDOT = new OperatorType(":", LTdir_DENIED, 2, 0);
    public static OperatorType LT_SUBSTRING = new OperatorType("$=", LTdir_DENIED, 2, 4);
    public static OperatorType LT_GETBINDING = new OperatorType(".$", LTdir_DENIED, 2, 6);
    public static OperatorType LT_GETFEATURE = new OperatorType(".", LTdir_DENIED, 2, 6);
    public static OperatorType LT_INDEX = new OperatorType("[]", LTdir_DENIED, 2, 6); // only as a flag
    public static Font font = new Font("Courier", 0, 12);
    static char[][] opNames = new char[][]{
            LT_PLUS.name.toCharArray(),
            LT_MINUS.name.toCharArray(),
            LT_NOTEQ.name.toCharArray(),
            LT_EQUAL.name.toCharArray(),
            LT_LESS.name.toCharArray(),
            LT_MORE.name.toCharArray(),
            LT_EQLESS.name.toCharArray(),
            LT_EQMORE.name.toCharArray(),
            LT_AND.name.toCharArray(),
            LT_OR.name.toCharArray(),
            LT_PENALTY.name.toCharArray(),
            LT_QUESTION.name.toCharArray(),
            LT_DDOT.name.toCharArray(),
            LT_SUBSTRING.name.toCharArray(),
            LT_GETBINDING.name.toCharArray(),
            LT_GETFEATURE.name.toCharArray()
    };

    static OperatorType[] operations = new OperatorType[]{
            LT_PLUS,
            LT_MINUS,
            LT_NOTEQ,
            LT_EQUAL,
            LT_LESS,
            LT_MORE,
            LT_EQLESS,
            LT_EQMORE,
            LT_AND,
            LT_OR,
            LT_PENALTY,
            LT_QUESTION,
            LT_DDOT,
            LT_SUBSTRING,
            LT_GETBINDING,
            LT_GETFEATURE
    };

    static HashSet<ScapeExpressionFunction> functions = new HashSet<ScapeExpressionFunction>();
    private static char[][] bracket = new char[][]{new char[]{')'}};
    private static char[][] indexBracket = new char[][]{new char[]{R_INDEXBRACKET}};

    static {
        ScapeExpressionFunction rus2eng = new ScapeExpressionFunction<String>(
                "rus2eng",
                String.class,
                new Class[]{String.class}
        ) {
            protected String implementation(AbstractList params) {
                Object o = params.get(0);
                if (o == null) {
                    return null;
                }
                return (treeton.core.util.sut.cyr2translit(o.toString()));
            }
        };
        functions.add(rus2eng);
        ScapeExpressionFunction eng2rus = new ScapeExpressionFunction<String>(
                "eng2rus",
                String.class,
                new Class[]{String.class}
        ) {
            protected String implementation(AbstractList params) {
                Object o = params.get(0);
                if (o == null) {
                    return null;
                }
                return (treeton.core.util.sut.lat2translitcyr((o.toString())));
            }
        };
        functions.add(eng2rus);
        ScapeExpressionFunction funcIndexOf = new ScapeExpressionFunction<Integer>(
                "indexOf",
                Integer.class,
                new Class[]{String.class, String.class}
        ) {
            protected Integer implementation(AbstractList params) {
                if (params.get(0) == null || params.get(1) == null) {
                    return -1;
                }
                return (params.get(0).toString().toLowerCase().indexOf(params.get(1).toString().toLowerCase()));
            }
        };
        functions.add(funcIndexOf);

        ScapeExpressionFunction funcLastIndexOf = new ScapeExpressionFunction<Integer>(
                "lastIndexOf",
                Integer.class,
                new Class[]{String.class, String.class}
        ) {
            protected Integer implementation(AbstractList params) {
                if (params.get(0) == null || params.get(1) == null) {
                    return -1;
                }
                return (params.get(0).toString().toLowerCase().lastIndexOf(params.get(1).toString().toLowerCase()));
            }
        };
        functions.add(funcLastIndexOf);

        ScapeExpressionFunction funcOverlaps = new ScapeExpressionFunction<Boolean>(
                "overlaps",
                Boolean.class,
                new Class[]{ScapeVariable.class, ScapeVariable.class}
        ) {
            protected Boolean implementation(AbstractList params) {
                if (params.get(0) == null || params.get(1) == null) {
                    return false;
                }
                ScapeVariable first = (ScapeVariable) params.get(0);
                ScapeVariable second = (ScapeVariable) params.get(1);
                Integer fs = (Integer) first.getValue(TrnType.start_FEATURE);
                Integer fe = (Integer) first.getValue(TrnType.end_FEATURE);
                Integer ss = (Integer) second.getValue(TrnType.start_FEATURE);
                Integer se = (Integer) second.getValue(TrnType.end_FEATURE);

                return fe > ss && se > fs;
            }
        };
        functions.add(funcOverlaps);

//    ScapeExpressionFunction funcIntesectsWithType = new ScapeExpressionFunction<Boolean>(
//        "intersectsWithType",
//        Boolean.class,
//        new Class[]{ ScapeVariable.class, String.class }
//    ){
//      protected Boolean implementation(AbstractList params) {
//        if(params.get(0)==null||params.get(1)==null){
//          return false;
//        }
//        ScapeBinding bind = (ScapeBinding)params.get(0);
//        Token start = bind.getStartToken();
//        Token end = bind.getEndToken();
//
//        TrnType tp = start.getStorage().getTypes().get((String) params.get(1));
//
//        TokenImpl cur = (TokenImpl) start;
//        if (cur.hasParentOfType(tp)) {
//          return true;
//        }
//
//        while (cur != end) {
//          cur = (TokenImpl) cur.getNextToken();
//          if (cur.hasParentOfType(tp)) {
//            return true;
//          }
//        }
//
//        return false;
//      }
//    };
//    functions.add(funcIntesectsWithType);

        ScapeExpressionFunction funcCompareLetters = new ScapeExpressionFunction<Boolean>(
                "compareLetters",
                Boolean.class,
                new Class[]{String.class, String.class}
        ) {
            protected Boolean implementation(AbstractList params) {
                return params.get(0) != null && params.get(1) != null && ScapeExpression.compareLetters((String) params.get(0), (String) params.get(1));
            }
        };
        functions.add(funcCompareLetters);

        ScapeExpressionFunction funcIndexOfLetters = new ScapeExpressionFunction<Integer>(
                "indexOfLetters",
                Integer.class,
                new Class[]{String.class, String.class}
        ) {
            protected Integer implementation(AbstractList params) {
                if (params.get(0) == null || params.get(1) == null) {
                    return -1;
                }
                return ScapeExpression.indexOfLetters((String) params.get(0), (String) params.get(1), false);
            }
        };
        functions.add(funcIndexOfLetters);

        ScapeExpressionFunction funcIndexOfWord = new ScapeExpressionFunction<Integer>(
                "indexOfWord",
                Integer.class,
                new Class[]{String.class, String.class}
        ) {
            protected Integer implementation(AbstractList params) {
                if (params.get(0) == null || params.get(1) == null) {
                    return -1;
                }
                return ScapeExpression.indexOfLetters((String) params.get(0), (String) params.get(1), true);
            }
        };
        functions.add(funcIndexOfWord);
        ScapeExpressionFunction adjoinL = new ScapeExpressionFunction<Boolean>(
                "adjoin",
                Boolean.class,
                new Class[]{ScapeVariable.class, ScapeVariable.class}
        ) {
            protected Boolean implementation(AbstractList params) {
                if (params.get(0) == null || params.get(1) == null) {
                    return false;
                }
                ScapeVariable first = (ScapeVariable) params.get(0);
                ScapeVariable second = (ScapeVariable) params.get(1);

                return ((AdjoiningDetector) first).adjoin(second);
            }
        };
        functions.add(adjoinL);
        ScapeExpressionFunction adjoinR = new ScapeExpressionFunction<Boolean>(
                "adjoinR",
                Boolean.class,
                new Class[]{ScapeVariable.class, ScapeVariable.class}
        ) {
            protected Boolean implementation(AbstractList params) {
                if (params.get(0) == null || params.get(1) == null) {
                    return false;
                }
                ScapeVariable first = (ScapeVariable) params.get(0);
                ScapeVariable second = (ScapeVariable) params.get(1);

                return ((AdjoiningDetector) first).adjoinR(second);
            }
        };
        functions.add(adjoinR);
    }

    Node root;
    int ID = 0;
    BlockStack rootStack = new BlockStack();

    public static int indexOfLetters(String x, String y, boolean wholeword) {
        // индекс подстроки, игнорируя не буквы

        int xlen = x.length();
        for (int k = 0; k < xlen; k++)
            if (compareLetters(x, y, k, 0, wholeword, true)) {
                while (k < xlen && !isLetter(x.charAt(k))) k++;
                return k;
            }
        return -1;
    }

    public static boolean isLetter(char a) {
        return Character.isLetter(a) || Character.isDigit(a);
    }

    public static Boolean compareLetters(String x, String y) {
        return compareLetters(x, y, 0, 0, false, false);
    }

    private static Boolean compareLetters(String x, String y, int xi, int yi, boolean wholeword, boolean starts) {
        int xlen = x.length();
        int ylen = y.length();
        while (xlen > 0 && !isLetter(x.charAt(xlen - 1))) xlen--;
        while (ylen > 0 && !isLetter(y.charAt(ylen - 1))) ylen--;
        while (xi < xlen && !isLetter(x.charAt(xi))) xi++;
        while (yi < ylen && !isLetter(y.charAt(yi))) yi++;

        for (; ; ) {

            if ((xi >= xlen || starts) && yi >= ylen) return true;
            if (xi >= xlen || yi >= ylen || x.charAt(xi) != y.charAt(yi)) return false;
            xi++;
            yi++;
            boolean xblank = false;
            boolean yblank = false;
            while (xi < xlen && !isLetter(x.charAt(xi))) {
                xi++;
                xblank = true;
            }
            while (yi < ylen && !isLetter(y.charAt(yi))) {
                yi++;
                yblank = true;
            }
            if (xi >= xlen) xblank = true;
            if (yi >= ylen) yblank = true;
            if (wholeword && xblank != yblank) return false;
        }
    }

    protected static Class validateExpression(char[] s, int endpl, Node a,
                                              HashMap<String, ? extends ScapeVariable> variables,
                                              HashMap<String, RegexpVariable> regexpVars,
                                              boolean regexVar) throws ParseException {
        int i;
        Class res, res1;

        if (a.function != null) return a.function.validate(s, endpl, a.functionParams, variables, regexpVars, regexVar);

        if (a.opType == null) {
            if (a.varName.startsWith("\"")) {
                a.valueType = String.class;
                a.value = a.varName.substring(1, a.varName.length() - 1);
                return a.valueType;
            } else {
                a.var = null;
                if (!regexVar) {
                    if (variables != null)
                        a.var = variables.get(a.varName);
                } else {
                    if (regexpVars != null)
                        a.var = regexpVars.get(a.varName);
                }
                if (a.var != null) {
                    a.value = null;
                    a.valueType = (a.var instanceof RegexpVariable) ?
                            RegexpVariable.class
                            : ScapeVariable.class;
                    return a.valueType;
                }

                if (a.varName.equals("null")) {
                    a.value = nu.ll;
                    a.valueType = null;
                    return a.valueType;
                }

                char[] varChars = a.varName.toCharArray();
                i = sut.skipNumber(varChars, 0, varChars.length - 1);
                if (i >= varChars.length) {
                    a.value = Integer.valueOf(a.varName);
                    a.valueType = Integer.class;
                    return a.valueType;
                }
                i = sut.skipBoolean(varChars, 0, varChars.length - 1);
                if (i >= varChars.length) {
                    a.value = "true".equals(a.varName) ? Boolean.TRUE : Boolean.FALSE;
                    a.valueType = Boolean.class;
                    return a.valueType;
                }
                a.valueType = String.class;
                a.value = new String(varChars);
                return a.valueType;
            }
        } else {
            res = null;
            if (a.op1 != null) {
                res = validateExpression(s, endpl, a.op1, variables, regexpVars, a.opType == LT_GETBINDING);
            } else if (a.opType.numargs != 1) {
                throw new ParseException("Missing first argument", null, s, a.pl, endpl);
            }
            if (a.op2 != null) {
                res1 = validateExpression(s, endpl, a.op2, variables, regexpVars, false);
            } else {
                throw new ParseException("Missing second argument", null, s, a.pl, endpl);
            }

            if (a.opType == LT_NOTEQ || a.opType == LT_EQUAL) {
                if (res == null || res1 == null) {
                    a.valueType = Boolean.class;
                    return a.valueType;
                }
                if (res == res1) {
                    a.valueType = Boolean.class;
                    return a.valueType;
                }
                throw new ParseException("Arguments types must match", null, s, a.pl, endpl);
            } else if (a.opType == LT_DDOT) {
                if (res == null || res1 == null) {
                    a.valueType = null;
                    return null;
                }
                if (res == res1) {
                    a.valueType = res;
                    return res;
                }
                throw new ParseException("Arguments types must match", null, s, a.pl, endpl);
            } else if (a.opType == LT_LESS || a.opType == LT_MORE || a.opType == LT_EQLESS || a.opType == LT_EQMORE) {
                if (res == null && res1 == null) {
                    a.valueType = Boolean.class;
                    return a.valueType;
                }
                if (res != null && res != Integer.class && res != HalfSegment.class || res1 != null && res1 != Integer.class && res1 != HalfSegment.class) {
                    throw new ParseException("Only integers allowed here as arguments", null, s, a.pl, endpl);
                }
                a.valueType = Boolean.class;
                return a.valueType;
            } else if (a.opType == LT_MINUS) {
                if (res == null && res1 == null) {
                    a.valueType = Integer.class;
                    return a.valueType;
                }
                if (res != null && res != Integer.class || res1 != null && res1 != Integer.class) {
                    throw new ParseException("Only integers allowed here as arguments", null, s, a.pl, endpl);
                }
                a.valueType = Integer.class;
                return a.valueType;
            } else if (a.opType == LT_PLUS) {
                if (res == null && res1 == null) {
                    a.valueType = null;
                    return a.valueType;
                }
                if (res != null && res != Integer.class && res != String.class || res1 != null && res1 != Integer.class && res1 != String.class) {
                    throw new ParseException("Only integers or strings allowed here as arguments", null, s, a.pl, endpl);
                }
                if (res == String.class || res1 == String.class) {
                    a.valueType = String.class;
                } else if (res == Integer.class || res1 == Integer.class) {
                    a.valueType = Integer.class;
                }
                return a.valueType;
            } else if (a.opType == LT_AND || a.opType == LT_OR) {
                if (res == null && res1 == null) {
                    a.valueType = Boolean.class;
                    return a.valueType;
                }
                if (res != null && res != Boolean.class || res1 != null && res1 != Boolean.class) {
                    throw new ParseException("Only booleans allowed here as arguments", null, s, a.pl, endpl);
                }
                a.valueType = Boolean.class;
                return a.valueType;
            } else if (a.opType == LT_PENALTY) {
                if (res == null && res1 == null) {
                    a.valueType = Boolean.class;
                    return a.valueType;
                }
                if (res != null && res != Integer.class || res1 != null && res1 != Boolean.class) {
                    throw new ParseException("usage: integer =p= boolean", null, s, a.pl, endpl);
                }
                a.valueType = Boolean.class;
                return a.valueType;
            } else if (a.opType == LT_SUBSTRING) {
                if (res == null && res1 == null) {
                    a.valueType = Boolean.class;
                    return a.valueType;
                }
                if (res != null && res != String.class || res1 != null && res1 != String.class) {
                    throw new ParseException("usage: substring $= string", null, s, a.pl, endpl);
                }
                a.valueType = Boolean.class;
                return a.valueType;
            } else if (a.opType == LT_GETBINDING) {
                if (res == null && res1 == null) {
                    a.valueType = String.class;
                    return a.valueType;
                }
                if (res != null && res != RegexpVariable.class || res1 != null && res1 != Integer.class) {
                    throw new ParseException("usage: regexp .$ integer", null, s, a.pl, endpl);
                }
                a.valueType = String.class;
                return a.valueType;
            } else if (a.opType == LT_INDEX) {
                if (res == null && res1 == null) {
                    a.valueType = ScapeVariable.class;
                    return a.valueType;
                }
                if (res != null && res != ScapeVariable.class || res1 != null && res1 != Integer.class) {
                    throw new ParseException("usage: variable [ integer ]", null, s, a.pl, endpl);
                }
                a.valueType = ScapeVariable.class;
                return a.valueType;
            } else if (a.opType == LT_GETFEATURE) {
                if (res == null && res1 == null) {
                    a.valueType = null;
                    return a.valueType;
                }
                if (res != null && res != ScapeVariable.class || res1 != null && res1 != String.class) {
                    throw new ParseException("usage: variable . feature", null, s, a.pl, endpl);
                }

                return null;
            } else if (a.opType == LT_QUESTION) {
                if (res != null && res != Boolean.class) {
                    throw new ParseException("First argument must be boolean here", null, s, a.pl, endpl);
                }
                a.valueType = res1;
                return res1;
            }
            throw new ParseException("Wrong opType " + a.opType.name, null, s, a.pl, endpl);
        }
    }

    static Object evaluate(Node a) {
        if (a.function != null) {
            Object o = a.function.evaluate(a.functionParams);
            if (o == null) {
                o = nu.ll;
            }
            return o;
        }

        Object res, res1;

        if (a.opType == null) {
            if (a.var == null) {
                return a.value == null ? nu.ll : a.value;
            } else {
                if (a.value == null) {
                    return a.var;
                }
            }
        } else {
            res = null;
            res1 = null;

            if (a.opType == LT_NOTEQ || a.opType == LT_EQUAL) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }
                if (res == null || res1 == null) {
                    throw new RuntimeException("Something wrong with the arguments");
                }
                if (res == nu.ll && res1 != nu.ll || res1 == nu.ll && res != nu.ll) {
                    if (a.opType == LT_NOTEQ) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.FALSE;
                    }
                }

                if (res == nu.ll && res1 == nu.ll) {
                    if (a.opType == LT_EQUAL) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.FALSE;
                    }
                }

                if (res.getClass() != res1.getClass()) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }

                if (a.opType == LT_NOTEQ) {
                    if (res instanceof String) {
                        return ((String) res).equalsIgnoreCase((String) res1) ? Boolean.FALSE : Boolean.TRUE;
                    } else {
                        return res.equals(res1) ? Boolean.FALSE : Boolean.TRUE;
                    }
                } else {
                    if (res instanceof String) {
                        return ((String) res).equalsIgnoreCase((String) res1) ? Boolean.TRUE : Boolean.FALSE;
                    } else {
                        return res.equals(res1) ? Boolean.TRUE : Boolean.FALSE;
                    }
                }
            } else if (a.opType == LT_LESS || a.opType == LT_MORE || a.opType == LT_EQLESS || a.opType == LT_EQMORE) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }
                if (!(res instanceof Integer) && !(res instanceof HalfSegment) || !(res1 instanceof Integer) && !(res instanceof HalfSegment)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }

                if (res instanceof Integer && res1 instanceof Integer) {
                    int v1 = (Integer) res;
                    int v2 = (Integer) res1;

                    if (a.opType == LT_LESS) {
                        return v1 < v2 ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_MORE) {
                        return v1 > v2 ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_EQLESS) {
                        return v1 <= v2 ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_EQMORE) {
                        return v1 >= v2 ? Boolean.TRUE : Boolean.FALSE;
                    }
                } else if (res instanceof HalfSegment && res1 instanceof HalfSegment) {
                    HalfSegment v1 = (HalfSegment) res;
                    HalfSegment v2 = (HalfSegment) res1;

                    if (a.opType == LT_LESS) {
                        return v1.mayBeLess(v2, true) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_MORE) {
                        return v2.mayBeLess(v1, true) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_EQLESS) {
                        return v1.mayBeLess(v2, false) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_EQMORE) {
                        return v2.mayBeLess(v1, false) ? Boolean.TRUE : Boolean.FALSE;
                    }
                } else if (res instanceof HalfSegment && res1 instanceof Integer) {
                    HalfSegment v1 = (HalfSegment) res;
                    Integer v2 = (Integer) res1;

                    if (a.opType == LT_LESS) {
                        return v1.mayBeLess(v2, true) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_MORE) {
                        return v1.mayBeMore(v2, true) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_EQLESS) {
                        return v1.mayBeLess(v2, false) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_EQMORE) {
                        return v1.mayBeMore(v2, false) ? Boolean.TRUE : Boolean.FALSE;
                    }
                } else if (res instanceof Integer && res1 instanceof HalfSegment) {
                    Integer v1 = (Integer) res;
                    HalfSegment v2 = (HalfSegment) res1;

                    if (a.opType == LT_LESS) {
                        return v2.mayBeMore(v1, true) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_MORE) {
                        return v2.mayBeLess(v1, true) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_EQLESS) {
                        return v2.mayBeMore(v1, false) ? Boolean.TRUE : Boolean.FALSE;
                    } else if (a.opType == LT_EQMORE) {
                        return v2.mayBeLess(v1, false) ? Boolean.TRUE : Boolean.FALSE;
                    }
                }
            } else if (a.opType == LT_MINUS) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }
                if (!(res instanceof Integer) || !(res1 instanceof Integer)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                int v1 = (Integer) res;
                int v2 = (Integer) res1;
                if (a.opType == LT_MINUS) {
                    return v1 - v2;
                }
            } else if (a.opType == LT_PLUS) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }

                if (res1 instanceof String || res instanceof String) {
                    return (res == nu.ll ? "" : res.toString()) +
                            (res1 == nu.ll ? "" : res1.toString());
                } else if (res1 instanceof Integer && res == nu.ll) {
                    return res1;
                } else if (res instanceof Integer && res1 == nu.ll) {
                    return res;
                } else if (res instanceof Integer && res1 instanceof Integer) {
                    return ((Integer) res) + ((Integer) res1);
                } else {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
            } else if (a.opType == LT_AND) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (!(res instanceof Boolean)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                if (res == Boolean.FALSE) {
                    return Boolean.FALSE;
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }
                if (!(res1 instanceof Boolean)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                return res1;
            } else if (a.opType == LT_OR) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (!(res instanceof Boolean)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                if (res == Boolean.TRUE) {
                    return Boolean.TRUE;
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }
                if (!(res1 instanceof Boolean)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                return res1;
            } else if (a.opType == LT_PENALTY) {
                return Boolean.TRUE;
            } else if (a.opType == LT_SUBSTRING) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }
                if (!(res instanceof String && res1 instanceof String)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                String v1 = res.toString().toLowerCase();
                String v2 = res1.toString().toLowerCase();
                return v2.indexOf(v1) >= 0;

            } else if (a.opType == LT_GETBINDING) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }
                if (!(res instanceof RegexpVariable && res1 instanceof Integer)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                RegexpVariable v1 = (RegexpVariable) res;
                Integer v2 = (Integer) res1;
                String s = v1.getBindingValue(v2);
                if (s == null) {
                    return nu.ll;
                }
                return s;
            } else if (a.opType == LT_INDEX) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }
                if (!(res instanceof ScapeVariable && res1 instanceof Integer)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                ScapeVariable v1 = (ScapeVariable) res;
                return new Object[]{v1, res1};
            } else if (a.opType == LT_GETFEATURE) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (a.op2 != null) {
                    res1 = evaluate(a.op2);
                }
                if (!((res instanceof ScapeVariable || res instanceof Object[]) && res1 instanceof String)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                ScapeVariable v1;
                String v2 = (String) res1;
                Object o;
                if (res instanceof Object[]) {
                    v1 = (ScapeVariable) ((Object[]) res)[0];
                    o = v1.getValue((Integer) ((Object[]) res)[1], v2);
                } else {
                    v1 = (ScapeVariable) res;
                    o = v1.getValue(v2);
                }
                if (o == null) {
                    return nu.ll;
                }
                if (o instanceof TString) {
                    o = o.toString();
                }
                return o;
            } else if (a.opType == LT_QUESTION) {
                if (a.op1 != null) {
                    res = evaluate(a.op1);
                }
                if (!(res instanceof Boolean)) {
                    throw new RuntimeException("Type mismatch error during evaluation");
                }
                if (a.op2 == null) {
                    throw new RuntimeException("Something wrong with the arguments");
                }
                if (res == Boolean.TRUE) {
                    return evaluate(a.op2.op1);
                } else {
                    return evaluate(a.op2.op2);
                }
            }
        }
        return nu.ll;
    }

    static boolean containsOneOfTheFeatures(Node a, Set<String> features) {
        if (a.function != null) {
            return a.function.containsOneOfTheFeatures(a.functionParams, features);
        }

        if (a.opType == null) {
            return a.var == null && features.contains(a.varName);
        } else {
            return a.op1 != null && containsOneOfTheFeatures(a.op1, features) || a.op2 != null && containsOneOfTheFeatures(a.op2, features);
        }
    }

    static boolean containsFunction(Node a, String funcName) {
        if (a.function != null) {
            return new String(a.function.getName()).equals(funcName) || a.function.argsContainFunction(a.functionParams, funcName);
        }

        return a.op1 != null && containsFunction(a.op1, funcName) || a.op2 != null && containsFunction(a.op2, funcName);
    }

    protected static void resetBindings(Node a,
                                        HashMap<String, ? extends ScapeVariable> variables) {
        if (a.function != null) {
            a.function.resetBindings(a.functionParams, variables);
            return;
        }

        if (a.opType == null) {
            if (!a.varName.startsWith("\"")) {
                a.var = null;
                a.var = variables.get(a.varName);
            }
        } else {
            if (a.op1 != null) {
                resetBindings(a.op1, variables);
            }
            if (a.op2 != null) {
                resetBindings(a.op2, variables);
            }
        }
    }

    public State getStartState() {
        return root;
    }

    private Node findDoubleQuestion(Node a) {
        Node b;
        if (a == null)
            return null;

        if ((b = findDoubleQuestion(a.op2)) != null) {
            return b;
        } else if (a.opType == LT_QUESTION && (a.op2 == null || a.op2.opType != LT_DDOT)) {
            return a;
        } else {
            return null;
        }
    }

    private int readConditionOperand(char[] s, int pl, int endpl) throws ParseException {
        int beg, end;
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] == '(') {
            pl++;
            return _readIn(s, pl, endpl, bracket);
        } else {
            Node root;
            root = new Node();
            rootStack.push(root);
            int n;
            if ((n = sut.checkDelims(s, pl, endpl, opNames)) != -1) {
                if (operations[n].numargs == 1) {
                    root.opType = operations[n];
                    root.pl = pl;
                    return pl - 1 + opNames[n].length;
                }
            }

            for (ScapeExpressionFunction fun : functions) {
                Node[] funParams = new Node[fun.getParamsNumber()];
                int p = fun.readIn(s, pl, endpl, funParams);
                if (p >= 0) {
                    root.varName = sut.extractString(s, pl, p - pl);
                    root.functionParams = funParams;
                    root.function = fun;
                    root.pl = pl;
                    return p - 1;
                }
            }

            beg = pl;
            pl = sut.skipVarValue(s, pl, endpl);
            end = pl;
            if (end == beg) {
                throw new ParseException("missing function, string or variable name", null, s, pl, endpl);
            }
            root.varName = sut.extractString(s, beg, end - beg);
            root.pl = beg;
            return pl - 1;
        }
    }

    public int readConditionOperator(char[] s, int pl, int endpl) throws ParseException {
        Node root;
        root = new Node();
        rootStack.push(root);
        int n;
        if ((n = sut.checkDelims(s, pl, endpl, opNames)) != -1) {
            if (operations[n].numargs != 1) {
                root.opType = operations[n];
                root.pl = pl;
                return pl + opNames[n].length;
            }
        }
        throw new ParseException("missing operator", null, s, pl, endpl);
    }

    public int readIn(char[] s, int pl, int endpl, char[][] finalDelims) throws ParseException {
        pl = _readIn(s, pl, endpl, finalDelims);
        root = (Node) rootStack.pop();
        return pl;
    }

    int _readIn(char[] s, int pl, int endpl, char[][] finalDelims) throws ParseException {
        Node oper, root, arg, a, b, notFullNode;
        int prOper;
        int nQuestions = 0;

        root = null;
        notFullNode = null;

        pl = sut.skipSpacesEndls(s, pl, endpl);
        while (true) {
            pl = readConditionOperand(s, pl, endpl);
            arg = (Node) rootStack.pop();
            if (arg.opType != null && arg.opType.numargs == 1) { //Unary operation was read
                pl++;
                if (notFullNode != null) {
                    notFullNode.op2 = arg;
                } else {
                    root = arg;
                }
                notFullNode = arg;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                continue;
            }
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            if (pl > endpl) {
                throw new ParseException("uncomplete expression", null, s, pl, endpl);
            }
            if (sut.checkDelims(s, pl, endpl, finalDelims) != -1) {
                if (nQuestions == 0) {
                    if (root != null) {
                        notFullNode.op2 = arg;
                    } else {
                        root = arg;
                    }
                    rootStack.push(root);
                    return pl;
                } else {
                    throw new ParseException("there is unclosed \'?\' symbol", null, s, pl, endpl);
                }
            }
            if (s[pl] == L_INDEXBRACKET)       // indices  a[5-2]
            {
                pl = readIndex(s, pl, endpl);
                b = (Node) rootStack.pop();
                a = arg;
                arg = new Node();
                arg.op1 = a;
                arg.op2 = b;
                arg.opType = LT_INDEX;
                pl++;
            }
            if (sut.checkDelims(s, pl, endpl, finalDelims) != -1) {
                if (nQuestions == 0) {
                    if (root != null) {
                        notFullNode.op2 = arg;
                    } else {
                        root = arg;
                    }
                    rootStack.push(root);
                    return pl;
                } else {
                    throw new ParseException("there is unclosed \'?\' symbol", null, s, pl, endpl);
                }
            }
            pl = readConditionOperator(s, pl, endpl);
            oper = (Node) rootStack.pop();
            if (root == null) {
                root = oper;
                root.op1 = arg;
                notFullNode = root;
                if (oper.opType == LT_QUESTION)
                    nQuestions++;
            } else {
                a = null;
                b = root;
                if (oper.opType == LT_QUESTION) {
                    nQuestions++;
                    while (b != null && (b.opType == LT_QUESTION || b.opType == LT_DDOT)) {
                        a = b;
                        b = b.op2;
                    }
                } else if (oper.opType == LT_DDOT) {
                    a = findDoubleQuestion(root);
                    if (a != null) {
                        b = a.op2;
                    } else {
                        throw new ParseException("unbalanced \':\' symbol", null, s, pl, endpl);
                    }
                    nQuestions--;
                } else {
                    prOper = oper.opType.priority;
                    while (b != null && b.opType.priority < prOper) {
                        a = b;
                        b = b.op2;
                    }
                    if (oper.opType.direction == LTdir_R) {
                        while (b != null && b.opType == oper.opType) {
                            a = b;
                            b = b.op2;
                        }
                    }
                }
                if (b != null) {
                    oper.op1 = b;
                    if (a == null) {
                        root = oper;
                    } else {
                        if (a.opType == oper.opType && oper.opType.direction == LTdir_DENIED) {
                            throw new ParseException("wrong sequence of operators", null, s, pl, endpl);
                        }
                        a.op2 = oper;
                    }
                    notFullNode.op2 = arg;
                } else {
                    if (a.opType == oper.opType && oper.opType.direction == LTdir_DENIED) {
                        throw new ParseException("wrong sequence of operators", null, s, pl, endpl);
                    }
                    a.op2 = oper;
                    oper.op1 = arg;
                }
                notFullNode = oper;
            }
            pl = sut.skipSpacesEndls(s, pl, endpl);
        }
    }

    public void validateExpression(char[] s, int endpl,
                                   HashMap<String, ? extends ScapeVariable> vars,
                                   HashMap<String, RegexpVariable> regexpVars
    ) throws ParseException {
        validateExpression(s, endpl, root, vars, regexpVars, true);
    }

    public Object evaluate() {
        return evaluate(root);
    }

    public boolean containsOneOfTheFeatures(Set<String> features) {
        return containsOneOfTheFeatures(root, features);
    }

    public boolean containsFunction(String funcName) {
        return containsFunction(root, funcName);
    }

    private int readIndex(char[] s, int pl, int endpl) throws ParseException {

        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] == L_INDEXBRACKET) {
            pl++;
            return _readIn(s, pl, endpl, indexBracket);
        }
        return -1;
    }

    public void resetBindings(HashMap<String, ? extends ScapeVariable> vars) {
        resetBindings(root, vars);
    }

    public void addRelevantFeaturesForType(TrnType tp, HashSet<Integer> set) {
        Set<String> tset = new HashSet<String>();
        try {
            for (String s : tp.getFeatureNames()) {
                tset.clear();
                tset.add(s);
                if (containsOneOfTheFeatures(tset)) {
                    try {
                        set.add(tp.getFeatureIndex(s));
                    } catch (TreetonModelException e) {
                        //do nothing
                    }
                }
            }
        } catch (TreetonModelException e) {
            //do nothing
        }
    }

    public Iterator findPattern(BinaryLogicPattern pattern) {
        return null;
    }

    private Collection<BinaryRelation> findExactPatterns(Node nd, BinaryLogicPattern pattern) {
        List<BinaryRelation> result = new ArrayList<BinaryRelation>();
        if (nd.opType == LT_AND) {
            Collection<BinaryRelation> leftCollection = findExactPatterns(nd.op1, pattern);
            if (leftCollection != null) {
                result.addAll(leftCollection);
            }
            Collection<BinaryRelation> rightCollection = findExactPatterns(nd.op2, pattern);
            if (rightCollection != null) {
                result.addAll(rightCollection);
            }
        } else if (match(nd, pattern)) {

        }
        return result;
    }

    private boolean match(Node nd, BinaryLogicPattern pattern) {
        return false;  //TODO
    }

    public static class OperatorType {
        String name;
        int direction;
        int numargs;
        int priority;

        public OperatorType(String name, int direction, int numargs, int priority) {
            this.direction = direction;
            this.name = name;
            this.numargs = numargs;
            this.priority = priority;
        }
    }

    class Node implements State {
        int id;


        OperatorType opType;


        Class valueType;
        Object value;

        String varName;
        int pl;
        Object var;
        Node op1, op2;
        ScapeExpressionFunction function = null;
        Node[] functionParams = null;

        Node() {
            opType = null;
            id = ID++;

            valueType = null;
            value = null;
            varName = null;
            var = null;
            op1 = op2 = null;
            pl = -1;
        }

        public Iterator<TermStatePair> pairsIterator() {
            return new ArgsIterator();
        }

        public int getNumberOfPairs() {
            int n = 0;
            if (op1 != null) {
                n++;
            }
            if (op2 != null) {
                n++;
            }
            return n;
        }

        public int getId() {
            return id;
        }

        public boolean isFinal() {
            return false;
        }

        public String getString() {
            if (opType == null) {
                return varName;
            } else {
                return opType.name;
            }
        }

        private class PairImpl implements TermStatePair {
            State s;

            PairImpl(State s) {
                this.s = s;
            }

            public Term getTerm() {
                return CharTerm.empty;
            }

            public State getState() {
                return s;
            }

            public String getString() {
                return "";
            }
        }

        private class ArgsIterator implements Iterator<TermStatePair> {
            int n;
            boolean firstPassed;
            boolean secondPassed;

            ArgsIterator() {
                n = getNumberOfPairs();
                firstPassed = secondPassed = false;
            }

            public void remove() {
            }

            public boolean hasNext() {
                return n > 0;
            }

            public TermStatePair next() {
                if (op1 != null && !firstPassed) {
                    n--;
                    firstPassed = true;
                    return new PairImpl(op1);
                }
                if (op2 != null && !secondPassed) {
                    n--;
                    secondPassed = true;
                    return new PairImpl(op2);
                }
                return null;
            }
        }
    }
}
