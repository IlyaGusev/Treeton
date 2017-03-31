/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import java.util.BitSet;

public class PhoneticWord {
    public PhoneticWord(int firstSyllableIndex, BitSet[] stressVariants, int nSyllables ) {
        this.firstSyllableIndex = firstSyllableIndex;
        this.stressVariants = stressVariants;
        this.stressVariantsProbabilities = new double[stressVariants.length];
        for (int i = 0; i < stressVariantsProbabilities.length; i++) {
            stressVariantsProbabilities[i] = 0.0;
        }
        this.nSyllables = nSyllables;
    }

    int nSyllables;
    int firstSyllableIndex;

    public BitSet[] getStressVariants() {
        return stressVariants;
    }

    BitSet[] stressVariants;
    double[] stressVariantsProbabilities;

    public int getFreezedVariantIndex() {
        return freezedVariantIndex;
    }

    int freezedVariantIndex = -1;

    public double[] getStressVariantsProbabilities() {
        return stressVariantsProbabilities;
    }

    public void freeze(int variantIndex ) {
        assert variantIndex >= 0 && variantIndex < stressVariants.length;

        freezedVariantIndex = variantIndex;
    }

    public void unfreeze() {
        freezedVariantIndex = -1;
    }

    public int getNumberOfStressVariants() {
        return stressVariants.length;
    }

    public boolean isStronglyAmbiguos() {
        return nSyllables == 1 ? stressVariants.length > 1 : stressVariants.length >= nSyllables;
    }

    public int getNumberOfSyllables() {
        return nSyllables;
    }
}
