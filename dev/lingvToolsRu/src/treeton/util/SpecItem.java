/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

import java.util.Arrays;
import java.util.regex.Matcher;

public class SpecItem {
    public boolean isCompound;
    public int[] parts;
    public String val;
    public String pVals[];

    public SpecItem(String entry, String value) {
        Matcher matcher = SpecialWords.compoundPattern.matcher(entry);
        int[] p = SpecialWords.getStringPartsLens(
                entry, SpecialWords.compoundDelimeter);
        isCompound = p.length > 1;
        if (isCompound) {
            parts = p;
            pVals = new String[parts.length];
            Arrays.fill(pVals, null);
            int figOpen = value.indexOf(SpecialWords.compoundPartsBegin);
            int figClose = -1;
            if (figOpen >= 0) {
                figClose = value.indexOf(SpecialWords.compoundPartsEnd, figOpen);
            }
            if (figOpen >= 0) {
                this.val = SpecialWords.getAttribsString(
                        value.substring(0, figOpen).trim());
                if (figClose < 0) {
                    figClose = value.length();
                }
                String sVals = value.substring(figOpen + 1, figClose).trim();
                String[] tVals = SpecialWords.getStringParts(
                        sVals, SpecialWords.compoundDelimeter);
                int n = Math.min(tVals.length, pVals.length);
                for (int i = 0; i < n; i++) {
                    String tv = tVals[i].trim();
                    pVals[i] = tv.length() > 0 ?
                            SpecialWords.getAttribsString(tv) : null;
                }
            } else {
                this.val = SpecialWords.getAttribsString(value.trim());
            }
        } else {
            this.val = SpecialWords.getAttribsString(value);
        }
    }
}
