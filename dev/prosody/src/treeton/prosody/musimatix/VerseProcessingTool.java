/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.musimatix;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationProsodyImpl;
import treeton.core.util.ObjectPair;
import treeton.prosody.SyllableInfo;

import java.io.*;
import java.util.*;

public class VerseProcessingTool {
    private static void printUsage() {
        System.out.println("Usage: 'VerseProcessingTool algorithmPropertiesFile lyricsFile'");
        System.out.println("or 'VerseProcessingTool --distance algorithmPropertiesFile queryLyricsFile1 responseLyricsFile2'");
        System.out.println("or 'VerseProcessingTool --distance algorithmPropertiesFile fileWithPairs'");
    }

    private static VerseProcessor createVerseProcessor( Properties props ) throws VerseProcessor.VerseProcessorException {
        try {
            BasicConfiguration.createInstance();
            ContextConfiguration.registerConfigurationClass(ContextConfigurationProsodyImpl.class);
            ContextConfiguration.createInstance();
            Logger.getRootLogger().setLevel(Level.INFO);
        } catch (Exception e) {
            throw new RuntimeException("Verse processor initialization error (treeton context or logger problem)", e );
        }

        VerseProcessor processor = new VerseProcessor( props );
        processor.initialize();

        return processor;
    }
    
    private static ArrayList<VerseDescription> getVerseDescriptions(File inputFile, InputFormat inputFormat, VerseProcessor processor,
                                                                    ArrayList<Integer> sourceLineNumbers, ArrayList<String> plainLyrics,
                                                                    HashSet<String> tags, boolean noHeader ) throws IOException {
        ArrayList<String> formattedLyrics = new ArrayList<>();

        parseFile(inputFile, formattedLyrics, sourceLineNumbers, tags, noHeader);

        ArrayList<StressDescription> stressDescriptions = new ArrayList<>();

        if( inputFormat == InputFormat.RAW_SYLLABLES ) {
            try {
                processor.parseRawSyllables(formattedLyrics, plainLyrics, stressDescriptions);
            } catch (Exception e) {
                System.err.println("Unable to parse raw syllables (file "+inputFile.getPath()+"): " + e.getMessage());
                return null;
            }
        } else if( inputFormat == InputFormat.FORMATTED ) {
            try {
                processor.parseFormattedVerses(formattedLyrics, plainLyrics, stressDescriptions);
            } catch (Exception e) {
                System.err.println("Unable to parse formatted lyrics (file "+inputFile.getPath()+"): " + e.getMessage());
                return null;
            }
        } else {
            assert inputFormat == InputFormat.STANDARD;
            plainLyrics.addAll( formattedLyrics );
            stressDescriptions = null;
        }

        ArrayList<VerseDescription> verseDescriptions = processor.process(plainLyrics, stressDescriptions, false);
        
        assert verseDescriptions.size() == plainLyrics.size();

        return verseDescriptions;
    }

    enum InputFormat {
        STANDARD,
        FORMATTED,
        RAW_SYLLABLES
    }

    public static void main(String[] argv) throws Exception {
        if( argv.length != 2 ) {
            if( argv.length >= 3 ) {
                String firstArg = argv[0];

                if( "--distance".equals(firstArg) ) {
                    if( argv.length >= 4 ) {
                        runCountDistanceArgsMode(argv);
                    } else {
                        runCountDistanceFileWithPairsMode(argv);
                    }
                } else {
                    printUsage();
                }

                return;
            }

            printUsage();
            return;
        }

        String algorithmProperties = argv[0];
        String lyricsPath = argv[1];

        if( !new File(algorithmProperties).exists() ) {
            System.out.println(algorithmProperties + " doesn't exist");
            return;
        }
        if( !new File(lyricsPath).exists() ) {
            System.out.println(lyricsPath + " doesn't exist");
            return;
        }

        Properties props = new Properties();
        FileInputStream propsStream = new FileInputStream( algorithmProperties );
        props.load(propsStream);
        propsStream.close();

        boolean compactOutput = Boolean.valueOf(props.getProperty("compactOutput"));
        InputFormat inputFormat = InputFormat.valueOf(props.getProperty("inputFormat"));

        HashSet<String> tagsWhereToSkipHeader = getTagsWhereToSkipHeader(props);

        VerseProcessor processor;
        try {
            processor = createVerseProcessor(props);
        } catch (VerseProcessor.VerseProcessorException e) {
            e.printStackTrace();
            return;
        }

        ArrayList<File> toProcess = new ArrayList<>();

        File lyricsFile = new File(lyricsPath);
        if( lyricsFile.isDirectory()) {
            Collections.addAll(toProcess, lyricsFile.listFiles(
                    (dir, name) -> new File(dir,name).isFile() && name.endsWith(".txt")
            ));
        } else {
            toProcess.add(lyricsFile);
        }

        boolean noHeader = Boolean.valueOf(props.getProperty("noHeader"));

        int counter = 0;
        for (File inputFile : toProcess) {
            System.out.println(counter++ + ": processing " + inputFile.getPath());

            ArrayList<Integer> sourceLineNumbers = new ArrayList<>();
            HashSet<String> tags = new HashSet<>();
            ArrayList<String> plainLyrics = new ArrayList<>();

            ArrayList<VerseDescription> verseDescriptions = 
                    getVerseDescriptions( inputFile, inputFormat, processor, sourceLineNumbers, plainLyrics, tags, noHeader );
            
            if( verseDescriptions == null ) {
                continue;
            }

            int firstLineIndex = getFirstLineIndex(tagsWhereToSkipHeader, tags, plainLyrics);

            Vector<Double> averageVector = processor.countAverage( verseDescriptions, firstLineIndex );

            if( compactOutput ) {
                File outputFile = new File(inputFile.getPath()+".meta");
                if( !outputFile.exists() ) {
                    //noinspection ResultOfMethodCallIgnored
                    outputFile.createNewFile();
                }

                PrintStream out = new PrintStream(outputFile);

                StringBuilder sb = new StringBuilder();

                sb.append("(");
                for (int j = 0; j < processor.getMetricVectorDimension(); j++ ) {
                    if( j > 0 ) {
                        sb.append(";");
                    }
                    Double aDouble = averageVector.elementAt(j);
                    if( aDouble == null ) {
                        aDouble = 0.0;
                    }
                    sb.append(aDouble);
                }
                sb.append(")");

                out.println(sb);

                int prevFragmentId = -2;
                for (VerseDescription verseDescription : verseDescriptions) {
                    int fragmentId = verseDescription.fragmentId;

                    if( fragmentId != -1 && prevFragmentId != -2 && fragmentId != prevFragmentId ) {
                        out.println("### fragment ###");
                    }
                    if ( fragmentId != -1 ) {
                        prevFragmentId = fragmentId;
                    }

                    for (SyllableInfo syllableInfo : verseDescription.syllables) {
                        out.print(syllableInfo.toString());
                        out.print(";");
                    }
                    sb = new StringBuilder();

                    sb.append("(");
                    for (int j = 0; j < processor.getMetricVectorDimension(); j++ ) {
                        if( j > 0 ) {
                            sb.append(";");
                        }
                        Double aDouble = verseDescription.metricVector.elementAt(j);
                        if( aDouble == null ) {
                            aDouble = 0.0;
                        }
                        sb.append(aDouble);
                    }
                    sb.append(")");

                    out.print(sb.toString());

                    out.println();
                }
                out.close();
            } else {
                File outputFile = new File(inputFile.getPath()+".out");
                if( !outputFile.exists() ) {
                    //noinspection ResultOfMethodCallIgnored
                    outputFile.createNewFile();
                }

                PrintStream out = new PrintStream(outputFile);
                out.println("Average metric vector:");
                out.println(processor.formatMetricInfo(averageVector));
                out.println("Metric vectors and syllables info for each line:");
                Iterator<VerseDescription> iterator = verseDescriptions.iterator();
                for (int i = 0; i < plainLyrics.size(); i++ ) {
                    String s = plainLyrics.get(i);
                    assert (iterator.hasNext());
                    VerseDescription verseDescription = iterator.next();

                    if( verseDescription.metricVector != null ) {
                        out.print( sourceLineNumbers.get(i).toString() + ": " );
                        out.println(processor.formatMetricInfo(verseDescription.metricVector));
                    }
                    out.print( sourceLineNumbers.get(i).toString() + ": " );
                    out.println(verseDescription.formatVerse(s));
                }
                out.close();
            }
        }

        processor.deinitialize();
    }

    private static HashSet<String> getTagsWhereToSkipHeader(Properties props) {
        HashSet<String> tagsWhereToSkipHeader = new HashSet<>();
        String[] tagsWhereToSkipHeaderArray = props.getProperty("tagsWhereToSkipHeader").split(";");
        for (String tag : tagsWhereToSkipHeaderArray) {
            tagsWhereToSkipHeader.add(tag.trim());
        }
        return tagsWhereToSkipHeader;
    }

    private static int getFirstLineIndex(HashSet<String> tagsWhereToSkipHeader, HashSet<String> sourceTags, ArrayList<String> plainLyrics) {
        int firstLineIndex = 0;

        while( firstLineIndex < plainLyrics.size() && plainLyrics.get(firstLineIndex).trim().isEmpty()) {
            firstLineIndex++;
        }

        boolean needToSkipHeader = false;

        for (String tag : tagsWhereToSkipHeader) {
            if( sourceTags.contains(tag) ) {
                needToSkipHeader = true;
                break;
            }
        }

        if( needToSkipHeader ) {
            int next = firstLineIndex+1;

            if( next < plainLyrics.size() && plainLyrics.get(next).trim().isEmpty() ) {
                firstLineIndex = next + 1;

                while( firstLineIndex < plainLyrics.size() && plainLyrics.get(firstLineIndex).trim().isEmpty()) {
                    firstLineIndex++;
                }
            }
        }
        return firstLineIndex;
    }

    private static void parseFile(File inputFile, ArrayList<String> formattedLyrics, ArrayList<Integer> sourceLineNumbers,
                                  HashSet<String> tags, boolean noHeader ) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        boolean headerFinished = noHeader;

        int nLine = 0;
        while(reader.ready()) {
            String s = reader.readLine();
            nLine++;

            if( !headerFinished ) {
                if( s.isEmpty() ) {
                    continue;
                }

                if( "#".equals(s.trim())) {
                    headerFinished = true;
                    continue;
                } else {
                    String prefix = "tags:";
                    if( s.startsWith(prefix)) {
                        String[] tagsArray = s.substring(prefix.length()).split(";");
                        for (String tag : tagsArray) {
                            String trimmedTag = tag.trim();

                            if( trimmedTag.isEmpty()) {
                                continue;
                            }
                            tags.add(trimmedTag);
                        }

                    }
                    continue;
                }
            }

            formattedLyrics.add(s);
            if( sourceLineNumbers != null ) {
                sourceLineNumbers.add(nLine);
            }
        }
    }

    private static void runCountDistanceArgsMode(String[] argv) throws IOException {
        String algorithmProperties = argv[1];
        String queryLyricsPath = argv[2];

        if( !new File(algorithmProperties).exists() ) {
            System.out.println(algorithmProperties + " doesn't exist");
            return;
        }

        File queryFile = new File(queryLyricsPath);
        if( !queryFile.exists() ) {
            System.out.println(queryLyricsPath + " doesn't exist");
            return;
        }
        if( !queryFile.isFile() ) {
            System.out.println(queryLyricsPath + " is not a file");
            return;
        }

        ArrayList<File> queryFiles = new ArrayList<>();
        ArrayList<File> responseFiles = new ArrayList<>();

        for (int i = 3; i < argv.length; i++) {
            String responseLyricsPath = argv[i];

            File responseFile = new File(responseLyricsPath);

            if( !responseFile.exists() ) {
                System.out.println(responseLyricsPath + " doesn't exist");
                return;
            }
            if( !responseFile.isFile() ) {
                System.out.println(responseLyricsPath + " is not a file");
                return;
            }

            responseFiles.add(responseFile);
            queryFiles.add(queryFile);
        }


        processPairs(algorithmProperties, queryFiles, responseFiles);
    }

    private static void processPairs(String algorithmProperties, ArrayList<File> queryFiles, ArrayList<File> responseFiles) throws IOException {
        Properties props = new Properties();
        FileInputStream propsStream = new FileInputStream( algorithmProperties );
        props.load(propsStream);
        propsStream.close();

        InputFormat inputFormat = InputFormat.valueOf(props.getProperty("inputFormat"));
        HashSet<String> tagsWhereToSkipHeader = getTagsWhereToSkipHeader(props);

        VerseProcessor processor;
        try {
            processor = createVerseProcessor(props);
        } catch (VerseProcessor.VerseProcessorException e) {
            e.printStackTrace();
            return;
        }

        Map<File,Set<File>> pairsMap = new HashMap<>();

        assert  queryFiles.size() == responseFiles.size();

        for (int i = 0; i < queryFiles.size(); i++) {
            File qf = queryFiles.get(i);

            Set<File> fileSet = pairsMap.get(qf);
            if( fileSet == null ) {
                fileSet = new HashSet<>();
                pairsMap.put( qf, fileSet );
            }

            fileSet.add(responseFiles.get(i));
        }

        Map<ObjectPair<File,File>,PreciseVerseDistanceCounter.DistanceWithShift> results = new HashMap<>();
        boolean noHeader = Boolean.valueOf(props.getProperty("noHeader"));

        for (File queryFile : pairsMap.keySet()) {
            System.out.println( "processing " + queryFile.getPath());

            HashSet<String> queryTags = new HashSet<>();
            ArrayList<String> queryPlainLyrics = new ArrayList<>();

            ArrayList<VerseDescription> queryVerseDescriptions =
                    getVerseDescriptions( queryFile, inputFormat, processor, null, queryPlainLyrics, queryTags, noHeader );

            if( queryVerseDescriptions == null ) {
                continue;
            }

            double[] regressionCoefficients = new double[5];
            for( int i = 0; i < regressionCoefficients.length; i++ ) {
                regressionCoefficients[i] = 0.0;
            }

            String[] stringCoefs = props.getProperty("regressionCoefficients").split(";");
            assert stringCoefs.length == regressionCoefficients.length;
            for( int i = 0; i < stringCoefs.length; i++ ) {
                regressionCoefficients[i] = Double.valueOf( stringCoefs[i] );
            }

            int queryFirstLineIndex = getFirstLineIndex(tagsWhereToSkipHeader, queryTags, queryPlainLyrics);
            PreciseVerseDistanceCounter distanceCounter =
                    processor.createVerseDistanceCounter(queryVerseDescriptions, queryFirstLineIndex, regressionCoefficients);

            Set<File> responseFileSet = pairsMap.get(queryFile);

            for (File responseFile : responseFileSet) {
                HashSet<String> responseTags = new HashSet<>();
                ArrayList<String> responsePlainLyrics = new ArrayList<>();

                ArrayList<VerseDescription> responseVerseDescriptions =
                        getVerseDescriptions( responseFile, inputFormat, processor, null, responsePlainLyrics, responseTags, noHeader );

                if( responseVerseDescriptions == null ) {
                    continue;
                }

                int responseFirstLineIndex = getFirstLineIndex(tagsWhereToSkipHeader,responseTags,responsePlainLyrics);

                PreciseVerseDistanceCounter.DistanceWithShift distanceWithShift = distanceCounter.countDistance(responseVerseDescriptions,responseFirstLineIndex);

                results.put( new ObjectPair<>(queryFile,responseFile), distanceWithShift );
            }
        }

        for (int i = 0; i < queryFiles.size(); i++) {
            File qf = queryFiles.get(i);
            File rf = responseFiles.get(i);

            System.out.println(results.get(new ObjectPair<>(qf,rf)));
        }

    }

    private static void runCountDistanceFileWithPairsMode(String[] argv) throws IOException {
        String algorithmProperties = argv[1];
        String pairsFilePath = argv[2];

        if( !new File(algorithmProperties).exists() ) {
            System.out.println(algorithmProperties + " doesn't exist");
            return;
        }

        File pairsFile = new File(pairsFilePath);
        if( !pairsFile.exists() ) {
            System.out.println(pairsFilePath + " doesn't exist");
            return;
        }
        if( !pairsFile.isFile() ) {
            System.out.println(pairsFilePath + " is not a file");
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(pairsFile));

        ArrayList<File> queryFiles = new ArrayList<>();
        ArrayList<File> responseFiles = new ArrayList<>();

        while(reader.ready()) {
            String s = reader.readLine().trim();

            if(s.isEmpty()) {
                continue;
            }


            int colonPlace = s.indexOf(':');

            if( colonPlace == -1 ) {
                System.err.println("Skipping wrong line in pairs file: " + s);
                continue;
            }

            String queryLyricsPath = s.substring(0,colonPlace).trim();
            String responseLyricsPath = s.substring(colonPlace+1).trim();

            File queryFile = new File(queryLyricsPath);
            if( !queryFile.exists() ) {
                System.err.println(queryLyricsPath + " doesn't exist");
                System.err.println("Skipping wrong line in pairs file: " + s);
                continue;
            }
            if( !queryFile.isFile() ) {
                System.err.println(queryLyricsPath + " is not a file");
                System.err.println("Skipping wrong line in pairs file: " + s);
                continue;
            }

            File responseFile = new File(responseLyricsPath);
            if( !responseFile.exists() ) {
                System.err.println(responseLyricsPath + " doesn't exist");
                System.err.println("Skipping wrong line in pairs file: " + s);
                continue;
            }
            if( !responseFile.isFile() ) {
                System.err.println(responseLyricsPath + " is not a file");
                System.err.println("Skipping wrong line in pairs file: " + s);
                continue;
            }

            queryFiles.add(queryFile);
            responseFiles.add(responseFile);
        }

        processPairs(algorithmProperties,queryFiles,responseFiles);
    }
}
