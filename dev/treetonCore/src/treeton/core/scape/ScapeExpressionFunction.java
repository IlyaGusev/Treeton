/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

import treeton.core.util.nu;
import treeton.core.util.sut;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public abstract class ScapeExpressionFunction<T> {
    protected char L_PAR = '(';
    protected char R_PAR = ')';
    protected char SEPARATOR = ',';
    ScapeExpressionFunction clonedFrom = null;
    char[][] fDelims;
    private Class[] _paramsClasses;
    private Class resultClass;
    private char[] name;

    public ScapeExpressionFunction(String name, Class resultClass, Class[] paramsClasses) {
        this.resultClass = resultClass;
        fDelims = new char[][]{new char[]{SEPARATOR}, new char[]{R_PAR}};
        this.name = name.toCharArray();
        _paramsClasses = paramsClasses;
    }

    public int getParamsNumber() {
        return _paramsClasses == null ? -1 : _paramsClasses.length;
    }

    public Class getResultClass() {
        return resultClass;
    }

    public char[] getName() {
        return name;
    }

    protected abstract T implementation(AbstractList params);

    public Class validate(char[] s, int endpl,
                          ScapeExpression.Node[] params,
                          HashMap<String, ? extends ScapeVariable> variables,
                          HashMap<String, RegexpVariable> regexpVars,
                          boolean regexVar) throws ParseException {

        for (int i = 0; i < _paramsClasses.length; i++) {
            ScapeExpression.Node root = params[i];
            if (root == null)
                throw new ParseException("null parameter at " + Integer.toString(i) + " " + this.toString(), null, s, 0, endpl);
            Class result = ScapeExpression.validateExpression(s, endpl, root, variables, regexpVars, _paramsClasses[i] == RegexpVariable.class);
            if (result != null && result != _paramsClasses[i])
                throw new ParseException("type mismatch at " + Integer.toString(i) + " " + this.toString(), null, s, root.pl, endpl);
        }
        return resultClass;
    }

    public T evaluate(ScapeExpression.Node[] params) {
        ArrayList<Object> par = new ArrayList<Object>();
        for (int i = 0; i < _paramsClasses.length; i++) {
            ScapeExpression.Node root = params[i];
            if (root == null)
                throw new RuntimeException("null parameter at " + Integer.toString(i) + " " + this.toString());
            Object p = ScapeExpression.evaluate(root);
            if (p != nu.ll && !_paramsClasses[i].isInstance(p))
                throw new RuntimeException("type mismatch at " + Integer.toString(i) + " " + this.toString());
            par.add(p == nu.ll ? null : p);
        }
        return implementation(par);
    }

    public int readIn(char[] s, int pl, int endpl, ScapeExpression.Node[] params) throws ParseException {
        if (endpl - pl - 2 < name.length) return -1;
        for (int i = 0; i < name.length; i++) {
            if (s[pl + i] != name[i]) return -1;
        }
        pl += name.length;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        if (s[pl] != L_PAR) return -1;
        pl++;
        pl = sut.skipSpacesEndls(s, pl, endpl);

        for (int paramNum = 0; paramNum < _paramsClasses.length; paramNum++) {
            if (pl >= endpl)
                return -1;  // throw new ParseException("Function "+this.toString()+" must have " +_paramsClasses.length + " parameter" + (_paramsClasses.length > 1 ? "s" : ""),null,s,pl,endpl);
            ScapeExpression lt = new ScapeExpression();
            int p = lt.readIn(s, pl, endpl, fDelims);
            if (p < 0)
                return -1;  //throw new ParseException("Function "+this.toString()+" must have " +_paramsClasses.length + " parameter" + (_paramsClasses.length > 1 ? "s" : ""),null,s,pl,endpl);
            params[paramNum] = lt.root;
            pl = p;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            if (paramNum < _paramsClasses.length - 1) {
                if (s[pl] != SEPARATOR)
                    return -1;  //throw new ParseException("Function "+this.toString()+" : "+SEPARATOR+" expected",null,s,pl,endpl);
                pl++;
                pl = sut.skipSpacesEndls(s, pl, endpl);
            }
        }
        if (s[pl] != R_PAR)
            return -1;  //throw new ParseException("Function "+this.toString()+" : "+R_PAR+" expected",null,s,pl,endpl);
        pl++;
        return pl;
    }

    public String toString() {
        String res = new String(name) + L_PAR;

        for (int i = 0; i < _paramsClasses.length; i++) {
            res += _paramsClasses[i].getName();
            if (i < _paramsClasses.length - 1) res += SEPARATOR;
        }
        res += R_PAR;
        return res;
    }

    public void resetBindings(ScapeExpression.Node[] params, HashMap<String, ? extends ScapeVariable> variables) {
        for (int i = 0; i < _paramsClasses.length; i++) {
            ScapeExpression.Node root = params[i];
            ScapeExpression.resetBindings(root, variables);
        }
    }

    public boolean containsOneOfTheFeatures(ScapeExpression.Node[] params, Set<String> features) {
        for (int i = 0; i < _paramsClasses.length; i++) {
            ScapeExpression.Node root = params[i];
            if (root == null)
                throw new RuntimeException("null parameter at " + Integer.toString(i) + " " + this.toString());
            if (ScapeExpression.containsOneOfTheFeatures(root, features))
                return true;
        }
        return false;
    }

    public boolean argsContainFunction(ScapeExpression.Node[] params, String funcName) {
        for (int i = 0; i < _paramsClasses.length; i++) {
            ScapeExpression.Node root = params[i];
            if (root == null)
                throw new RuntimeException("null parameter at " + Integer.toString(i) + " " + this.toString());
            if (ScapeExpression.containsFunction(root, funcName))
                return true;
        }
        return false;
    }

}
