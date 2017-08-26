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
                (starling_form != null ? "    starling_form: " + starling_form + '\n' : "") +
                (awkward ? ("    awkward: true\n") : "") +
                (starling_infl_info != null ? "    starling_infl_info: " + starling_infl_info + '\n' : "")+
                "    accent: " + mainAccent + '\n' +
                (yoPlace >= 0 ? ("    yo_place: " + yoPlace + '\n') : "") +
                (secondaryAccent >= 0 ? ("    sec_accent: " + secondaryAccent + '\n') : "") +
                "    gramm: [" + String.join(",", inflGrammemes) + "]\n";
    }

    public ParadigmElement(String starling_form, String starling_infl_info) {
        this.starling_form = starling_form != null ? starling_form.toLowerCase() : null;
        this.starling_infl_info = starling_infl_info;
    }

    public boolean extractAccentInfo() {
        StringBuilder sb = new StringBuilder();
        boolean garbageFound = false;
        for(int i = 0; i < starling_form.length();i++) {
            char c = starling_form.charAt(i);

            if(c == '\'') {
                mainAccent = sb.length() - 1;
            } else if(c == '`') {
                secondaryAccent = sb.length() - 1;
            } else if(c == '"') {
                if (yoPlace != -1 && yoPlace != sb.length() - 1) {
                    return false;
                }
                yoPlace = sb.length() - 1;
            } else if(c == '*') {
                awkward=true;
            } else {
                if(c == 'ё' ) {
                    if(yoPlace != -1 && yoPlace != sb.length()) {
                        return false;
                    }

                    yoPlace = sb.length();
                }

                if(!Character.isLetter(c) && c != '-') {
                    garbageFound = true;
                }

                sb.append(c);
            }
        }

        if(mainAccent == -1 && yoPlace != -1) {
            mainAccent = yoPlace;
        }

        form = sb.toString();

        //noinspection RedundantIfStatement
        if(mainAccent == -1 || !YandexBagOfWordsCreator.vowels.contains(form.charAt(mainAccent))) {
            if(mainAccent == -1) {
                int vowelPlace = -1;

                for(int i=0; i<form.length();i++) {
                    char c = form.charAt(i);

                    if(YandexBagOfWordsCreator.vowels.contains(c)) {
                        if(vowelPlace == -1) {
                            vowelPlace = i;
                        } else {
                            vowelPlace = -2;
                        }
                    }
                }

                if(vowelPlace >= 0) {
                    mainAccent = vowelPlace;
                }
            }
            return false;
        }

        return !garbageFound;
    }

    private static BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);

    boolean extractInflGrammemes(String infl_info, StarlingMorphEngine morphEngine, Paradigm paradigm) {
        StringToTrnMapperRuleStorage inflMapper = morphEngine.getInflMapper();

        int nGrammemesAtStart = inflGrammemes.size();

        localBoard.clean();

        String prepared_infl_info = infl_info.replaceAll("\\.", " ").trim();

        Object[] inflRules = inflMapper.getRules(prepared_infl_info);
        if (inflRules != null && inflRules.length >= 0) {
            for (Object o : inflRules) {
                if (o == null) {
                    continue;
                }
                StringToTrnMapperRule inflRule = (StringToTrnMapperRule) o;
                inflRule.bind(prepared_infl_info);

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

        if((paradigm.hasGrammeme("ADJ") || inflGrammemes.contains("Part")) && !paradigm.hasGrammeme("Short")) {
            inflGrammemes.add("Pos");
        }

        return inflGrammemes.size() > nGrammemesAtStart;
    }
}

enum ParadigmStatus {
    auto,
    unsure,
    error
}

class Paradigm implements Comparable<Paradigm> {
    private static BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);

    private String lemma;
    private Double frequency;
    private int id;
    private String source_zindex;
    private String source_accent_info;
    private String normalized_zindex;
    private String starling_zindex;
    private String starling_paradigm;
    private String error_message;
    private String pltant_zindex;
    private Set<String> lexGrammemes = new HashSet<>();
    private ArrayList<ParadigmElement> paradigm = new ArrayList<>();
    private ParadigmStatus status;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        //noinspection StringConcatenationInsideStringBufferAppend
        buf.append(
            "- lemma: " + lemma + '\n' +
            "  id: " + id + '\n' +
            "  frequency: " + frequency.toString() + '\n' +
            "  status: " + status.toString() + '\n' +
            "  zindex: " + normalized_zindex + '\n' +
            "  source_zindex: " + source_zindex + '\n' +
            "  source_acc_info: " + source_accent_info + '\n' +
            (starling_zindex != null ? "  starling_zindex: " + starling_zindex + '\n' : "") +
            (starling_paradigm != null ? "  starling_paradigm: " + starling_paradigm + '\n' : "") +
            (error_message != null ? "  error_message: " + error_message + '\n' : "") +
            "  gramm: [" + String.join(",", lexGrammemes) + "]\n" +
            "  paradigm:\n"
        );
        for (ParadigmElement paradigmElement : paradigm) {
            buf.append(paradigmElement.toString());
        }
        return buf.toString();
    }

    public Paradigm(ZindexKey zindexKey, String normalized_zindex, ParadigmStatus status) {
        this.lemma = zindexKey.lemma;
        this.id = zindexKey.id;
        frequency = YandexBagOfWordsCreator.frequencies.getOrDefault(lemma.toLowerCase(),0.0);
        this.source_zindex = zindexKey.zindex;
        this.source_accent_info = zindexKey.accentInfo;
        this.status = status;
        this.normalized_zindex = normalized_zindex;
        this.pltant_zindex = zindexKey.pltant_zindex;
    }

    public String getNormalized_zindex() {
        return normalized_zindex;
    }

    public void setStarling_zindex(String starling_zindex) {
        this.starling_zindex = starling_zindex;
    }

    public void setStarling_paradigm(String starling_paradigm) {
        this.starling_paradigm = starling_paradigm;
    }

    public void addError_message(String error_message) {
        if(this.error_message == null) {
            this.error_message = error_message;
        } else {
            this.error_message += ", " + error_message;
        }
    }

    public ParadigmStatus getStatus() {
        return status;
    }

    public void addParadigmElement(ParadigmElement pe) {
        paradigm.add(pe);
    }

    public boolean hasGrammeme(String grammeme) {
        return lexGrammemes.contains(grammeme);
    }

    public void modifyStatus(ParadigmStatus status) {
        if(status.ordinal() > this.status.ordinal()) {
            this.status = status;
        }
    }

    private boolean _extractLexGrammemes(StarlingMorphEngine morphEngine, String zindex) {
        StringToTrnMapperRuleStorage lexMapper = morphEngine.getLexMapper();

        localBoard.clean();

        String prepared_zindex = zindex.replaceAll("\\.", " ").trim();

        Object[] lexRules = lexMapper.getRules(prepared_zindex);
        if (lexRules != null && lexRules.length >= 0) {
            for (Object o : lexRules) {
                if (o == null) {
                    continue;
                }
                StringToTrnMapperRule lexRule = (StringToTrnMapperRule) o;
                lexRule.bind(prepared_zindex);

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

        return !lexGrammemes.isEmpty();
    }

    boolean extractLexGrammemes(StarlingMorphEngine morphEngine, boolean proper) {
        boolean res = _extractLexGrammemes(morphEngine, normalized_zindex);

        if(!res && pltant_zindex != null) {
            res = _extractLexGrammemes(morphEngine, pltant_zindex);

            if(res) {
                lexGrammemes.add("PLT");
            }
        }

        if(proper) {
            lexGrammemes.add("proper");
        }

        return res;
    }

    public int getParadigmSize() {
        return paradigm.size();
    }

    @Override
    public int compareTo(Paradigm o) {
        double freqDelta = frequency - o.frequency;

        if (freqDelta < 0) {
            return 1;
        } else if (freqDelta > 0) {
            return -1;
        }

        return lemma.compareTo(o.lemma);
    }

    public String getError_message() {
        return error_message;
    }
}

@SuppressWarnings("SpellCheckingInspection")
class ZindexKey {
    String lemma;
    String zindex;
    String accentInfo;
    boolean unsure;
    boolean proper;
    String pltant_zindex; // "мн. _от_" case
    int id;

    public ZindexKey(String lemma, String zindex, String accentInfo, boolean unsure, String pltant_zindex, int id, boolean proper) {
        this.lemma = lemma;
        this.zindex = zindex;
        this.accentInfo = accentInfo;
        this.unsure = unsure;
        this.pltant_zindex = pltant_zindex;
        this.id = id;
        this.proper = proper;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZindexKey zindexKey = (ZindexKey) o;

        return unsure == zindexKey.unsure && lemma.equals(zindexKey.lemma) && zindex.equals(zindexKey.zindex) && accentInfo.equals(zindexKey.accentInfo);

    }

    @Override
    public int hashCode() {
        int result = lemma.hashCode();
        result = 31 * result + zindex.hashCode();
        result = 31 * result + accentInfo.hashCode();
        result = 31 * result + (unsure ? 1 : 0);
        return result;
    }
}

public class YandexBagOfWordsCreator {
    static String megaPatternString =
            "(^[а-я-]+) +" +
            "([0-9]+(\\.[0-9]+)?(,[0-9]+)?) +(\\([^\\)]*\\) +)?" +
            "((вводн|предик|межд|част|союз|предл\\.?|сравн\\.?|н|(мн\\.? +((неод|одуш)\\.? +)?)?((м|с|ж)о?( п)?|м с|мо-жо|мо жо|м ж|мо со|(числ.-п *)?п|п мс|мс-п|мс|(св|нсв|св-нсв)( нп)?) +(0|[1-9][0-9]*\\*?\\*?\\*?(а|а/с|а/в|в/в|в/с|в|с|е|D|F)[\\'\\-\\!\\~\\?]*( \\([^\\)]+\\))?(\\\"[1-9]\\\"(\\\"[1-9]\\\")?-?| *\\[\\\"[1-9]\\\"(\\\"[1-9]\\\")?\\](\\\"[1-9]\\\")?\\!?)?))\\.? *(\\,? ?((P|Р)2|\\[?(п|П)2 *(\\([^\\)]+\\))?\\]?|_(Р\\.)? *мн\\. *затрудн\\._|безл\\.?|многокр\\.?|_страд. нет_|_прич\\. страд\\._ -жд-|_сравн\\.? затрудн\\.?_|\\\"[0-9]\\\"|_пф +нет_|_[ а-я\\.0-9]+ (затрудн\\.|нет)_|\\[_проф\\.[^\\]]+\\]|#[0-9]+|\\╡|о|\\$ *((_нсв также_ *)?I+(\\([^\\)]+\\))?(//I+(\\([^\\)]+\\))?)?|[0-9]+)(\\([^\\)]+\\))?))*( *@.*$)?)( |((СИН)?:|%).*|\\(//\\)|\\!\\![а-я0-9a-z\\\\\\.]+|=>|//>|<//|<=||\\$ .*|\\([^\\)^\\']+\\)| \\[//[^\\]]*\\])*$";
    static Pattern megaPattern = Pattern.compile(
            megaPatternString,
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    ); //$1 $2 $6

    static Pattern pluralPattern = Pattern.compile(
            "(^[а-я-]+) +([0-9]+(\\.[0-9]+)?(,[0-9]+)?) мн(\\.)? _от_ ([^ ]+ )?(([а-я]([а-я])?) .*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    ); //$1 $2 $7

    static Pattern weakPattern = Pattern.compile(
            "(^[а-я-]+) +([0-9]+(\\.[0-9]+)?(,[0-9]+)?) +(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    ); //$1 $2 $5

    static Pattern normalizationPattern = Pattern.compile(
            "\\[(//[^\\]\\n]*|[^\"0-9Пп][^\\]\\n]*)\\]|; \\(?_?см. также( отдельно)?_ *[^ $\\n]+$|\\( *_([^к]|к[^ф])[^\\)\\n]+\\)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    ); //$1 $2 $5

    static Pattern inflPattern = Pattern.compile(
            " *([a-zA-Z0-9,\\.]+|//Nsm)( Nsm)? *(([ёЁа-яА-Я\\'\\-`\"/\\*]|\\(ши\\)|<=|=>)+) *",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    ); //$1 $2 $3

    static Pattern accentPattern = Pattern.compile(
            "([0-9]+)(\\.([0-9]+))?(,([0-9]+))?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    ); //$1 $3 $5

    static Map<String,Double> frequencies = readFrequencies();

    private static String normalize(String s) {
        return s.trim().replaceAll(" +", " ");
    }

    private static String getPathForZindex(String prefix, String zindex, ParadigmStatus status) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append("/");
        sb.append(status.toString());
        sb.append("/");

        zindex = zindex.toLowerCase();

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

    static class SmartOutput {
        PrintStream autoOut = null;
        PrintStream unsureOut = null;
        PrintStream errorOut = null;
        Map<String,String> autoPathsForZindexes;
        Map<String,String> unsurePathsForZindexes;
        Map<String,String> errorPathsForZindexes;
        ArrayList<Paradigm> unsureParadigms = new ArrayList<>();
        ArrayList<Paradigm> errorParadigms = new ArrayList<>();

        int nAuto = 0;
        int nUnsure = 0;
        int nError = 0;
        File autoPath = null;
        File unsurePath = null;
        File errorPath = null;
        int normalizedZindexCounter;

        public SmartOutput(
                Map<String, String> autoPathsForZindexes,
                Map<String, String> unsurePathsForZindexes,
                Map<String, String> errorPathsForZindexes,
                ArrayList<Paradigm> unsureParadigms,
                ArrayList<Paradigm> errorParadigms,
                int normalizedZindexCounter) {
            this.autoPathsForZindexes = autoPathsForZindexes;
            this.unsurePathsForZindexes = unsurePathsForZindexes;
            this.errorPathsForZindexes = errorPathsForZindexes;
            this.unsureParadigms = unsureParadigms;
            this.errorParadigms = errorParadigms;
            this.normalizedZindexCounter = normalizedZindexCounter;
        }

        public void print(Paradigm p) throws IOException {
            if(autoOut == null && unsureOut == null && errorOut == null) {
                System.out.println(Integer.toString(normalizedZindexCounter) + ": starting to process words with zindex like " + p.getNormalized_zindex());
            }

            if(p.getStatus() == ParadigmStatus.unsure) {
                if(unsureOut == null) {
                    unsurePath = new File(unsurePathsForZindexes.get(p.getNormalized_zindex()));
                    if(!unsurePath.exists()) {
                        if(!unsurePath.createNewFile()) {
                            throw new IOException("Unable to create " + unsurePath.getPath());
                        }
                    }

                    unsureOut = new PrintStream(new FileOutputStream(unsurePath, true));
                }

                unsureOut.print(p.toString());
                unsureParadigms.add(p);
                nUnsure++;
            } else if(p.getStatus() == ParadigmStatus.auto) {
                if(autoOut == null) {
                    autoPath = new File(autoPathsForZindexes.get(p.getNormalized_zindex()));
                    if(!autoPath.exists()) {
                        if (!autoPath.createNewFile()) {
                            throw new IOException("Unable to create " + autoPath.getPath());
                        }
                    }

                    autoOut = new PrintStream(new FileOutputStream(autoPath, true));
                }

                autoOut.print(p.toString());
                nAuto++;
            } else {
                assert  p.getStatus() == ParadigmStatus.error;

                if(errorOut == null) {
                    errorPath = new File(errorPathsForZindexes.get(p.getNormalized_zindex()));
                    if(!errorPath.exists()) {
                        if (!errorPath.createNewFile()) {
                            throw new IOException("Unable to create " + errorPath.getPath());
                        }
                    }

                    errorOut = new PrintStream(new FileOutputStream(errorPath, true));
                }

                errorOut.print(p.toString());
                errorParadigms.add(p);
                nError++;
            }
        }

        public void close() throws IOException {
            if(autoOut != null) {
                autoOut.close();
                if(!autoPath.renameTo(new File(autoPath.getPath()+"."+String.format("%05d", nAuto)))) {
                    throw new IOException("Unable to finalize " + autoPath.getPath());
                }
            }

            if(unsureOut != null) {
                unsureOut.close();
                if(!unsurePath.renameTo(new File(unsurePath.getPath()+"."+String.format("%05d", nUnsure)))) {
                    throw new IOException("Unable to finalize " + unsurePath.getPath());
                }
            }

            if(errorOut != null) {
                errorOut.close();
                if(!errorPath.renameTo(new File(errorPath.getPath()+"."+String.format("%05d", nError)))) {
                    throw new IOException("Unable to finalize " + errorPath.getPath());
                }
            }
        }
    }


    public static Map<String,Double> readFrequencies() {
        Map<String,Double> stats = new HashMap<>();

        try {
            InputStream is = new FileInputStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/freqrnc2011.csv");
            InputStreamReader rd = new InputStreamReader( is );
            BufferedReader reader = new BufferedReader( rd );
            reader.readLine();
            while(reader.ready() ) {
                String s = reader.readLine().trim();
                String[] data = s.split("\t");
                String lemma = data[0].trim().toLowerCase();
                Double frequency = Double.valueOf(data[2].trim());

                Double freq = stats.get(lemma);
                if(freq == null || frequency >= freq) {
                    stats.put(lemma, frequency);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stats;
    }

    public static void main(String[] argv) throws Exception {
        System.out.println(megaPatternString);

        BasicConfiguration.createInstance();
        ContextConfiguration.registerConfigurationClass(ContextConfigurationProsodyImpl.class);
        ContextConfiguration.createInstance();

        TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
        TrnType grammType = trnContext.getType("UD_Gramm");

        StarlingMorphEngine engine = new StarlingMorphEngine();

        engine.init(trnContext, grammType,
                "C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/conversionLex_UD.map",
                "C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/conversionInfl_UD.map", 8001);

        File resultsDir = new File("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results");
        if(!resultsDir.exists() && !resultsDir.mkdir()) {
            throw new IOException("Unable to create directory for results");
        }
        resultsDir = new File("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results/auto");
        if(resultsDir.exists()) {
            FileUtils.deleteDirectory(resultsDir);
        }
        if (!resultsDir.mkdir()) {
            throw new IOException("Unable to create directory for auto results");
        }
        resultsDir = new File("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results/unsure");
        if(resultsDir.exists()) {
            FileUtils.deleteDirectory(resultsDir);
        }
        if (!resultsDir.mkdir()) {
            throw new IOException("Unable to create directory for unsure results");
        }
        resultsDir = new File("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results/error");
        if(resultsDir.exists()) {
            FileUtils.deleteDirectory(resultsDir);
        }
        if (!resultsDir.mkdir()) {
            throw new IOException("Unable to create directory for error results");
        }

        Set<Integer> checkedIds = new HashSet<>();

        File checkedDir = new File("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results/checked");
        if(checkedDir.exists()) {
            if(!checkedDir.isDirectory()) {
                throw new IOException(checkedDir.getPath()+" is not a directory");
            }

            File[] checkedFiles = checkedDir.listFiles();

            for(File checkedFile : checkedFiles) {
                InputStream is = new FileInputStream(checkedFile);
                InputStreamReader rd = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(rd);

                while(reader.ready()) {
                    String s = reader.readLine().trim();
                    if(s.startsWith("id: ")) {
                        checkedIds.add(Integer.valueOf(s.substring(4).trim()));
                    }
                }

                reader.close();
            }
        }

        InputStream is = new FileInputStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/dict_ru.dct");
        InputStreamReader rd = new InputStreamReader(is, "866");
        BufferedReader reader = new BufferedReader(rd);
        Set<String> allLinesSet = new HashSet<>();

        Map<String, ArrayList<ZindexKey>> allWords = new HashMap<>();
        Set<String> outfileNames = new HashSet<>();
        Map<String, Integer> allEncounteredInflExpressions = new HashMap<>();
        Map<Integer, Integer> paradigmElementsCountStats = new HashMap<>();
        Map<String, String> autoPathsForZindexes = new HashMap<>();
        Map<String, String> unsurePathsForZindexes = new HashMap<>();
        Map<String, String> errorPathsForZindexes = new HashMap<>();

        PrintStream unmatchedLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/unmatched.log");
        PrintStream zindexesLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/zindexes.log");
        PrintStream inflExpressionsLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/inflExpressions.log");
        PrintStream paradigmElementsCountLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/paradigmElementsCount.log");
        PrintStream unsureParadigmsLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/unsureParadigms.log");
        PrintStream errorParadigmsLog = new PrintStream("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/errorParadigms.log");
        ArrayList<Paradigm> unsureParadigms = new ArrayList<>();
        ArrayList<Paradigm> errorParadigms = new ArrayList<>();

        //noinspection TryFinallyCanBeTryWithResources
        try {
            int nLines = 0;
            int wordCount = 0;
            while (reader.ready()) {
                String s = reader.readLine().trim();
                int i = s.indexOf('\u0004');
                if (i > 0) {
                    s = s.substring(0, i).trim();
                }
                nLines++;

                if (allLinesSet.contains(s) || checkedIds.contains(nLines)) {
                    continue;
                }

                allLinesSet.add(s);

                Matcher m = megaPattern.matcher(s);

                String zindex;
                String accentInfo;
                String word;
                boolean unsure = false;
                String pltant_zindex = null;

                if (!m.matches()) {
                    m = weakPattern.matcher(s);
                    if (!m.matches()) {
                        unmatchedLog.println(s);
                        continue;
                    }
                    zindex = normalize(m.group(5));
                    accentInfo = normalize(m.group(2));
                    word = normalize(m.group(1)).toLowerCase();

                    m = pluralPattern.matcher(s);

                    if(m.matches()) {
                        pltant_zindex = normalize(m.group(7));
                    } else {
                        unsure = true;
                    }
                } else {
                    zindex = normalize(m.group(6));
                    accentInfo = normalize(m.group(2));
                    word = normalize(m.group(1)).toLowerCase();
                }

                String normalizedZindex = normalize(normalizationPattern.matcher(zindex).replaceAll(""));

                ArrayList<ZindexKey> words = allWords.get(normalizedZindex);
                if (words == null) {
                    words = new ArrayList<>();
                    allWords.put(normalizedZindex, words);
                }
                words.add(new ZindexKey(word, zindex, accentInfo, unsure, pltant_zindex, nLines, s.contains("!!ч2") || s.contains("!!ф")));
                wordCount++;
            }
            reader.close();

            for (Map.Entry<String, ArrayList<ZindexKey>> entry : allWords.entrySet()) {
                String outPath = getPathForZindex("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results", entry.getKey(), ParadigmStatus.auto);

                outPath += "_" + Integer.toString(entry.getValue().size());

                while (outfileNames.contains(outPath)) {
                    outPath += "_";
                }

                outfileNames.add(outPath);
                autoPathsForZindexes.put(entry.getKey(), outPath);

                outPath = getPathForZindex("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results", entry.getKey(), ParadigmStatus.unsure);

                outPath += "_" + Integer.toString(entry.getValue().size());

                while (outfileNames.contains(outPath)) {
                    outPath += "_";
                }

                outfileNames.add(outPath);
                unsurePathsForZindexes.put(entry.getKey(), outPath);

                outPath = getPathForZindex("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results", entry.getKey(), ParadigmStatus.error);

                outPath += "_" + Integer.toString(entry.getValue().size());

                while (outfileNames.contains(outPath)) {
                    outPath += "_";
                }

                outfileNames.add(outPath);
                errorPathsForZindexes.put(entry.getKey(), outPath);
            }

            System.out.println(String.format("%d strings scanned, found %d zindexes and %d words", nLines, allWords.size(), wordCount));

            HashSet<String> exceptionWords = new HashSet<>();
            exceptionWords.add("яя");

            List<String> sortedZindexes = new ArrayList<>(allWords.keySet());
            sortedZindexes.sort(String.CASE_INSENSITIVE_ORDER);
            Set<ZindexKey> processed = new HashSet<>();

            for (int i = 0; i < sortedZindexes.size(); i++) {
                String normalizedZindex = sortedZindexes.get(i);
                ArrayList<ZindexKey> zindexKeys = allWords.get(normalizedZindex);

                zindexesLog.println(normalizedZindex);
                SmartOutput smartOut = new SmartOutput(autoPathsForZindexes, unsurePathsForZindexes, errorPathsForZindexes, unsureParadigms, errorParadigms, i);

                try {
                    int counter = 0;
                    for (ZindexKey zindexKey : zindexKeys) {
                        Paradigm invarParadigm = tryInvar(zindexKey, engine, normalizedZindex);
                        if (invarParadigm != null) {
                            ParadigmElement pe = new ParadigmElement(null, null);
                            pe.form = zindexKey.lemma;

                            String accentInfo = normalize(zindexKey.accentInfo);
                            Matcher am = accentPattern.matcher(accentInfo);

                            if (!am.matches() || invarParadigm.hasGrammeme("unsure")) {
                                invarParadigm.modifyStatus(ParadigmStatus.unsure);
                            }

                            pe.mainAccent = Integer.valueOf(am.group(1)) - 1;
                            if (am.group(3) != null && !am.group(3).isEmpty()) {
                                pe.secondaryAccent = Integer.valueOf(am.group(3)) - 1;
                            }
                            if (am.group(5) != null && !am.group(5).isEmpty()) {
                                pe.yoPlace = Integer.valueOf(am.group(5)) - 1;
                            }

                            invarParadigm.addParadigmElement(pe);

                            Integer stat = paradigmElementsCountStats.get(1);
                            if (stat == null) {
                                stat = 0;
                            }
                            paradigmElementsCountStats.put(1, stat + 1);

                            if(invarParadigm.getStatus() == ParadigmStatus.unsure) {
                                unsureParadigmsLog.println(zindexKey.lemma + " " + normalizedZindex + " : " + invarParadigm.getError_message());
                            } else {
                                assert invarParadigm.getStatus() == ParadigmStatus.auto;
                            }
                            smartOut.print(invarParadigm);
                            continue;
                        }

                        Paradigm paradigm = new Paradigm(zindexKey, normalizedZindex, zindexKey.unsure ? ParadigmStatus.unsure : ParadigmStatus.auto);
                        if(zindexKey.unsure) {
                            paradigm.addError_message("weak regexp zindex match");
                        }

                        if (exceptionWords.contains(zindexKey.lemma)) {
                            paradigm.modifyStatus(ParadigmStatus.error);
                            paradigm.addError_message("starling fatal error");
                            errorParadigmsLog.println(zindexKey.lemma + " " + normalizedZindex + " : " + paradigm.getError_message());
                            smartOut.print(paradigm);
                            continue;
                        }

                        if (zindexKey.lemma.length() >= 20) {
                            paradigm.modifyStatus(ParadigmStatus.error);
                            paradigm.addError_message("too long lemma");
                            errorParadigmsLog.println(zindexKey.lemma + " " + normalizedZindex + " : " + paradigm.getError_message());
                            smartOut.print(paradigm);
                            continue;
                        }

                        ArrayList<String[]> variants;

                        try {
                            variants = engine.lowlevelProcess(zindexKey.lemma);
                        } catch (Exception e) {
                            paradigm.modifyStatus(ParadigmStatus.error);
                            paradigm.addError_message("starling internal error");
                            errorParadigmsLog.println(zindexKey.lemma + " " + normalizedZindex + " : " + paradigm.getError_message());
                            smartOut.print(paradigm);
                            throw new RuntimeException("Starling internal error with word " + zindexKey.lemma, e);
                        }

                        if (variants.isEmpty()) {
                            paradigm.modifyStatus(ParadigmStatus.error);
                            paradigm.addError_message("empty starling analysis");
                            errorParadigmsLog.println(zindexKey.lemma + " " + normalizedZindex + " : " + paradigm.getError_message());
                            smartOut.print(paradigm);
                            continue;
                        }

                        if ((counter++ % 100) == 0) {
                            System.out.println(counter);
                        }

                        String matchedParadigm = null;
                        String matchedStarlingZindex = null;
                        int bestCommonPrefixLength = -1;
                        ZindexKey matchedCheckKey = null;
                        ZindexKey checkKey;
                        for (String[] strings : variants) {
                            if (strings.length < 4 || strings[3] == null) {
                                bestCommonPrefixLength = -1;
                                break;
                            }

                            String starlingBaseForm = normalize(strings[0]);

                            if (zindexKey.lemma.equals(starlingBaseForm)) {
                                String starlingZindex = normalize(strings[1]);
                                checkKey = new ZindexKey(starlingBaseForm, starlingZindex, "", false, null, -1, false);
                                if (processed.contains(checkKey)) {
                                    continue;
                                }

                                int commonPrefixLength = countCommonPrefixLength(starlingZindex, normalizedZindex);
                                int commonPrefixLength2 = countCommonPrefixLength(starlingZindex, zindexKey.zindex);

                                commonPrefixLength = Math.max(commonPrefixLength, commonPrefixLength2);

                                if (bestCommonPrefixLength == -1 || commonPrefixLength > bestCommonPrefixLength) {
                                    bestCommonPrefixLength = commonPrefixLength;
                                    matchedParadigm = strings[3].trim();
                                    matchedStarlingZindex = starlingZindex;
                                    matchedCheckKey = checkKey;
                                }
                            }
                        }

                        if (bestCommonPrefixLength <= 0) {
                            paradigm.modifyStatus(ParadigmStatus.error);
                            paradigm.addError_message("unable to find matching zindex");
                            errorParadigmsLog.println(zindexKey.lemma + " " + normalizedZindex + " : " + paradigm.getError_message());
                            smartOut.print(paradigm);
                            continue;
                        }

                        processed.add(matchedCheckKey);
                        paradigm.setStarling_paradigm(matchedParadigm);
                        paradigm.setStarling_zindex(matchedStarlingZindex);

                        if (!paradigm.extractLexGrammemes(engine, zindexKey.proper)) {
                            paradigm.modifyStatus(ParadigmStatus.error);
                            paradigm.addError_message("unable to extract lexical info");
                        }

                        StringTokenizer tok = new StringTokenizer(matchedParadigm, ";");

                        boolean paradigmParsingProblemDetected = false;
                        boolean accentuationProblemDetected = false;
                        boolean inflParsingProblemDetected = false;
                        Set<String> commonPart = null;
                        while (tok.hasMoreTokens()) {
                            String p_element = normalize(tok.nextToken());

                            if (p_element.isEmpty()) {
                                commonPart = null;
                                continue;
                            }

                            Matcher m = inflPattern.matcher(p_element);

                            if (!m.matches()) {
                                paradigmParsingProblemDetected = true;
                                ParadigmElement paradigmElement = new ParadigmElement(p_element, null);
                                paradigmElement.unparsed = true;
                                paradigm.addParadigmElement(paradigmElement);
                                continue;
                            }
                            String infl_info = normalize(m.group(1));

                            StringTokenizer inflTokenizer = new StringTokenizer(infl_info,",");

                            while(inflTokenizer.hasMoreTokens()) {
                                infl_info = normalize(inflTokenizer.nextToken());

                                Integer n = allEncounteredInflExpressions.get(infl_info);
                                if (n == null) {
                                    n = 0;
                                }
                                allEncounteredInflExpressions.put(infl_info, n + 1);

                                String complex_form = normalize(m.group(3)).toLowerCase();
                                ParadigmElement protoParadigmElement = new ParadigmElement(complex_form, infl_info);
                                if (commonPart != null) {
                                    protoParadigmElement.inflGrammemes.addAll(commonPart);
                                }
                                if (!protoParadigmElement.extractInflGrammemes(infl_info, engine, paradigm)) {
                                    inflParsingProblemDetected = true;
                                }

                                String secondary_info = m.group(2);

                                if (secondary_info != null && !secondary_info.isEmpty()) {
                                    secondary_info = normalize(secondary_info);
                                    n = allEncounteredInflExpressions.get(secondary_info);
                                    if (n == null) {
                                        n = 0;
                                    }
                                    allEncounteredInflExpressions.put(secondary_info, n + 1);

                                    commonPart = new HashSet<>(protoParadigmElement.inflGrammemes);

                                    protoParadigmElement.starling_infl_info += " " + secondary_info;

                                    if (!protoParadigmElement.extractInflGrammemes(secondary_info, engine, paradigm)) {
                                        inflParsingProblemDetected = true;
                                    }
                                }

                                complex_form = complex_form.replaceAll("\\[|\\]", "");
                                @SuppressWarnings("StringTokenizerDelimiter") StringTokenizer formTok = new StringTokenizer(complex_form, "//");

                                while (formTok.hasMoreTokens()) {
                                    String form = normalize(formTok.nextToken().replaceAll("=>|<=",""));

                                    if("-".equals(form)) {
                                        continue;
                                    }

                                    boolean shiFound = false;
                                    if(form.endsWith("(ши)")) {
                                        shiFound = true;
                                        form = form.substring(0,form.length()-4);
                                    }

                                    ParadigmElement paradigmElement = new ParadigmElement(form, protoParadigmElement.starling_infl_info);
                                    if (!paradigmElement.extractAccentInfo()) {
                                        accentuationProblemDetected = true;
                                    }
                                    paradigmElement.inflGrammemes.addAll(protoParadigmElement.inflGrammemes);
                                    paradigm.addParadigmElement(paradigmElement);

                                    if(shiFound) {
                                        paradigmElement = new ParadigmElement(form + "ши", protoParadigmElement.starling_infl_info);
                                        if (!paradigmElement.extractAccentInfo()) {
                                            accentuationProblemDetected = true;
                                        }
                                        paradigmElement.inflGrammemes.addAll(protoParadigmElement.inflGrammemes);
                                        paradigm.addParadigmElement(paradigmElement);
                                    }
                                }
                            }
                        }

                        if (paradigm.getParadigmSize() == 0) {
                            paradigm.modifyStatus(ParadigmStatus.error);
                            paradigm.addError_message("empty paradigm");
                            errorParadigmsLog.println(zindexKey.lemma + " " + normalizedZindex + " : " + paradigm.getError_message());
                            smartOut.print(paradigm);
                            continue;
                        }

                        if (paradigmParsingProblemDetected || accentuationProblemDetected ||
                                inflParsingProblemDetected || paradigm.hasGrammeme("unsure") ||
                                bestCommonPrefixLength < normalizedZindex.length() && bestCommonPrefixLength < zindexKey.zindex.length()) {
                            paradigm.modifyStatus(ParadigmStatus.unsure);

                            if (paradigmParsingProblemDetected) {
                                paradigm.addError_message("complex parsing problem");
                            }

                            if (accentuationProblemDetected) {
                                paradigm.addError_message("accentuation problem");
                            }

                            if (inflParsingProblemDetected) {
                                paradigm.addError_message("grammar info problem");
                            }

                            if (paradigm.hasGrammeme("unsure")) {
                                paradigm.addError_message("uncertain lex conversion");
                            }

                            if (bestCommonPrefixLength < normalizedZindex.length() && bestCommonPrefixLength < zindexKey.zindex.length()) {
                                paradigm.addError_message("fuzzy zindex match");
                            }
                        }

                        int elementCount = paradigm.getParadigmSize();

                        Integer stat = paradigmElementsCountStats.get(elementCount);
                        if (stat == null) {
                            stat = 0;
                        }
                        paradigmElementsCountStats.put(elementCount, stat + 1);

                        if(paradigm.getStatus() == ParadigmStatus.unsure) {
                            unsureParadigmsLog.println(zindexKey.lemma + " " + normalizedZindex + " : " + paradigm.getError_message());
                        } else if(paradigm.getStatus() == ParadigmStatus.error) {
                            errorParadigmsLog.println(zindexKey.lemma + " " + normalizedZindex + " : " + paradigm.getError_message());
                        }
                        smartOut.print(paradigm);
                    }
                } finally {
                    smartOut.close();
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
            unmatchedLog.close();
            zindexesLog.close();
            inflExpressionsLog.close();
            paradigmElementsCountLog.close();
            unsureParadigmsLog.close();
            errorParadigmsLog.close();

            File unsureSortedFile = new File("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results/unsure/sortedByFrequency." + String.format("%05d", unsureParadigms.size()));
            if (!unsureSortedFile.exists()) {
                if (!unsureSortedFile.createNewFile()) {
                    System.err.println("Unable to create " + unsureSortedFile.getPath());
                }
            }
            PrintStream unsureOut = new PrintStream(new FileOutputStream(unsureSortedFile));
            Collections.sort(unsureParadigms);
            for (Paradigm unsureParadigm : unsureParadigms) {
                unsureOut.print(unsureParadigm.toString());
            }
            unsureOut.close();

            File errorsSortedFile = new File("C:/projects/treeton-git/runtime/domains/Russian/resources/starlingMorph/4Yandex/results/error/sortedByFrequency." + String.format("%05d", errorParadigms.size()));
            if (!errorsSortedFile.exists()) {
                if (!errorsSortedFile.createNewFile()) {
                    System.err.println("Unable to create " + errorsSortedFile.getPath());
                }
            }
            PrintStream errorsOut = new PrintStream(new FileOutputStream(errorsSortedFile));
            Collections.sort(errorParadigms);
            for (Paradigm errorParadigm : errorParadigms) {
                errorsOut.print(errorParadigm.toString());
            }
            errorsOut.close();
        }
    }

    private static Paradigm tryInvar(ZindexKey zindexKey, StarlingMorphEngine engine, String normalizedZindex) {
        Paradigm p = new Paradigm(zindexKey, normalizedZindex, zindexKey.unsure ? ParadigmStatus.unsure : ParadigmStatus.auto);
        if(zindexKey.unsure) {
            p.addError_message("weak regexp zindex match");
        }

        return p.extractLexGrammemes(engine, zindexKey.proper) && p.hasGrammeme("INVAR") ? p : null;
    }

    static Map<String,Character> mapping = new HashMap<>();
    public static Set<Character> vowels = new HashSet<>();

    static {
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

        mapping.put("а'", (char) 9617);
        mapping.put("е'", (char) 9619);
        mapping.put("и'", (char) 9558);
        mapping.put("о'", (char) 9571);
        mapping.put("у'", (char) 9563);
        mapping.put("ы'", (char) 9516);
        mapping.put("э'", (char) 9472);
        mapping.put("ю'", (char) 9566);
        mapping.put("я'", (char) 9577);
        mapping.put("а`", (char) 9618);
        mapping.put("е`", (char) 9508);
        mapping.put("ё`", (char) 9570);
        mapping.put("и`", (char) 9557);
        mapping.put("о`", (char) 9564);
        mapping.put("у`", (char) 9524);
        mapping.put("ы`", (char) 9500);
        mapping.put("э`", (char) 9532);
        mapping.put("ю`", (char) 9567);
        mapping.put("я`", (char) 9574);
        mapping.put("е\"", (char) 9569);
    }

    private static int countCommonPrefixLength(String starlingString, String dictString) {
        int i = 0, j = 0;
        for( ; i < starlingString.length() && j < dictString.length(); i++, j++) {
            char starlingChar = starlingString.charAt(i);
            char dictChar = dictString.charAt(j);

            if(dictChar == '~' && starlingChar == (char) 9484) {
                continue;
            }

            if(vowels.contains(starlingChar) && i + 1 < starlingString.length()) {
                String twoStarlingChars = starlingString.substring(i,i+2);
                Character replacement = mapping.get(twoStarlingChars);

                if(replacement != null && replacement == dictChar) {
                    i++;
                    continue;
                }
            }

            if(starlingChar != dictChar) {
                break;
            }
        }

        return j + (j == dictString.length() && i < starlingString.length() &&
                (starlingString.charAt(i) == ' ' || starlingString.charAt(i) == '(') ? 1 : 0);
    }
}
