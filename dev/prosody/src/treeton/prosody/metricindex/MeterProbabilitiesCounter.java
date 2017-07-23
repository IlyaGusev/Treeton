/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.metricindex;

import org.apache.log4j.Logger;
import treeton.core.*;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TrnType;
import treeton.prosody.mdlcompiler.MdlEngine;
import treeton.prosody.mdlcompiler.fsm.*;

import java.util.*;

public class MeterProbabilitiesCounter {
    private static final Logger logger = Logger.getLogger(MeterProbabilitiesCounter.class);

    private double stressRestrictionViolationWeight;
    private double reaccentuationRestrictionViolationWeight;
    private int maxStressRestrictionViolations;
    private int maxReaccentuationRestrictionViolations;
    private int maxSyllablesPerVerse;
    private double fragmentSimilarityThreshold;
    private int maxMeterPriority;
    private boolean noMorph;
    private boolean heursiticOptimization;
    private double worstPenalty;

    public MeterProbabilitiesCounter(TreenotationsContext trnContext, String metricGrammarPath,
                                     double stressRestrictionViolationWeight, double reaccentuationRestrictionViolationWeight,
                                     int maxStressRestrictionViolations, int maxReaccentuationRestrictionViolations, int maxSyllablesPerVerse,
                                     boolean noMorph, double fragmentSimilarityThreshold, boolean heursiticOptimization ) throws Exception {
        this.stressRestrictionViolationWeight = stressRestrictionViolationWeight;
        this.reaccentuationRestrictionViolationWeight = reaccentuationRestrictionViolationWeight;
        this.maxStressRestrictionViolations = maxStressRestrictionViolations;
        this.maxReaccentuationRestrictionViolations = maxReaccentuationRestrictionViolations;
        this.maxSyllablesPerVerse = maxSyllablesPerVerse;
        this.noMorph = noMorph;
        this.fragmentSimilarityThreshold = fragmentSimilarityThreshold;
        this.heursiticOptimization = heursiticOptimization;

        worstPenalty = maxStressRestrictionViolations * stressRestrictionViolationWeight +
                maxReaccentuationRestrictionViolations * reaccentuationRestrictionViolationWeight;

        verseTp = trnContext.getType("Verse");
        phonWordTp = trnContext.getType("PhonWord");
        accVariantTp = trnContext.getType("AccVariant");
        syllTp = trnContext.getType("Syllable");
        userVariantFeatureId = accVariantTp.getFeatureIndex("userVariant");
        mdlEngine = new MdlEngine(metricGrammarPath, true);

        maxMeterPriority = 0;
        for (Meter meter : mdlEngine) {
            maxMeterPriority = Math.max(meter.getPriority(),maxMeterPriority);
        }
    }

    public Collection<Meter> getMeters() {
        ArrayList<Meter> result = new ArrayList<>();

        assert (mdlEngine != null);

        for (Meter meter : mdlEngine) {
            result.add(meter);
        }

        return result;
    }

    private TrnType verseTp;
    private TrnType phonWordTp;
    private TrnType accVariantTp;
    private TrnType syllTp;
    private int userVariantFeatureId;
    private MdlEngine mdlEngine;

    private boolean adjoin( TreenotationImpl verse1, TreenotationImpl verse2 ) {
        Token next = verse1.getEndToken().getNextToken();
        assert (next != null);

        return next == verse2.getStartToken() || verse2.getStartToken().getPreviousToken() == next;
    }

    MeterMatcherInput extractVerseStructure(TreenotationStorageImpl storage, Treenotation verse, boolean reverse) {
        ArrayList<PhoneticWord> result = new ArrayList<>();

        TypeIteratorInterface iterator = storage.typeIterator(phonWordTp, verse.getStartToken(), verse.getEndToken());

        ArrayList<Treenotation> phonWordTreenotations = new ArrayList<>();

        while (iterator.hasNext()) {
            Treenotation phonWordTrn = (Treenotation) iterator.next();
            if (phonWordTrn.getStartToken().compareTo(verse.getStartToken()) < 0 ||
                    phonWordTrn.getEndToken().compareTo(verse.getEndToken()) > 0) {
                continue;
            }
            phonWordTreenotations.add(phonWordTrn);
        }

        if (reverse) {
            ArrayList<Treenotation> t = new ArrayList<>();

            for (int i = phonWordTreenotations.size() - 1; i >= 0; i--) {
                t.add(phonWordTreenotations.get(i));
            }

            phonWordTreenotations = t;
        }

        int firstSyllableIndex = 0;
        for (Treenotation phonWordTrn : phonWordTreenotations) {
            HashSet<Treenotation> forceStressed = new HashSet<>();
            HashSet<Treenotation> forceUnstressed = new HashSet<>();
            ArrayList<TreenotationImpl> potentialVariants = new ArrayList<>();

            TypeIteratorInterface accVariantsIterator =
                    storage.typeIterator(accVariantTp, phonWordTrn.getStartToken(), phonWordTrn.getEndToken());

            while (accVariantsIterator.hasNext()) {
                TreenotationImpl accVariant = (TreenotationImpl) accVariantsIterator.next();

                if (accVariant.getStartToken().compareTo(phonWordTrn.getStartToken()) < 0 ||
                        accVariant.getEndToken().compareTo(phonWordTrn.getEndToken()) > 0) {
                    continue;
                }

                if (Boolean.TRUE.equals(accVariant.get(userVariantFeatureId))) {
                    TreenotationImpl.Node[] syllables = accVariant.getTrees();

                    for (TreenotationImpl.Node syllableNode : syllables) {
                        if (syllableNode.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                            forceStressed.add(syllableNode.getTrn());
                        } else {
                            forceUnstressed.add(syllableNode.getTrn());
                        }
                    }
                } else if (accVariant.getStartToken() == phonWordTrn.getStartToken() &&
                        accVariant.getEndToken() == phonWordTrn.getEndToken() && !noMorph) {
                    assert accVariant.getStartToken().compareTo(phonWordTrn.getStartToken()) == 0 &&
                            accVariant.getEndToken().compareTo(phonWordTrn.getEndToken()) == 0;

                    potentialVariants.add(accVariant);
                }
            }

            Set<BitSet> bitSetVariants = new HashSet<>();

            int syllablesCount = -1;

            for (TreenotationImpl accVariant : potentialVariants) {
                TreenotationImpl.Node[] syllables = accVariant.getTrees();
                if (syllablesCount == -1) {
                    syllablesCount = syllables.length;
                } else {
                    assert syllablesCount == syllables.length;
                }


                BitSet bitSetVariant = new BitSet(syllablesCount);

                int i = 0;
                for (; i < syllables.length; i++) {
                    TreenotationImpl.Node syllableNode = syllables[i];

                    if (syllableNode.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                        if (forceUnstressed.contains(syllableNode.getTrn())) {
                            break;
                        }
                        bitSetVariant.set(reverse ? syllablesCount - i - 1 : i);
                    } else {
                        if (forceStressed.contains(syllableNode.getTrn())) {
                            break;
                        }
                        bitSetVariant.clear(reverse ? syllablesCount - i - 1 : i);
                    }
                }

                if (i == syllables.length) {
                    bitSetVariants.add(bitSetVariant);
                }
            }

            if (bitSetVariants.isEmpty()) {
                TypeIteratorInterface sylIterator = storage.typeIterator(syllTp, phonWordTrn.getStartToken(), phonWordTrn.getEndToken());
                ArrayList<Treenotation> syllables = new ArrayList<>();
                while (sylIterator.hasNext()) {
                    syllables.add((Treenotation) sylIterator.next());
                }

                if (syllablesCount == -1) {
                    syllablesCount = syllables.size();
                } else {
                    assert syllablesCount == syllables.size();
                }

                BitSet singleVariant = new BitSet(syllables.size());

                for (int i = 0; i < syllablesCount; i++) {
                    if (forceStressed.contains(syllables.get(i))) {
                        singleVariant.set(reverse ? syllablesCount - i - 1 : i);
                    }
                }

                bitSetVariants.add(singleVariant);
            }

            BitSet[] bitSetsArray = bitSetVariants.toArray(new BitSet[bitSetVariants.size()]);

            PhoneticWord phonWord = new PhoneticWord(firstSyllableIndex, bitSetsArray, syllablesCount);
            firstSyllableIndex += syllablesCount;
            result.add(phonWord);
        }

        return new MeterMatcherInput(result);
    }

    public String getStats() {
        return "";
        //return "total: " + total + ", hits: " + hits + ", accuracy: " + Double.toString( ((double) hits ) / total );
    }

    class StressSequenceInfo {
        BitSet sequence;
        Map<String, List<Double>> info;

        public boolean contains(StressSequenceInfo other, Set<Integer> skip ) {
            for( int i = 0; i < sequence.length(); i++ ) {
                if( skip.contains(i) ) {
                    continue;
                }

                if( sequence.get( i ) && !other.sequence.get( i ) ) {
                    return false;
                }
            }

            return true;
        }
    }

    public static String MASCULINE_CADENCE = "__Masculine_Cadence";
    public static String FEMININE_CADENCE = "__Feminine_Cadence";
    public static String DACTYLIC_CADENCE = "__Dactylic_Cadence";
    public static String[] Cadences = new String[] {MASCULINE_CADENCE, FEMININE_CADENCE, DACTYLIC_CADENCE};
    public static int NUMBER_OF_CADENCES_TYPES = 3;

    public void countMeterProbabilities(TreenotationStorageImpl storage, Map<String, List<double[]>> meterProbabilities,
                                        List<BitSet> stressSequences, List<Integer> fragmentIds ) {
        for (Meter meter : mdlEngine) {
            meterProbabilities.put(meter.getName(), new ArrayList<>());
        }

        ArrayList<double[]> l = new ArrayList<>();
        l.add(null);
        meterProbabilities.put( MASCULINE_CADENCE, l );
        l = new ArrayList<>();
        l.add(null);
        meterProbabilities.put( FEMININE_CADENCE, l);
        l = new ArrayList<>();
        l.add(null);
        meterProbabilities.put( DACTYLIC_CADENCE, l);

        ArrayList<Integer> breaks = new ArrayList<>();

        TypeIteratorInterface tit = storage.typeIterator(verseTp);
        int verseCount = 0;
        TreenotationImpl prevVerse = null;
        while (tit.hasNext()) {
            TreenotationImpl verse = (TreenotationImpl) tit.next();

            if( prevVerse != null && !adjoin( prevVerse, verse ) ) {
                breaks.add( verseCount );
            }

            verseCount++;
            prevVerse = verse;
        }

        tit = storage.typeIterator(verseTp);

        ArrayList<MeterMatcherInput> matcherInputs = new ArrayList<>();
        ArrayList<String> matcherInputStrings = new ArrayList<>();

        int verseIndex = -1;
        while (tit.hasNext()) {
            verseIndex++;
            Treenotation verse = (Treenotation) tit.next();
            assert verse.getText().trim().length() > 0; // сюда пустые строки приходить не должны
            matcherInputStrings.add(verse.getText());

            /*if( verse.getText().equalsIgnoreCase("Посоленная белью песка,") ) {
                System.out.println("!!!" + verseIndex);
            }*/

            MeterMatcherInput matcherInput = extractVerseStructure(storage, verse, mdlEngine.isReverseAutomata());
            if (matcherInput.getPhoneticWordsBySyllableIndexes().length == 0 || matcherInput.getPhoneticWordsBySyllableIndexes().length > maxSyllablesPerVerse) {
                matcherInputs.add(null);
                continue;
            }

            if (matcherInput.getAmbiguityLevel() > 5) {
                matcherInputs.add(null);
                continue;
            }
            matcherInputs.add(matcherInput);

            double masculineCadenceProbability = 0.0;
            double feminineCadenceProbability = 0.0;
            double dactylicCadenceProbability = 0.0;

            for (Meter meter : mdlEngine) {
                List<Double> meterProbabilitiesByFootness = new ArrayList<>();

                MeterMatcher matcher = new MeterMatcher(meter, new MeterMatcherCursorImpl(matcherInput), stressRestrictionViolationWeight, reaccentuationRestrictionViolationWeight,
                        maxStressRestrictionViolations, maxReaccentuationRestrictionViolations, 3);
                matcher.match(true);

                int n = matcher.getNumberOfResults();
                for (int i = 0; i < n; i++) {
                    int footness = matcher.getNumberOfStressedSyllables(i);
                    //System.out.println(Arrays.toString(matcher.createMatchingArray(i, mdlEngine.isReverseAutomata())));

                    while (footness >= meterProbabilitiesByFootness.size()) {
                        meterProbabilitiesByFootness.add(0.0);
                    }

                    Double probability = meterProbabilitiesByFootness.get(footness);

                    double newProbability = 1 - matcher.getPenalty(i) / worstPenalty;

                    meterProbabilitiesByFootness.set(footness, Math.max(probability, newProbability));

                    BitSet stressSequence = matcher.getStressSequence( i, mdlEngine.isReverseAutomata() );

                    int nSylls = matcherInput.getPhoneticWordsBySyllableIndexes().length;

                    if( stressSequence.get( nSylls - 1 ) ) {
                        masculineCadenceProbability = Math.max( masculineCadenceProbability, newProbability );
                    } else if( nSylls > 1 && stressSequence.get( nSylls - 2 ) ) {
                        feminineCadenceProbability = Math.max( feminineCadenceProbability, newProbability );
                    } else if( nSylls > 2 && stressSequence.get( nSylls - 3 ) ) {
                        dactylicCadenceProbability = Math.max( dactylicCadenceProbability, newProbability );
                    }

                    ArrayList<PhoneticWord> phoneticWords = new ArrayList<>();
                    matcherInput.getPhoneticWords( phoneticWords );

                    int shift = 0;

                    for (PhoneticWord phoneticWord : phoneticWords) {
                        BitSet[] stressVariants = phoneticWord.getStressVariants();

                        for (int j = 0; j < stressVariants.length; j++) {
                            BitSet phoneticWordStressSequence = stressVariants[j];

                            int k = 0;
                            for (; k < phoneticWord.getNumberOfSyllables(); k++) {
                                boolean seqStress = mdlEngine.isReverseAutomata() ?
                                        stressSequence.get( nSylls - shift - k - 1 ) : stressSequence.get(shift + k);

                                if ( seqStress != phoneticWordStressSequence.get(k) ) {
                                    break;
                                }
                            }

                            if (k == phoneticWord.getNumberOfSyllables()) {
                                double p = phoneticWord.getStressVariantsProbabilities()[j];
                                phoneticWord.getStressVariantsProbabilities()[j] = Math.max(p, newProbability);
                            }
                        }

                        shift += phoneticWord.getNumberOfSyllables();
                    }
                }

                List<double[]> resultMeterProbabilitiesList = meterProbabilities.get(meter.getName());

                while (resultMeterProbabilitiesList.size() < meterProbabilitiesByFootness.size()) {
                    resultMeterProbabilitiesList.add(null);
                }

                for (int i = 0; i < meterProbabilitiesByFootness.size(); i++) {
                    Double footnessProbability = meterProbabilitiesByFootness.get(i);
                    if (footnessProbability > 0) {
                        double[] targetArray = resultMeterProbabilitiesList.get(i);
                        if (targetArray == null) {
                            targetArray = new double[verseCount];
                            for (int j = 0; j < targetArray.length; j++) {
                                targetArray[j] = 0.0;
                            }
                            resultMeterProbabilitiesList.set(i, targetArray);
                        }
                        targetArray[verseIndex] = footnessProbability;
                    }
                }
            }

            List<double[]> resultMeterProbabilitiesList = meterProbabilities.get( MASCULINE_CADENCE );

            if( masculineCadenceProbability > 0 ) {
                double[] targetArray = resultMeterProbabilitiesList.get(0);
                if (targetArray == null) {
                    targetArray = new double[verseCount];
                    for (int j = 0; j < targetArray.length; j++) {
                        targetArray[j] = 0.0;
                    }
                    resultMeterProbabilitiesList.set(0, targetArray);
                }
                targetArray[verseIndex] = masculineCadenceProbability;
            }

            resultMeterProbabilitiesList = meterProbabilities.get( FEMININE_CADENCE );

            if( feminineCadenceProbability > 0 ) {
                double[] targetArray = resultMeterProbabilitiesList.get(0);
                if (targetArray == null) {
                    targetArray = new double[verseCount];
                    for (int j = 0; j < targetArray.length; j++) {
                        targetArray[j] = 0.0;
                    }
                    resultMeterProbabilitiesList.set(0, targetArray);
                }
                targetArray[verseIndex] = feminineCadenceProbability;
            }

            resultMeterProbabilitiesList = meterProbabilities.get( DACTYLIC_CADENCE );

            if( dactylicCadenceProbability > 0 ) {
                double[] targetArray = resultMeterProbabilitiesList.get(0);
                if (targetArray == null) {
                    targetArray = new double[verseCount];
                    for (int j = 0; j < targetArray.length; j++) {
                        targetArray[j] = 0.0;
                    }
                    resultMeterProbabilitiesList.set(0, targetArray);
                }
                targetArray[verseIndex] = dactylicCadenceProbability;
            }
        }

        if (stressSequences == null) {
            return;
        }

        // второй проход

        int[] allIndexes = new int[verseCount];

        for( int i = 0; i < allIndexes.length;i++) {
            allIndexes[i]=i;
        }

        Map<String, List<Double>> wholeTextAverageProbabilities = countAverageProbs(meterProbabilities,allIndexes,allIndexes.length,true);

        ArrayList<Map<String, List<Double>>> averageProbabilitiesByFragments = null;
        int[] fragmentIdsArray = null;

        if( fragmentIds != null ) {
            averageProbabilitiesByFragments = new ArrayList<>();

            fragmentIdsArray = new int[matcherInputs.size()];
            int nFragments = splitIntoFragments(matcherInputs, breaks, meterProbabilities, fragmentIdsArray, false );

            verseIndex = 0;
            for (int fragmentId = 0; fragmentId < nFragments; fragmentId++) {
                int currentFragmentSize = 0;
                while (verseIndex < fragmentIdsArray.length) {
                    int verseFragment = fragmentIdsArray[verseIndex];

                    if (verseFragment == -1) {
                        verseIndex++;
                    } else if (verseFragment == fragmentId) {
                        allIndexes[currentFragmentSize++] = verseIndex++;
                    } else {
                        break;
                    }
                }

                averageProbabilitiesByFragments.add(countAverageProbs(meterProbabilities, allIndexes, currentFragmentSize,true));
            }
        }

        meterProbabilities.clear();
        for (Meter meter : mdlEngine) {
            meterProbabilities.put(meter.getName(), new ArrayList<>());
        }

        l = new ArrayList<>();
        l.add(null);
        meterProbabilities.put( MASCULINE_CADENCE, l );
        l = new ArrayList<>();
        l.add(null);
        meterProbabilities.put( FEMININE_CADENCE, l);
        l = new ArrayList<>();
        l.add(null);
        meterProbabilities.put( DACTYLIC_CADENCE, l);

        verseIndex = -1;
        for (MeterMatcherInput matcherInput : matcherInputs) {
            verseIndex++;
            if (matcherInput == null) {
                stressSequences.add(null);
                continue;
            }

            if( logger.isTraceEnabled() ) {
                logger.trace("Verse '" + matcherInputStrings.get(verseIndex) + "'");
            }

            StressSequenceInfo bestSequenceInfo;

            if( heursiticOptimization ) {
                bestSequenceInfo = getBestStressSequenceInfoHeuristic( verseIndex, wholeTextAverageProbabilities, averageProbabilitiesByFragments, fragmentIdsArray, matcherInput);
            } else {
                bestSequenceInfo = getBestStressSequenceInfo( verseIndex, wholeTextAverageProbabilities, averageProbabilitiesByFragments, fragmentIdsArray, matcherInput);
            }

            /*HashSet<Integer> skip = new HashSet<>();

            PhoneticWord[] words = matcherInput.getPhoneticWordsBySyllableIndexes();

            for (int i = 0; i < words.length; i++) {
                PhoneticWord word = words[i];

                if( word.getNumberOfSyllables() == 1 ) {
                    skip.add( mdlEngine.isReverseAutomata() ? words.length - i - 1 : i );
                }
            }

            if( bestSequenceInfo_old.contains( bestSequenceInfo, skip  ) ) {
                hits++;
            } else {
                System.out.println(matcherInputStrings.get( verseIndex ) + " | old method " + bestSequenceInfo_old.sequence + ", new method " + bestSequenceInfo.sequence);
            }
            total++;*/

            if( bestSequenceInfo.info != null ) {
                if (logger.isTraceEnabled()) {
                    logger.trace("  Best sequence " + bestSequenceInfo.sequence.toString());
                }

                for (Map.Entry<String, List<Double>> meterInfo : bestSequenceInfo.info.entrySet()) {
                    List<Double> meterProbabilitiesByFootness = meterInfo.getValue();
                    List<double[]> resultMeterProbabilitiesList = meterProbabilities.get(meterInfo.getKey());

                    while (resultMeterProbabilitiesList.size() < meterProbabilitiesByFootness.size()) {
                        resultMeterProbabilitiesList.add(null);
                    }

                    for (int i = 0; i < meterProbabilitiesByFootness.size(); i++) {
                        Double footnessProbability = meterProbabilitiesByFootness.get(i);
                        if (footnessProbability > 0) {
                            double[] targetArray = resultMeterProbabilitiesList.get(i);
                            if (targetArray == null) {
                                targetArray = new double[verseCount];
                                for (int j = 0; j < targetArray.length; j++) {
                                    targetArray[j] = 0.0;
                                }
                                resultMeterProbabilitiesList.set(i, targetArray);
                            }
                            targetArray[verseIndex] = footnessProbability;
                        }
                    }
                }
            } else if (logger.isTraceEnabled()) {
                logger.trace("  No correct sequence found " );
            }

            stressSequences.add( bestSequenceInfo.sequence );
        }

        if( fragmentIds != null ) {
            fragmentIdsArray = new int[matcherInputs.size()];
            splitIntoFragments(matcherInputs, breaks, meterProbabilities, fragmentIdsArray, true );
            fragmentIds.clear();
            for (int id : fragmentIdsArray) {
                fragmentIds.add(id);
            }
        }
    }

    private StressSequenceInfo getBestStressSequenceInfo( int verseIndex, Map<String, List<Double>> wholeTextAverageProbabilities, ArrayList<Map<String, List<Double>>> averageProbabilitiesByFragments, int[] fragmentIdsArray, MeterMatcherInput matcherInput) {
        Map<BitSet, Map<String, List<Double>>> stressSequenceInfos = new HashMap<>();

        for (Meter meter : mdlEngine) {
            MeterMatcher matcher = new MeterMatcher(meter, new MeterMatcherCursorImpl(matcherInput), stressRestrictionViolationWeight, reaccentuationRestrictionViolationWeight,
                    maxStressRestrictionViolations, maxReaccentuationRestrictionViolations, 3);
            matcher.match(false);

            int n = matcher.getNumberOfResults();
            for (int i = 0; i < n; i++) {
                BitSet stressSequence = matcher.getStressSequence(i, mdlEngine.isReverseAutomata());

                Map<String, List<Double>> probsMap = stressSequenceInfos.get(stressSequence);

                if (probsMap == null) {
                    probsMap = new HashMap<>();
                    stressSequenceInfos.put(stressSequence, probsMap);

                    List<Double> l = new ArrayList<>();
                    l.add(0.0);
                    probsMap.put(MASCULINE_CADENCE,l);

                    l = new ArrayList<>();
                    l.add(0.0);
                    probsMap.put(FEMININE_CADENCE,l);

                    l = new ArrayList<>();
                    l.add(0.0);
                    probsMap.put(DACTYLIC_CADENCE,l);
                }

                List<Double> probsByFootness = probsMap.get(meter.getName());

                if (probsByFootness == null) {
                    probsByFootness = new ArrayList<>();
                    probsMap.put(meter.getName(), probsByFootness);
                }

                int footness = matcher.getNumberOfStressedSyllables(i);

                while (probsByFootness.size() <= footness) {
                    probsByFootness.add(0.0);
                }

                double probability = 1 - matcher.getPenalty(i) / worstPenalty;

                probsByFootness.set(footness, Math.max(probability, probsByFootness.get(footness)));

                int nSylls = matcherInput.getPhoneticWordsBySyllableIndexes().length;

                if( stressSequence.get( nSylls - 1 ) ) {
                    List<Double> mascList = probsMap.get(MASCULINE_CADENCE);
                    if( probability > mascList.get(0) ) {
                        mascList.set(0,probability);
                    }
                } else if( nSylls > 1 && stressSequence.get( nSylls - 2 ) ) {
                    List<Double> femList = probsMap.get(FEMININE_CADENCE);
                    if( probability > femList.get(0) ) {
                        femList.set(0,probability);
                    }
                } else if( nSylls > 2 && stressSequence.get( nSylls - 3 ) ) {
                    List<Double> dactList = probsMap.get(DACTYLIC_CADENCE);
                    if( probability > dactList.get(0) ) {
                        dactList.set(0,probability);
                    }
                }
            }
        }

        StressSequenceInfo bestSequenceInfo = new StressSequenceInfo();
        double bestBonus = 0.0;

        for (Map.Entry<BitSet, Map<String, List<Double>>> stressSequenceInfo : stressSequenceInfos.entrySet()) {
            BitSet stressSequence = stressSequenceInfo.getKey();

            if( logger.isTraceEnabled() ) {
                logger.trace("  Stress sequence " + stressSequence.toString());
            }
            Map<String, List<Double>> info = stressSequenceInfo.getValue();

            if (bestSequenceInfo.sequence == null) {
                bestSequenceInfo.sequence = stressSequence;
                bestSequenceInfo.info = info;
                bestBonus = countBonus(info, wholeTextAverageProbabilities) +
                        (averageProbabilitiesByFragments == null ? 0.0 :
                        countBonus(info, averageProbabilitiesByFragments.get(fragmentIdsArray[verseIndex]) ));
            } else {
                double bonus = countBonus(info, wholeTextAverageProbabilities ) +
                        (averageProbabilitiesByFragments == null ? 0.0 :
                        countBonus(info, averageProbabilitiesByFragments.get(fragmentIdsArray[verseIndex]) ));
                if (bonus > bestBonus) {
                    bestSequenceInfo.sequence = stressSequence;
                    bestSequenceInfo.info = info;
                    bestBonus = bonus;
                }
            }
        }

        return bestSequenceInfo;
    }

    private StressSequenceInfo getBestStressSequenceInfoHeuristic(
            int verseIndex,
            Map<String, List<Double>> wholeTextAverageProbabilities,
            ArrayList<Map<String, List<Double>>> averageProbabilitiesByFragments,
            int[] fragmentIdsArray,
            MeterMatcherInput matcherInput) {

        ArrayList<PhoneticWord> phoneticWords = new ArrayList<>();
        matcherInput.getPhoneticWords( phoneticWords );

        for (PhoneticWord phoneticWord : phoneticWords) {
            if (phoneticWord.getNumberOfStressVariants() > 1) {
                // выбираем лучший по грубому максимуму вероятности
                double[] probabilities = phoneticWord.getStressVariantsProbabilities();

                double max = 0;
                int maxj = -1;
                for (int j = 0; j < probabilities.length; j++) {
                    double probability = probabilities[j];

                    if ( maxj == -1 || probability > max) {
                        max = probability;
                        maxj = j;
                    }
                }

                phoneticWord.freeze(maxj);
            }
        }

        StressSequenceInfo bestSequenceInfo = countStressSequenceInfoOnFreezedInput(matcherInput);
        Double bestBonus = null;

        if( bestSequenceInfo.info != null ) {
            bestBonus = countBonus(bestSequenceInfo.info, wholeTextAverageProbabilities) +
                    (averageProbabilitiesByFragments == null ? 0.0 :
                            countBonus(bestSequenceInfo.info, averageProbabilitiesByFragments.get(fragmentIdsArray[verseIndex])));
        }

        for (PhoneticWord phoneticWord : phoneticWords) {
            int nVariants = phoneticWord.getNumberOfStressVariants();

            if( nVariants == 1 ) {
                continue;
            }

            int initiallyFreezedIndex = phoneticWord.getFreezedVariantIndex();
            int chosenIndex = initiallyFreezedIndex;
            for( int j = 0; j < nVariants; j++) {
                if( j == initiallyFreezedIndex ) {
                    continue;
                }

                phoneticWord.freeze(j);

                StressSequenceInfo sequenceInfo = countStressSequenceInfoOnFreezedInput(matcherInput);

                if (sequenceInfo.info != null) {
                    double bonus = countBonus(sequenceInfo.info, wholeTextAverageProbabilities) +
                            (averageProbabilitiesByFragments == null ? 0.0 :
                                    countBonus(sequenceInfo.info, averageProbabilitiesByFragments.get(fragmentIdsArray[verseIndex])));

                    if (bestBonus == null || bonus > bestBonus) {
                        bestSequenceInfo = sequenceInfo;
                        bestBonus = bonus;
                        chosenIndex = j;
                    }
                }
            }

            phoneticWord.freeze(chosenIndex);
        }

        return bestSequenceInfo;
    }

    private StressSequenceInfo countStressSequenceInfoOnFreezedInput(MeterMatcherInput matcherInput ) {
        Map<String, List<Double>> probsMap = new HashMap<>();
        BitSet resultSequence = null;

        assert matcherInput.isFreezed();

        double masculineCadenceProbability = 0.0;
        double feminineCadenceProbability = 0.0;
        double dactylicCadenceProbability = 0.0;

        for (Meter meter : mdlEngine) {
            MeterMatcher matcher = new MeterMatcher(meter, new MeterMatcherCursorImpl(matcherInput), stressRestrictionViolationWeight, reaccentuationRestrictionViolationWeight,
                    1000, 1000, 1000 );
            matcher.match( true );

            int n = matcher.getNumberOfResults();

            for (int i = 0; i < n; i++) {
                if( resultSequence == null ) {
                    resultSequence = matcher.getStressSequence(i, mdlEngine.isReverseAutomata());

                    if( logger.isTraceEnabled() ) {
                        logger.trace("  Stress sequence " + resultSequence.toString());
                    }

                }

                List<Double> probsByFootness = probsMap.get(meter.getName());

                if (probsByFootness == null) {
                    probsByFootness = new ArrayList<>();
                    probsMap.put(meter.getName(), probsByFootness);
                }

                int footness = matcher.getNumberOfStressedSyllables(i);

                while (probsByFootness.size() <= footness) {
                    probsByFootness.add(0.0);
                }

                double probability = 1 - matcher.getPenalty(i) / worstPenalty;

                if( probability < 0 ) {
                    probability = 0;
                }

                probsByFootness.set(footness, Math.max(probability, probsByFootness.get(footness)));

                int nSylls = matcherInput.getPhoneticWordsBySyllableIndexes().length;

                if( resultSequence.get( nSylls - 1 ) ) {
                    masculineCadenceProbability = Math.max( masculineCadenceProbability, probability );
                } else if( nSylls > 1 && resultSequence.get( nSylls - 2 ) ) {
                    feminineCadenceProbability = Math.max( feminineCadenceProbability, probability );
                } else if( nSylls > 2 && resultSequence.get( nSylls - 3 ) ) {
                    dactylicCadenceProbability = Math.max( dactylicCadenceProbability, probability );
                }
            }
        }

        List<Double> l = new ArrayList<>();
        l.add(masculineCadenceProbability);
        probsMap.put(MASCULINE_CADENCE,l);

        l = new ArrayList<>();
        l.add(feminineCadenceProbability);
        probsMap.put(FEMININE_CADENCE,l);

        l = new ArrayList<>();
        l.add(dactylicCadenceProbability);
        probsMap.put(DACTYLIC_CADENCE,l);

        StressSequenceInfo result = new StressSequenceInfo();

        result.info = probsMap;
        result.sequence = resultSequence;

        return result;
    }

    private Map<String, List<Double>> countAverageProbs(Map<String, List<double[]>> meterProbabilities, int[] verseIndexes, int verseCount, boolean doFilterSmallProbs ) {
        Map<String, List<Double>> averageProbabilities = new HashMap<>();

        for (Map.Entry<String, List<double[]>> meterInfo : meterProbabilities.entrySet()) {
            String meterName = meterInfo.getKey();

            List<Double> averageProbabilitiesForMeter = new ArrayList<>();

            for (int footness = 0; footness < meterInfo.getValue().size(); footness++) {
                double[] probabilities = meterInfo.getValue().get(footness);

                double sum = 0;

                if( probabilities != null ) {
                    for (int i = 0; i < verseIndexes.length && i < verseCount; i++) {
                        sum += probabilities[verseIndexes[i]];
                    }
                }

                double p = probabilities == null || verseCount == 0 ? 0.0 : sum / verseCount;

                averageProbabilitiesForMeter.add( (!doFilterSmallProbs || p > 0.5) ? p : 0.0 );
            }

            averageProbabilities.put(meterName, averageProbabilitiesForMeter);
        }
        return averageProbabilities;
    }

    private int splitIntoFragments(ArrayList<MeterMatcherInput> matcherInputs, ArrayList<Integer> breaks,
                                   Map<String, List<double[]>> meterProbabilities, int[] fragmentIds, boolean doMerge ) {

        ArrayList<Integer> blocks = new ArrayList<>();

        int numberOfStringsInCurrentBlock = 0;
        boolean singleStringsBlockStarted = false;

        assert breaks.size() == 0 || breaks.get(0) > 0; // не должно быть раздела на самой первой строке

        int breakNumber = 0, i = 0;
        while( i < matcherInputs.size() ) {
            if( breakNumber < breaks.size() && breaks.get( breakNumber ) == i ) {
                if( blocks.size() > 0 && numberOfStringsInCurrentBlock == 1 ) {
                    Integer prevStringsCount = blocks.get(blocks.size() - 1);

                    //Идущие подряд единичные строки объединяем в один блок

                    if( prevStringsCount == 1 || singleStringsBlockStarted ) {
                        blocks.set( blocks.size() - 1, prevStringsCount + 1 );

                        if( prevStringsCount == 1 ) {
                            singleStringsBlockStarted = true;
                        }
                    } else {
                        blocks.add( numberOfStringsInCurrentBlock );
                        singleStringsBlockStarted = false;
                    }
                } else {
                    blocks.add( numberOfStringsInCurrentBlock );
                    singleStringsBlockStarted = false;
                }

                breakNumber++;
                numberOfStringsInCurrentBlock = 0;
            }

            i++;
            numberOfStringsInCurrentBlock++;
        }

        if( blocks.size() > 0 && numberOfStringsInCurrentBlock == 1 ) {
            Integer prevStringsCount = blocks.get( blocks.size() - 1 );

            if( prevStringsCount == 1 || singleStringsBlockStarted ) {
                blocks.set( blocks.size() - 1, prevStringsCount + 1 );
            } else {
                blocks.add( numberOfStringsInCurrentBlock );
            }
        } else if( numberOfStringsInCurrentBlock > 0 ) {
            blocks.add( numberOfStringsInCurrentBlock );
        }

        if( doMerge && fragmentSimilarityThreshold >= 0 ) {
            // Будем сливать похожие фрагменты
            Map<String, List<Double>> topAverageProbabilities;
            int[] topVerseIndexes;
            Map<String, List<Double>> bottomAverageProbabilities = null;

            int verseIndex = matcherInputs.size() - 1;

            for( i = blocks.size() - 1; i >= 1; i-- ) {
                if (bottomAverageProbabilities == null) {
                    Integer nStringsInBottomBlock = blocks.get(i);
                    int[] bottomVerseIndexes = new int[nStringsInBottomBlock];

                    for( int j = nStringsInBottomBlock - 1; j >= 0; j-- ) {
                        bottomVerseIndexes[j] = verseIndex--;
                    }

                    bottomAverageProbabilities = countAverageProbs(meterProbabilities, bottomVerseIndexes, bottomVerseIndexes.length,false);
                }

                Integer nStringsInTopBlock = blocks.get( i - 1 );
                topVerseIndexes = new int[nStringsInTopBlock];

                for (int j = nStringsInTopBlock - 1; j >= 0; j--) {
                    topVerseIndexes[j] = verseIndex--;
                }

                topAverageProbabilities = countAverageProbs(meterProbabilities, topVerseIndexes, topVerseIndexes.length,false);

                double similarity = countSimilarity(topAverageProbabilities, bottomAverageProbabilities);

                if (similarity >= fragmentSimilarityThreshold) {
                    blocks.set(i - 1, blocks.get(i - 1) + blocks.get(i));
                    blocks.remove(i);
                    //System.out.println("similarity is bigger then threshold: "+similarity);
                } else {
                    //System.out.println("similarity is less then threshold: "+similarity);
                }

                bottomAverageProbabilities = topAverageProbabilities;
            }
        }

        int verseIndex = 0;
        for( i = 0; i < blocks.size(); i++) {
            Integer nStringsInBlock = blocks.get(i);

            for( int j = 0; j < nStringsInBlock; j++ ) {
                fragmentIds[verseIndex++] = i;
            }
        }

        return blocks.size();
    }

    private double countBonus(Map<String, List<Double>> lineProbs, Map<String, List<Double>> wholeTextAverageProbs ) {
        double bonus = 0.0;

        if( logger.isTraceEnabled() ) {
            logger.trace("    Counting bonus...");
        }

        for( String meterName : wholeTextAverageProbs.keySet() ) {
            if( logger.isTraceEnabled() ) {
                logger.trace("      Meter " + meterName);
            }

            List<Double> lineProbsByFootness = lineProbs.get(meterName);
            List<Double> averageProbsByFootness = wholeTextAverageProbs.get(meterName);

            if (averageProbsByFootness == null || averageProbsByFootness.isEmpty()) {
                if( logger.isTraceEnabled() ) {
                    logger.trace("        Empty average vector, meterBonus = 0.0");
                }
                continue;
            }

            if (lineProbsByFootness == null) {
                if( logger.isTraceEnabled() ) {
                    logger.trace("        Empty line vector, meterBonus = 0.0");
                }

                continue;
            }

            double meterBonus = 0.0;
            int maxSize = Math.max(lineProbsByFootness.size(), averageProbsByFootness.size());

            double maxInLine = 0;
            double maxInAverage = 0;

            for (int i = 0; i < maxSize; i++) {
                double lineP = i < lineProbsByFootness.size() ? lineProbsByFootness.get(i) : 0.0;
                double averageP = i < averageProbsByFootness.size() ? averageProbsByFootness.get(i) : 0.0;

                if( logger.isTraceEnabled() ) {
                    logger.trace("        Footness " + i);
                    logger.trace("         averageP = " + averageP);
                    logger.trace("         lineP = " + lineP);
                }

                maxInLine = Math.max(lineP, maxInLine);
                maxInAverage = Math.max(averageP, maxInAverage);

                double b = countSingleBonus(lineP, averageP);
                if( logger.isTraceEnabled() ) {
                    logger.trace("         bonus = " + b);
                }

                meterBonus += b;
            }

//            if( !meterName.startsWith("__") ) {
            if (logger.isTraceEnabled()) {
                logger.trace("        maxInAverage = " + maxInAverage);
                logger.trace("        maxInLine = " + maxInLine);
            }
            double b = countSingleBonus(maxInLine, maxInAverage);
            if (logger.isTraceEnabled()) {
                logger.trace("        bonus = " + b);
            }

            meterBonus += b;
//            }

            if( logger.isTraceEnabled() ) {
                logger.trace("      meter bonus = " + Double.toString(meterBonus));
            }
            bonus += meterBonus;
        }

        if( logger.isTraceEnabled() ) {
            logger.trace("    Result bonus is " + Double.toString(bonus));
        }
        return bonus;
    }

    private double countSimilarity(Map<String, List<Double>> probs1, Map<String, List<Double>> probs2 ) {
        double[] maxProbs = new double[maxMeterPriority+2];
        double[] pairwiseProbs = new double[maxMeterPriority+2];
        double[] weights = new double[maxMeterPriority+2];

        for (int i = 0; i < maxProbs.length; i++) {
            maxProbs[i] = 0.0;
            weights[i] = Math.exp( (double) i * -0.1);
        }
        weights[maxMeterPriority+1]=1.0;

        for (Meter meter : mdlEngine) {
            updateProbs(probs1, probs2, maxProbs,pairwiseProbs, meter.getName(), meter.getPriority(), true );
        }

        updateProbs(probs1,probs2,maxProbs,pairwiseProbs,MASCULINE_CADENCE,maxMeterPriority+1, false );
        updateProbs(probs1,probs2,maxProbs,pairwiseProbs,FEMININE_CADENCE,maxMeterPriority+1, false );
        updateProbs(probs1,probs2,maxProbs,pairwiseProbs,DACTYLIC_CADENCE,maxMeterPriority+1, false );

        double sum = 0.0;
        double sumMax = 0.0;

        int i = 0;
        for (double maxProb : maxProbs) {
            sum += maxProb * weights[i] * pairwiseProbs[i];
            sumMax += maxProb * weights[i];
            i++;
        }

        return sumMax < 0.001 ? 0 : sum / sumMax;
    }

    private void updateProbs(Map<String, List<Double>> probs1, Map<String, List<Double>> probs2, double[] maxProbs,
                             double[] pairwiseProbs, String name, int priority, boolean multOrDelta) {
        List<Double> probs1ByFootness = probs1.get(name);
        List<Double> probs2ByFootness = probs2.get(name);

        if (probs2ByFootness == null || probs2ByFootness.isEmpty()) {
            return;
        }

        if (probs1ByFootness == null || probs1ByFootness.isEmpty()) {
            return;
        }

        int maxSize = Math.max(probs1ByFootness.size(), probs2ByFootness.size());

        for (int i = 0; i < maxSize; i++) {
            double p1 = i < probs1ByFootness.size() ? probs1ByFootness.get(i) : 0.0;
            double p2 = i < probs2ByFootness.size() ? probs2ByFootness.get(i) : 0.0;
            double pp = multOrDelta ? p1 * p2 : 1 - Math.abs( p1 - p2 );

            maxProbs[priority] = Math.max( maxProbs[priority], p1 );
            maxProbs[priority] = Math.max( maxProbs[priority], p2 );
            pairwiseProbs[priority] = Math.max(pairwiseProbs[priority],pp);
        }
    }

    private double countSingleBonus(double p1, double p2) {
        return p1 * p2;
    }

    public boolean isNoMorph() {
        return noMorph;
    }

    public int getMaxMeterPriority() {
        return maxMeterPriority;
    }
}
