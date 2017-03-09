/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;

import java.util.*;

public class TokenImpl extends TreenotationImpl implements Token {
    int endNumerator;
    int endDenominator;
    TokenImpl next;
    TokenImpl previous;
    IntFeatureMapImpl parents;
    TreenotationStorageImpl storage;
    String text;
    private int startNumerator;
    private int startDenominator;

    TokenImpl(int _startNumerator, int _startDenominator,
              int _endNumerator, int _endDenominator, TrnType type, BlackBoard board) {
        super(null, null, type, board);
        startNumerator = _startNumerator;
        startDenominator = _startDenominator;
        endNumerator = _endNumerator;
        endDenominator = _endDenominator;
        next = null;
        previous = null;
        parents = new IntFeatureMapImpl();
    }

    TokenImpl(Token previous, int lengthNumerator, int lengthDenominator, TrnType type, BlackBoard board) {
        super(null, null, type, board);
        startNumerator = ((TokenImpl) previous).endNumerator;
        startDenominator = ((TokenImpl) previous).endDenominator;
        endNumerator = startNumerator * lengthDenominator + startDenominator * lengthNumerator;
        endDenominator = startDenominator * lengthDenominator;
        simplifyFractions();

        next = null;
        this.previous = null;
        parents = new IntFeatureMapImpl();
    }

    public TreenotationStorage getStorage() {
        return storage;
    }

    void simplifyFractions() {
        int min = Math.min(startNumerator, startDenominator);
        for (int i = 2; i < min; i++) {
            if (startNumerator % i == 0 && startDenominator % i == 0) {
                startNumerator /= i;
                startDenominator /= i;
            }
        }
        min = Math.min(endNumerator, endDenominator);
        for (int i = 2; i < min; i++) {
            if (endNumerator % i == 0 && endDenominator % i == 0) {
                endNumerator /= i;
                endDenominator /= i;
            }
        }
    }

    protected void addParent(TreenotationImpl trn) {
        TreenotationImpl[] pars;
        try {
            pars = (TreenotationImpl[]) parents.get(trn.getType().getIndex());
        } catch (TreetonModelException e) {
            pars = null;
        }
        if (pars == null) {
            pars = new TreenotationImpl[1];
            pars[0] = trn;
        } else {
            for (int i = 0; i < pars.length; i++) {
                if (pars[i] == null) {
                    pars[i] = trn;
                    return;
                }
                if (pars[i] == trn) {
                    return;
                }
            }
            TreenotationImpl[] t = new TreenotationImpl[pars.length + 1];
            System.arraycopy(pars, 0, t, 0, pars.length);
            t[pars.length] = trn;
            pars = t;
        }
        try {
            parents.put(trn.getType().getIndex(), pars);
        } catch (TreetonModelException e) {
            //do nothing
        }
    }

    public int compareTo(Token anotherToken) {
        int ad = this.startNumerator * ((TokenImpl) anotherToken).startDenominator;
        int bc = this.startDenominator * ((TokenImpl) anotherToken).startNumerator;
        return (ad < bc ? -1 : (ad == bc ? 0 : 1));
    }

    public int compareTo(Fraction f) {
        int ad = this.startNumerator * f.denominator;
        int bc = this.startDenominator * f.numerator;
        return (ad < bc ? -1 : (ad == bc ? 0 : 1));
    }

    public int compareTo(double f) {
        double s = (double) startNumerator / (double) startDenominator;
        return (s < f ? -1 : s == f ? 0 : 1);
    }

    public int compareTo(Object o) {
        return compareTo((TokenImpl) o);
    }

    public String toString() {
        return new StringBuffer().append("( ").append(startNumerator).append("/").append(startDenominator).append(" : ").append(getString()).append(" : ").append(endNumerator).append("/").append(endDenominator).append(" )").toString();
    }

    public boolean isChildOf(Treenotation trn) {
        if (trn == null)
            return false;
        if (parents == null)
            return false;

        TreenotationImpl[] arr;
        try {
            arr = (TreenotationImpl[]) parents.get(trn.getType().getIndex());
        } catch (TreetonModelException e) {
            arr = null;
        }

        if (arr == null)
            return false;

        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == trn)
                return true;
        }

        return false;
    }

    public boolean corrupted(TrnType type) {
        TreenotationImpl[] par;
        try {
            par = (TreenotationImpl[]) parents.get(type.getIndex());
        } catch (TreetonModelException e) {
            par = null;
        }

        if (par == null) {
            return false;
        }

        for (int i = 0; i < par.length; i++) {
            if (par[i] != null) {
                for (int j = i + 1; j < par.length; j++) {
                    if (par[j] != null && par[i] == par[j])
                        return true;
                }
            }
        }

        return false;
    }

    public Token getStartToken() {
        return this;
    }

    public Token getEndToken() {
        return this;
    }

    public Token getNextToken() {
        return next;
    }

    protected void setNextToken(TokenImpl tok) {
        next = tok;
    }

    public Token getPreviousToken() {
        return previous;
    }

    protected void setPreviousToken(TokenImpl tok) {
        previous = tok;
    }

    public TreenotationImpl[] getParent(TrnType tp) {
        try {
            return (TreenotationImpl[]) parents.get(tp.getIndex());
        } catch (TreetonModelException e) {
            return null;
        }
    }

    public Collection<TreenotationImpl> listTrnsEndingWithThis() {
        Set<TreenotationImpl> result = new HashSet<TreenotationImpl>();
        if (parents == null) {
            return result;
        }

        Stack<TreenotationImpl> pars = new Stack<TreenotationImpl>();
        int sz = parents.size();
        for (int i = 0; i < sz; i++) {
            TreenotationImpl[] pp = (TreenotationImpl[]) parents.getByIndex(i);
            if (pp != null) {
                for (TreenotationImpl par : pp) {
                    if (par.getEndToken().equals(this)) {
                        pars.push(par);
                    }
                }
            }
        }

        while (!pars.isEmpty()) {
            TreenotationImpl trn = pars.pop();

            if (result.add(trn)) {
                TreenotationImpl[] pp = trn.getParent();
                if (pp != null) {
                    for (TreenotationImpl par : pp) {
                        if (par.getEndToken().equals(this)) {
                            pars.push(par);
                        }
                    }
                }
            }
        }
        return result;
    }

    public Collection<TreenotationImpl> listTrnsStartingWithThis() {
        Set<TreenotationImpl> result = new HashSet<TreenotationImpl>();
        if (parents == null) {
            return result;
        }

        Stack<TreenotationImpl> pars = new Stack<TreenotationImpl>();
        int sz = parents.size();
        for (int i = 0; i < sz; i++) {
            TreenotationImpl[] pp = (TreenotationImpl[]) parents.getByIndex(i);
            if (pp != null) {
                for (TreenotationImpl par : pp) {
                    if (par.getStartToken().equals(this)) {
                        pars.push(par);
                    }
                }
            }
        }

        while (!pars.isEmpty()) {
            TreenotationImpl trn = pars.pop();

            if (result.add(trn)) {
                TreenotationImpl[] pp = trn.getParent();
                if (pp != null) {
                    for (TreenotationImpl par : pp) {
                        if (par.getStartToken().equals(this)) {
                            pars.push(par);
                        }
                    }
                }
            }
        }
        return result;
    }

    public Collection<TreenotationImpl> listMinimalTrnsCoveringThis() {
        Set<TreenotationImpl> result = new HashSet<TreenotationImpl>();
        if (parents == null) {
            return result;
        }

        int sz = parents.size();
        for (int i = 0; i < sz; i++) {
            TreenotationImpl[] pp = (TreenotationImpl[]) parents.getByIndex(i);
            if (pp != null) {
                result.addAll(Arrays.asList(pp));
            }
        }

        return result;
    }

    public boolean hasParentOfType(TrnType tp) {
        TreenotationImpl[] tokpars = getParent(tp);
        if (tokpars != null) {
            for (int i = 0; i < tokpars.length; i++) {
                if (tokpars[i] != null)
                    return true;
            }

        }
        return false;
    }

    public String getText() {
        return text;
    }

    void setText(String text) {
        this.text = text;
    }

    public int getStartNumerator() {
        return startNumerator;
    }

    protected void setStartNumerator(int n) {
        startNumerator = n;
    }

    public int getStartDenominator() {
        return startDenominator;
    }

    protected void setStartDenominator(int d) {
        startDenominator = d;
    }

    public int getEndNumerator() {
        return endNumerator;
    }

    protected void setEndNumerator(int n) {
        endNumerator = n;
    }

    public int getEndDenominator() {
        return endDenominator;
    }

    protected void setEndDenominator(int d) {
        endDenominator = d;
    }

    Iterator parentsIterator() {
        return parents.valueIterator();
    }

    public double toDouble() {
        return (double) startNumerator / (double) startDenominator;
    }

    public double endToDouble() {
        return (double) endNumerator / (double) endDenominator;
    }

    public int toInt() {
        return startNumerator / startDenominator;
    }

    public int endToInt() {
        return endNumerator / endDenominator;
    }

}
