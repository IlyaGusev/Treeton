/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.morph._native;

import org.apache.log4j.Logger;
import treeton.core.BlackBoard;
import treeton.core.TreetonFactory;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.scape.trnmapper.StringToTrnMapperRule;
import treeton.core.scape.trnmapper.StringToTrnMapperRuleStorage;
import treeton.dict.Dictionary;
import treeton.morph.MorphException;
import treeton.morph.MorphInterface;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * User: Anatoli Starostin
 * Date: 07.03.2010
 * Time: 20:35:08
 */
public class NativeRusMorphEngine implements MorphInterface {
    private static final Logger logger = Logger.getLogger(NativeRusMorphEngine.class);
    private static final String rusmorph_filename = "rusmorph.dll";

    //Arrays.asList("терактах","терактов","нью-йорк","нью-йорке","теракты","нью-йорком","нью-йорка","теракт","теракте","терактом","теракта","терактам")
//    private static Set<String> stopSet = new HashSet<String>(Arrays.asList("достигается","гидратироваться","делятся","реутилизироваться","делится","делиться",
//            "возрадывайтесь","самонагреваются","делается","достигаются","непредставляется","перекрылись","делалось","отслеживались","пообщаться","достигаться",
//            "сосредотачиваться","терактов","растухнется","приудобился","расгащивается","преобразовалось","нью-йорк","терактах","делался","давалась","нью-йорке",
//            "вычучилась","кругтелся","зацикливаться","делились","сфокусироваться","давались","сфокусировавшись","достигались","подзаряжаемся","ингибироваться",
//            "деляется","достигались","личается","такполучается","запустится","предохраниться","испольняется","применияется","возврашается","преобразовалась",
//            "подсоединиться","сравненивается","рассыплется","поупражнявшись","телескопировалась","похмеляться"));
    private static Set<String> stopSet = new HashSet<String>();
    private static boolean libLoaded = false;
    private static boolean engineInited = false;
    private static String undef = "undef";
    private static String[] tpsp = new String[]{undef, "N", "A", "NUM", "V", "ADV", "PREP", "CONJ", "PCL", "INTJ", "V", "COPULA", "unknown"};
    private static String[] tnmb = new String[]{undef, "sg", "pl"};
    private static String[] tcas = new String[]{undef, "nom", "gen", "gen2", "dat", "acc", "inst", "prp", "prp2"};
    private static String[] tanim = new String[]{undef, "anim", "inan"};
    private static String[] tgen = new String[]{undef, "m", "f", "n", "mf"};
    private static String norm = "norm", shrt = "shrt", cmp = "cmp";
    private static String[] ta_repr = new String[]{undef, norm, shrt, cmp};
    private static String[] tv_repr = new String[]{undef, "inf", "fin", "part", "gern"};
    private static String[] tv_asp = new String[]{undef, "ipf", "pf", "pf_ipf"};

    //    typedef enum {tpsp_undef,noun,adj,num,verb,adv,prep,conj,part,intj,copula,unknown_psp} tpsp; // Часть речи
    private static String[] tv_vox = new String[]{undef, "pass", "act"};

    //    typedef enum {tnmb_undef,sg,pl} tnmb; // Число
    private static String[] tv_md = new String[]{undef, "ind", "imp", "sbjn"};

    //    typedef enum {tcas_undef,nom,gen,gen2,dat,acc,instnt,prp,prp2} tcas; // Падеж
    private static String[] tv_tns = new String[]{undef, "pres", "past", "fut"};

    //    typedef enum {tanim_undef,anim,inanim} tanim; // Одушевленность
    private static String[] tv_prs = new String[]{undef, "1", "2", "3"};

    //    typedef enum {tgen_undef,masc,fem,neut,masc_fem} tgen; // Род
    private BlackBoard localBoard;

    //    typedef enum {ta_repr_undef,norm,shrt,cmp} ta_repr; // репрезентация прилагательного
    private StringToTrnMapperRuleStorage lexMapper;
    private Map<String, Properties> lexInfo;

    //    typedef enum {tv_repr_undef,inf,fin,particip,gern} tv_repr; // репрезентация глагола

    public static void main(String[] args) throws UnsupportedEncodingException, TreetonModelException {
        System.load(NativeRusMorphEngine.class.getResource("/resources/rusmorph.dll").getPath());
        NativeRusMorphEngine engine = new NativeRusMorphEngine();

        String dictPath = "C:\\StarSoft\\dict\\dict_ru.dct";
        if (!engine.loadDictionary(dictPath))
            throw new RuntimeException("Unable to load dictionary " + dictPath);

        if (!engineInited && !engine.initEngine())
            throw new RuntimeException("Unable to initialize morphological engine");

        engineInited = true;

        String word = "столм";
        byte[] bytes = engine.processWord(word.getBytes("IBM866"));

        Collection<Properties> unknowns = new ArrayList<Properties>();
        Collection<Properties> res = engine.parseBytes(bytes, unknowns, word);
        System.out.println("Result:");
        System.out.println(res);
        System.out.println("Unknown hypothesis:");
        System.out.println(unknowns);
    }

    //    typedef enum {tv_asp_undef,ipf,pf,pf_ipf} tv_asp; // вид глагола

    private native byte[] processWord(byte[] word);

    //    typedef enum {tv_vox_undef,pass,act} tv_vox; // залог

    private native boolean loadDictionary(String path);

    //    typedef enum {tv_md_undef,ind,imper,sbjn} tv_md; // наклонение

    private native boolean initEngine();

    //    typedef enum {tv_tns_undef,pres,past,fut} tv_tns; // время

    private native boolean deinitEngine();

    //    typedef enum {tv_prs_undef,first,second,third} tv_prs; // лицо глагола (только в личной форме наст. или буд. времени)

    Properties getLexInfo(String gramminfo) throws TreetonModelException {
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
            lexInfo.put(gramminfo, props);
        }
        return props;
    }

//    gboolean unknown; // "Гипотеза, не подтвержденная словарем"
//    gboolean pron; // "Местоименность"
//    gboolean indecl; // Для несклоняемых существительных
//    gboolean plTant; // Pluralia Tantum
//    gboolean ord; // Флаг порядкового прилагательного
//    gboolean v_reflp; // "пассивная возвратность"
//    gboolean tr; // переходный глагол
//    gboolean impers; //     безличный глагол
//    gboolean frec; // многократный глагол

    //source,0,zindex,0,unknown,psp,nmb,cas,gen,anim,pron,indecl,plTant,a_repr,ord,v_repr,v_asp,v_vox,v_md,v_tns,v_prs,v_reflp,tr,impers,frec,
    //source,0,zindex,0,unknown,psp,nmb,cas,gen,anim,pron,indecl,plTant,a_repr,ord,v_repr,v_asp,v_vox,v_md,v_tns,v_prs,v_reflp,tr,impers,frec,
    //...
    //source,0,zindex,0,unknown,psp,nmb,cas,gen,anim,pron,indecl,plTant,a_repr,ord,v_repr,v_asp,v_vox,v_md,v_tns,v_prs,v_reflp,tr,impers,frec,

    public Collection<Properties> processOneWord(String word, Dictionary dictArray) throws MorphException {
        if (stopSet.contains(word)) {
            logger.trace("Skipping word " + word + " according to stopset");
            Properties p = new Properties();
            p.setProperty("string", "stopList!!!");
            return Arrays.asList(p);
        }

        logger.trace("Processing word " + word);

        try {
            byte[] bytes = processWord(word.getBytes("IBM866"));
            return parseBytes(bytes, null, word);
        } catch (Exception e) {
            throw new MorphException(e);
        }
    }

    public void reset() {
    }

    @SuppressWarnings({"StringEquality"})
    private Collection<Properties> parseBytes(byte[] bytes, Collection<Properties> unknowns, String word) throws UnsupportedEncodingException, TreetonModelException {
        Collection<Properties> res = new ArrayList<Properties>();

        int pos = 0;

        while (pos < bytes.length) {
            Properties props = new Properties();
            int lastpos = pos;
            while (pos < bytes.length && bytes[pos] != 0) {
                pos++;
            }
            String s = new String(bytes, lastpos, pos - lastpos, "IBM866");
            if ("?".equals(s))
                return res;
            props.put("base", s);
            props.put("WORDFORM", word);
            props.put("ACCPL", "-1");
            pos++;
            lastpos = pos;
            while (pos < bytes.length && bytes[pos] != 0) {
                pos++;
            }
            String zindex = new String(bytes, lastpos, pos - lastpos, "IBM866");
            props.put("ZINDEX", zindex);
            pos++;

            boolean unknown = bytes[pos++] != 0;

            String value = tpsp[bytes[pos++]];
            if (value != undef)
                props.put("POS", value);

            value = tnmb[bytes[pos++]];
            if (value != undef)
                props.put("NMB", value);

            value = tcas[bytes[pos++]];
            if (value != undef)
                props.put("CAS", value);

            value = tgen[bytes[pos++]];
            if (value != undef)
                props.put("GEND", value);

            value = tanim[bytes[pos++]];
            if (value != undef)
                props.put("ANIM", value);

            boolean pron = bytes[pos++] != 0;

            if (pron) {
                props.put("PRN", "prn");
            }

            boolean indecl = bytes[pos++] != 0;

            if (indecl) {
                props.put("INVAR", "invar");
            }

            boolean plTant = bytes[pos++] != 0;

            if (plTant) {
                props.put("GEND", "plt");
            }

            value = ta_repr[bytes[pos++]];
            if (value != undef) {
                if (value == shrt) {
                    props.put("ATTR", "sh");
                } else if (value == cmp) {
                    props.put("DGR", "comp");
                }
            }

            boolean ord = bytes[pos++] != 0;

            if (ord) {
                props.put("ORDIN", "ordin");
            }

            value = tv_repr[bytes[pos++]];
            if (value != undef)
                props.put("REPR", value);

            value = tv_asp[bytes[pos++]];
            if (value != undef)
                props.put("ASP", value);

            value = tv_vox[bytes[pos++]];
            if (value != undef)
                props.put("VOX", value);

            value = tv_md[bytes[pos++]];
            if (value != undef)
                props.put("MD", value);

            value = tv_tns[bytes[pos++]];
            if (value != undef)
                props.put("TNS", value);

            value = tv_prs[bytes[pos++]];
            if (value != undef)
                props.put("PRS", value);

            boolean v_reflp = bytes[pos++] != 0;

            if (v_reflp) {
                //todo
                //props.put("ORDIN","ordin");
            }

            boolean tr = bytes[pos++] != 0;

            if (tr) {
                props.put("TRANS", "vt");
            }

            boolean impers = bytes[pos++] != 0;

            if (impers) {
                props.put("IMPERS", "impers");
            }

            boolean frec = bytes[pos++] != 0;

            if (frec) {
                props.put("FREQ", "freq");
            }

            if (zindex != null) {
                Properties lexInfo = getLexInfo(zindex);
                props.putAll(lexInfo);
            }

            if (unknown) {
                if (unknowns != null)
                    unknowns.add(props);
            } else {
                res.add(props);
            }
        }

        return res;
    }

    public void init(String dictPath, String lexMappingPath, TreenotationsContext trnContext, TrnType targetType) throws TreetonModelException, IOException, ParseException {
        if (!libLoaded) {
            URL rusmorph = NativeRusMorphEngine.class.getResource("/resources/" + rusmorph_filename);
            if ("file".equals(rusmorph.getProtocol()))
                System.load(rusmorph.getPath()); // oprimization: if file is in local filesystem and not packed - just load it
            else
                System.load(copyResource(rusmorph)); // otherwise we need to copy or unpack it to some local directory first
            libLoaded = true;
        }

        lexMapper = new StringToTrnMapperRuleStorage(trnContext.getTypes(), targetType);
        lexMapper.readInFromFile(lexMappingPath);
        localBoard = TreetonFactory.newBlackBoard(100, false);
        lexInfo = new HashMap<String, Properties>();

        if (!loadDictionary(dictPath))
            throw new IOException("Unable to load dictionary " + dictPath);
        if (!engineInited && !initEngine())
            throw new IOException("Unable to initialize morphological engine");

        engineInited = true;
    }

    private String copyResource(URL url) throws IOException {
        String dir = System.getProperty("java.io.tmpdir");
        String name = rusmorph_filename;

        File res = new File(dir, name);
        // usually we don't need to create a new copy of resource each time - just rewrite old on (since it's stored in tempdir)
        if (res.exists() && !res.delete())
            res = new File(dir, new Date().getTime() + name); // create a new copy if old copy is locked by other instance of application

        logger.warn("Copying resource from " + url.toString() + " to " + res.toString());
        InputStream in = url.openStream();
        FileOutputStream out = new FileOutputStream(res);

        try {
            byte[] buffer = new byte[1024];
            int size;
            while ((size = in.read(buffer)) != -1)
                out.write(buffer, 0, size);
        } finally {
            out.close();
            in.close();
        }

        return res.getCanonicalPath();
    }

    public void deInit() {
        lexMapper = null;
        localBoard = null;
        lexInfo = null;

        if (!deinitEngine())
            throw new RuntimeException("Unable to deinitialize morphological engine");
        engineInited = false;
    }
}
