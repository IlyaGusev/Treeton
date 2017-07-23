/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.musimatix;

import com.sun.tools.javac.util.Pair;
import treeton.core.*;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.LogListener;
import treeton.core.config.context.resources.ResourceChain;
import treeton.core.config.context.resources.ResourceUtils;
import treeton.core.config.context.resources.xmlimpl.ResourcesContextXMLImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TrnType;
import treeton.core.util.ProgressListener;
import treeton.prosody.SyllableInfo;
import treeton.prosody.VerseProcessingUtilities;
import treeton.prosody.mdlcompiler.fsm.Meter;
import treeton.prosody.metricindex.MeterProbabilitiesCounter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class VerseProcessor {
    private final double meterMult;
    private final double footnessMult;
    private final double footnessVarianceMult;
    private final int numberOfVersesForAverageVector;
    private final boolean filterSmallProbabilities;
    private final boolean secondPass;
    private final boolean calculateFragments;

    public VerseProcessor(Properties properties) throws VerseProcessorException {
        String metricGrammarPath = properties.getProperty("metricGrammarPath");

        if (metricGrammarPath == null || metricGrammarPath.isEmpty() || !new File(metricGrammarPath).exists()) {
            throw new VerseProcessorException("Problem with metricGrammarPath", new FileNotFoundException(metricGrammarPath == null || metricGrammarPath.isEmpty() ? "<empty>" : metricGrammarPath));
        }

        double stressRestrictionViolationWeight;
        try {
            stressRestrictionViolationWeight = Double.valueOf(properties.getProperty("stressRestrictionViolationWeight"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with stressRestrictionViolationWeight", e);
        }
        double reaccentuationRestrictionViolationWeight;
        try {
            reaccentuationRestrictionViolationWeight = Double.valueOf(properties.getProperty("reaccentuationRestrictionViolationWeight"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with reaccentuationRestrictionViolationWeight", e);
        }
        double fragmentSimilarityThreshold;
        try {
            fragmentSimilarityThreshold = Double.valueOf(properties.getProperty("fragmentSimilarityThreshold"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with fragmentSimilarityThreshold", e);
        }
        int spacePerMeter;
        try {
            spacePerMeter = Integer.valueOf(properties.getProperty("spacePerMeter"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with spacePerMeter", e);
        }
        try {
            meterMult = Double.valueOf(properties.getProperty("meterMultiplier"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with meterMultiplier", e);
        }
        try {
            footnessMult = Double.valueOf(properties.getProperty("footnessMultiplier"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with footnessMultiplier", e);
        }
        try {
            footnessVarianceMult = Double.valueOf(properties.getProperty("footnessVarianceMultiplier"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with footnessVarianceMultiplier", e);
        }
        int maxStressRestrictionViolations;
        try {
            maxStressRestrictionViolations = Integer.valueOf(properties.getProperty("maxStressRestrictionViolations"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with maxStressRestrictionViolations", e);
        }
        int maxReaccentuationRestrictionViolations;
        try {
            maxReaccentuationRestrictionViolations = Integer.valueOf(properties.getProperty("maxReaccentuationRestrictionViolations"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with maxReaccentuationRestrictionViolations", e);
        }
        int maxSyllablesPerVerse;
        try {
            maxSyllablesPerVerse = Integer.valueOf(properties.getProperty("maxSyllablesPerVerse"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with maxSyllablesPerVerse", e);
        }
        boolean noMorph;
        try {
            noMorph = Boolean.valueOf(properties.getProperty("noMorph"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with noMorph", e);
        }
        try {
            filterSmallProbabilities = properties.containsKey("filterSmallProbabilities") ? Boolean.valueOf(properties.getProperty("filterSmallProbabilities")) : false;
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with filterSmallProbabilities", e);
        }

        try {
            if( properties.containsKey("secondPass") ) {
                secondPass =  Boolean.valueOf(properties.getProperty("secondPass"));
            } else {
                secondPass =  false;
            }
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with filterSmallProbabilities", e);
        }

        boolean heuristicOptimization;
        try {
            if( properties.containsKey("heuristicOptimization") ) {
                heuristicOptimization =  Boolean.valueOf(properties.getProperty("heuristicOptimization"));
            } else {
                heuristicOptimization =  true;
            }
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with heuristicOptimization", e);
        }

        try {
            if( properties.containsKey("calculateFragments") ) {
                calculateFragments =  Boolean.valueOf(properties.getProperty("calculateFragments"));
            } else {
                // По умолчанию фрагменты на втором проходе анализируем
                calculateFragments =  secondPass;
            }
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with filterSmallProbabilities", e);
        }

        try {
            numberOfVersesForAverageVector = Integer.valueOf(properties.getProperty("numberOfVersesForAverageVector"));
        } catch (Exception e) {
            throw new VerseProcessorException("Problem with numberOfVersesForAverageVector", e);
        }

        try {
            TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
            ResourcesContextXMLImpl resContext = (ResourcesContextXMLImpl)
                    ContextConfiguration.resourcesManager().get(ContextUtil.getFullName(trnContext));

            chain = new ResourceUtils().createResourceChain(resContext.getResourceChainModel("MusimatixProsodyChain", true));
            probsCounter = new MeterProbabilitiesCounter(trnContext, metricGrammarPath,
                    stressRestrictionViolationWeight, reaccentuationRestrictionViolationWeight, maxStressRestrictionViolations,
                    maxReaccentuationRestrictionViolations, maxSyllablesPerVerse, noMorph, fragmentSimilarityThreshold, heuristicOptimization );

            Collection<Meter> meters = probsCounter.getMeters();
            this.spacePerMeter = spacePerMeter == -1 ? 3 : spacePerMeter;
            averageFootnessMode = spacePerMeter == -1;

            metricVectorDimension = meters.size() * this.spacePerMeter + MeterProbabilitiesCounter.NUMBER_OF_CADENCES_TYPES;
            meterInsideVectorOrder = new HashMap<>();

            ArrayList<String> metersArray = new ArrayList<>();
            for (Meter meter : meters) {
                metersArray.add(meter.getName());
            }

            metersArray.sort(String::compareTo);

            for (int i = 0; i < metersArray.size(); i++) {
                String m = metersArray.get(i);
                meterInsideVectorOrder.put(m, i * this.spacePerMeter);
            }

            accVariantTp = trnContext.getType("AccVariant");
            syllableTp = trnContext.getType("Syllable");
            verseTp = trnContext.getType("Verse");
            userVariantFeatureId = accVariantTp.getFeatureIndex("userVariant");

            verseProcessingUtilities = new VerseProcessingUtilities(trnContext);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create VerseProcessor", e);
        }
    }

    public void initialize() {
        try {
            TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
            chain.initialize(trnContext);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize VerseProcessor", e);
        }
    }

    public void deinitialize() {
        try {
            chain.deInitialize();
        } catch (Exception e) {
            throw new RuntimeException("Unable to deinitialize VerseProcessor", e);
        }
    }

    public void setProgressListener(ProgressListener listener) {
        assert (chain != null);

        chain.setProgressListener(listener);
    }

    public void addLogListener(LogListener listener) {
        assert (chain != null);

        chain.addLogListener(listener);
    }

    public void removeLogListener(LogListener listener) {
        assert (chain != null);

        chain.removeLogListener(listener);
    }

    public int getMetricVectorDimension() {
        return metricVectorDimension;
    }

    public ArrayList<VerseDescription> process(Collection<String> verses, boolean onlySyllableInfo) {
        return process(verses, null, onlySyllableInfo);
    }

    public ArrayList<VerseDescription> process(Collection<String> verses,
                                               Collection<StressDescription> externalStressInfo, boolean onlySyllableInfo) {
        ArrayList<VerseDescription> result = new ArrayList<>();
        TreenotationsContext trnContext;
        try {
            trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
        } catch (ContextException e) {
            throw new RuntimeException("Unable to find prosody context", e);
        }

        TreenotationStorageImpl storage = (TreenotationStorageImpl) TreetonFactory.newTreenotationStorage(trnContext);

        StringBuilder sb = new StringBuilder();

        for (String verse : verses) {
            sb.append(verse);
            sb.append("\n");
        }

        try {
            chain.execute(sb.toString(), storage, new HashMap<>());
        } catch (ExecutionException e) {
            throw new RuntimeException("Error during processing lyrics: " + sb.toString(), e);
        }

        Map<Integer, TreenotationImpl> syllablesMap = new HashMap<>();
        TypeIteratorInterface syllablesIterator = storage.typeIterator(syllableTp, storage.firstToken(), storage.lastToken());

        while (syllablesIterator.hasNext()) {
            TreenotationImpl syllable = (TreenotationImpl) syllablesIterator.next();
            syllablesMap.put(syllable.getStartNumerator(), syllable);
        }

        HashSet<Treenotation> forceStressed = null;
        HashSet<Treenotation> forceUnstressed = null;
        Iterator<StressDescription> externalStressesIterator = externalStressInfo == null ? null : externalStressInfo.iterator();

        int offset = 0;
        for (String verse : verses) {
            if (verse.trim().isEmpty()) {
                offset+=verse.length() + 1;
                continue;
            }

            Token start = storage.getTokenByStartOffset(offset, 1);
            assert start != null;
            Token end = start;
            while (end.getEndNumerator() < offset + verse.length()) {
                end = end.getNextToken();
                assert end != null;
            }

            storage.add(TreetonFactory.newTreenotation(start, end, verseTp));

            if (externalStressesIterator != null) {
                assert externalStressesIterator.hasNext();

                forceStressed = new HashSet<>();
                forceUnstressed = new HashSet<>();

                StressDescription externalStressDescription = externalStressesIterator.next();

                for (SyllableInfo syllableInfo : externalStressDescription) {
                    if (syllableInfo.stressStatus == SyllableInfo.StressStatus.AMBIGUOUS) {
                        continue;
                    }

                    TreenotationImpl syllable = syllablesMap.get(offset + syllableInfo.startOffset);
                    if (syllable == null) { //TODO надо бы варнинги куда-то писать
                        continue;
                    }
                    int length = syllable.getEndNumerator() - syllable.getStartNumerator();
                    if (length != syllableInfo.length) { //TODO надо бы варнинги куда-то писать
                        continue;
                    }

                    TreenotationImpl accVariant = (TreenotationImpl) TreetonFactory.newSyntaxTreenotation(storage, syllable.getStartToken(),
                            syllable.getEndToken(), accVariantTp);
                    accVariant.put(userVariantFeatureId, Boolean.TRUE);
                    accVariant.addTree(new TreenotationImpl.Node(
                            syllableInfo.stressStatus == SyllableInfo.StressStatus.STRESSED ?
                                    TreenotationImpl.PARENT_CONNECTION_STRONG :
                                    TreenotationImpl.PARENT_CONNECTION_WEAK, syllable));
                    if (syllableInfo.stressStatus == SyllableInfo.StressStatus.STRESSED) {
                        forceStressed.add(syllable);
                    } else {
                        forceUnstressed.add(syllable);
                    }

                    storage.add(accVariant);
                }
            }

            offset += verse.length() + 1;
        }

        HashMap<String, List<double[]>> meterProbabilities = new HashMap<>();
        ArrayList<BitSet> stressSequences = secondPass ? new ArrayList<>() : null;
        ArrayList<Integer> fragmentIds = secondPass && calculateFragments ? new ArrayList<>() : null;
        probsCounter.countMeterProbabilities(storage, meterProbabilities, stressSequences, fragmentIds );

        if (stressSequences != null) {
            if (forceStressed == null) {
                forceStressed = new HashSet<>();
            }

            if (forceUnstressed == null) {
                forceUnstressed = new HashSet<>();
            }

            TypeIteratorInterface verseIterator = storage.typeIterator(verseTp, storage.firstToken(), storage.lastToken());

            int verseIndex = 0;
            while (verseIterator.hasNext()) {
                Treenotation verseTrn = (Treenotation) verseIterator.next();
                BitSet stressSequence = stressSequences.get(verseIndex++);

                List<Treenotation> syllables = verseProcessingUtilities.getVerseSyllables(storage, verseTrn);

                int i = 0;
                for (Treenotation syllable : syllables) {
                    if ( stressSequence != null && stressSequence.get(i++)) {
                        forceStressed.add(syllable);
                    } else {
                        forceUnstressed.add(syllable);
                    }
                }
            }
        }

        int verseIndex = -1;
        TypeIteratorInterface verseIterator = storage.typeIterator(verseTp, storage.firstToken(), storage.lastToken());
        for (String verse : verses) {
            if (verse.trim().isEmpty()) {
                if (onlySyllableInfo) {
                    result.add(new VerseDescription(null, new ArrayList<>(), fragmentIds == null ? -1 : fragmentIds.get(verseIndex)));
                    continue;
                }

                Vector<Double> metricVector = new Vector<>();
                metricVector.setSize(metricVectorDimension);
                for (int i = 0; i < metricVectorDimension; i++) {
                    metricVector.set(i, 0.0);
                }

                result.add(new VerseDescription(metricVector, new ArrayList<>(), -1));
                continue;
            }

            Treenotation verseTrn = (Treenotation) verseIterator.next();
            verseIndex++;

            if (onlySyllableInfo) {
                result.add(new VerseDescription(null, verseProcessingUtilities.generateSyllableInfo(storage, forceStressed, forceUnstressed, verseTrn),
                        fragmentIds == null ? -1 : fragmentIds.get(verseIndex)));
            } else {
                Vector<Double> metricVector = new Vector<>();
                metricVector.setSize(metricVectorDimension);

                if (averageFootnessMode) {
                    for( Meter meter : probsCounter.getMeters() ) {
                        String meterName = meter.getName();
                        List<double[]> probabilities = meterProbabilities.get(meterName);

                        Integer place = meterInsideVectorOrder.get(meterName);
                        assert place != null;

                        for (int footness = 0; footness < probabilities.size(); footness++) {
                            double[] probabilitiesForAllVerses = probabilities.get(footness);
                            if (probabilitiesForAllVerses == null || verseIndex >= probabilitiesForAllVerses.length) {
                                continue;
                            }

                            double p = probabilitiesForAllVerses[verseIndex];

                            Double oldp = metricVector.get(place);
                            if (oldp == null || p >= oldp) {
                                metricVector.set(place, p);
                                metricVector.set(place + 1, (double) footness);
                                metricVector.set(place + 2, null); // внутри одной строки разброс нулевой
                            }
                        }
                    }
                } else {
                    for ( Meter meter : probsCounter.getMeters() ) {
                        String meterName = meter.getName();
                        List<double[]> probabilities = meterProbabilities.get(meterName);

                        Integer place = meterInsideVectorOrder.get(meterName);
                        assert place != null;

                        for (int footness = 0; footness < spacePerMeter - 1 &&
                                footness < probabilities.size(); footness++) {
                            double[] probabilitiesForAllVerses = probabilities.get(footness);
                            double p = (probabilitiesForAllVerses == null || verseIndex >= probabilitiesForAllVerses.length) ? 0.0 :
                                    probabilitiesForAllVerses[verseIndex];

                            metricVector.set(place + footness, p);
                        }

                        double tail = 0;
                        int nonNullCount = 0;

                        for (int footness = spacePerMeter - 1; footness < probabilities.size(); footness++) {
                            double[] probabilitiesForAllVerses = probabilities.get(footness);
                            if (probabilitiesForAllVerses == null || verseIndex >= probabilitiesForAllVerses.length) {
                                continue;
                            }

                            tail += probabilitiesForAllVerses[verseIndex];
                            nonNullCount++;
                        }

                        if (tail > 0 && nonNullCount > 0) {
                            metricVector.set(place + spacePerMeter - 1, tail / nonNullCount);
                        }
                    }

                    for( int i = 0; i < MeterProbabilitiesCounter.NUMBER_OF_CADENCES_TYPES; i++) {
                        List<double[]> probabilities = meterProbabilities.get(MeterProbabilitiesCounter.Cadences[i]);

                        double[] probabilitiesForAllVerses = probabilities.get(0);
                        double p = (probabilitiesForAllVerses == null || verseIndex >= probabilitiesForAllVerses.length) ? 0.0 :
                                probabilitiesForAllVerses[verseIndex];

                        metricVector.set(metricVectorDimension - 3 + i, p);
                    }

                }

                result.add(new VerseDescription(metricVector, verseProcessingUtilities.generateSyllableInfo(storage, forceStressed, forceUnstressed, verseTrn), fragmentIds == null ? -1 : fragmentIds.get(verseIndex)));
            }
        }

        return result;
    }

    private ResourceChain chain;
    private MeterProbabilitiesCounter probsCounter;
    private int metricVectorDimension;
    private int spacePerMeter;
    private boolean averageFootnessMode;
    private Map<String, Integer> meterInsideVectorOrder;
    // Пока-что отказались идеи априорной дифференциации различных шаблонов
    //private Map<String,Double> meterStrength;
    private TrnType accVariantTp;
    private TrnType syllableTp;
    private TrnType verseTp;
    private int userVariantFeatureId;

    private VerseProcessingUtilities verseProcessingUtilities;

    public String formatMetricInfo(Vector<Double> metricVector) {
        Collection<Meter> meters = probsCounter.getMeters();
        ArrayList<String> metersArray = meters.stream().map(Meter::getName).collect(Collectors.toCollection(ArrayList::new));

        metersArray.sort(String::compareTo);

        StringBuilder sb = new StringBuilder();

        sb.append("( ");

        int i = 0;
        for (String meterName : metersArray) {
            sb.append(meterName);
            sb.append(" (");
            for (int j = 0; j < spacePerMeter; j++) {
                assert i < getMetricVectorDimension();
                Double aDouble = metricVector.elementAt(i++);
                if (aDouble == null) {
                    sb.append("0");
                } else {
                    sb.append(String.format("%.2f", aDouble.floatValue()));
                }
                sb.append(";");
            }

            sb.append(") ");
        }

        sb.append(" )");

        return sb.toString();
    }

    private static String locateUserAccentMarkup(String verse, ArrayList<Integer> stressPlaces, ArrayList<Pair<Integer, Integer>> unstressedZones) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < verse.length(); i++) {
            char c = verse.charAt(i);

            if (c == '{') {
                int j = i + 1;
                for (; j < verse.length(); j++) {
                    c = verse.charAt(j);
                    if (c == '}') {
                        break;
                    }
                }
                if (j == verse.length()) {
                    throw new RuntimeException("Unclosed '{' in the input file, verse" + verse);
                }

                String syl = verse.substring(i + 1, j);
                int sylPlace = syl.indexOf('\'');
                if (sylPlace == -1) {
                    unstressedZones.add(new Pair<>(sb.length(), sb.length() + syl.length()));
                    sb.append(syl);
                    i = j;
                } else if (sylPlace == 0) {
                    throw new RuntimeException("Accent sign right after '{', verse" + verse);
                } else {
                    if (!Character.isLetter(syl.charAt(sylPlace - 1))) {
                        throw new RuntimeException("Accent sign not after letter, verse" + verse);
                    }
                    stressPlaces.add(sb.length() + sylPlace - 1);
                    sb.append(syl.substring(0, sylPlace)).append(syl.substring(sylPlace + 1));
                    i = j;
                }
            } else if (c == '\\') {
                i++;
                if (i == verse.length()) {
                    break;
                }
                sb.append(verse.charAt(i));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public void parseFormattedVerses(ArrayList<String> formattedInput, ArrayList<String> plainOutput, ArrayList<StressDescription> stressDescriptions) throws Exception {
        TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
        ResourcesContextXMLImpl resContext = (ResourcesContextXMLImpl)
                ContextConfiguration.resourcesManager().get(ContextUtil.getFullName(trnContext));

        ResourceChain syllabizatorChain = new ResourceUtils().createResourceChain(resContext.getResourceChainModel("OnlySyllabizatorChain", true));
        syllabizatorChain.initialize(trnContext);

        TreenotationStorageImpl storage = (TreenotationStorageImpl) TreetonFactory.newTreenotationStorage(trnContext);
        TrnType syllableTp = trnContext.getType("Syllable");

        for (String verse : formattedInput) {
            ArrayList<Integer> stressPlaces = new ArrayList<>();
            ArrayList<Pair<Integer, Integer>> unstressedZones = new ArrayList<>();

            String pureString = locateUserAccentMarkup(verse, stressPlaces, unstressedZones);

            plainOutput.add(pureString);

            syllabizatorChain.execute(pureString, storage, new HashMap<>());

            Map<Integer, TreenotationImpl> syllablesCoverage = new HashMap<>();

            TypeIteratorInterface syllablesIterator = storage.typeIterator(syllableTp, storage.firstToken(), storage.lastToken());

            while (syllablesIterator.hasNext()) {
                TreenotationImpl syllable = (TreenotationImpl) syllablesIterator.next();

                for (int i = syllable.getStartNumerator(); i < syllable.getEndNumerator(); i++) {
                    syllablesCoverage.put(i, syllable);
                }
            }

            HashSet<TreenotationImpl> userStressedSyllables = new HashSet<>();

            for (Integer stressPlace : stressPlaces) {
                TreenotationImpl syllable = syllablesCoverage.get(stressPlace);

                if (syllable == null) {
                    throw new RuntimeException("No syllable for position " + stressPlace + ", verse " + verse);
                }

                userStressedSyllables.add(syllable);
            }

            HashSet<TreenotationImpl> userUnstressedSyllables = new HashSet<>();

            for (Pair<Integer, Integer> unstressedZone : unstressedZones) {
                boolean found = false;
                for (int j = unstressedZone.fst; j < unstressedZone.snd; j++) {
                    TreenotationImpl syllable = syllablesCoverage.get(j);

                    if (syllable == null) {
                        continue;
                    }

                    found = true;

                    if (userStressedSyllables.contains(syllable)) {
                        throw new RuntimeException("Ambiguous user accent markup, syllable " + syllable.getText() + ", verse " + verse);
                    }

                    userUnstressedSyllables.add(syllable);
                }

                if (!found) {
                    System.err.println("No syllable for zone [" + unstressedZone.fst + "," + unstressedZone.snd + "], verse " + verse);
                    return;
                }
            }

            ArrayList<SyllableInfo> sylInfo = new ArrayList<>();

            for (TreenotationImpl syllable : userStressedSyllables) {
                sylInfo.add(new SyllableInfo(syllable.getStartNumerator(),
                        syllable.getEndNumerator() - syllable.getStartNumerator(), SyllableInfo.StressStatus.STRESSED));
            }

            for (TreenotationImpl syllable : userUnstressedSyllables) {
                sylInfo.add(new SyllableInfo(syllable.getStartNumerator(),
                        syllable.getEndNumerator() - syllable.getStartNumerator(), SyllableInfo.StressStatus.UNSTRESSED));
            }

            stressDescriptions.add(new StressDescription(sylInfo));
        }
    }

    public Vector<Double> countAverage(ArrayList<VerseDescription> verseDescriptions, int firstLineIndex) {
        Vector<Double> averageVector = new Vector<>();
        averageVector.setSize(metricVectorDimension);

        int numberOfNonEmptyLines = 0;
        for (int i = firstLineIndex; i < verseDescriptions.size(); i++) {
            VerseDescription verseDescription = verseDescriptions.get(i);

            if (verseDescription.metricVector == null) {
                continue;
            }

            int j = 0;
            for (; j < metricVectorDimension; j++) {
                Double current = verseDescription.metricVector.get(j);
                if (current != null && current != 0.0) {
                    break;
                }
            }

            if (j == metricVectorDimension) {
                continue;
            }

            if (numberOfVersesForAverageVector != -1 && numberOfNonEmptyLines == numberOfVersesForAverageVector) {
                break;
            }

            numberOfNonEmptyLines++;

            for (j = 0; j < metricVectorDimension; j++) {
                Double average = averageVector.get(j);
                Double current;

                if (averageFootnessMode && (j % 3) == 2 && j < metricVectorDimension - MeterProbabilitiesCounter.NUMBER_OF_CADENCES_TYPES ) {
                    current = verseDescription.metricVector.get(j - 2);
                    if (current != null) {
                        current *= verseDescription.metricVector.get(j - 1);
                    }
                } else {
                    current = verseDescription.metricVector.get(j);
                }

                Double newValue;

                if (average == null) {
                    newValue = current;
                } else if (current == null) {
                    newValue = average;
                } else {
                    newValue = current + average;
                }
                averageVector.set(j, newValue);
            }
        }

        for (int i = 0; i < metricVectorDimension; i++) {
            Double aDouble = averageVector.get(i);

            if (aDouble == null) {
                continue;
            }

            if (numberOfNonEmptyLines == 0) {
                averageVector.set(i, null);
                continue;
            }

            aDouble = aDouble / numberOfNonEmptyLines;
            averageVector.set(i, (!filterSmallProbabilities || aDouble > 0.5) ? aDouble : null);
        }

        if (averageFootnessMode) {
            // в каждом третьем измерении расчитаем отклонения

            Vector<Double> variances = new Vector<>();
            variances.setSize(metricVectorDimension - MeterProbabilitiesCounter.NUMBER_OF_CADENCES_TYPES);
            numberOfNonEmptyLines = 0;
            for (VerseDescription verseDescription : verseDescriptions) {
                if (verseDescription.metricVector == null) {
                    continue;
                }

                int i = 0;
                for (; i < metricVectorDimension - MeterProbabilitiesCounter.NUMBER_OF_CADENCES_TYPES; i++) {
                    Double current = verseDescription.metricVector.get(i);
                    if (current != null && current != 0.0) {
                        break;
                    }
                }

                if (i == metricVectorDimension) {
                    continue;
                }

                if (numberOfVersesForAverageVector != -1 && numberOfNonEmptyLines == numberOfVersesForAverageVector) {
                    break;
                }

                numberOfNonEmptyLines++;

                for (i = 2; i < metricVectorDimension - MeterProbabilitiesCounter.NUMBER_OF_CADENCES_TYPES; i += 3) {
                    Double averageFootness = averageVector.get(i);
                    if (averageFootness == null) {
                        continue;
                    }

                    Double currentMeter = verseDescription.metricVector.get(i - 2);
                    Double currentFootness;
                    if (currentMeter == null) {
                        currentFootness = 0.0;
                    } else {
                        currentFootness = currentMeter * verseDescription.metricVector.get(i - 1);
                    }

                    Double averageVariance = variances.get(i);
                    Double newValue;

                    if (averageVariance == null) {
                        newValue = (currentFootness - averageFootness) * (currentFootness - averageFootness);
                    } else {
                        newValue = averageVariance + (currentFootness - averageFootness) * (currentFootness - averageFootness);
                    }

                    variances.set(i, newValue);
                }
            }

            for (int i = 0; i < metricVectorDimension - MeterProbabilitiesCounter.NUMBER_OF_CADENCES_TYPES; i++) {
                int x = i % 3;

                if (x == 0) {
                    Double aDouble = averageVector.get(i);
                    averageVector.set(i, (aDouble == null ? null : aDouble * meterMult));
                } else if (x == 1) {
                    Double aDouble = averageVector.get(i);
                    averageVector.set(i, (aDouble == null ? null : aDouble * footnessMult));
                } else {
                    Double aDouble = variances.get(i);
                    if (aDouble == null) {
                        averageVector.set(i, null);
                    } else {
                        aDouble = aDouble / numberOfNonEmptyLines;
                        averageVector.set(i, aDouble * footnessVarianceMult);
                    }
                }
            }
        }

        return averageVector;
    }

    public double countAverageDistance(Vector<Double> x, Vector<Double> y) {
        assert x.size() == y.size() && x.size() > 0;
        double d = 0.0;
        for (int k = 0; k < x.size(); k++) {
            Double xk = x.get(k);
            Double yk = y.get(k);

            double delta = (xk == null ? 0.0 : xk) - (yk == null ? 0.0 : yk);
            d += delta * delta;
        }

        return Math.sqrt(d);
    }

    public class VerseProcessorException extends Throwable {
        public VerseProcessorException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public String getHtmlFormattedMetricVector(Vector<Double> metricVector) {
        assert metricVector.size() == metricVectorDimension;

        Collection<Meter> meters = probsCounter.getMeters();
        ArrayList<String> metersArray = meters.stream().map(Meter::getName).collect(Collectors.toCollection(ArrayList::new));

        metersArray.sort(String::compareTo);

        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (String meterName : metersArray) {
            sb.append("<b>").append(meterName).append("</b>: ");
            for (int j = 0; j < spacePerMeter; j++) {
                if (j > 0) {
                    sb.append(", ");
                }
                assert i < getMetricVectorDimension();
                Double aDouble = metricVector.elementAt(i++);
                if (aDouble == null) {
                    sb.append("0.00");
                } else {
                    sb.append(String.format("%.2f", aDouble.floatValue()));
                }
            }

            sb.append("<br/>");
        }

        sb.append("<b>").append("Masculine cadence").append("</b>: ").append(metricVector.elementAt(metricVectorDimension-3));
        sb.append("<br/>");
        sb.append("<b>").append("Feminine cadence").append("</b>: ").append(metricVector.elementAt(metricVectorDimension-2));
        sb.append("<br/>");
        sb.append("<b>").append("Dactylic cadence").append("</b>: ").append(metricVector.elementAt(metricVectorDimension-1));
        sb.append("<br/>");

        return sb.toString();
    }

    public PreciseVerseDistanceCounter createVerseDistanceCounter(ArrayList<VerseDescription> verseDescriptions, int firstLineIndex) {
        PreciseVerseDistanceCounter result = new PreciseVerseDistanceCounter(verseDescriptions, countAverage(verseDescriptions,firstLineIndex), firstLineIndex, averageFootnessMode, meterMult, footnessMult);
        if( !averageFootnessMode ) {
            int[] dimensionPriorities = new int[metricVectorDimension];
            boolean[] multOrDeltaForDimensions = new boolean[metricVectorDimension];

            Collection<Meter> meters = probsCounter.getMeters();
            ArrayList<Meter> metersArray = new ArrayList<>( meters );

            for (Meter meter : metersArray) {
                Integer offset = meterInsideVectorOrder.get(meter.getName());
                for (int j = 0; j < spacePerMeter; j++) {
                    int priority = meter.getPriority();
                    assert priority >= 0;
                    dimensionPriorities[offset+j]= priority;
                    multOrDeltaForDimensions[offset+j]=true;
                }
            }

            dimensionPriorities[metricVectorDimension-3]=probsCounter.getMaxMeterPriority()+1;
            dimensionPriorities[metricVectorDimension-2]=probsCounter.getMaxMeterPriority()+1;
            dimensionPriorities[metricVectorDimension-1]=probsCounter.getMaxMeterPriority()+1;
            multOrDeltaForDimensions[metricVectorDimension-3]=false;
            multOrDeltaForDimensions[metricVectorDimension-2]=false;
            multOrDeltaForDimensions[metricVectorDimension-1]=false;

            result.setDimensionInfo(probsCounter.getMaxMeterPriority()+2,dimensionPriorities,multOrDeltaForDimensions);
        }
        return result;
    }
}
