/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import java.util.BitSet;

public class MeterMatcherCursorImpl implements MeterMatcherCursor {
    private MeterMatcherCursorImpl() {
    }

    public MeterMatcherCursorImpl(MeterMatcherInput input) {
        this.input = input;
        currentSyllableIndex = 0;
        lastChosenStress = false;
        previous = null;
    }

    @Override
    public int isStressed() {
        PhoneticWord phoneticWord = input.getPhoneticWordsBySyllableIndexes()[currentSyllableIndex];

        int bitPos = currentSyllableIndex - phoneticWord.firstSyllableIndex;

        if( phoneticWord.stressVariants.length == 0 ) {
            return 0;
        }

        int result = -2;

        for (int i = 0; i < phoneticWord.stressVariants.length; i++) {
            if( phoneticWord.freezedVariantIndex != -1 && phoneticWord.freezedVariantIndex != i ) {
                continue;
            }

            BitSet stressVariant = phoneticWord.stressVariants[i];

            int prevPos = bitPos - 1;
            MeterMatcherCursorImpl cursor = this;

            while( cursor.previous != null ) {
                assert( prevPos >= 0 );
                if( stressVariant.get(prevPos) != cursor.lastChosenStress ) {
                    stressVariant = null;
                    break;
                }

                prevPos--;
                cursor = cursor.previous;
            }

            if( stressVariant == null ) {
                continue;
            }

            int localResult = stressVariant.get(bitPos) ? 1 : 0;

            if( result == -2 ) {
                result = localResult;
            } else {
                if (localResult != result) {
                    return -1;
                }
            }
        }

        return result == -2 ? 0 : result;
    }

    @Override
    public MeterMatcherCursor shift( boolean stressed ) {
        MeterMatcherCursorImpl result = new MeterMatcherCursorImpl();
        result.currentSyllableIndex = currentSyllableIndex + 1;
        result.lastChosenStress = stressed;
        result.input = input;
        result.previous = result.isPhoneticWordEnded() ? null : this;
        return result;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean isPhoneticWordEnded() {
        if( currentSyllableIndex == input.getPhoneticWordsBySyllableIndexes().length ) {
            return true;
        }

        if( currentSyllableIndex == 0 ) {
            return false;
        }

        return input.getPhoneticWordsBySyllableIndexes()[currentSyllableIndex] != input.getPhoneticWordsBySyllableIndexes()[currentSyllableIndex - 1];
    }

    @Override
    public boolean isEndOfInput() {
        return currentSyllableIndex == input.getPhoneticWordsBySyllableIndexes().length;
    }

    private MeterMatcherInput input;
    private int currentSyllableIndex;
    private boolean lastChosenStress;
    private MeterMatcherCursorImpl previous;
}
