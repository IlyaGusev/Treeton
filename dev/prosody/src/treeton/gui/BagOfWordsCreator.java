/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import treeton.core.BlackBoard;
import treeton.core.TString;
import treeton.core.TreetonFactory;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationProsodyImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TrnType;
import treeton.core.scape.trnmapper.StringToTrnMapperRule;
import treeton.core.scape.trnmapper.StringToTrnMapperRuleStorage;
import treeton.core.util.FileMapper;
import treeton.res.ru.BagOfWordsAccentDetector;
import treeton.res.ru.StarlingMorphEngine;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class BagOfWordsCreator {
    public static void main(String[] argv) throws Exception {
        BasicConfiguration.createInstance();
        ContextConfiguration.registerConfigurationClass(ContextConfigurationProsodyImpl.class);
        ContextConfiguration.createInstance();

        TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");

        StarlingMorphEngine engine = new StarlingMorphEngine();
        TrnType grammType = trnContext.getType("Gramm");

        HashSet<TString> klitikPOSes = new HashSet<>();
        klitikPOSes.add(TreetonFactory.newTString("PREP"));
        klitikPOSes.add(TreetonFactory.newTString("CONJ"));
        klitikPOSes.add(TreetonFactory.newTString("INTJ"));
        klitikPOSes.add(TreetonFactory.newTString("PCL"));

        HashSet<Character> vowels = new HashSet<>();

        vowels.add('а');
        vowels.add('е');
        vowels.add('ё');
        vowels.add('и');
        vowels.add('о');
        vowels.add('у');
        vowels.add('ы');
        vowels.add('э');
        vowels.add('ю');
        vowels.add('я');

        int pos_feature = grammType.getFeatureIndex("POS");
        engine.init( trnContext, grammType,
                "C:/projects/treeton/runtime/domains/Russian/resources/starlingMorph/conversionLex.map",
                "C:/projects/treeton/runtime/domains/Russian/resources/starlingMorph/conversionInfl.map",8001);

        StringToTrnMapperRuleStorage lexMapper = engine.getLexMapper();
        BlackBoard localBoard = TreetonFactory.newBlackBoard( 100, false );

        /*ArrayList<String[]> variants = engine.lowlevelProcess("же");

        System.out.println("Result:");
        for (String[] string : variants) {
            for (String s : string) {
                System.out.println(s);
            }
        }*/

        InputStream is = new FileInputStream("C:/projects/treeton/runtime/domains/Russian/resources/starlingMorph/dict_ru.dct");
        InputStreamReader rd = new InputStreamReader( is, "866" );

        PrintStream skippedWordsLog = new PrintStream("C:/projects/treeton/runtime/domains/Russian/resources/starlingMorph/skippedWords.log");
        PrintStream errorsLog = new PrintStream("C:/projects/treeton/runtime/domains/Russian/resources/starlingMorph/errors.log");
        PrintStream klitiksLog = new PrintStream("C:/projects/treeton/runtime/domains/Russian/resources/starlingMorph/klitiks.log");

        HashSet<String> potentialBugs = new HashSet<String>();

        potentialBugs.add("свою");

        BufferedReader reader = new BufferedReader( rd );
        HashSet<String> allWords = new HashSet<String>();
        while(reader.ready() ) {
            String s = reader.readLine().trim();
            String word = s.substring(0, s.indexOf(' '));

            if( word.startsWith("*")) {
                continue;
            }

            int i = word.indexOf("_(");

            if( i > 0 ) {
                word = word.substring(0,i);
            }

            i = word.indexOf(")_");

            if( i > 0 ) {
                word = word.substring(i+2);
            }

            if( word.startsWith("-")) {
                word = word.substring(1);
            }

            if(word.isEmpty()) {
                continue;
            }

            if( word.contains("'") ) {
                skippedWordsLog.println("word with ' detected: " + word);
                continue;
            }

            if( word.contains("_") ) {
                skippedWordsLog.println("word with _ detected: " + word);
                continue;
            }

            allWords.add(word);
        }
        reader.close();

        ArrayList<String> allWordsArray = new ArrayList<String>(allWords);
        allWordsArray.sort(Comparator.<String>naturalOrder());

        HashSet<String> exceptionWords = new HashSet<String>();
        exceptionWords.add("покрепче");
        exceptionWords.add("яя");

        WordReadResult wordReadResult = new WordReadResult();

        BagOfWordsAccentDetector accentDetector = new BagOfWordsAccentDetector();

        for (int j = 50000; j < allWordsArray.size(); j++) {
            String word = allWordsArray.get(j);

            if( exceptionWords.contains(word)) {
                continue;
            }

            if( word.length() >= 20 ) {
                skippedWordsLog.println("Skipping too long word "+word);
                continue;
            }

            ArrayList<String[]> variants;
            try {
                variants = engine.lowlevelProcess(word);
            } catch (Exception e) {
                errorsLog.println("Exception word "+word);
                break;
            }

            if( variants.isEmpty() ) {
                errorsLog.println("No analysis for word "+word);
            }

            if( (j % 100) == 0 ) {
                System.out.println(j);
            }

            for (String[] strings : variants) {
                if( strings.length < 4 || strings[3] == null ) {
                    errorsLog.println("Detected variant of analysis for word "+word+
                            " with no paradigm, source form is " + strings[0] );
                    continue;
                }

                char[] paradigm = strings[3].toLowerCase().toCharArray();
                String grammInfo = strings[1].replaceAll("\\."," ").trim();

                boolean isKlitik = false;
                int nVowels = 0;
                for( int i = 0; i < strings[0].length(); i++ ) {
                    char c = strings[0].charAt(i);

                    if( vowels.contains(c) ) {
                        nVowels++;
                    }
                }

                if( nVowels == 1 ) {
                    Object[] lexRules = lexMapper.getRules(grammInfo);

                    if( lexRules != null ) {
                        for (Object rule : lexRules) {
                            StringToTrnMapperRule lexRule = (StringToTrnMapperRule) rule;
                            lexRule.bind(grammInfo);
                            localBoard.clean();
                            lexRule.assign(localBoard);

                            TString posString = (TString) localBoard.get(pos_feature);

                            if (posString == null) {
                                errorsLog.println("Unable to detect part of speech for word " + strings[0]);
                                continue;
                            }

                            if (klitikPOSes.contains(posString)) {
                                isKlitik = true;
                            }
                        }
                    }
                }

                boolean explicitNoAccent = grammInfo.contains("без удар");

                int from = 0;
                while( (from = findNextWord( paradigm, from )) >=  0 ) {
                    from = readNextWord( paradigm, from, wordReadResult );

                    if( wordReadResult.word == null) {
                        break;
                    }

                    if( explicitNoAccent ) {
                        wordReadResult.nAccents = 0;
                        wordReadResult.nSecAccents = 0;
                    }

                    byte[] accPlaces = new byte[ Math.max( wordReadResult.nSecAccents + wordReadResult.nAccents, 1) ];

                    int l = 0;
                    for( int k = 0; k < wordReadResult.nAccents; k++ ) {
                        int accPl = (byte) wordReadResult.accentPlaces[k];

                        if( accPl < 0 || accPl >= wordReadResult.word.length() ) {
                            errorsLog.println( "Corrupted accent place " + accPl + " for word " + wordReadResult.word );
                        }
                        accPlaces[l++] = (byte) wordReadResult.accentPlaces[k];
                    }

                    for( int k = 0; k < wordReadResult.nSecAccents; k++ ) {
                        int accPl = (byte) wordReadResult.secAccentPlaces[k];

                        if( accPl < 0 || accPl >= wordReadResult.word.length() ) {
                            errorsLog.println( "Corrupted second accent place " + accPl + " for word " + wordReadResult.word );
                        }
                        accPlaces[l++] = (byte) wordReadResult.secAccentPlaces[k];
                    }

                    if( l == 0 ) {
                        accPlaces[0] = -1;
                    }

                    if( potentialBugs.contains( wordReadResult.word.toLowerCase() ) ) {
                        System.out.println( wordReadResult.word + "!!!" );
                    }

                    accentDetector.addString(wordReadResult.word,accPlaces,isKlitik);

                    if( isKlitik ) {
                        klitiksLog.println( "Klitik found: "+wordReadResult.word);
                    }
                }
            }
        }

        byte[] places = accentDetector.getAccentPlaces("абакой");
        assert( places.length == 2 );
        assert( places[0] == 2 );
        assert( places[1] == 4 );

        accentDetector.pack();

        runTests(accentDetector);

        String outPath = "C:/projects/treeton/runtime/domains/Russian/resources/starlingMorph/accentsBag.bin";

        byte[] data = accentDetector.getByteRepresentation();
        FileOutputStream out = new FileOutputStream(outPath);
        out.write( data );
        out.close();

        data = FileMapper.map2bytes(outPath);
        accentDetector = new BagOfWordsAccentDetector(data,0);

        runTests( accentDetector );

        skippedWordsLog.close();
        errorsLog.close();
        klitiksLog.close();
    }

    private static void runTests(BagOfWordsAccentDetector accentDetector) {
        byte[] places;
        places = accentDetector.getAccentPlaces("абакой");
        assert( places.length == 2 );
        assert( places[0] == 2 );
        assert( places[1] == 4 );

        places = accentDetector.getAccentPlaces("же");
        assert( places.length == 2 );
        assert( places[0] == -1 );
        assert( places[1] == 1 );
        assert( accentDetector.getKlitikInfo("же") == BagOfWordsAccentDetector.KlitikInfo.AMBIG );

        places = accentDetector.getAccentPlaces("аж");
        assert( places.length == 1 );
        assert( places[0] == 0 );
        assert( accentDetector.getKlitikInfo("аж") == BagOfWordsAccentDetector.KlitikInfo.YES );
    }

    static class WordReadResult {
        String word;
        int nAccents = 0;
        int[] accentPlaces = new int[10];
        int nSecAccents = 0;
        int[] secAccentPlaces = new int[10];
    }

    static char[] charBuffer = new char[1000];

    private static int readNextWord(char[] input, int from, WordReadResult wordReadResult) {
        int i = 0;
        wordReadResult.word = null;
        wordReadResult.nAccents = 0;
        wordReadResult.nSecAccents = 0;
        while( from < input.length ) {
            char c = input[from];
            if (c >= 'а' && c <= 'я' || c == '-' || c == 'ё') {
                charBuffer[i++] = c;
                from++;
                continue;
            } else if (c == '\'' || c=='"') {
                assert( i > 0 );
                wordReadResult.accentPlaces[wordReadResult.nAccents++] = i - 1;
                from++;
                continue;
            } else if (c == '`') {
                assert( i > 0 );
                wordReadResult.secAccentPlaces[wordReadResult.nSecAccents++] = i - 1;
                from++;
                continue;
            }
            break;
        }

        if( i > 0 ) {
            wordReadResult.word = new String( charBuffer, 0, i );
        }

        return from;
    }

    static int findNextWord( char[] input, int from ) {
        while( from < input.length ) {
            char c = input[from];

            if( c >= 'а' && c <= 'я' || c == '-' || c == 'ё' || c == '\'' || c == '`' || c == '"') {
                return from;
            }

            from++;
        }

        return -1;
    }
}
