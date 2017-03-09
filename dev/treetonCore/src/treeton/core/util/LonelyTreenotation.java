/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util;

import treeton.core.*;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;

import java.util.HashMap;

public class LonelyTreenotation extends IntFeatureMapImpl implements Treenotation {
    Token start;
    Token end;
    TrnType tp;
    String text;
    private boolean locked = false;

    public LonelyTreenotation(Token start, Token end, TrnType tp, String text) {
        this.start = start;
        this.end = end;
        this.tp = tp;
        this.text = text;
    }

    public LonelyTreenotation(int _blockSize, Token start, Token end, TrnType tp, String text) {
        super(_blockSize);
        this.start = start;
        this.end = end;
        this.tp = tp;
        this.text = text;
    }

    public LonelyTreenotation(BlackBoard board, Token start, Token end, TrnType tp, String text) {
        super(board);
        this.start = start;
        this.end = end;
        this.tp = tp;
        this.text = text;
    }

    public LonelyTreenotation(BlackBoard board, int _blockSize, Token start, Token end, TrnType tp, String text) {
        super(board, _blockSize);
        this.start = start;
        this.end = end;
        this.tp = tp;
        this.text = text;
    }

    public TrnType getType() {
        return tp;

    }

    public boolean insideOf(Treenotation trn) {
        return false;
    }

    public boolean intersects(Treenotation trn) {
        return false;
    }

    public int getStartNumerator() {
        return 0;

    }

    public int getStartDenominator() {
        return 0;

    }

    public int getEndNumerator() {
        return 0;

    }

    public int getEndDenominator() {
        return 0;

    }

    public Token getStartToken() {
        return start;
    }

    public Token getEndToken() {
        return end;

    }

    public boolean isChildOf(Treenotation trn) {
        return false;

    }

    public boolean corrupted() {
        return false;

    }

    public double toDouble() {
        return 0;
    }

    public double endToDouble() {
        return 0;
    }

    public int toInt() {
        return 0;
    }

    public int endToInt() {
        return 0;

    }

    public Object get(String featureName) {
        int fn;
        try {
            fn = tp.getFeatureIndex(featureName);
        } catch (TreetonModelException e) {
            return null;
        }
        if (fn == -1)
            return null;
        return this.get(fn);
    }

    public void put(String featureName, Object value) {
        int fn;
        try {
            fn = tp.getFeatureIndex(featureName);
        } catch (TreetonModelException e) {
            return;
        }
        if (fn == -1)
            return;
        put(fn, value);
    }

    public String getString() {
        return null;

    }

    public String getHtmlString() {
        return null;

    }

    public String getText() {
        if (text != null) {
            return text;
        }

        if (start == null) {
            return null;
        }

        if (start == end) {
            return start.getText();
        }

        StringBuffer sb = new StringBuffer();


        Token cur = start;
        while (cur != end) {
            sb.append(cur.getText());
            cur = cur.getNextToken();
        }
        sb.append(cur.getText());

        return sb.toString();
    }

    public void setText(String s) {
        text = s;
    }

    public void appendTrnStringView(StringBuffer buf) {
    }

    public int readInFromStringView(TrnTypeStorage types, char[] view, int pl) {
        return 0;
    }

    public int getTokenLength() {
        return 0;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isEmpty() {
        return true;
    }

    public Object getContext() {
        return null;
    }

    public boolean contains(Treenotation trn) {
        return false;
    }

    public Treenotation getCopy(HashMap<Treenotation, Treenotation> old2new, boolean cloneLocked) {
        return null;
    }

    public boolean isAdded() {
        return false;
    }

    public long getId() {
        return 0;
    }

    public TreenotationStorage getStorage() {
        return null;
    }

    public String getUri() {
        return null;
    }

    public void setTp(TrnType tp) {
        this.tp = tp;
    }
}
