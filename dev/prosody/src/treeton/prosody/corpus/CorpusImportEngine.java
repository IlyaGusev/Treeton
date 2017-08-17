/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import treeton.core.*;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.ResourceChain;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.FileMapper;
import treeton.core.util.ProgressListener;
import treeton.prosody.StressDescription;
import treeton.prosody.VerseProcessingUtilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class CorpusImportEngine {
    CorpusImportEngine(ResourceChain preprocessingChain, ResourceChain postprocessingChain) throws TreetonModelException, ContextException, ResourceInstantiationException {
        TreenotationsContext trnContext = preprocessingChain.getTrnContext();

        corpusElementTrnType = trnContext.getType("CorpusElement");
        ignoredTextTrnType = trnContext.getType("IgnoredText");
        verseTrnType = trnContext.getType("Verse");

        authorFeature = corpusElementTrnType.getFeatureIndex("author");
        titleFeature = corpusElementTrnType.getFeatureIndex("title");
        yearFeature = corpusElementTrnType.getFeatureIndex("year");
        meterFeature = corpusElementTrnType.getFeatureIndex("meter");
        cycleFeature = corpusElementTrnType.getFeatureIndex("cycle");

        this.preprocessingChain = preprocessingChain;
        this.postprocessingChain = postprocessingChain;

        verseProcessingUtilities = new VerseProcessingUtilities(trnContext);
    }

    private TrnType corpusElementTrnType;
    private TrnType ignoredTextTrnType;
    private TrnType verseTrnType;
    private int authorFeature;
    private int titleFeature;
    private int yearFeature;
    private int meterFeature;
    private int cycleFeature;
    private ResourceChain preprocessingChain;
    private ResourceChain postprocessingChain;
    private VerseProcessingUtilities verseProcessingUtilities;

    void Import( CorpusFolder targetFolder, File source, ProgressListener progressListener ) throws IOException, CorpusException, ExecutionException {
        Corpus corpus = targetFolder.getCorpus();

        if( source.isDirectory() ) {
            importFolder(targetFolder, source, progressListener, corpus);
        } else {
            importFile(targetFolder, source, progressListener, corpus);
        }
    }

    private void importFolder(CorpusFolder targetFolder, File folder, ProgressListener progressListener, Corpus corpus) throws IOException, CorpusException, ExecutionException {
        ArrayList<File> toProcess = new ArrayList<>();

        Collections.addAll(toProcess, folder.listFiles(
                (dir, name) -> new File(dir,name).isFile() && name.endsWith(".txt")
        ));

        for (File f : toProcess) {
            char[] chars = FileMapper.map2memory(f, "UTF-8");

            CorpusEntry entry = corpus.createEntry( f.getName(), targetFolder );
            corpus.changeEntryText(entry, new String(chars));
            TreenotationStorageImpl metadataStorage = entry.getMetadata();

            preprocessingChain.setProgressListener(progressListener);
            preprocessingChain.execute(entry.getText(),metadataStorage, new HashMap<>());
            preprocessingChain.setProgressListener(null);

            Token first = metadataStorage.firstToken();
            Token last = metadataStorage.lastToken();
            Treenotation corpusElement = TreetonFactory.newTreenotation(first,last,corpusElementTrnType);

            corpusElement.put( titleFeature, TreetonFactory.newTString( f.getName() ) );
            metadataStorage.add( corpusElement );

            int verseStart = -1;
            for(int i = 0; i < chars.length;i++) {
                char c = chars[i];
                if(c == '\n') {
                    if(verseStart != -1) {
                        Token start = metadataStorage.getTokenByStartOffset( verseStart, 1 );
                        Token end = start;
                        while( end != null ) {
                            if( end.getEndNumerator() == i && end.getEndDenominator() == 1 ) {
                                break;
                            }
                            end = end.getNextToken();
                        }
                        if( start == null || end == null ) {
                            throw new CorpusException("Something wrong with tokeniser. Offsets of verse do not match token bounds.");
                        }

                        Treenotation verseTrn = TreetonFactory.newTreenotation(start,end,verseTrnType);
                        metadataStorage.add(verseTrn);

                        verseStart = -1;
                    }
                } else if(verseStart == -1) {
                    verseStart = i;
                }
            }

            if( postprocessingChain != null ) {
                postprocessingChain.setProgressListener(progressListener);
                postprocessingChain.execute(entry.getText(), metadataStorage, new HashMap<>());
                postprocessingChain.setProgressListener(null);
            }

            // нулевая версию трактуем как, что лингвоакцентный анализ произведен, а ритмико-метрический - нет
            corpus.metadataWasReloaded(entry);
        }
    }

    private void importFile(CorpusFolder targetFolder, File source, ProgressListener progressListener, Corpus corpus) throws IOException, CorpusException, ExecutionException {
        SimpleCorpusTextFormatParser parser = new SimpleCorpusTextFormatParser( source );

        while( parser.nextFragment() ) {
            CorpusEntry entry = corpus.createEntry( parser.getProperties().get("title"), targetFolder );
            corpus.changeEntryText(entry,parser.getWholeFragmentText());
            TreenotationStorageImpl metadataStorage = entry.getMetadata();

            preprocessingChain.setProgressListener(progressListener);
            preprocessingChain.execute(entry.getText(),metadataStorage, new HashMap<>());
            preprocessingChain.setProgressListener(null);

            Token first = metadataStorage.firstToken();
            Token last = metadataStorage.lastToken();
            Treenotation corpusElement = TreetonFactory.newTreenotation(first,last,corpusElementTrnType);

            Map<String,String> properties = parser.getProperties();
            if( properties.containsKey("author")) {
                corpusElement.put( authorFeature, TreetonFactory.newTString( properties.get("author") ) );
            }
            if( properties.containsKey("title")) {
                corpusElement.put( titleFeature, TreetonFactory.newTString( properties.get("title") ) );
            }
            if( properties.containsKey("year")) {
                corpusElement.put( yearFeature, Integer.valueOf(properties.get("year")) );
            }
            if( properties.containsKey("meter")) {
                corpusElement.put( meterFeature, TreetonFactory.newTString( properties.get("meter") ) );
            }
            if( properties.containsKey("cycle")) {
                corpusElement.put( cycleFeature, TreetonFactory.newTString( properties.get("cycle") ) );
            }
            metadataStorage.add( corpusElement );

            ArrayList<StressDescription> stressDescriptions = new ArrayList<>();

            int startOffset = 0;
            for (SimpleCorpusTextFormatParser.Chunk chunk : parser.getChunks()) {
                int endOffset = startOffset + chunk.getText().length();

                if(endOffset - startOffset > 1) {
                    Token start = metadataStorage.getTokenByStartOffset(startOffset, 1);
                    Token end = start;
                    while (end != null) {
                        if (end.getEndNumerator() == endOffset && end.getEndDenominator() == 1) {
                            break;
                        }
                        end = end.getNextToken();
                    }
                    if (start == null || end == null) {
                        throw new CorpusException("Something wrong with tokeniser. Offsets of ignored area do not match token bounds.");
                    }
                    if (chunk.isNoIndex()) {
                        Treenotation ignoredZone = TreetonFactory.newTreenotation(start, end, ignoredTextTrnType);
                        metadataStorage.add(ignoredZone);
                    } else {
                        Treenotation verseTrn = TreetonFactory.newTreenotation(start, end, verseTrnType);
                        metadataStorage.add(verseTrn);
                        stressDescriptions.add(chunk.getStressDescription());
                    }
                }

                startOffset = endOffset;
            }

            verseProcessingUtilities.injectSyllableInfo(metadataStorage, stressDescriptions);

            if( postprocessingChain != null ) {
                postprocessingChain.setProgressListener(progressListener);
                postprocessingChain.execute(entry.getText(), metadataStorage, new HashMap<>());
                postprocessingChain.setProgressListener(null);
            }

            // нулевую версию трактуем как, что лингвоакцентный анализ произведен, а ритмико-метрический - нет
            corpus.metadataWasReloaded(entry);
        }
    }
}
