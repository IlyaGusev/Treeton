/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

public enum SyllablePatternType {
    STRESSED ("s"),
    UNSTRESSED ("u"),
    OBLIGATORY_STRESSED ("S"),
    OBLIGATORY_UNSTRESSED ("U");

    private String shortName;

    SyllablePatternType(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }

    @Override
    public String toString() {
        return getShortName();
    }
}
