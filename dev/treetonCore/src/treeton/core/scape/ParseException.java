/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

public class ParseException extends Exception {
    boolean noLineInMessage;
    int sShift;
    private String s;
    private String fName;
    private byte srcString[];
    private char srcStringChars[];
    private int pl;
    private int endpl;

    public ParseException(String message, String src, int pl) {
        this.s = message;
        this.fName = null;
        this.srcString = null;
        this.srcStringChars = src.toCharArray();
        this.pl = pl;
        this.endpl = src.length() - 1;
        noLineInMessage = false;
    }

    public ParseException(String s, String fName, byte srcString[], int pl, int endpl) {
        this.s = s;
        this.fName = fName;
        this.srcString = srcString;
        this.srcStringChars = null;
        this.pl = pl;
        this.endpl = endpl;
        noLineInMessage = false;
    }

    public ParseException(String s, String fName, char srcString[], int pl, int endpl) {
        this.s = s;
        this.fName = fName;
        this.srcString = null;
        this.srcStringChars = srcString;
        this.pl = pl;
        this.endpl = endpl;
    }

    public void setFileName(String fName) {
        this.fName = fName;
    }

    public void setNoLineInMessage(boolean noLineInMessage) {
        this.noLineInMessage = noLineInMessage;
    }

    public void setSymbolShift(int shift) {
        sShift = shift;
    }

    public String getMessage() {
        String errstr;

        if (srcString != null) {
            int i = 0;
            int nl, ns;
            nl = 1;
            ns = 1 + sShift;
            while (i <= endpl && i < pl) {
                if (srcString[i] == '\n') {
                    nl++;
                    ns = 1;
                } else {
                    ns++;
                }
                i++;
            }
            errstr = (fName != null ? "File " + fName + ": " : "") + "Error at symbol " + ns + (noLineInMessage ? "" : " at line " + nl) + ": ";
        } else if (srcStringChars != null) {
            int i = 0;
            int nl, ns;
            nl = 1;
            ns = sShift + 1;
            while (i <= endpl && i < pl) {
                if (srcStringChars[i] == '\n') {
                    nl++;
                    ns = 1;
                } else {
                    ns++;
                }
                i++;
            }
            errstr = (fName != null ? "File " + fName + ": " : "") + "Error at symbol " + ns + (noLineInMessage ? "" : " at line " + nl) + ": ";
        } else {
            errstr = (fName != null ? "File " + fName + ": " : "") + "Error: ";
        }
        errstr += s;

        return errstr;
    }

    public String getSource() {
        return new String(srcStringChars);
    }
}
