/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import java.util.Collection;

public class MeterMatcherInput {
    public MeterMatcherInput(Collection<PhoneticWord> phoneticWords) {
        int nSyllables = 0;

        for (PhoneticWord phoneticWord : phoneticWords) {
            nSyllables += phoneticWord.nSyllables;
        }

        phoneticWordsBySyllableIndexes = new PhoneticWord[nSyllables];

        int i = 0;
        ambiguityLevel = 0;
        int nAmbigInsuccession = 0;
        int nAmbigLongWords = 0;
        for (PhoneticWord phoneticWord : phoneticWords) {
            for( int j = 0; j < phoneticWord.nSyllables; j++ ) {
                phoneticWordsBySyllableIndexes[i++] = phoneticWord;
            }

            if( phoneticWord.isStronglyAmbiguos() ) {
                nAmbigInsuccession++;

                if( phoneticWord.nSyllables > 1 ) {
                    nAmbigLongWords++;
                    ambiguityLevel = Math.max(nAmbigLongWords,ambiguityLevel);
                }
            } else {
                ambiguityLevel = Math.max(nAmbigInsuccession,ambiguityLevel);
                nAmbigInsuccession = 0;
            }
        }
        ambiguityLevel = Math.max(nAmbigInsuccession,ambiguityLevel);
    }

    public PhoneticWord[] getPhoneticWordsBySyllableIndexes() {
        return phoneticWordsBySyllableIndexes;
    }

    private PhoneticWord[] phoneticWordsBySyllableIndexes;
    private int ambiguityLevel;

    // Количество подряд идущих фонетических слов, принимающих ударение на любое место
    public int getAmbiguityLevel() {
        return ambiguityLevel;
    }

    public void getPhoneticWords( Collection<PhoneticWord> targetCollection ) {
        PhoneticWord last = null;
        for (PhoneticWord word : phoneticWordsBySyllableIndexes) {
            if (word != last) {
                targetCollection.add(word);
            }

            last = word;
        }
    }


    public boolean isFreezed() {
        PhoneticWord last = null;
        for (PhoneticWord word : phoneticWordsBySyllableIndexes) {
            if (word != last) {
                if( word.stressVariants.length > 1 && word.freezedVariantIndex == -1 ) {
                    return false;
                }
            }

            last = word;
        }

        return true;
    }
}
