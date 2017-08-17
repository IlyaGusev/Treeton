/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru.prosody;

import treeton.core.*;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TrnType;
import treeton.prosody.metricindex.MeterProbabilitiesCounter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class AccentDisambiguator extends Resource {
    public String process(String text, TextMarkingStorage _storage, Map<String, Object> runtimeParameters) throws ExecutionException {
        assert _storage instanceof TreenotationStorageImpl;

        TreenotationStorageImpl storage = (TreenotationStorageImpl) _storage;

        Map<String,List<double[]>> probabilities = new HashMap<>();
        List<BitSet> stressSequences = new ArrayList<>();
        ArrayList<Integer> fragmentIds = new ArrayList<>();

        probsCounter.countMeterProbabilities( storage, probabilities, stressSequences, fragmentIds );

        int i = 0;
        TypeIteratorInterface verseIt = storage.typeIterator(verseTp);
        while (verseIt.hasNext()) {
            Treenotation verse = (Treenotation) verseIt.next();
            BitSet stresses = stressSequences.get(i++);

            if( stresses == null ) {
                continue;
            }

            TypeIteratorInterface syllIt = storage.typeIterator(syllTp, verse.getStartToken(), verse.getEndToken());

            Map<Treenotation,Integer> syllIndexes = new HashMap<>();

            while (syllIt.hasNext()) {
                Treenotation syll = (Treenotation) syllIt.next();

                syllIndexes.put(syll,syllIndexes.size());
            }

            List<TreenotationImpl> toDelete = new ArrayList<>();

            TypeIteratorInterface accVariantIt = storage.typeIterator(accvarTp, verse.getStartToken(), verse.getEndToken());

            while( accVariantIt.hasNext() ) {
                TreenotationImpl accVariant = (TreenotationImpl) accVariantIt.next();

                boolean contradictionFound = false;
                TreenotationImpl.Node[] syllables = accVariant.getTrees();

                for (TreenotationImpl.Node syllableNode : syllables) {
                    int syllIndex = syllIndexes.get(syllableNode.getTrn());
                    boolean isStressed = syllableNode.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG;

                    if( isStressed != stresses.get(syllIndex) ) {
                        contradictionFound = true;
                        break;
                    }
                }

                if( contradictionFound ) {
                    toDelete.add(accVariant);
                }
            }

            for (TreenotationImpl treenotation : toDelete) {
                storage.remove(treenotation);
            }
        }

        return null;
    }

    TrnType verseTp;
    TrnType syllTp;
    TrnType accvarTp;
    private MeterProbabilitiesCounter probsCounter;

    public void init() throws ResourceInstantiationException {
        try {
            verseTp = getTrnContext().getType("Verse");
            syllTp = getTrnContext().getType("Syllable");
            accvarTp = getTrnContext().getType("AccVariant");

            URL path;
            try {
                path = new URL(getResContext().getFolder(), (String) getInitialParameters().get("verseProcessingPropertiesPath"));
            } catch (MalformedURLException e) {
                throw new ResourceInstantiationException("Malformed url exception during MdlMeterAnalyzer instantiation", e);
            }

            Properties properties = new Properties();
            FileInputStream propsStream = new FileInputStream(path.getPath());
            properties.load(propsStream);
            propsStream.close();

            String metricGrammarPath = properties.getProperty("metricGrammarPath");
            if (metricGrammarPath == null || metricGrammarPath.isEmpty()) {
                throw new ResourceInstantiationException("metricGrammarPath is absent");
            } else {
                File metricGrammarFile = new File( new File(path.getPath()).getParent(), metricGrammarPath );

                if( !metricGrammarFile.exists() ) {
                    throw new ResourceInstantiationException("Problem with metricGrammarPath", new FileNotFoundException( metricGrammarPath ));
                }
                metricGrammarPath = metricGrammarFile.getPath();
            }

            double stressRestrictionViolationWeight = Double.valueOf(properties.getProperty("stressRestrictionViolationWeight"));
            double reaccentuationRestrictionViolationWeight = Double.valueOf(properties.getProperty("reaccentuationRestrictionViolationWeight"));
            double fragmentSimilarityThreshold = Double.valueOf(properties.getProperty("fragmentSimilarityThreshold"));
            int maxStressRestrictionViolations = Integer.valueOf(properties.getProperty("maxStressRestrictionViolations"));
            int maxReaccentuationRestrictionViolations = Integer.valueOf(properties.getProperty("maxReaccentuationRestrictionViolations"));
            int maxSyllablesPerVerse = Integer.valueOf(properties.getProperty("maxSyllablesPerVerse"));

            boolean heuristicOptimization = true;
            if( properties.containsKey("heuristicOptimization") ) {
                heuristicOptimization =  Boolean.valueOf(properties.getProperty("heuristicOptimization"));
            } else {
                heuristicOptimization =  true;
            }
            probsCounter = new MeterProbabilitiesCounter(getTrnContext(), metricGrammarPath, stressRestrictionViolationWeight, reaccentuationRestrictionViolationWeight,
                    maxStressRestrictionViolations, maxReaccentuationRestrictionViolations, maxSyllablesPerVerse, false, fragmentSimilarityThreshold, heuristicOptimization);
        } catch (Exception e) {
            deInit();
            throw new ResourceInstantiationException(null, "Exception during AccentGenerator instantiation: " + e.getMessage(), e);
        }
    }

    public void deInit() {
        System.out.println( "MeterProbabilitiesCounter stats: " + probsCounter.getStats() );
    }

    public void stop() {}

    public void processTerminated() {}
}