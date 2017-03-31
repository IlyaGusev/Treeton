/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody;

public class SyllableInfo {
    public SyllableInfo(int startOffset, int length, StressStatus stressStatus) {
        this.startOffset = startOffset;
        this.length = length;
        this.stressStatus = stressStatus;
    }

    public final int startOffset;
    public final int length;
    public final StressStatus stressStatus;

    public enum StressStatus {
        STRESSED,
        UNSTRESSED,
        AMBIGUOUS
    }

    @Override
    public String toString() {
        return startOffset + "," + length + "," +
                ( stressStatus == StressStatus.STRESSED ? "S": (stressStatus == StressStatus.UNSTRESSED ? "U" : "?") );

    }
}
