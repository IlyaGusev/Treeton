/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui;

import org.apache.commons.io.FileUtils;
import treeton.core.BlackBoard;
import treeton.core.TreetonFactory;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationProsodyImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TrnType;
import treeton.core.scape.trnmapper.StringToTrnMapperRule;
import treeton.core.scape.trnmapper.StringToTrnMapperRuleStorage;
import treeton.res.ru.StarlingMorphEngine;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParadigmElement {
    String form;
    String starling_form;
    String starling_infl_info;
    int mainAccent = -1;
    int secondaryAccent = -1;
    int yoPlace = -1;
    boolean unparsed = false;
    boolean awkward = false;
    Set<String> inflGrammemes = new HashSet<>();

    @Override
    public String toString() {
        if(unparsed) {
            return  "  - starling_unparsed: " + starling_form + '\n';
        }

        return  "  - form: " + form + '\n' +
                "    starling_form: " + starling_form + '\n' +
                (awkward ? ("    awkward: true\n") : "") +
                "    starling_infl_info: " + starling_infl_info + '\n' +
                "    accent: " + mainAccent + '\n' +
                (yoPlace >= 0 ? ("    yo_place: " + yoPlace + '\n') : "") +
                (secondaryAccent >= 0 ? ("    sec_accent: " + secondaryAccent + '\n') : "") +
                "    gramm: [" + String.join(",", inflGrammemes) + "]\n";
    }

    public ParadigmElement(String starling_form, String starling_infl_info) {
        this.starling_form = starling_form.toLowerCase();
        this.starling_infl_info = starling_infl_info;
    }

    public boolean extractAccentInfo() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < starling_form.length();i++) {
            char c = starling_form.charAt(i);

            if(c == '\'') {
                mainAccent = i - 1;
            } else if(c == '`') {
                secondaryAccent = i - 1;
            } else if(c == '"') {
                if (yoPlace != -1 && yoPlace != i - 1) {
                    return false;
                }
                yoPlace = i - 1;
            } else if(c == '*') {
                awkward=true;
            } else {
                if(c == 'ё' ) {
                    if(yoPlace != -1 && yoPlace != i) {
                        return false;
                    }

                    yoPlace = i;
                }

                sb.append(c);
            }
        }

        if(mainAccent == -1 && yoPlace != -1) {
            mainAccent = yoPlace;
        }

        if(mainAccent == -1) {
            return false;
        }

        form = sb.toString();

        return true;
    }

    private static BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);

    boolean extractInflGrammemes(StarlingMorphEngine morphEngine) {
        StringToTrnMapperRuleStorage inflMapper = morphEngine.getInflMapper();

        localBoard.clean();

        Object[] inflRules = inflMapper.getRules(starling_infl_info);
        if (inflRules != null && inflRules.length >= 0) {
            for (Object o : inflRules) {
                if (o == null) {
                    continue;
                }
                StringToTrnMapperRule inflRule = (StringToTrnMapperRule) o;
                inflRule.bind(starling_infl_info);

                while (inflRule.next()) {
                    if(localBoard.getNumberOfObjects() != 0) {
                        return false;
                    }

                    inflRule.assign(localBoard);
                }
            }
        }

        for (int i = 0; i <= localBoard.getDepth(); i++) {
            Object obj = localBoard.get(i);
            if (obj != null) {
                inflGrammemes.add(obj.toString());
            }
        }

        return true;
    }
}

class Paradigm {
    private static BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);

    String lemma;
    String source_zindex;
    String normalized_zindex;
    String starling_zindex;
    String starling_paradigm;
    Set<String> lexGrammemes = new HashSet<>();
    ArrayList<ParadigmElement> paradigm = new ArrayList<>();
    boolean unsure = false;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        //noinspection StringConcatenationInsideStringBufferAppend
        buf.append(
                "- lemma: " + lemma + '\n' +
                "  status: " + (unsure ? "unsure" : "auto") + '\n' +
                "  zindex: " + normalized_zindex + '\n' +
                "  source_zindex: " + source_zindex + '\n' +
                "  starling_zindex: " + starling_zindex + '\n' +
                "  starling_paradigm: " + starling_paradigm + '\n' +
                "  gramm: [" + String.join(",", lexGrammemes) + "]\n"
        );
        for (ParadigmElement paradigmElement : paradigm) {
            buf.append(paradigmElement.toString());
        }
        return buf.toString();
    }

    public Paradigm(String lemma, String source_zindex) {
        this.lemma = lemma;
        this.source_zindex = source_zindex;
    }

    boolean extractLexGrammemes(StarlingMorphEngine morphEngine) {
        StringToTrnMapperRuleStorage lexMapper = morphEngine.getLexMapper();

        localBoard.clean();

        Object[] lexRules = lexMapper.getRules(normalized_zindex);
        if (lexRules != null && lexRules.length >= 0) {
            for (Object o : lexRules) {
                if (o == null) {
                    continue;
                }
                StringToTrnMapperRule lexRule = (StringToTrnMapperRule) o;
                lexRule.bind(normalized_zindex);

                while (lexRule.next()) {
                    if(localBoard.getNumberOfObjects() != 0) {
                        return false;
                    }

                    lexRule.assign(localBoard);
                }
            }
        }

        for (int i = 0; i <= localBoard.getDepth(); i++) {
            Object obj = localBoard.get(i);
            if (obj != null) {
                lexGrammemes.add(obj.toString());
            }
        }

        return true;
    }
}

class ZindexKey {
    String lemma;
    String zindex;

    public ZindexKey(String lemma, String zindex) {
        this.lemma = lemma;
        this.zindex = zindex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZindexKey zindexKey = (ZindexKey) o;

        if (!lemma.equals(zindexKey.lemma)) return false;
        return zindex.equals(zindexKey.zindex);

    }

    @Override
    public int hashCode() {
        int result = lemma.hashCode();
        result = 31 * result + zindex.hashCode();
        return result;
    }
}

public class YandexBagOfWordsCreator {
    static Pattern megaPattern = Pattern.compile(
               "(^[а-я-]+) +" +
               "([0-9]+(\\.[0-9]+)?) +(\\([^\\)]*\\) +)?" +
               "((вводн|предик|межд|част|союз|предл\\.?|сравн\\.?|н|(мн\\.? +((неод|одуш)\\.? +)?)?((м|с|ж)о?( п)?|м с|мо-жо|мо жо|м ж|мо со|(числ.-п *)?п|п мс|мс-п|мс|(св|нсв|св-нсв)( нп)?) +(0|[1-9]+\\*?\\*?(а|а/с|а/в|в/в|в/с|в|с|е|D|F)[\\'\\-\\!\\~\\?]*( \\([^\\)]+\\))?(\\\"[1-9]\\\"(\\\"[1-9]\\\")?-?| *\\[\\\"[1-9]\\\"(\\\"[1-9]\\\")?\\](\\\"[1-9]\\\")?\\!?)?))\\.? *(\\,? ?(Р2|\\[?(п|П)2 *(\\([^\\)]+\\))?\\]?|_(Р\\.)? *мн\\. *затрудн\\._|безл\\.?|многокр\\.?|_страд. нет_|_прич\\. страд\\._ -жд-|_сравн\\.? затрудн\\.?_|\\\"[0-9]\\\"|_пф +нет_|_[ а-я\\.0-9]+ (затрудн\\.|нет)_|\\[_проф\\.[^\\]]+\\]|#[0-9]+|\\╡|о|\\$ *((_нсв также_ *)?I+|[0-9]+)(\\([^\\)]+\\))?))*( *@.*$)?)" +
               "( |((СИН)?:|%).*|\\(//\\)|\\!\\![а-я0-9\\\\\\.]+|=>|<=||\\$ .*|\\([^\\)^\\']+\\)| \\[//[^\\]]*\\])*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    ); //$1 $2 $5

    static Pattern inflPattern = Pattern.compile(
            " *([a-zA-Z 0-9\\.]+) *([ёЁа-яА-Я\\'-`\"\\*]+) *",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    ); //$1 $2

    private static String normalize(String s) {
        return s.trim().replaceAll(" +", " ");
    }

    private static String getPathForZindex(String prefix, String zindex) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append("/");

        for(int i = 0; i < zindex.length();i++) {
            Character c = zindex.charAt(i);

            if( Character.isLetter(c) || Character.isDigit(c)) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }

        return sb.toString();
    }

    public static void main(String[] argv) throws Exception {
        BasicConfiguration.createInstance();
        ContextConfiguration.registerConfigurationClass(ContextConfigurationProsodyImpl.class);
        ContextConfiguration.createInstance();

        TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
        TrnType grammType = trnContext.getType("UD_Gramm");

        StarlingMorphEngine engine = new StarlingMorphEngine();

        int pos_feature = grammType.getFeatureIndex("POS");
        engine.init( trnContext, grammType,
                "C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/conversionLex_UD.map",
                "C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/conversionInfl_UD.map", 8001);

        StringToTrnMapperRuleStorage lexMapper = engine.getLexMapper();
        BlackBoard localBoard = TreetonFactory.newBlackBoard( 100, false );

        Set<ZindexKey> secondTryFilter = null;

        File secondTryFilterFile = new File("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/secondTryFilter.txt");

        if(secondTryFilterFile.exists()) {
            InputStream is = new FileInputStream(secondTryFilterFile);
            InputStreamReader rd = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(rd);

            secondTryFilter = new HashSet<>();

            while (reader.ready()) {
                String word = reader.readLine().trim();
                assert (reader.ready());
                String zindex = reader.readLine().trim();

                secondTryFilter.add(new ZindexKey(word, zindex));
            }
        } else {
            File resultsDir = new File("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results");
            if(resultsDir.exists()) {
                FileUtils.deleteDirectory(resultsDir);
            }
            resultsDir.mkdir();
        }

        InputStream is = new FileInputStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/dict_ru_clean.dct");
        InputStreamReader rd = new InputStreamReader( is, "866" );
        BufferedReader reader = new BufferedReader( rd );
        Set<String> allLinesSet = new HashSet<>();

        Map<String,ArrayList<ZindexKey>> allWords = new HashMap<>();
        Set<String> outfileNames = new HashSet<>();
        Map<String,String> pathsForZindexes = new HashMap<>();
        Map<String,Integer> allEncounteredInflExpressions = new HashMap<>();
        Map<Integer,Integer> paradigmElementsCountStats = new HashMap<>();

        PrintStream errorsLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/errors.log");
        PrintStream unmatchedLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/unmatched.log");
        PrintStream zindexesLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/zindexes.log");
        PrintStream secondTryLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/secondTry.log");
        PrintStream inflExpressionsLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/inflExpressions.log");
        PrintStream paradigmElementsCountLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/paradigmElementsCount.log");
        PrintStream paradigmsToCheck = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/paradigmsToCheck.log");

        try {
            int nLines = 0;
            int wordCount = 0;
            while(reader.ready() ) {
                String s = reader.readLine().trim();
                nLines++;

                if( allLinesSet.contains(s) ) {
                    continue;
                }

                allLinesSet.add(s);

                Matcher m = megaPattern.matcher(s);

                if(!m.matches()) {
                    unmatchedLog.println(s);
                    continue;
                }
                String zindex = normalize(m.group(5));
                String word = normalize(m.group(1)).toLowerCase();

                String normalizedZindex = normalize(zindex.replaceAll("\\([^\\)]+\\)",""));

                ArrayList<ZindexKey> words = allWords.get(normalizedZindex);
                if(words == null) {
                    words = new ArrayList<>();
                    allWords.put(normalizedZindex, words);
                }
                words.add(new ZindexKey(word, zindex));
                wordCount++;
            }
            reader.close();

            for (Map.Entry<String, ArrayList<ZindexKey>> entry : allWords.entrySet()) {
                String outPath = getPathForZindex("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results", entry.getKey());

                outPath += "_" + Integer.toString(entry.getValue().size());

                while (outfileNames.contains(outPath)) {
                    outPath += "_";
                }

                outfileNames.add(outPath);
                pathsForZindexes.put(entry.getKey(), outPath);
            }

            System.out.println(String.format("%d strings scanned, found %d zindexes and %d words", nLines, allWords.size(), wordCount));

            HashSet<String> exceptionWords = new HashSet<>();

            List<String> sortedZindexes = new ArrayList<>(allWords.keySet());
            sortedZindexes.sort(String.CASE_INSENSITIVE_ORDER);

            for (int i = 0; i < sortedZindexes.size(); i++) {
                String normalizedZindex = sortedZindexes.get(i);
                ArrayList<ZindexKey> zindexKeys = allWords.get(normalizedZindex);

                zindexesLog.println(normalizedZindex);
                System.out.println(Integer.toString(i) + ": starting to process words with zindex like " + normalizedZindex);
                File f = new File(pathsForZindexes.get(normalizedZindex));
                if(!f.exists()) {
                    f.createNewFile();
                }
                PrintStream out = new PrintStream(new FileOutputStream(f, true));
                try {
                    int counter = 0;
                    for (ZindexKey zindexKey : zindexKeys) {
                        if( secondTryFilter != null && !secondTryFilter.contains(zindexKey)) {
                            continue;
                        }

                        if (exceptionWords.contains(zindexKey.lemma)) {
                            errorsLog.println(zindexKey.lemma + " : starling fatal error, skipping");
                            continue;
                        }

                        if (zindexKey.lemma.length() >= 20) {
                            errorsLog.println(zindexKey.lemma + " : too long, skipping");
                            continue;
                        }

                        ArrayList<String[]> variants;

                        try {
                            variants = engine.lowlevelProcess(zindexKey.lemma);
                        } catch (Exception e) {
                            errorsLog.println(zindexKey.lemma + " : starling error, aborting");
                            throw new RuntimeException("Starling internal error with word " + zindexKey.lemma, e);
                        }

                        if( variants.isEmpty() ) {
                            errorsLog.println(zindexKey.lemma + " : empty analysis, zindex " + zindexKey.zindex);
                            secondTryLog.println(zindexKey.lemma);
                            secondTryLog.println(zindexKey.zindex);
                            continue;
                        }

                        if( (counter++ % 100) == 0 ) {
                            System.out.println(counter);
                        }

                        Set<ZindexKey> processed = new HashSet<>();

                        String matchedParadigm = null;
                        String starlingZindex = null;
                        int bestCommonPrefixLength = -1;
                        boolean currentIsAmbigious = false;
                        for (String[] strings : variants) {
                            if (strings.length < 4 || strings[3] == null) {
                                errorsLog.println(zindexKey.lemma + " : detected variant of analysis with no paradigm");
                                bestCommonPrefixLength = -1;
                                break;
                            }

                            ZindexKey checkKey = new ZindexKey(normalize(strings[0]), normalize(strings[1]));

                            if(processed.contains(checkKey)) {
                                continue;
                            }

                            if (zindexKey.lemma.equals(strings[0])) {
                                starlingZindex = normalize(strings[1]);
                                int commonPrefixLength = countCommonPrefixLength(zindexKey.zindex, starlingZindex);

                                if( bestCommonPrefixLength == -1 || commonPrefixLength > bestCommonPrefixLength ) {
                                    bestCommonPrefixLength = commonPrefixLength;
                                    matchedParadigm = strings[3].trim();
                                    currentIsAmbigious = false;
                                } else if( commonPrefixLength == bestCommonPrefixLength ) {
                                    currentIsAmbigious = true;
                                }
                            }

                            processed.add(checkKey);
                        }

                        if( currentIsAmbigious && bestCommonPrefixLength != zindexKey.zindex.length() || bestCommonPrefixLength <= 0) {
                            if( bestCommonPrefixLength <= 0 ) {
                                errorsLog.println(zindexKey.lemma + " : unable to find zindex " + zindexKey.zindex);
                            } else {
                                errorsLog.println(zindexKey.lemma + " : ambiguous zindex " + zindexKey.zindex);
                            }
                            secondTryLog.println(zindexKey.lemma);
                            secondTryLog.println(zindexKey.zindex);
                            continue;
                        }

                        Paradigm paradigm = new Paradigm(zindexKey.lemma, zindexKey.zindex);
                        paradigm.normalized_zindex = normalizedZindex;
                        paradigm.starling_zindex = starlingZindex;
                        paradigm.starling_paradigm = matchedParadigm;

                        if(!paradigm.extractLexGrammemes(engine)) {
                            errorsLog.println(zindexKey.lemma + " : unable to extract lexical info, zindex " + zindexKey.zindex);
                            secondTryLog.println(zindexKey.lemma);
                            secondTryLog.println(zindexKey.zindex);
                            continue;
                        }

                        StringTokenizer tok = new StringTokenizer(matchedParadigm, ";");

                        boolean paradigmParsingProblemDetected = false;
                        boolean accentuationProblemDetected = false;
                        boolean inflParsingProblemDetected = false;
                        while (tok.hasMoreTokens()) {
                            String p_element = normalize(tok.nextToken());

                            if(p_element.isEmpty()) {
                                continue;
                            }

                            Matcher m = inflPattern.matcher(p_element);

                            if(!m.matches()) {
                                paradigmParsingProblemDetected = true;
                                ParadigmElement paradigmElement = new ParadigmElement(p_element, null);
                                paradigmElement.unparsed = true;
                                paradigm.paradigm.add(paradigmElement);
                                break;
                            }
                            String infl_info = normalize(m.group(1));
                            String form = normalize(m.group(2)).toLowerCase();

                            ParadigmElement paradigmElement = new ParadigmElement(form, infl_info);
                            if(!paradigmElement.extractAccentInfo()) {
                                accentuationProblemDetected = true;
                            }

                            if(!paradigmElement.extractInflGrammemes(engine)) {
                                inflParsingProblemDetected = true;
                            }

                            paradigm.paradigm.add(paradigmElement);

                            Integer n = allEncounteredInflExpressions.get(infl_info);

                            if(n == null) {
                                n = 0;
                            }

                            allEncounteredInflExpressions.put(infl_info, n + 1);
                        }

                        if(paradigm.paradigm.isEmpty()) {
                            errorsLog.println(zindexKey.lemma + " : empty paradigm, zindex " + zindexKey.zindex +
                                    ", source paradigm string " + matchedParadigm);
                            secondTryLog.println(zindexKey.lemma);
                            secondTryLog.println(zindexKey.zindex);
                            continue;
                        }

                        if(paradigmParsingProblemDetected || accentuationProblemDetected ||
                                inflParsingProblemDetected || paradigm.lexGrammemes.contains("unsure")) {
                            paradigm.unsure = true;

                            paradigmsToCheck.println(zindexKey.lemma + " " + normalizedZindex );
                        }

                        int elementCount = paradigm.paradigm.size();

                        Integer stat = paradigmElementsCountStats.get(elementCount);
                        if(stat == null) {
                            stat = 0;
                        }
                        paradigmElementsCountStats.put(elementCount, stat + 1);

                        out.print(paradigm.toString());
                    }
                } finally {
                    out.close();
                }
            }

            List<String> sortedInflInfos = new ArrayList<>(allEncounteredInflExpressions.keySet());
            sortedInflInfos.sort(String.CASE_INSENSITIVE_ORDER);

            for (String info : sortedInflInfos) {
                int stat = allEncounteredInflExpressions.get(info);
                inflExpressionsLog.println(info + " : " + Integer.toString(stat));
            }

            List<Integer> sortedParadigmElementCounts = new ArrayList<>(paradigmElementsCountStats.keySet());
            Collections.sort(sortedParadigmElementCounts);

            for (Integer elementCount : sortedParadigmElementCounts) {
                int stat = paradigmElementsCountStats.get(elementCount);
                paradigmElementsCountLog.println(Integer.toString(elementCount) + " : " + Integer.toString(stat));
            }
        } finally {
            errorsLog.close();
            unmatchedLog.close();
            zindexesLog.close();
            secondTryLog.close();
            inflExpressionsLog.close();
            paradigmElementsCountLog.close();
            paradigmsToCheck.close();
        }
    }

    private static int countCommonPrefixLength(String s1, String s2) {
        int i = 0;
        for( ; i < Math.min(s1.length(),s2.length()); i++) {
            if(s1.charAt(i) != s2.charAt(i)) {
                break;
            }
        }

        return i;
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
