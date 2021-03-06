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
import treeton.prosody.StressDescription;
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

            offset += verse.length() + 1;
        }

        HashSet<Treenotation> forceStressed = null;
        HashSet<Treenotation> forceUnstressed = null;

        if(externalStressInfo != null) {
            forceStressed = new HashSet<>();
            forceUnstressed = new HashSet<>();

            verseProcessingUtilities.injectSyllableInfo(storage, forceStressed, forceUnstressed, externalStressInfo);
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

                result.add(new VerseDescription(metricVector, verseProcessingUtilities.generateSyllableInfo(storage, forceStressed, forceUnstressed, verseTrn), fragmentIds == null ? -1 : fragmentIds.get(verseIndex)));
            }
        }

        return result;
    }

    private ResourceChain chain;
    private MeterProbabilitiesCounter probsCounter;
    private int metricVectorDimension;
    private int spacePerMeter;
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

    void parseRawSyllables(ArrayList<String> rawSyllableStrings, ArrayList<String> plainOutput, ArrayList<StressDescription> stressDescriptions) throws Exception {
        for (String syllableString : rawSyllableStrings) {
            int bracketIndex = syllableString.indexOf('(');
            if(bracketIndex >= 0) {
                syllableString = syllableString.substring(0, bracketIndex);
            }
            String[] syllables = syllableString.split(";");

            ArrayList<SyllableInfo> syllInfos = new ArrayList<>();
            StringBuilder buf = new StringBuilder();
            int prevEnd = -1;
            for (String syllable : syllables) {
                if(syllable.isEmpty()) {
                    continue;
                }

                String[] syllDescr = syllable.split(",");

                int shift = Integer.valueOf(syllDescr[0]);
                int length = Integer.valueOf(syllDescr[1]);
                boolean stressed = syllDescr[2].equals("S");

                if(prevEnd >= 0 && prevEnd != shift) {
                    buf.append(' ');
                }

                prevEnd = shift + length;

                syllInfos.add(new SyllableInfo(buf.length(),2,
                        stressed ? SyllableInfo.StressStatus.STRESSED : SyllableInfo.StressStatus.UNSTRESSED));

                buf.append("ла");
            }

            plainOutput.add(buf.toString());
            stressDescriptions.add(new StressDescription(syllInfos));
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
                Double current = verseDescription.metricVector.get(j);
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

    public PreciseVerseDistanceCounter createVerseDistanceCounter(ArrayList<VerseDescription> verseDescriptions,
                                                                  int firstLineIndex, double[] regressionCoefficients)
    {

        int[] meterRegressionIndex = new int[metricVectorDimension];
        PreciseVerseDistanceCounter.DimensionOperation[] dimensionOperations =
                new PreciseVerseDistanceCounter.DimensionOperation[metricVectorDimension];
        Collection<Meter> meters = probsCounter.getMeters();
        ArrayList<Meter> metersArray = new ArrayList<>( meters );

        // Все измерения, кроме 3 последних - типы метра: ямб, хорей и т.д.
        // Их "вероятности" между строчками мы будем потом перемножать.
        // В зависимости от колонки потом им будут проставлены разные веса.
        for( Meter meter : metersArray ) {
            Integer offset = meterInsideVectorOrder.get( meter.getName() );
            for (int j = 0; j < spacePerMeter; j++) {
                int regressionIndex = -1;
                if (meter.getName().contains("Амфибрахий")) {
                    regressionIndex = 0;
                } else if (meter.getName().contains("Анапест")) {
                    regressionIndex = 1;
                } else if (meter.getName().contains("Дактиль")) {
                    regressionIndex = 2;
                } else if (meter.getName().contains("Хорей")) {
                    regressionIndex = 3;
                } else if (meter.getName().contains("Ямб")) {
                    regressionIndex = 4;
                }
                meterRegressionIndex[offset+j] = regressionIndex;
                dimensionOperations[offset+j] = PreciseVerseDistanceCounter.DimensionOperation.Multiplication;
            }
        }

        // Последние 3 измерения - тип рифмы, у них отличается способ сложения.
        meterRegressionIndex[metricVectorDimension-3] = 5;
        meterRegressionIndex[metricVectorDimension-2] = 5;
        meterRegressionIndex[metricVectorDimension-1] = 5;
        dimensionOperations[metricVectorDimension-3] = PreciseVerseDistanceCounter.DimensionOperation.Delta;
        dimensionOperations[metricVectorDimension-2] = PreciseVerseDistanceCounter.DimensionOperation.Delta;
        dimensionOperations[metricVectorDimension-1] = PreciseVerseDistanceCounter.DimensionOperation.Delta;

        return new PreciseVerseDistanceCounter( verseDescriptions, firstLineIndex,
                6, meterRegressionIndex,
                dimensionOperations, regressionCoefficients );
    }
}
