/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.musimatix;

import treeton.prosody.SyllableInfo;

import java.util.ArrayList;
import java.util.Iterator;

public class StressDescription implements Iterable<SyllableInfo> {
    public StressDescription(ArrayList<SyllableInfo> syllables ) {
        this.syllables = syllables;
    }

    public final ArrayList<SyllableInfo> syllables;

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( "Syllables: ");
        for (SyllableInfo syllable : syllables) {
            buf.append( "(" ).append( syllable.startOffset ).append( "[").append( syllable.length ).append("] ");
            buf.append(syllable.stressStatus.name());
            buf.append( "), " );
        }
        return buf.toString();
    }

    @Override
    public Iterator<SyllableInfo> iterator() {
        return syllables.iterator();
    }
}
