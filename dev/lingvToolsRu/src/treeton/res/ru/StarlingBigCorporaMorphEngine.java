/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.BlackBoard;
import treeton.core.TreetonFactory;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationXMLImpl;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.scape.trnmapper.StringToTrnMapperRule;
import treeton.core.scape.trnmapper.StringToTrnMapperRuleStorage;
import treeton.core.util.sut;
import treeton.dict.Dictionary;
import treeton.morph.MorphException;
import treeton.morph.MorphInterface;
import treeton.morph._native.NativeRusMorphEngine;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class StarlingBigCorporaMorphEngine implements MorphInterface, GrammAndZindexLogger {
    private TreeMap<String, Collection<Properties>> data;
    private HashMap<String, Object[]> zindexes;
    private HashSet<String> errors;
    private HashMap<String, Object[]> gramms;
    private URL loggingFolder;
    private StringToTrnMapperRuleStorage lexMapper;
    private StringToTrnMapperRuleStorage inflMapper;
    private BlackBoard localBoard;
    private BlackBoard localBoard1;
    private BlackBoard localBoard2;
    private Map<String, Properties> lexInfo;
    private Map<String, Properties> inflInfo;

    public static void main(String[] args) throws Exception {
        StarlingBigCorporaMorphEngine engine = new StarlingBigCorporaMorphEngine();

        BasicConfiguration.createInstance();
        ContextConfiguration.registerConfigurationClass(ContextConfigurationXMLImpl.class);
        ContextConfiguration.createInstance();

        TreenotationsContext context = ContextConfiguration.getInstance().getTreenotationsContextManager().get("Common.Russian");
        String lexMapping = "C:\\projects\\treeton\\runtime\\domains\\Russian\\resources\\starlingMorph\\conversionLex.map";
        TrnType targetType = context.getTypes().get("Gramm");
        engine.init(context, targetType, lexMapping, "C:\\projects\\treeton\\runtime\\domains\\Russian\\resources\\starlingMorph\\conversionInfl.map");
        engine.setLoggingFolder(new File("./").toURI().toURL());

//        for (File file : new File("C:\\projects\\treeton\\doc\\russianWordForms\\russianWordForms\\").listFiles()) {
//            engine.readFile(file);
//            System.out.println("File "+file+" was read succesfully "+ Utils.memoryState());
//        }

        engine.readFile(new File("C:\\projects\\treeton\\doc\\russianWordForms\\russianWordForms\\29ya.csv"));
        engine.logZindexesGrammsAndErrors();

        MorphInterfaceBenchMarkingTool benchMarkingTool = new MorphInterfaceBenchMarkingTool();

        benchMarkingTool.setGoldStandard(engine);

        NativeRusMorphEngine nativeRusMorphEngine = new NativeRusMorphEngine();
        nativeRusMorphEngine.init("C:\\StarSoft\\dict\\dict_ru.dct", lexMapping, context, targetType);

        benchMarkingTool.addMorphInterface(nativeRusMorphEngine, null);
        benchMarkingTool.addIgnoredProperty("ZINDEX");

        FileWriter buf = new FileWriter("comparison.html");

        buf.append("<html><body><table>");
        benchMarkingTool.benchMark(engine.getAllWords().iterator(), buf);
        buf.append("</html></body></table>");

        buf.close();
    }

    public void readFile(File f) throws IOException, TreetonModelException {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        while (reader.ready()) {
            String s = reader.readLine().trim();

            char[] chars = s.substring(1, s.length() - 1).toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];

                if (c == '"' && i < chars.length - 2 && chars[i + 1] == ',' && chars[i + 2] == '\"') {
                    chars[i] = 0;
                    chars[i + 1] = 0;
                    chars[i + 2] = 0;
                }
            }


            String[] strings = new String(chars).split("\u0000");

            String base = strings[0].trim();

            if ("WORD".equals(base))
                continue;

            String zindex = strings[3].trim();
            int idx = zindex.indexOf(" ");
            zindex = idx >= 0 ? zindex.substring(idx).trim() : zindex;

            String paradigm = strings.length > 9 ? strings[9].trim() : null;

            importString(base, zindex, paradigm);
        }
        reader.close();
    }

    public void importString(String base, String zindex, String paradigm) throws TreetonModelException {
        ArrayList<Properties> buffer = new ArrayList<Properties>();
        processStrings(buffer, base, zindex, paradigm);

        for (Properties properties : buffer) {
            String form = properties.getProperty("WORDFORM");
            Collection<Properties> collection = data.get(form);
            if (collection == null) {
                collection = new ArrayList<Properties>(Arrays.asList(properties));
                data.put(form, collection);
            } else {
                collection.add(properties);
            }
        }
    }

    public Collection<Properties> processOneWord(String word, Dictionary dictArray) throws MorphException {
        return data.get(word);
    }

    public void init(TreenotationsContext trnContext, TrnType targetType, String lexPath, String inflPath) throws TreetonModelException, IOException, ParseException {
        lexMapper = new StringToTrnMapperRuleStorage(trnContext.getTypes(), targetType);
        lexMapper.readInFromFile(lexPath);
        inflMapper = new StringToTrnMapperRuleStorage(trnContext.getTypes(), targetType);
        inflMapper.readInFromFile(inflPath);

        localBoard = TreetonFactory.newBlackBoard(50, false);
        localBoard1 = TreetonFactory.newBlackBoard(50, false);
        localBoard2 = TreetonFactory.newBlackBoard(50, false);

        data = new TreeMap<String, Collection<Properties>>();

        lexInfo = new HashMap<String, Properties>();
        inflInfo = new HashMap<String, Properties>();

        reset();
    }

    public void deInit() {
        lexMapper = null;
        inflMapper = null;

        localBoard = null;
        localBoard1 = null;
        localBoard2 = null;

        errors = null;
        zindexes = null;
        gramms = null;
        lexInfo = null;
        inflInfo = null;
    }

    Properties getLexInfo(String gramminfo, String base) throws TreetonModelException {
        gramminfo = gramminfo.replaceAll("\\.", " ").trim() + " ";

        Properties props = lexInfo.get(gramminfo);
        if (props == null) {
            props = new Properties();
            Object[] lexRules = lexMapper.getRules(gramminfo);
            if (lexRules != null && lexRules.length >= 0) {
                for (Object o : lexRules) {
                    if (o == null) {
                        continue;
                    }
                    StringToTrnMapperRule lexRule = (StringToTrnMapperRule) o;
                    lexRule.bind(gramminfo);
                    while (lexRule.next()) {
                        localBoard.clean();
                        lexRule.assign(localBoard);
                        for (int i = 0; i <= localBoard.getDepth(); i++) {
                            Object obj = localBoard.get(i);
                            if (obj != null) {
                                props.setProperty(lexMapper.getTargetType().getFeatureNameByIndex(i), obj.toString());
                            }
                        }
                    }
                    lexRule.unbind();
                }
            }
            if (props.size() > 0)
                lexInfo.put(gramminfo, props);
            else
                unableToConvertGrammInfoError(gramminfo, base);

            if (isLogging()) {
                Object[] oarr = zindexes.get(gramminfo);
                if (oarr == null) {
                    oarr = new Object[]{base};
                    zindexes.put(gramminfo, oarr);
                }
            }
        }
        return props;
    }

    Properties getInflInfo(String gramminfo, String base, String zindex) throws TreetonModelException {
        gramminfo = gramminfo.replaceAll("\\.", " ").trim();

        Properties props = inflInfo.get(gramminfo);
        if (props == null) {
            props = new Properties();
            Object[] inflRules = inflMapper.getRules(gramminfo);
            if (inflRules != null && inflRules.length >= 0) {
                for (Object o : inflRules) {
                    if (o == null) {
                        continue;
                    }
                    StringToTrnMapperRule inflRule = (StringToTrnMapperRule) o;
                    inflRule.bind(gramminfo);
                    while (inflRule.next()) {
                        localBoard.clean();
                        inflRule.assign(localBoard);
                        for (int i = 0; i <= localBoard.getDepth(); i++) {
                            Object obj = localBoard.get(i);
                            if (obj != null) {
                                props.setProperty(lexMapper.getTargetType().getFeatureNameByIndex(i), obj.toString());
                            }
                        }
                    }
                    inflRule.unbind();
                }
            }
            if (props.size() > 0)
                inflInfo.put(gramminfo, props);
            else
                unableToConvertGrammInfoError(gramminfo, base);

            if (isLogging()) {
                Object[] oarr = gramms.get(gramminfo);
                if (oarr == null) {
                    oarr = new Object[]{base, zindex};
                    gramms.put(gramminfo, oarr);
                }
            }
        }

        return props;
    }

    public void processStrings(ArrayList<Properties> buffer, String base, String zindex, String paradigm) throws TreetonModelException {
        Properties lex = getLexInfo(zindex, base);
        if (lex.size() == 0)
            return;

        int initialSize = buffer.size();

        paradigm = paradigm == null ? null : paradigm.trim();

        if (paradigm == null || paradigm.length() == 0) {
            Properties props = new Properties();
            props.putAll(lex);
            props.setProperty("base", base);
            props.setProperty("WORDFORM", base);
            props.setProperty("ZINDEX", zindex);
            buffer.add(props);
            return;
        }

        String[] strings = paradigm.split(",");

        List<String> currentForms = new ArrayList<String>();

        boolean grammInfoWasRead = false;

        for (String string : strings) {
            string = string.trim();

            if (isCyrillic(string) || "-".equals(string)) {
                if (grammInfoWasRead)
                    currentForms.clear();

                currentForms.add(string);
            } else {
                grammInfoWasRead = true;
                Properties additionalInfo = null;
                if (string.contains("part.")) {
                    int i = string.lastIndexOf(" ");
                    if (i >= 0) {
                        String partInfl = string.substring(0, i).trim();
                        string = string.substring(i + 1);
                        additionalInfo = getInflInfo(partInfl, base, zindex);
                    }
                }

                Properties infl = getInflInfo(string, base, zindex);
                if (infl.size() > 0 && (additionalInfo == null || additionalInfo.size() > 0)) {
                    for (String currentForm : currentForms) {
                        if ("-".equals(currentForm))
                            continue;

                        currentForm = currentForm.replaceAll("\\*", " "); //todo в атрибут
                        Properties props = new Properties();
                        props.putAll(lex);
                        props.setProperty("base", base);
                        props.setProperty("WORDFORM", currentForm);
                        props.setProperty("ZINDEX", zindex);
                        props.putAll(infl);
                        if (additionalInfo != null) {
                            props.putAll(additionalInfo);
                        }
                        buffer.add(props);
                    }
                } else {
                    while (buffer.size() > initialSize) {
                        buffer.remove(buffer.size() - 1);
                    }

                    return;
                }
            }
        }
    }

    private boolean isCyrillic(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (sut.isLetterCyrillic(string.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    private void unableToConvertGrammInfoError(String gramm, String base) {
        errors.add("Unable to convert grammarInfo: <" + gramm + ":" + base + ">");
    }

    public void reset() {
        errors = new HashSet<String>();
        zindexes = new HashMap<String, Object[]>();
        gramms = new HashMap<String, Object[]>();

        localBoard.clean();
        localBoard1.clean();
        localBoard2.clean();
    }

    public void logZindexesGrammsAndErrors() throws ContextException {
        Map.Entry[] arr = zindexes.entrySet().toArray(new Map.Entry[zindexes.entrySet().size()]);
        Comparator<Map.Entry> kc = new Comparator<Map.Entry>() {
            public int compare(Map.Entry e1, Map.Entry e2) {
                //noinspection unchecked
                return ((Comparable) e1.getKey()).compareTo(e2.getKey());
            }
        };

        Arrays.sort(arr, kc);
        try {
            File path = new File(new URL(loggingFolder, "./temp/zindexes.log").getPath());
            path.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(path);
            for (Map.Entry e : arr) {
                fos.write(e.getKey().toString().getBytes());
                Object[] oarr = (Object[]) e.getValue();
                fos.write(" <".getBytes());
                fos.write(oarr[0].toString().getBytes());
                fos.write(">\n".getBytes());
            }
            fos.close();
            arr = gramms.entrySet().toArray(new Map.Entry[gramms.entrySet().size()]);
            Arrays.sort(arr, kc);

            path = new File(new URL(loggingFolder, "./temp/gramms.log").getPath());
            path.getParentFile().mkdirs();

            fos = new FileOutputStream(path);
            for (Map.Entry e : arr) {
                fos.write(e.getKey().toString().getBytes());
                Object[] oarr = (Object[]) e.getValue();
                fos.write(" <".getBytes());
                fos.write(oarr[0].toString().getBytes());
                fos.write(":".getBytes());
                fos.write(oarr[1].toString().getBytes());
                fos.write(">\n".getBytes());
            }
            fos.close();

            String[] sarr = errors.toArray(new String[errors.size()]);
            Arrays.sort(sarr);

            path = new File(new URL(loggingFolder, "./temp/errors.log").getPath());
            path.getParentFile().mkdirs();


            fos = new FileOutputStream(path);
            for (String s : sarr) {
                fos.write("StarlingBigCorporaMorphEngine: ".getBytes());
                fos.write(s.getBytes());
                fos.write("\n".getBytes());
            }
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLoggingFolder(URL loggingFolder) {
        this.loggingFolder = loggingFolder;
    }

    public boolean isLogging() {
        return loggingFolder != null;
    }

    public Collection<String> getAllWords() {
        return data.keySet();
    }
}
