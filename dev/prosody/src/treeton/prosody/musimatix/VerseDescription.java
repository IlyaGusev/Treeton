/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.musimatix;

import treeton.prosody.SyllableInfo;

import java.util.ArrayList;
import java.util.Vector;

public class VerseDescription {
    public VerseDescription( Vector<Double> metricVector, ArrayList<SyllableInfo> syllables, int fragmentId ) {
        this.metricVector = metricVector;
        this.syllables = new StressDescription(syllables);
        this.fragmentId = fragmentId;
    }

    public final Vector<Double> metricVector;
    public final StressDescription syllables;
    public final int fragmentId;

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append( "Metric vector: (");
        if( metricVector == null ) {
            buf.append("null");
        } else {
            for (Double aDouble : metricVector) {
                if (aDouble == null) {
                    buf.append("0");
                } else {
                    buf.append(String.format("%.2f", aDouble.floatValue()));
                }
                buf.append(";");
            }
        }
        buf.append( ")\n" );
        buf.append( "Syllables: ");
        for (SyllableInfo syllable : syllables) {
            buf.append( "(" ).append( syllable.startOffset ).append( "[").append( syllable.length ).append("] ");
            buf.append(syllable.stressStatus.name());
            buf.append( "), " );
        }
        buf.append("\nfragment id: ").append(fragmentId);
        return buf.toString();
    }

    public String formatVerse(String verse) {
        StringBuilder sb = new StringBuilder();

        for (SyllableInfo syllableInfo : syllables) {
            if( sb.length() > 0 ) {
                sb.append("-");
            }
            sb.append(verse.substring(syllableInfo.startOffset,syllableInfo.startOffset+syllableInfo.length));

            switch ( syllableInfo.stressStatus ) {
                case UNSTRESSED:
                    break;
                case STRESSED:
                    sb.append("'");
                    break;
                case AMBIGUOUS:
                    sb.append("?");
                    break;
                default:
                    assert false;
            }
        }

        return sb.toString();
    }

    public boolean metricVectorIsZero() {
        if( metricVector == null ) {
            return true;
        }

        for ( Double aDouble : metricVector ) {
            if( aDouble != null && aDouble != 0.0 ) {
                return false;
            }
        }

        return true;
    }
}
