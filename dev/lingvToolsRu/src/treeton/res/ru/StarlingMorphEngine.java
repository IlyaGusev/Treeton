/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.BlackBoard;
import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.TreetonFactory;
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

import java.io.*;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

public class StarlingMorphEngine implements MorphInterface, GrammAndZindexLogger {
    private int port;
    private Socket socket;
    private OutputStream out;
    private InputStream in;

    private ByteBuffer buf;
    private StringBuffer tbuf;

    private HashMap<String, Object[]> zindexes;
    private HashSet<String> errors;
    private HashMap<String, Object[]> gramms;
    private URL loggingFolder;

    private StringToTrnMapperRuleStorage lexMapper;
    private StringToTrnMapperRuleStorage inflMapper;

    private BlackBoard localBoard;
    private BlackBoard localBoard1;
    private BlackBoard localBoard2;
    private TrnType targetType;

    private String _t_gramm;
    private String _t_base;
    private String _t_zindex;
    private String _t_paradigm;

    private int ZINDEX_feature;
    private int base_feature;
    private int REPR_feature;
    private int ACCPL_feature;

    private int WORDFORM_feature;

    public Collection<Properties> processOneWord(String word, Dictionary dictArray) throws MorphException {
        word = word.toLowerCase();
        ArrayList<String[]> l = null;
        try {
            l = lowlevelProcess(word);
        } catch (Exception e) {
            e.printStackTrace();
        }


        ArrayList<Properties> res = new ArrayList<Properties>();

        if (l == null || l.size() == 0) {
            //fillBlackBoard(null,null,source,null,null);
            //localBoard.put(AGGROTYPE_feature,"unknown");
            //buffer.add(TreetonFactory.newTreenotation(r.getStartToken(), r.getEndToken(), targetType, localBoard));
            return res;
        }


        for (String[] arr : l) {
            ArrayList<Treenotation> buffer = new ArrayList<Treenotation>();

            processArray(buffer, arr, word, null, null);

            for (Treenotation treenotation : buffer) {
                Properties props = new Properties();
                for (int i = 0; i < treenotation.size(); i++) {
                    try {
                        String nm = targetType.getFeatureNameByIndex(treenotation.getKey(i));
                        Object o = treenotation.getByIndex(i);
                        props.setProperty(nm, o == null ? null : o.toString());
                    } catch (TreetonModelException e) {
                        //do nothing
                    }
                }
                res.add(props);
            }
        }

        return res;
    }

    public void init(TreenotationsContext trnContext, TrnType targetType, String lexPath, String inflPath, int port) throws TreetonModelException, IOException, ParseException {
        lexMapper = new StringToTrnMapperRuleStorage(trnContext.getTypes(), targetType);
        lexMapper.readInFromFile(lexPath);
        inflMapper = new StringToTrnMapperRuleStorage(trnContext.getTypes(), targetType);
        inflMapper.readInFromFile(inflPath);
        this.port = port;

        localBoard = TreetonFactory.newBlackBoard(50, false);
        localBoard1 = TreetonFactory.newBlackBoard(50, false);
        localBoard2 = TreetonFactory.newBlackBoard(50, false);
        buf = ByteBuffer.allocate(30000);

        base_feature = targetType.getFeatureIndex("base");
        ZINDEX_feature = targetType.getFeatureIndex("ZINDEX");
        WORDFORM_feature = targetType.getFeatureIndex("WORDFORM");
        ACCPL_feature = targetType.getFeatureIndex("ACCPL");
        REPR_feature = targetType.getFeatureIndex("REPR");

        tbuf = new StringBuffer();

        this.targetType = targetType;

        reset();
    }

    public void deInit() {
        lexMapper = null;
        inflMapper = null;

        localBoard = null;
        localBoard1 = null;
        localBoard2 = null;
        buf = null;
        tbuf = null;

        errors = null;
        zindexes = null;
        gramms = null;
    }

    private void initSocket() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        socket = null;
        while (socket == null) {
            try {
                socket = new Socket("localhost", port);
            } catch (BindException e) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                System.out.println("trying to connect...");
            }
        }
        out = socket.getOutputStream();
        in = socket.getInputStream();
    }


    public void processArray(ArrayList<Treenotation> buffer, String[] strings, String word, Token start, Token end) {
        int sz = buffer.size();

        String s = strings[1].replaceAll("\\.", " ").trim();
        String base = strings[0];
        String paradigm = strings[3];

        Object[] lexRules = lexMapper.getRules(s);
        if (lexRules != null && lexRules.length >= 0) {
            for (Object o : lexRules) {
                if (o == null) {
                    continue;
                }
                StringToTrnMapperRule lexRule = (StringToTrnMapperRule) o;
                lexRule.bind(s);
                while (lexRule.next()) {
                    StringTokenizer tok = new StringTokenizer(strings[2], ",");
                    localBoard2.clean();
                    while (tok.hasMoreTokens()) {
                        s = tok.nextToken().replaceAll("\\*", " ").replaceAll("\\.", " ").replaceAll("/", " ").trim();
                        localBoard.clean();
                        inflMapper.assign(localBoard, s);
                        if (localBoard.contains(REPR_feature) && localBoard.get(REPR_feature).equals("part")) {
                            localBoard2.clean();
                            localBoard2.put(localBoard);
                            localBoard.clean();
                            continue;
                        }
                        localBoard.clean();


                        Object[] inflRules = inflMapper.getRules(s);
                        if (inflRules != null && inflRules.length > 0) {
                            for (Object o1 : inflRules) {
                                if (o1 == null) {
                                    continue;
                                }
                                StringToTrnMapperRule inflRule = (StringToTrnMapperRule) o1;
                                inflRule.bind(s);
                                while (inflRule.next()) {
                                    boolean noAccent = strings[1].indexOf("без удар") >= 0;
                                    if (paradigm != null && !noAccent) {
                                        int curBufSize = buffer.size();
                                        for (int i = 0; i < paradigm.length(); i++) {
                                            if (sut.isLetterCyrillic(paradigm.charAt(i)) && (i == 0 || !sut.isLetterCyrillic(paradigm.charAt(i - 1)))) {
                                                int j = i, k = 0;
                                                while (j < paradigm.length() && k < word.length()) {
                                                    if (paradigm.charAt(j) == '\'' || paradigm.charAt(j) == '"' || paradigm.charAt(j) == '`') {
                                                        j++;
                                                    } else if (paradigm.charAt(j) == word.charAt(k)) {
                                                        j++;
                                                        k++;
                                                    } else {
                                                        break;
                                                    }
                                                }
                                                if (k == word.length()) {
                                                    while (j < paradigm.length() && (paradigm.charAt(j) == '\'' || paradigm.charAt(j) == '"' || paradigm.charAt(j) == '`'))
                                                        j++;

                                                    if (j == paradigm.length() || !sut.isLetterCyrillic(paradigm.charAt(j)) && paradigm.charAt(j) != '-') {
                                                        for (k = i; k >= 0; k--) {
                                                            if (paradigm.charAt(k) == ';') {
                                                                k = k + 1;
                                                                break;
                                                            }
                                                        }

                                                        if (k < 0)
                                                            k = 0;

                                                        int l = k;
                                                        while (l < paradigm.length() && !sut.isLetterCyrillic(paradigm.charAt(l)) && "(-_".indexOf(paradigm.charAt(l)) == -1)
                                                            l++;
                                                        String gramm = paradigm.substring(k, l).replaceAll("\\*", " ").replaceAll("\\.", " ").replaceAll("/", " ").trim();

                                                        if (gramm.length() == 0) {
                                                            for (l = i; l < j; l++) {
                                                                if (paradigm.charAt(l) == '\'' || paradigm.charAt(l) == '"') {
                                                                    localBoard.put(ACCPL_feature, l - i - 1);
                                                                    break;
                                                                }
                                                            }

                                                            if (l == j) {
                                                                formWithoutAccentInTheParadigm(word, strings, paradigm.substring(i, j));
                                                            }

                                                            fillBlackBoard(lexRule, inflRule, word, base, strings[1]);
                                                            buffer.add(TreetonFactory.newTreenotation(start, end, targetType, localBoard));
                                                        } else {
                                                            fillBlackBoard(lexRule, inflRule, word, base, strings[1]);

                                                            inflMapper.assign(localBoard1, gramm);
                                                            for (l = 0; l <= localBoard1.getDepth(); l++) {
                                                                Object v = localBoard1.get(l);
                                                                if (v == null)
                                                                    continue;
                                                                if (!v.equals(localBoard.get(l))) {
                                                                    break;
                                                                }
                                                            }

                                                            if (l > localBoard1.getDepth()) {
                                                                for (l = i; l < j; l++) {
                                                                    if (paradigm.charAt(l) == '\'' || paradigm.charAt(l) == '"') {
                                                                        localBoard.put(ACCPL_feature, l - i - 1);
                                                                        break;
                                                                    }
                                                                }

                                                                if (l == j) {
                                                                    formWithoutAccentInTheParadigm(word, strings, paradigm.substring(i, j));
                                                                }

                                                                buffer.add(TreetonFactory.newTreenotation(start, end, targetType, localBoard));
                                                            }
                                                            localBoard1.clean();
                                                            localBoard.clean();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (buffer.size() == curBufSize) {
                                            noAccentError(word, strings);
                                            fillBlackBoard(lexRule, inflRule, word, base, strings[1]);
                                            buffer.add(TreetonFactory.newTreenotation(start, end, targetType, localBoard));
                                        }
                                    } else {
                                        if (!noAccent) {
                                            noAccentError(word, strings);
                                        } else {
                                            localBoard.put(ACCPL_feature, -1);
                                        }

                                        fillBlackBoard(lexRule, inflRule, word, base, strings[1]);
                                        buffer.add(TreetonFactory.newTreenotation(start, end, targetType, localBoard));
                                    }
                                }
                                inflRule.unbind();
                            }
                        }
                    }
                }
                lexRule.unbind();
            }
        }

        if (sz == buffer.size())
            unableToConvertError(word, strings);
    }

    private void unableToConvertError(String word, String[] arr) {
        errors.add("Unable to convert variant of analysis: <" + word + ":" + arr[0] + " [" + arr[1] + "] " + arr[2] + "; " + (arr[3] == null ? "null" : arr[3]) + ">");
    }

    private void noAccentError(String word, String[] arr) {
        errors.add("Unable to find accent for the variant of analysis: " + " <" + word + ":" + arr[0] + " [" + arr[1] + "] " + arr[2] + "; " + (arr[3] == null ? "null" : arr[3]) + ">");
    }

    private void formWithoutAccentInTheParadigm(String word, String[] arr, String form) {
        errors.add("Form without accent (" + form + ") was located in the paradigm. Variant of analysis: " + " <" + word + ":" + arr[0] + " [" + arr[1] + "] " + arr[2] + "; " + (arr[3] == null ? "null" : arr[3]) + ">");
    }

    public void reset() {
        buf = ByteBuffer.allocate(30000);
        tbuf = new StringBuffer();

        errors = new HashSet<String>();
        zindexes = new HashMap<String, Object[]>();
        gramms = new HashMap<String, Object[]>();

        localBoard.clean();
        localBoard1.clean();
        localBoard2.clean();
    }

    private void fillBlackBoard(StringToTrnMapperRule lexRule, StringToTrnMapperRule inflRule, String source, String base, String zindex) {
        if (localBoard2.getNumberOfObjects() > 0) {
            localBoard.put(localBoard2);
        }

        if (base != null) {
            localBoard.put(base_feature, base);
        }
        if (zindex != null) {
            localBoard.put(ZINDEX_feature, zindex);
        }

        localBoard.put(WORDFORM_feature, source);

        if (lexRule != null) {
            lexRule.assign(localBoard);
        }
        if (inflRule != null) {
            inflRule.assign(localBoard);
        }
    }


    public ArrayList<String[]> lowlevelProcess(String word) throws IOException, ParseException {
        //System.out.println("w: "+word);

        initSocket();


        buf.clear();
        buf.put("||127.0.0.1".getBytes());
        buf.put((byte) 13);
        buf.put((byte) 10);
        buf.put("http://localhost/".getBytes());
        buf.put((byte) 13);
        buf.put((byte) 10);
        buf.put("_MO_-a ".getBytes());
        buf.put(word.getBytes("CP866"));
        buf.put((byte) 13);
        buf.put((byte) 10);
        buf.put((byte) 13);
        buf.put((byte) 10);

        out.write(buf.array(), 0, buf.position());
        //socket.
        buf.clear();
        int b;
        while ((b = in.read()) >= 0) {
            buf.put((byte) b);
        }

        //if (word.equals("я"))
        //  System.out.print("");

        String result = new String(buf.array(), 0, buf.position(), "IBM866");
        ArrayList<String[]> res = new ArrayList<String[]>();
        parseInput(result, 0, res);
        if (loggingFolder != null) {
            fulfillZindexesAndGramms(word, res);
        }

        //System.out.println("From starling: "+new String(buf.array(),0,buf.position(),"IBM866"));
        //System.out.println(j);
        socket.close();
        return res;
    }

    private void fulfillZindexesAndGramms(String word, ArrayList<String[]> arr) {
        for (String[] strings : arr) {
            if (strings[3] == null) {
                errors.add("No paradigm found for one of the entries (word " + word + ")");
            }

            String s = strings[1].replaceAll("\\.", " ").trim();
            Object[] oarr = zindexes.get(s);
            if (oarr == null) {
                oarr = new Object[]{1, word, strings[0], strings[1], strings[2], strings[3]};
                zindexes.put(s, oarr);
            } else {
                oarr[0] = ((Integer) oarr[0]) + 1;
            }
            StringTokenizer tok = new StringTokenizer(strings[2], ",");
            while (tok.hasMoreTokens()) {
                s = tok.nextToken().replaceAll("\\*", " ").replaceAll("\\.", " ").replaceAll("/", " ").trim();
                oarr = gramms.get(s);
                if (oarr == null) {
                    oarr = new Object[]{1, word, strings[0], strings[1], strings[2], strings[3]};
                    gramms.put(s, oarr);
                } else {
                    oarr[0] = ((Integer) oarr[0]) + 1;
                }
            }

            if (strings[3] != null) {
                tok = new StringTokenizer(strings[3], ";");
                while (tok.hasMoreTokens()) {
                    s = tok.nextToken().trim();

                    int j = 0;
                    while (j < s.length() && !sut.isLetterCyrillic(s.charAt(j)) && "(-_".indexOf(s.charAt(j)) == -1)
                        j++;

                    s = s.substring(0, j).replaceAll("\\*", " ").replaceAll("\\.", " ").replaceAll("/", " ").trim();

                    if (s.length() == 0)
                        continue;

                    StringTokenizer tok1 = new StringTokenizer(s, ",");
                    while (tok1.hasMoreTokens()) {
                        s = tok1.nextToken().trim();

                        oarr = gramms.get(s);
                        if (oarr == null) {
                            oarr = new Object[]{1, word, strings[0], strings[1], strings[2], strings[3]};
                            gramms.put(s, oarr);
                        } else {
                            oarr[0] = ((Integer) oarr[0]) + 1;
                        }
                    }
                }
            }
        }
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
            new File(new URL(loggingFolder, "./temp").getPath()).mkdirs();
            FileOutputStream fos = new FileOutputStream(new URL(loggingFolder, "./temp/zindexes.log").getPath());
            for (Map.Entry e : arr) {
                fos.write(e.getKey().toString().getBytes());
                Object[] oarr = (Object[]) e.getValue();
                fos.write(" <".getBytes());
                fos.write(oarr[0].toString().getBytes());
                fos.write(":".getBytes());
                fos.write(oarr[1].toString().getBytes());
                fos.write(":".getBytes());
                fos.write(oarr[2].toString().getBytes());
                fos.write(" [".getBytes());
                fos.write(oarr[3].toString().getBytes());
                fos.write("] ".getBytes());
                fos.write(oarr[4].toString().getBytes());
                fos.write("; ".getBytes());
                fos.write(oarr[5] == null ? "null".getBytes() : oarr[5].toString().getBytes());
                fos.write(">\n".getBytes());
            }
            fos.close();
            arr = gramms.entrySet().toArray(new Map.Entry[gramms.entrySet().size()]);
            Arrays.sort(arr, kc);

            fos = new FileOutputStream(new URL(loggingFolder, "./temp/gramms.log").getPath());
            for (Map.Entry e : arr) {
                fos.write(e.getKey().toString().getBytes());
                Object[] oarr = (Object[]) e.getValue();
                fos.write(" <".getBytes());
                fos.write(oarr[0].toString().getBytes());
                fos.write(":".getBytes());
                fos.write(oarr[1].toString().getBytes());
                fos.write(":".getBytes());
                fos.write(oarr[2].toString().getBytes());
                fos.write(" [".getBytes());
                fos.write(oarr[3].toString().getBytes());
                fos.write("] ".getBytes());
                fos.write(oarr[4].toString().getBytes());
                fos.write("; ".getBytes());
                fos.write(oarr[5] == null ? "null".getBytes() : oarr[5].toString().getBytes());
                fos.write(">\n".getBytes());
            }
            fos.close();

            String[] sarr = errors.toArray(new String[errors.size()]);
            Arrays.sort(sarr);

            fos = new FileOutputStream(new URL(loggingFolder, "./temp/errors.log").getPath());
            for (String s : sarr) {
                fos.write("StarlingMorphApplier: ".getBytes());
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

    private void checkEOS(String s, int pl) throws ParseException {
        if (pl >= s.length()) {
            throw new ParseException("Unexpected end of string", s, pl);
        }
    }

    private void parseInput(String s, int pl, ArrayList<String[]> res) throws ParseException {
        int resIndex = res.size();
        checkEOS(s, pl);
        if (s.charAt(pl) != '\n') {
            throw new ParseException("Wrong character '" + s.charAt(pl) + "'. Was expecting '\\n'", s, pl);
        }
        pl++;
        checkEOS(s, pl);
        if (s.charAt(pl) != '\u0011') {
            throw new ParseException("Wrong character '" + s.charAt(pl) + "'. Was expecting '\\u0011'", s, pl);
        }
        pl++;
        checkEOS(s, pl);
        if (s.charAt(pl) != '\n') {
            throw new ParseException("Wrong character '" + s.charAt(pl) + "'. Was expecting '\\n'", s, pl);
        }
        pl++;
        pl = parseVariant(s, pl);
        if (_t_base == null)
            return;
        res.add(new String[]{_t_base, _t_zindex, _t_gramm, null});
        char c = s.charAt(pl);
        while (c != '\r') {
            pl++;
            pl = parseVariant(s, pl);
            res.add(new String[]{_t_base, _t_zindex, _t_gramm, null});
            c = s.charAt(pl);
        }
        pl++;
        checkEOS(s, pl);
        if (s.charAt(pl) != '\n') {
            throw new ParseException("Wrong character '" + s.charAt(pl) + "'. Was expecting '\\n'", s, pl);
        }
        pl++;

        pl = parseParadigm(s, pl);
        int cnt = 0;
        while (_t_paradigm.length() > 0) {
            res.get(resIndex + cnt++)[3] = _t_paradigm;
            pl += 2;
            pl = parseParadigm(s, pl);
        }

    }

    private int parseVariant(String s, int pl) throws ParseException {
        int beg = pl;
        checkEOS(s, pl);
        while (pl < s.length() && s.charAt(pl) != '[') {
            pl++;
        }
        if (s.length() == pl) {
            _t_base = null;
            return pl;
        }
        _t_base = s.substring(beg, pl).replaceAll("'", "").replaceAll("\"", "").replaceAll("`", "").trim();
        pl++;
        tbuf.setLength(0);
        checkEOS(s, pl);
        char c = s.charAt(pl);
        while (c != ']') {
            if (c == '\\') {
                pl++;
                checkEOS(s, pl);
                tbuf.append(s.charAt(pl));
            } else {
                tbuf.append(c);
            }
            pl++;
            checkEOS(s, pl);
            c = s.charAt(pl);
        }
        _t_zindex = tbuf.toString().trim();
        int i = _t_zindex.indexOf('\u0004');
        if (i > 0) {
            _t_zindex = _t_zindex.substring(0, i).trim();
        }
        pl++;
        tbuf.setLength(0);
        checkEOS(s, pl);
        c = s.charAt(pl);
        while (c != ';' && c != '\r') {
            if (c == '\\') {
                pl++;
                checkEOS(s, pl);
                tbuf.append(s.charAt(pl));
            } else {
                tbuf.append(c);
            }
            pl++;
            checkEOS(s, pl);
            c = s.charAt(pl);
        }
        _t_gramm = tbuf.toString().trim();
        return pl;
    }

    private int parseParadigm(String s, int pl) throws ParseException {
        tbuf.setLength(0);
        checkEOS(s, pl);
        char c = s.charAt(pl);
        while (c != '\r' && c != '\u0000' && c != '\n') {
            if (c == '\\') {
                pl++;
                checkEOS(s, pl);
                tbuf.append(s.charAt(pl));
            } else {
                tbuf.append(c);
            }
            pl++;
            checkEOS(s, pl);
            c = s.charAt(pl);
        }
        _t_paradigm = tbuf.toString().trim();
        return pl;
    }

    public void setLoggingFolder(URL loggingFolder) {
        this.loggingFolder = loggingFolder;
    }

    public boolean isLogging() {
        return loggingFolder != null;
    }

    public StringToTrnMapperRuleStorage getLexMapper() {
        return lexMapper;
    }
}
