/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import treeton.core.IntFeatureMapImpl;
import treeton.core.Recoder;
import treeton.core.util.NumeratedObject;

import java.awt.*;
import java.util.Iterator;

public class CharFSMState extends IntFeatureMapImpl implements State {
    public static Font font = new Font("Courier", 0, 12);
    public static Font numbersFont = new Font("Times New Roman", Font.BOLD, 20);

    protected int index;
    protected int value;
    protected char[] appendix;
    protected CharFSMState nextInStateList;
    protected Recoder recoder;
    protected boolean isFinal;

    public CharFSMState(Recoder _recoder) {
        super();
        index = -1;
        value = -1;
        nextInStateList = null;
        appendix = null;
        isFinal = false;
        recoder = _recoder;
    }

    public CharFSMState(Recoder _recoder, int blockSize) {
        super(blockSize);
        index = -1;
        value = -1;
        nextInStateList = null;
        appendix = null;
        isFinal = false;
        recoder = _recoder;
    }


    public State get(Term t) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TermStatePair put(Term t, State s) {
        return null;
    }

    public Iterator followingStatesIterator() {
        return valueIterator();
    }

    public void addCharArray(char[] tmpArr, int from, int length) {
        if (length != from) {
            appendix = new char[length - from];
            System.arraycopy(tmpArr, from, appendix, 0, length - from);
        }
        finalizeState();
    }

    public Iterator<TermStatePair> pairsIterator() {
        return new PairsIterator();
    }

    public int getNumberOfPairs() {
        return size();
    }

    public int getId() {
        return index;
    }

    public boolean isFinal() {
        return isFinal;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void finalizeState() {
        isFinal = true;
    }

    public String getString() {
        return "value: " + value + (appendix != null ? "; appendix <b>" + new String(appendix) + "</b>" : "") + (isFinal ? "; final" : "");
    }

    protected void setIndex(int _index) {
        index = _index;
    }

    private class PairsIterator implements Iterator<TermStatePair> {
        Iterator it;

        PairsIterator() {
            it = numeratedObjectIterator();
        }

        public void remove() {
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public TermStatePair next() {
            NumeratedObject no = (NumeratedObject) it.next();
            return new CharFSMPair(new CharTerm(recoder.getSymbolByNumber(no.n)), (CharFSMState) no.o);
        }
    }
}
