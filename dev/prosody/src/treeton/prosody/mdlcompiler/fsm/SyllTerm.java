/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.prosody.mdlcompiler.api.custom.Term;

public class SyllTerm implements Term {
    SyllablePatternType type;

    public SyllTerm(SyllablePatternType type) {
        this.type = type;
    }

    public SyllablePatternType getType() {
        return type;
    }

    Object meta;

    public void setMetaInfo(Object o) {
        meta = o;
    }

    public Object getMetaInfo() {
        return meta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyllTerm syllTerm = (SyllTerm) o;

        return type == syllTerm.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type.getShortName();
    }

    boolean match ( boolean isStressed ) {
        if ( type == SyllablePatternType.STRESSED || type == SyllablePatternType.OBLIGATORY_STRESSED ) {
            return isStressed;
        } else {
            return !isStressed;
        }
    }
}
