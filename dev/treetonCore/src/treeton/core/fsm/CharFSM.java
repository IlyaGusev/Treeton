/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import treeton.core.TString;

public interface CharFSM {
    public int changeValue(String s, int newValue);

    public int changeValue(TString s, int newValue);

    public int get(String s);

    public int get(TString s);

    public int addString(String s);

    public int addString(TString s);

    public int getSize();

    public char[] getCharRepresentation();

    public int readInFromChars(char[] arr, int from);
}
