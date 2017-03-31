/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.metricindex;

import treeton.core.model.TreetonModelException;
import treeton.prosody.corpus.Corpus;
import treeton.prosody.corpus.CorpusEntry;
import treeton.prosody.corpus.CorpusFolder;
import treeton.prosody.corpus.CorpusListener;
import treeton.prosody.mdlcompiler.fsm.Meter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MetricIndex implements CorpusListener {
    public static String MDL_ANLYZER_GRAMMAR_PATH_PROPERTY_NAME = "mdlMeterAnalyzerGrammarPath";
    private Corpus corpus;
    private long mdlFileTimestamp = -1;
    private File mdlFile;

    MeterProbabilitiesCounter probsCounter;

    class EntryInfo {
        // метр (ключ в мапе) - стопность (позиция в List-е) - строка (позиция в массиве даблов)
        Map<String,List<double[]>> meterProbabilities = new HashMap<>();
        long mdlTimestamp;
        int manualEditionStamp;
    }

    Map<String,EntryInfo> metricInfo = new HashMap<>();

    public MetricIndex(Corpus corpus) throws TreetonModelException {
        this.corpus = corpus;
        corpus.addListener( this );
        File rootPath = corpus.getRootPath();
        String mdlRelativePath = corpus.getGlobalProperty( MDL_ANLYZER_GRAMMAR_PATH_PROPERTY_NAME );

        if( mdlRelativePath == null ) {
            return;
        }

        mdlFile = new File( rootPath, mdlRelativePath );

        if( !mdlFile.exists() ) {
            mdlFile = null;
            return;
        }

        try {
            mdlFileTimestamp = mdlFile.lastModified();
            probsCounter = new MeterProbabilitiesCounter( corpus.getTrnContext(), mdlFile.getPath(), 1, 3, 3, 2, 23, false, 0.8, true);
        } catch (Exception e) {
            e.printStackTrace();
            probsCounter = null;
            mdlFile = null;
            mdlFileTimestamp = -1;
        }
    }

    public Collection<String> getMeters() {
        checkMdlFile();

        if( probsCounter == null ) {
            return new ArrayList<>();
        }

        return probsCounter.getMeters().stream().map(Meter::getName).collect(Collectors.toCollection(ArrayList::new));
    }

    public double[] getMeterProbabilities( CorpusEntry entry, String meter, int footCount ) {
        checkMdlFile();
        EntryInfo info = retrieveEntryInfo( entry );

        List<double[]> probs = info.meterProbabilities.get(meter);

        if( probs == null ) {
            return null;
        }
        assert footCount >= 0;

        if( probs.size() <= footCount ) {
            return null;
        }

        return probs.get( footCount );
    }

    public int getMaxFootCount( CorpusEntry entry, String meter ) {
        checkMdlFile();
        EntryInfo info = retrieveEntryInfo( entry );

        List<double[]> result = info.meterProbabilities.get(meter);

        return result == null ? 0 : result.size() - 1;
    }

    public void close() {
        corpus.removeListener( this );
    }

    @Override
    public void entryCreated(CorpusEntry entry) {
        // сразу не индексируем, все делаем лениво
    }

    @Override
    public void entryDeleted(CorpusEntry entry, Collection<CorpusFolder> parentFolders) {
        metricInfo.remove( entry.getGuid() );
    }

    @Override
    public void entryNameChanged(CorpusEntry entry) {
    }

    @Override
    public void entryTextChanged(CorpusEntry entry) {
        metricInfo.remove( entry.getGuid() );
    }

    @Override
    public void entryMetadataManuallyEdited(CorpusEntry entry) {
        metricInfo.remove( entry.getGuid() );
    }

    @Override
    public void entryMetadataReloaded(CorpusEntry entry) {
        metricInfo.remove( entry.getGuid() );
    }

    @Override
    public void folderCreated(CorpusFolder folder) {
    }

    @Override
    public void folderNameChanged(CorpusFolder folder) {
    }

    @Override
    public void folderParentChanged(CorpusFolder folder, CorpusFolder oldParent) {
    }

    @Override
    public void entryWasPlacedIntoFolder(CorpusEntry entry, CorpusFolder folder) {
    }

    @Override
    public void entryWasRemovedFromFolder(CorpusEntry entry, CorpusFolder folder) {
    }

    @Override
    public void folderDeleted(CorpusFolder folder) {
    }

    @Override
    public void corpusLabelChanged() {
    }

    @Override
    public void globalCorpusPropertyChanged(String propertyName) {
        File rootPath = corpus.getRootPath();
        String mdlRelativePath = corpus.getGlobalProperty( MDL_ANLYZER_GRAMMAR_PATH_PROPERTY_NAME );

        if( mdlRelativePath == null ) {
            metricInfo.clear();
            probsCounter = null;
            mdlFile = null;
            mdlFileTimestamp = -1;
            return;
        }

        File newMdlFile = new File( rootPath, mdlRelativePath );

        if( !newMdlFile.exists() ) {
            metricInfo.clear();
            probsCounter = null;
            mdlFile = null;
            mdlFileTimestamp = -1;
            return;
        }

        if( mdlFile != null && newMdlFile.compareTo( mdlFile ) == 0 ) {
            return;
        }

        metricInfo.clear();
        mdlFile = newMdlFile;

        try {
            mdlFileTimestamp = mdlFile.lastModified();
            probsCounter = new MeterProbabilitiesCounter( corpus.getTrnContext(), mdlFile.getPath(), 1, 3, 2, 1, 23, false, 0.8, true);
        } catch (Exception e) {
            e.printStackTrace();
            probsCounter = null;
            mdlFile = null;
            mdlFileTimestamp = -1;
        }
    }

    private void checkMdlFile() {
        if( mdlFile == null ) {
            return;
        }

        if( mdlFile.lastModified() > mdlFileTimestamp ) {
            metricInfo.clear();
            try {
                mdlFileTimestamp = mdlFile.lastModified();
                probsCounter = new MeterProbabilitiesCounter( corpus.getTrnContext(), mdlFile.getPath(), 1, 3, 2, 1, 23, false, 0.8, true);
            } catch (Exception e) {
                e.printStackTrace();
                probsCounter = null;
                mdlFile = null;
                mdlFileTimestamp = -1;
            }
        }
    }

    private EntryInfo retrieveEntryInfo(CorpusEntry entry) {
        EntryInfo info = metricInfo.get( entry.getGuid() );

        if( info == null || info.mdlTimestamp < mdlFileTimestamp || info.manualEditionStamp < entry.getManualEditionStamp() ) {
            info = calculateInfo( entry );
            metricInfo.put( entry.getGuid(), info );
        }

        return info;
    }


    private EntryInfo calculateInfo(CorpusEntry entry) {
        EntryInfo result = new EntryInfo();

        result.mdlTimestamp = mdlFileTimestamp;
        result.manualEditionStamp = entry.getManualEditionStamp();

        probsCounter.countMeterProbabilities( entry.getMetadata(), result.meterProbabilities, null, null );

        return result;
    }

}
