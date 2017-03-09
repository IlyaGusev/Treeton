/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Node;
import gate.util.OffsetComparator;
import treeton.core.*;
import treeton.core.TString;
import treeton.core.model.*;
import treeton.core.scape.ParseException;
import treeton.core.scape.ScapeVariable;
import treeton.core.util.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;


public class ScapePhase {
    //todo учитывать output

    public static final int CONTROL_TYPE_BRILL = 0;
    public static final int CONTROL_TYPE_BRAPPELT = 1;
    public static final int CONTROL_TYPE_APPELT = 2;
    public static final int CONTROL_TYPE_FIRST = 3;
    static final int orthophaseKeywordNumber = 0;
    static final int phaseKeywordNumber = 1;
    static final int inputKeywordNumber = 2;
    static final int outputKeywordNumber = 3;
    static final int controlKeywordNumber = 4;
    static final int brillKeywordNumber = 5;
    static final int brappeltKeywordNumber = 6;
    static final int appeltKeywordNumber = 7;
    static final int firstKeywordNumber = 8;
    static final int ruleKeywordNumber = 9;
    static final int packageKeywordNumber = 10;
    static char[][] keywords = new char[][]{
            {'o', 'r', 't', 'h', 'o', 'p', 'h', 'a', 's', 'e'},
            {'p', 'h', 'a', 's', 'e'},
            {'i', 'n', 'p', 'u', 't'},
            {'o', 'u', 't', 'p', 'u', 't'},
            {'c', 'o', 'n', 't', 'r', 'o', 'l'},
            {'b', 'r', 'i', 'l', 'l'},
            {'b', 'r', 'a', 'p', 'p', 'e', 'l', 't'},
            {'a', 'p', 'p', 'e', 'l', 't'},
            {'f', 'i', 'r', 's', 't'},
            {'r', 'u', 'l', 'e'},
            {'p', 'a', 'c', 'k', 'a', 'g', 'e'}
    };
    public BlockStack scapeResultStack;
    TrnTypeStorage types;
    TString name;
    TrnTypeSet input;
    ScapeOutputItem[] output;
    int control;
    HashMap<TrnType, HashSet<Integer>> shadowedFeatures = new HashMap<TrnType, HashSet<Integer>>();
    HashMap<TrnType, HashSet<Integer>> caseInsensitiveFeatures = new HashMap<TrnType, HashSet<Integer>>();
    ScapeRuleSet allRules;
    ScapeFSMBinding[] allBindings;
    int nAllBindings = 0;
    ScapeAllBinding allBinding = new ScapeAllBinding();
    ArrayList<Treenotation> allTreenotations = new ArrayList<Treenotation>();
    ScapeTreenotationClassTreeWithGateSupport allClassesTree = null;
    TrnType[] startPointTokenTypes;
    TrnType[] startPointCommonTypes;
    ScapeNFSM nfsm;
    ScapeDFSM dfsm;
    boolean isOrtho = false;
    String scapePackage;
    List<ScapeRHSJavaAction> javaActions = new ArrayList<ScapeRHSJavaAction>();
    File javaContentLocation;
    TreenotationStorage storage;
    TypeIteratorInterface startPointsIterator;
    FollowIteratorInterface followIterator;
    Token from, to;
    HashMap<Treenotation, Integer> trn2classId = new HashMap<Treenotation, Integer>();
    AnnotationSet annotationSet;
    Annotation[] annotations;
    int cursor;
    Node startNode;
    Node endNode;
    Document doc;
    long curStartOffset;
    HashMap<Annotation, Integer> anns2classId = new HashMap<Annotation, Integer>();
    TrnTypeSetFactory tsFactory = new TrnTypeSetFactory();
    IntFeatureMapStaticNOIterator fmIt = new IntFeatureMapStaticNOIterator(null);
    TypesIterator typesIterator = new TypesIterator(null);
    private RBTreeMap bindingSetsIndex = new RBTreeMap();
    private RBTreeMap ruleSetsIndex = new RBTreeMap();
    private ScapeBinding[] bindingsArr = new ScapeBinding[100];
    private ScapeBindingSet bindingSetForSearch = new ScapeBindingSet();
    private ScapeRule[] rulesArr = new ScapeRule[100];
    private ScapeRuleSet ruleSetForSearch = new ScapeRuleSet();
    private ScapeOutputItem[] outputArr = new ScapeOutputItem[100];
    private TrnType[] inputArr = new TrnType[100];
    private BlockStack stack = new BlockStack();
    private Token curStart;

    public ScapePhase(TrnTypeStorage types) {
        this.types = types;
    }

    static int readInName(char[] s, int pl, int endpl, StringBuffer buf, MutableInteger isOrtho) throws ParseException {
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        if (n == phaseKeywordNumber) {
            if (isOrtho != null)
                isOrtho.value = 0;
            pl += keywords[phaseKeywordNumber].length;
        } else if (n == orthophaseKeywordNumber) {
            if (isOrtho != null)
                isOrtho.value = 1;
            pl += keywords[orthophaseKeywordNumber].length;
        } else {
            throw new ParseException("missing phase keyword", null, s, pl, endpl);
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing phase name", null, s, pl, endpl);
        }
        buf.append(new String(s, beg, pl - beg));
        return pl;
    }

    static int readInPackage(int pl, char[] s, int endpl, StringBuffer buf) throws ParseException {
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        if (n != packageKeywordNumber) {
            throw new ParseException("missing package keyword", null, s, pl, endpl);
        }
        pl += keywords[packageKeywordNumber].length;
        pl = sut.skipSpacesEndls(s, pl, endpl);

        while (true) {
            int beg = pl;
            pl = sut.skipVarName(s, pl, endpl);
            if (pl == beg) {
                throw new ParseException("missing package name", null, s, pl, endpl);
            }
            buf.append(new String(s, beg, pl - beg));
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] == '.') {
                pl++;
                buf.append('.');
            } else if (s[pl] == ';') {
                pl++;
                break;
            } else {
                throw new ParseException("unexpected character '" + s[pl] + "'", null, s, pl, endpl);
            }
        }
        return pl;
    }

    public ScapeNFSM getNfsm() {
        return nfsm;
    }

    public ScapeDFSM getDfsm() {
        return dfsm;
    }

    public void shadowFeature(TrnType tp, int f) {
        if (f < 0)
            return;
        HashSet<Integer> h = shadowedFeatures.get(tp);
        if (h == null) {
            h = new HashSet<Integer>();
            shadowedFeatures.put(tp, h);
        }
        h.add(f);
    }

    public void shadowFeature(TrnType tp, String fname) throws TreetonModelException {
        shadowFeature(tp, tp.getFeatureIndex(fname));
    }

    /*Transducer part (Treenotations) */

    public void makeFeatureCaseInsensitive(TrnType tp, int f) {
        if (f < 0)
            return;
        HashSet<Integer> h = caseInsensitiveFeatures.get(tp);
        if (h == null) {
            h = new HashSet<Integer>();
            caseInsensitiveFeatures.put(tp, h);
        }
        h.add(f);
    }

    public void makeFeatureCaseInsensitive(TrnType tp, String fname) throws TreetonModelException {
        makeFeatureCaseInsensitive(tp, tp.getFeatureIndex(fname));
    }

    public void registerTrn(Treenotation trn) {
        allTreenotations.add(trn);
    }

    public TrnTypeSet getInput() {
        return input;
    }

    ScapeBindingSet newBindingSet(BlockStack bindingsStack) {
        int n = (int) bindingsStack.getPosition() + 1;
        if (n > bindingsArr.length) {
            ScapeBinding[] tarr = new ScapeBinding[(int) (Math.max(bindingsArr.length * 1.5, n))];
            System.arraycopy(bindingsArr, 0, tarr, 0, bindingsArr.length);
            bindingsArr = tarr;
        }

        bindingsStack.fillArray(bindingsArr);

        long label = ScapeUniLabel.get();

        int j = 0;
        for (int i = 0; i < n; i++) {
            ScapeBinding b = bindingsArr[i];
            if (b.label == label)
                continue;
            bindingsArr[j++] = b;
            b.label = label;
        }
        n = j;
        Arrays.sort(bindingsArr, 0, n);
        bindingSetForSearch.bindings = bindingsArr;
        bindingSetForSearch.size = n;
        ScapeBindingSet bs = (ScapeBindingSet) bindingSetsIndex.get(bindingSetForSearch);
        if (bs == null) {
            bs = new ScapeBindingSet();
            bs.bindings = new ScapeBinding[n];
            System.arraycopy(bindingsArr, 0, bs.bindings, 0, n);
            bs.size = n;
            bindingSetsIndex.put(bs, bs);
        }
        return bs;
    }

    ScapeBindingSet newBindingSet(ScapeBinding[] arr, int size) { //arr must be sorted
        bindingSetForSearch.bindings = arr;
        bindingSetForSearch.size = size;
        ScapeBindingSet bs = (ScapeBindingSet) bindingSetsIndex.get(bindingSetForSearch);
        if (bs == null) {
            bs = new ScapeBindingSet();
            bs.bindings = new ScapeBinding[size];
            System.arraycopy(arr, 0, bs.bindings, 0, size);
            bs.size = size;
            bindingSetsIndex.put(bs, bs);
        }
        return bs;
    }

    ScapeRuleSet newRuleSet(BlockStack rulesStack) {
        int n = (int) rulesStack.getPosition() + 1;
        if (n > rulesArr.length) {
            ScapeRule[] tarr = new ScapeRule[(int) (Math.max(rulesArr.length * 1.5, n))];
            System.arraycopy(rulesArr, 0, tarr, 0, rulesArr.length);
            rulesArr = tarr;
        }

        rulesStack.fillArray(rulesArr);

        long label = ScapeUniLabel.get();

        int j = 0;
        for (int i = 0; i < n; i++) {
            ScapeRule r = rulesArr[i];
            if (r.label == label)
                continue;
            rulesArr[j++] = r;
            r.label = label;
        }
        n = j;
        Arrays.sort(rulesArr, 0, n);
        ruleSetForSearch.rules = rulesArr;
        ruleSetForSearch.size = n;
        ScapeRuleSet rs = (ScapeRuleSet) ruleSetsIndex.get(ruleSetForSearch);
        if (rs == null) {
            rs = new ScapeRuleSet();
            rs.rules = new ScapeRule[n];
            System.arraycopy(rulesArr, 0, rs.rules, 0, n);
            rs.size = n;
            ruleSetsIndex.put(rs, rs);
        }
        return rs;
    }

    ScapeRuleSet newRuleSet(ScapeRule[] arr, int size) { //arr must be sorted
        ruleSetForSearch.rules = arr;
        ruleSetForSearch.size = size;
        ScapeRuleSet rs = (ScapeRuleSet) ruleSetsIndex.get(ruleSetForSearch);
        if (rs == null) {
            rs = new ScapeRuleSet();
            rs.rules = new ScapeRule[size];
            System.arraycopy(arr, 0, rs.rules, 0, size);
            rs.size = size;
            ruleSetsIndex.put(rs, rs);
        }
        return rs;
    }

    public boolean isOrtho() {
        return isOrtho;
    }

    public int readIn(char s[], int pl, int endpl) throws ParseException, IOException {
        StringBuffer buf = new StringBuffer();
        pl = readInPackage(pl, s, endpl, buf);
        scapePackage = buf.toString();
        buf.setLength(0);
        MutableInteger mi = new MutableInteger();
        pl = readInName(s, pl, endpl, buf, mi);
        isOrtho = mi.value != 0;
        name = TreetonFactory.newTString(buf.toString());
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }
        pl++;
        int ruleN = 0;
        int inputItemN = 0;
        int outputItemN = 0;
        control = -1;

        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);

        HashSet<String> ruleNames = new HashSet<String>();

        while (n != -1) {
            if (n == inputKeywordNumber) {
                if (inputItemN > 0) {
                    throw new ParseException("input already specified", null, s, pl, endpl);
                }
                pl += keywords[inputKeywordNumber].length - 1;

                do {
                    pl++;
                    pl = sut.skipSpacesEndls(s, pl, endpl);
                    int beg;

                    beg = pl;
                    pl = sut.skipVarName(s, pl, endpl);
                    if (pl == beg) {
                        throw new ParseException("missing type name", null, s, pl, endpl);
                    }
                    String t = new String(s, beg, pl - beg);
                    if (inputItemN >= inputArr.length) {
                        TrnType[] tarr = new TrnType[(int) (inputArr.length * 1.5)];
                        System.arraycopy(inputArr, 0, tarr, 0, inputArr.length);
                        inputArr = tarr;
                    }
                    try {
                        if ((inputArr[inputItemN++] = types.get(t)) == null) {
                            throw new ParseException("unregistered type " + t, null, s, pl, endpl);
                        }
                    } catch (TreetonModelException e) {
                        throw new ParseException("unregistered type " + t, null, s, pl, endpl);
                    }
                    pl = sut.skipSpacesEndls(s, pl, endpl);
                    sut.checkEndOfStream(s, pl, endpl);
                } while (s[pl] == ',');
                if (s[pl] != ';') {
                    throw new ParseException("missing ';'", null, s, pl, endpl);
                }
                pl++;
            } else if (n == outputKeywordNumber) {
                if (outputItemN > 0) {
                    throw new ParseException("output already specified", null, s, pl, endpl);
                }

                pl += keywords[outputKeywordNumber].length - 1;

                do {
                    pl++;
                    pl = sut.skipSpacesEndls(s, pl, endpl);
                    int beg = pl;
                    pl = sut.skipVarName(s, pl, endpl);
                    if (pl == beg) {
                        throw new ParseException("missing type name", null, s, pl, endpl);
                    }
                    String t = new String(s, beg, pl - beg);
                    TrnType tp;
                    try {
                        if ((tp = types.get(t)) == null) {
                            throw new ParseException("unregistered type " + t, null, s, pl, endpl);
                        }
                    } catch (TreetonModelException e) {
                        throw new ParseException("unregistered type " + t, null, s, pl, endpl);
                    }
                    if (outputItemN >= outputArr.length) {
                        ScapeOutputItem[] tarr = new ScapeOutputItem[(int) (outputArr.length * 1.5)];
                        System.arraycopy(outputArr, 0, tarr, 0, outputArr.length);
                        outputArr = tarr;
                    }
                    pl = sut.skipSpacesEndls(s, pl, endpl);
                    sut.checkEndOfStream(s, pl, endpl);
                    if (s[pl] == ':') {
                        pl++;
                        pl = sut.skipSpacesEndls(s, pl, endpl);
                        beg = pl;
                        boolean c = false, r = false, m = false;
                        while (pl <= endpl) {
                            if (s[pl] == 'c') {
                                c = true;
                            } else if (s[pl] == 'r') {
                                r = true;
                            } else if (s[pl] == 'm') {
                                m = true;
                            } else {
                                break;
                            }
                            pl++;
                        }
                        if (pl == beg) {
                            throw new ParseException("missing type constraints", null, s, pl, endpl);
                        }
                        outputArr[outputItemN++] = new ScapeOutputItem(tp, c, r, m);
                    } else {
                        outputArr[outputItemN++] = new ScapeOutputItem(tp);
                    }
                    pl = sut.skipSpacesEndls(s, pl, endpl);
                    sut.checkEndOfStream(s, pl, endpl);
                } while (s[pl] == ',');
                if (s[pl] != ';') {
                    throw new ParseException("missing ';'", null, s, pl, endpl);
                }
                pl++;
            } else if (n == controlKeywordNumber) {
                if (isOrtho) {
                    throw new ParseException("control is not allowed here", null, s, pl, endpl);
                }
                if (control != -1) {
                    throw new ParseException("control type already specified", null, s, pl, endpl);
                }
                pl += keywords[controlKeywordNumber].length;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                n = sut.checkDelims(s, pl, endpl, keywords);
                if (n == brappeltKeywordNumber) {
                    control = CONTROL_TYPE_BRAPPELT;
                } else if (n == brillKeywordNumber) {
                    control = CONTROL_TYPE_BRILL;
                } else if (n == appeltKeywordNumber) {
                    control = CONTROL_TYPE_APPELT;
                } else if (n == firstKeywordNumber) {
                    control = CONTROL_TYPE_FIRST;
                } else {
                    throw new ParseException("wrong control type", null, s, pl, endpl);
                }
                pl += keywords[n].length;
                sut.skipSpacesEndls(s, pl, endpl);
                sut.checkEndOfStream(s, pl, endpl);
                if (s[pl] != ';') {
                    throw new ParseException("missing ';'", null, s, pl, endpl);
                }
                pl++;
            } else if (n == ruleKeywordNumber) {
                if (ruleN >= rulesArr.length) {
                    ScapeRule[] tarr = new ScapeRule[(int) (rulesArr.length * 1.5)];
                    System.arraycopy(rulesArr, 0, tarr, 0, rulesArr.length);
                    rulesArr = tarr;
                }
                ScapeRule r = new ScapeRule();
                r.index = ruleN;
                pl = r.readIn(s, pl, endpl, this);
                if (ruleNames.contains(r.name)) {
                    throw new ParseException("rule \"" + r.name + "\" already defined in the scope", null, s, pl, endpl);
                } else {
                    ruleNames.add(r.name);
                }
                nAllBindings += r.bindings.size() - 1;
                rulesArr[ruleN++] = r;
            }
            pl = sut.skipSpacesEndls(s, pl, endpl);
            n = sut.checkDelims(s, pl, endpl, keywords);
        }

        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '}') {
            throw new ParseException("missing '}'", null, s, pl, endpl);
        }
        if (!isOrtho && ruleN == 0) {
            throw new ParseException("no rules located", null, s, pl, endpl);
        }
        if (inputItemN == 0) {
            throw new ParseException("input not specified", null, s, pl, endpl);
        }
        if (!isOrtho && outputItemN == 0) {
            throw new ParseException("output not specified", null, s, pl, endpl);
        }
        pl++;

        if (control == -1) {
            control = CONTROL_TYPE_BRILL;
        }

        allRules = newRuleSet(rulesArr, ruleN);
        allBindings = new ScapeFSMBinding[nAllBindings];
        int j = 0;
        for (int i = 0; i < ruleN; i++) {
            for (ScapeVariable b : allRules.rules[i].bindings.values()) {
                if (b != allBinding) {
                    allBindings[j++] = (ScapeFSMBinding) b;
                }
            }
        }
        output = new ScapeOutputItem[outputItemN];
        System.arraycopy(outputArr, 0, output, 0, outputItemN);
        input = tsFactory.newTrnTypeSet(inputArr, inputItemN);
        return pl;
    }

    public void build(Iterator<ScapeRule> rules, String name, int controlType, TrnType[] input, ScapeOutputItem[] output) {
        this.input = tsFactory.newTrnTypeSet(input, input.length);
        this.output = output;
        this.control = controlType;

        int ruleN = 0;

        this.name = TreetonFactory.newTString(name);

        while (rules.hasNext()) {
            ScapeRule r = rules.next();
            r.index = ruleN;
            r.phase = this;
            if (ruleN >= rulesArr.length) {
                ScapeRule[] tarr = new ScapeRule[(int) (rulesArr.length * 1.5)];
                System.arraycopy(rulesArr, 0, tarr, 0, rulesArr.length);
                rulesArr = tarr;
            }
            rulesArr[ruleN++] = r;
        }

        allRules = newRuleSet(rulesArr, ruleN);
        allBindings = new ScapeFSMBinding[0];
        nAllBindings = 0;
        ScapeBindingSet emptySet = newBindingSet(new ScapeBinding[0], 0);
        for (int i = 0; i < ruleN; i++) {
            for (ScapeFSMState state1 : allRules.rules[i].fsm.states) {
                Iterator sit = state1.pairsIterator();
                while (sit.hasNext()) {
                    ScapeBindedPair p = (ScapeBindedPair) sit.next();
                    p.bindingSet = emptySet;

                    Treenotation trn = ((ScapeTreenotationTerm) p.getTerm()).trn;
                    if (trn != null) {
                        registerTrn(trn);
                    }
                }
            }
        }
    }

    public Token getCursor() {
        return curStart;
    }

    public void addJavaAction(ScapeRHSJavaAction ja) {
        javaActions.add(ja);
    }

    public File getJavaContentLocation() {
        return javaContentLocation;
    }

    public void setJavaContentLocation(File javaDir) {
        javaContentLocation = javaDir;
    }

    public void initialize() throws IOException {
        allClassesTree = new ScapeTreenotationClassTreeWithGateSupport(types);

        allClassesTree.importCaseInsensetiveInfo(caseInsensitiveFeatures);
        allClassesTree.importShadowedFeaturesInfo(shadowedFeatures);


        allClassesTree.build(allTreenotations.iterator(), allTreenotations.iterator());

        nfsm = new ScapeNFSM(this);
        nfsm.slurp(new ScapeFSMIterator());
        dfsm = new ScapeDFSM(this);
        dfsm.slurp(nfsm);


        Iterator it = dfsm.statesIterator();
        while (it.hasNext()) {
            ScapeDFSMState s = (ScapeDFSMState) it.next();
            Iterator pit = s.pairsIterator();
            while (pit.hasNext()) {
                ScapeBindedPair p = (ScapeBindedPair) pit.next();
                for (int i = 0; i < p.bindingSet.size; i++) {
                    ScapeFSMBinding bnd = (ScapeFSMBinding) p.bindingSet.bindings[i];
                    if (bnd.pairs == null) {
                        bnd.pairs = new ScapeBindedPair[bnd.nPairs];
                        bnd.nPairs = 0;
                    }
                    bnd.pairs[bnd.nPairs++] = p;
                }
            }
        }
        long label1 = ScapeUniLabel.get();
        for (int i = 0; i < allBindings.length; i++) {
            ScapeFSMBinding sample = allBindings[i];
            if (sample.label == label1)
                continue;
            long label2 = ScapeUniLabel.get();
            for (int j = 0; j < sample.nPairs; j++) {
                sample.pairs[j].label = label2;
            }
            sample.newBinding = new ScapeDFSMBinding(this);
            sample.newBinding.nOldBindings = 1;
            sample.label = label1;
            if (sample.regexBinding != null) {
                sample.newBinding.regexBinding = sample.regexBinding;
                sample.newBinding.regexFSM = sample.regexFSM;
                sample.newBinding.regexFeature = sample.regexFeature;
            } else {
                for (int j = i + 1; j < allBindings.length; j++) {
                    ScapeFSMBinding bnd = allBindings[j];
                    if (bnd.label == label1 || bnd.nPairs != sample.nPairs || bnd.regexBinding != null)
                        continue;
                    int k = 0;
                    for (; k < sample.nPairs; k++) {
                        if (bnd.pairs[k].label != label2)
                            break;
                    }
                    if (k == sample.nPairs) {
                        bnd.newBinding = sample.newBinding;
                        bnd.newBinding.nOldBindings++;
                        bnd.label = label1;
                    }
                }
            }
        }
        for (ScapeFSMBinding bnd : allBindings) {
            if (bnd.newBinding.oldBindings == null) {
                bnd.newBinding.oldBindings = new ScapeFSMBinding[bnd.newBinding.nOldBindings];
                bnd.newBinding.nOldBindings = 0;
            }
            bnd.newBinding.oldBindings[bnd.newBinding.nOldBindings++] = bnd;
        }

        it = dfsm.statesIterator();
        while (it.hasNext()) {
            ScapeDFSMState s = (ScapeDFSMState) it.next();
            Iterator pit = s.pairsIterator();
            while (pit.hasNext()) {
                ScapeBindedPair p = (ScapeBindedPair) pit.next();
                int n = 0;
                long label = ScapeUniLabel.get();
                for (int i = 0; i < p.bindingSet.size; i++) {
                    ScapeFSMBinding bnd = (ScapeFSMBinding) p.bindingSet.bindings[i];
                    if (bnd.newBinding.label != label) {
                        if (n >= bindingsArr.length) {
                            ScapeBinding[] tarr = new ScapeBinding[(int) (bindingsArr.length * 1.5)];
                            System.arraycopy(bindingsArr, 0, tarr, 0, bindingsArr.length);
                            bindingsArr = tarr;
                        }
                        bindingsArr[n++] = bnd.newBinding;
                        bnd.newBinding.label = label;
                    }
                }
                Arrays.sort(bindingsArr, 0, n);
                p.bindingSet = newBindingSet(bindingsArr, n);
            }
        }

        TrnTypeSet s = ((ScapeDFSMState) dfsm.getStartState()).trnTypes;
        startPointCommonTypes = s == null ? null : s.getCommonTypes();
        startPointTokenTypes = s == null ? null : s.getTokenTypes();

        processJavaActions();
        //showAll();
    }

    public ScapeTreenotationClassTreeWithGateSupport getAllClassesTree() {
        return allClassesTree;
    }

    private void processJavaActions() throws IOException {
        if (javaActions.size() == 0) {
            return;
        }
        int res = Javac.compile(javaContentLocation, javaContentLocation, "UTF-8");
        if (res != 0) {
            throw new RuntimeException("There were errors during one of the java RHS compilation. See system out for details.");
        }
        // URLClassLoader with URL like "file:/path" loades classes much much faster than with URL "file://path"
        // it's supposed that the latter tries to use network
        URLClassLoader loader = new URLClassLoader(new URL[]{new URL("file:/" + javaContentLocation.getPath() + "/")});

        for (ScapeRHSJavaAction javaAction : javaActions) {
            try {
                Class c = loader.loadClass(this.getClass().getPackage().getName() + "." + javaAction.javaClassName);
                javaAction.obtainClass(c);
            } catch (Exception e) {
                throw new RuntimeException("Unable to load class " + this.getClass().getPackage().getName() + "." + javaAction.javaClassName, e);
            }
        }
    }

    public void reset(TreenotationStorage storage) {
        reset(storage, null, null);
    }

    public void reset(TreenotationStorage storage, Token from, Token to) {
        this.from = from;
        this.to = to;
        if (startPointsIterator == null || this.storage != storage) {
            if (storage == null) {
                if (startPointsIterator != null) {
                    startPointsIterator.reset(null, null, null, null);
                    followIterator.close();
                }
            } else {
                startPointsIterator = storage.typeIterator(startPointCommonTypes, startPointTokenTypes, from, to);
                followIterator = storage.followIterator(input, null, null);
            }
        } else if (storage != null) {
            startPointsIterator.reset(startPointCommonTypes, startPointTokenTypes, from, to);
            followIterator.close();
        } else {
            startPointsIterator.reset(null, null, null, null);
            followIterator.close();
        }
        this.storage = storage;
        trn2classId.clear();
        curStart = null;
        scapeResultStack = new BlockStack(100);
    }

    public int getTreenotationClass(Treenotation trn) {
        Integer cls = trn2classId.get(trn);
        if (cls == null) {
            cls = allClassesTree.getTreenotationClass(trn);
            trn2classId.put(trn, cls);
        }
        return cls;
    }

    private ScapeResult newScapeResult() {
        if (scapeResultStack.isEmpty()) {
            return new ScapeResult(this);
        } else {
            ScapeResult r = (ScapeResult) scapeResultStack.pop();
            r.phase = this;
            r.currentState = null;
            r.lastMatched = null;
            r.nBindings = 0;
            r.nRegexBindings = 0;
            r.start = null;
            r.end = null;
            r.size = 0;
            return r;
        }
    }

    /*Transducer part (gate.Annotations) */

    private void freeScapeResult(ScapeResult r) {
        scapeResultStack.push(r);
    }

    public Treenotation nextStartPoint(Token skipUntil) {
        while (!stack.isEmpty()) {
            freeScapeResult((ScapeResult) stack.pop());
        }

        ScapeDFSMState start = (ScapeDFSMState) dfsm.getStartState();
        if (start.transitions == null) {
            return null;
        }
        while (startPointsIterator.hasNext()) {
            Treenotation trn = (Treenotation) startPointsIterator.next();
            Token trnStart = trn.getStartToken();
            if (skipUntil != null) {
                if (curStart == null || trnStart.compareTo(curStart) > 0) {
                    if (trnStart.compareTo(skipUntil) <= 0) {
                        startPointsIterator.skipTillToken(skipUntil);
                        if (startPointsIterator.hasNext()) {
                            trn = (Treenotation) startPointsIterator.next();
                            curStart = trn.getStartToken();
                        } else {
                            break;
                        }
                    } else {
                        curStart = trnStart;
                    }
                }
            } else {
                curStart = trnStart;
            }
            if (to != null && trn.getEndToken().compareTo(to) > 0) {
                continue;
            }
            int cls = getTreenotationClass(trn);

            if (cls == -1) {
                continue;
            }

            ScapeDFSMPair[] pairs;
            if ((pairs = (ScapeDFSMPair[]) start.transitions.get(cls)) == null) {
                continue;
            }

            for (ScapeDFSMPair p : pairs) {
                ScapeResult r = newScapeResult();
                r.lastMatched = LinkedTrns.addItem(trn, null);
                r.start = r.lastMatched.getTrn().getStartToken();
                r.end = r.lastMatched.getTrn().getEndToken();
                r.size = 1;
                r.currentState = (ScapeDFSMState) p.s;
                r.addTrnToBindings(p.bindingSet, trn);

                stack.push(r);
            }
            return trn;
        }

        scapeResultStack = null;
        startPointsIterator = null;
        followIterator = null;
        trn2classId.clear();

        return null;
    }

    public ScapeResult nextResult() {
        while (!stack.isEmpty()) {
            ScapeResult r = (ScapeResult) stack.pop();
            boolean isFinal = r.currentState.isFinal();

            if (r.currentState.transitions == null && r.currentState.nPairs > 0) { //e-transition
                ScapeBindedPair p = r.currentState.firstPair;
                if (!isFinal) {
                    r.currentState = (ScapeDFSMState) p.s;
                    stack.push(r);
                    p = p.next;
                }
                while (p != null) {
                    ScapeResult nr = newScapeResult();
                    nr.lastMatched = r.lastMatched;
                    nr.currentState = (ScapeDFSMState) p.s;
                    nr.start = r.start;
                    nr.end = r.end;
                    nr.size = r.size();
                    nr.importBindings(r);
                    stack.push(nr);
                    p = p.next;
                }
                continue;
            }

            if (r.currentState.nPairs > 0) {
                followIterator.reset(r.currentState.trnTypes, r.end);

                Treenotation todo = null;
                ScapeDFSMPair todoPair = null;


                IntFeatureMapStatic transitions = r.currentState.transitions;
                while (followIterator.hasNext()) {
                    Treenotation trn = (Treenotation) followIterator.next();
                    if (to != null && trn.getEndToken().compareTo(to) > 0) {
                        continue;
                    }

                    int cls = getTreenotationClass(trn);

                    if (cls == -1) {
                        continue;
                    }

                    ScapeDFSMPair[] pairs;
                    if ((pairs = (ScapeDFSMPair[]) transitions.get(cls)) == null) {
                        continue;
                    }

                    int i = 0;
                    if (!isFinal && todo == null) {
                        todo = trn;
                        todoPair = pairs[0];
                        i++;
                    }
                    for (; i < pairs.length; i++) {
                        ScapeResult nr = newScapeResult();
                        nr.lastMatched = LinkedTrns.addItem(trn, r.lastMatched);
                        nr.currentState = (ScapeDFSMState) pairs[i].s;
                        nr.start = r.start;
                        nr.end = trn.getEndToken();
                        nr.size = r.size() + 1;
                        nr.importBindings(r);
                        nr.addTrnToBindings(pairs[i].bindingSet, trn);
                        stack.push(nr);
                    }
                }
                if (todo != null) {
                    r.lastMatched = LinkedTrns.addItem(todo, r.lastMatched);
                    r.currentState = (ScapeDFSMState) todoPair.s;
                    r.end = todo.getEndToken();
                    r.size = r.size() + 1;
                    r.addTrnToBindings(todoPair.bindingSet, todo);
                    stack.push(r);
                } else if (!isFinal) {
                    freeScapeResult(r);
                }
            }

            if (isFinal) {
                return r;
            }
        }
        return null;
    }

    public void execute() {
        ArrayList<ScapeRHSActionResult> results = new ArrayList<ScapeRHSActionResult>();
        Vector<ScapeRHSActionResult> tarr = new Vector<ScapeRHSActionResult>();
        Token rightEdge = null;
        Treenotation cursor;
        Treenotation prevCursor = null;
        while ((cursor = nextStartPoint(rightEdge)) != null) {
            if (prevCursor != null && cursor.getStartToken() != prevCursor.getStartToken()) {
                results.addAll(tarr);
                tarr.setSize(0);
                rightEdge = null;
            } else if (control == CONTROL_TYPE_FIRST && tarr.size() > 0) {
                prevCursor = cursor;
                continue;
            }

            ScapeResult r;
            while ((r = nextResult()) != null) {
                if (control != CONTROL_TYPE_BRILL) {
                    if (control == CONTROL_TYPE_APPELT && rightEdge != null && r.getEndToken().compareTo(rightEdge) <= 0) {
                        continue;
                    } else if (control == CONTROL_TYPE_BRAPPELT && rightEdge != null && r.getEndToken().compareTo(rightEdge) < 0) {
                        continue;
                    }
                }

                ScapeRuleSet rules = r.getRules();
                r.activateBindings();
                while (r.nextCombination()) {
                    for (int i = 0; i < rules.size(); i++) {
                        ScapeRule rule = rules.get(i);
                        if (rule.checkConstraints()) {
                            if (control != CONTROL_TYPE_BRILL && (rightEdge == null || r.getEndToken().compareTo(rightEdge) > 0)) {
                                tarr.setSize(0);
                                rightEdge = r.getEndToken();
                            }

                            ArrayList<ScapeRHSAction> rhs = rule.getRhs();
                            for (ScapeRHSAction action : rhs) {
                                tarr.add(action.buildResult());
                            }

                            if (control == CONTROL_TYPE_FIRST) {
                                break;
                            }
                        }
                    }
                    if (tarr.size() > 0 && control == CONTROL_TYPE_FIRST) {
                        break;
                    }
                }
                r.deactivateBindings();
                freeScapeResult(r);
                if (tarr.size() > 0 && control == CONTROL_TYPE_FIRST) {
                    break;
                }
            }
            prevCursor = cursor;
        }
        results.addAll(tarr);

        for (ScapeRHSActionResult res : results) {
            res.applyTo(storage);
        }
    }

    public void executeForGate() {
        ArrayList<ScapeRHSActionResult> results = new ArrayList<ScapeRHSActionResult>();
        Vector<ScapeRHSActionResult> tarr = new Vector<ScapeRHSActionResult>();
        Node rightEdge = null;
        Annotation cursor;
        Annotation prevCursor = null;
        while ((cursor = nextStartPoint(rightEdge)) != null) {
            if (prevCursor != null && cursor.getStartNode() != prevCursor.getStartNode()) {
                results.addAll(tarr);
                tarr.setSize(0);
                rightEdge = null;
            } else if (control == CONTROL_TYPE_FIRST && tarr.size() > 0) {
                prevCursor = cursor;
                continue;
            }

            ScapeResultForGate r;
            while ((r = nextResultForGate()) != null) {
                if (control != CONTROL_TYPE_BRILL) {
                    if (control == CONTROL_TYPE_APPELT && rightEdge != null && r.getEndNode().getOffset().compareTo(rightEdge.getOffset()) <= 0) {
                        continue;
                    } else if (control == CONTROL_TYPE_BRAPPELT && rightEdge != null && r.getEndNode().getOffset().compareTo(rightEdge.getOffset()) < 0) {
                        continue;
                    }
                }

                ScapeRuleSet rules = r.getRules();
                r.activateBindings();
                while (r.nextCombination()) {
                    for (int i = 0; i < rules.size(); i++) {
                        ScapeRule rule = rules.get(i);
                        if (rule.checkConstraints()) {
                            if (control != CONTROL_TYPE_BRILL && (rightEdge == null || r.getEndNode().getOffset().compareTo(rightEdge.getOffset()) > 0)) {
                                tarr.setSize(0);
                                rightEdge = r.getEndNode();
                            }

                            ArrayList<ScapeRHSAction> rhs = rule.getRhs();
                            for (ScapeRHSAction action : rhs) {
                                tarr.add(action.buildResult());
                            }

                            if (control == CONTROL_TYPE_FIRST) {
                                break;
                            }
                        }
                    }
                    if (tarr.size() > 0 && control == CONTROL_TYPE_FIRST) {
                        break;
                    }
                }
                r.deactivateBindings();
                freeScapeResultForGate(r);
                if (tarr.size() > 0 && control == CONTROL_TYPE_FIRST) {
                    break;
                }
            }
            prevCursor = cursor;
        }
        results.addAll(tarr);

        for (ScapeRHSActionResult res : results) {
            res.applyTo(annotationSet, doc);
        }
    }

    public void resetForListProcessing() {
        trn2classId.clear();
        scapeResultStack = new BlockStack(100);
    }

    public void match(List<Treenotation> list, ArrayList result) {  //works without bindings
        result.clear();

        while (!stack.isEmpty()) {
            freeScapeResult((ScapeResult) stack.pop());
        }

        ScapeDFSMState start = (ScapeDFSMState) dfsm.getStartState();

        Treenotation trn = list.get(0);

        int cls = getTreenotationClass(trn);
        if (cls == -1) {
            return;
        }
        ScapeDFSMPair[] pairs;
        if ((pairs = (ScapeDFSMPair[]) start.transitions.get(cls)) == null) {
            return;
        }

        for (ScapeDFSMPair p : pairs) {
            ScapeResult r = newScapeResult();
            r.lastMatched = LinkedTrns.addItem(trn, null);
            r.size = 1;
            r.currentState = (ScapeDFSMState) p.s;

            stack.push(r);
        }

        while (!stack.isEmpty()) {
            ScapeResult r = (ScapeResult) stack.pop();

            if (r.currentState.isFinal() && list.size() == r.size()) {
                ScapeRuleSet rules = r.getRules();
                for (int i = 0; i < rules.size(); i++) {
                    ScapeRule rule = rules.get(i);
                    ArrayList<ScapeRHSAction> rhs = rule.getRhs();
                    for (ScapeRHSAction action : rhs) {
                        result.add(action.buildResult());
                    }
                }
            } else {
                if (r.currentState.transitions == null && r.currentState.nPairs > 0) {
                    ScapeBindedPair p = r.currentState.firstPair;
                    r.currentState = (ScapeDFSMState) p.s;
                    stack.push(r);
                    p = p.next;
                    while (p != null) {
                        ScapeResult nr = newScapeResult();
                        nr.lastMatched = r.lastMatched;
                        nr.currentState = (ScapeDFSMState) p.s;
                        nr.size = r.size();
                        stack.push(nr);
                        p = p.next;
                    }
                    continue;
                }

                if (list.size() < r.size() - 1) {
                    continue;
                }

                Treenotation next = list.get(r.size());
                cls = getTreenotationClass(next);

                if (cls == -1) {
                    return;
                }

                if ((pairs = (ScapeDFSMPair[]) r.currentState.transitions.get(cls)) == null) {
                    continue;
                }

                for (int i = 1; i < pairs.length; i++) {
                    ScapeResult nr = newScapeResult();
                    nr.lastMatched = LinkedTrns.addItem(next, r.lastMatched);
                    nr.currentState = (ScapeDFSMState) pairs[i].s;
                    nr.size = r.size() + 1;
                    stack.push(nr);
                }

                r.lastMatched = LinkedTrns.addItem(next, r.lastMatched);
                r.currentState = (ScapeDFSMState) pairs[0].s;
                r.size = r.size() + 1;
                stack.push(r);
            }
        }
    }

    public void orthoReset() {
        trn2classId.clear();
        scapeResultStack = new BlockStack(100);
    }

    public void match(Treenotation first, Treenotation second, ArrayList<ScapeRHSActionResult> result) {
        match(first, -2, second, -2, result);
    }

    public void match(Treenotation first, int firstCls, Treenotation second, int secondCls, ArrayList<ScapeRHSActionResult> result) {
        result.clear();

        while (!stack.isEmpty()) {
            freeScapeResult((ScapeResult) stack.pop());
        }

        if (firstCls == -2) {
            firstCls = getTreenotationClass(first);
        }

        if (firstCls == -1) {
            return;
        }

        ScapeDFSMState start = (ScapeDFSMState) dfsm.getStartState();
        ScapeDFSMPair[] pairs;
        if ((pairs = (ScapeDFSMPair[]) start.transitions.get(firstCls)) == null) {
            return;
        }

        for (ScapeDFSMPair p : pairs) {
            ScapeResult r = newScapeResult();
            r.lastMatched = LinkedTrns.addItem(first, null);
            r.start = r.lastMatched.getTrn().getStartToken();
            r.end = r.lastMatched.getTrn().getEndToken();
            r.size = 1;
            r.currentState = (ScapeDFSMState) p.s;
            r.addTrnToBindings(p.bindingSet, first);

            stack.push(r);
        }

        while (!stack.isEmpty()) {
            ScapeResult r = (ScapeResult) stack.pop();

            if (r.size() == 1) {
                if (r.currentState.transitions == null && r.currentState.nPairs > 0) {
                    ScapeBindedPair p = r.currentState.firstPair;
                    r.currentState = (ScapeDFSMState) p.s;
                    stack.push(r);
                    p = p.next;
                    while (p != null) {
                        ScapeResult nr = newScapeResult();
                        nr.lastMatched = r.lastMatched;
                        nr.currentState = (ScapeDFSMState) p.s;
                        nr.start = r.start;
                        nr.end = r.end;
                        nr.size = r.size();
                        nr.importBindings(r);
                        stack.push(nr);
                        p = p.next;
                    }
                    continue;
                }
                if (secondCls == -2) {
                    secondCls = getTreenotationClass(second);
                }

                if (secondCls == -1) {
                    return;
                }

                if ((pairs = (ScapeDFSMPair[]) r.currentState.transitions.get(secondCls)) == null) {
                    continue;
                }

                for (int i = 1; i < pairs.length; i++) {
                    ScapeResult nr = newScapeResult();
                    nr.lastMatched = LinkedTrns.addItem(second, r.lastMatched);
                    nr.currentState = (ScapeDFSMState) pairs[i].s;
                    nr.start = second.getStartToken().compareTo(r.start) < 0 ? second.getStartToken() : r.start;
                    nr.end = second.getEndToken().compareTo(r.end) > 0 ? second.getEndToken() : r.end;
                    nr.size = r.size() + 1;
                    nr.importBindings(r);
                    nr.addTrnToBindings(pairs[i].bindingSet, second);
                    stack.push(nr);
                }

                r.lastMatched = LinkedTrns.addItem(second, r.lastMatched);
                r.currentState = (ScapeDFSMState) pairs[0].s;
                r.start = second.getStartToken().compareTo(r.start) < 0 ? second.getStartToken() : r.start;
                r.end = second.getEndToken().compareTo(r.end) > 0 ? second.getEndToken() : r.end;
                r.size = r.size() + 1;
                r.addTrnToBindings(pairs[0].bindingSet, second);
                stack.push(r);
            } else if (r.size() == 2) {
                if (r.currentState.isFinal()) {
                    ScapeRuleSet rules = r.getRules();
                    r.activateBindings();
                    while (r.nextCombination()) {
                        for (int i = 0; i < rules.size(); i++) {
                            ScapeRule rule = rules.get(i);
                            if (rule.checkConstraints()) {
                                ArrayList<ScapeRHSAction> rhs = rule.getRhs();
                                for (ScapeRHSAction action : rhs) {
                                    if (action instanceof ScapeOrthoRHSAction &&
                                            !((ScapeOrthoRHSAction) action).match
                                            ) {
                                        result.clear();
                                        return;
                                    } else {
                                        result.add(action.buildResult());
                                    }
                                }
                            }
                        }
                    }
                    r.deactivateBindings();
                }
                freeScapeResult(r);
            }
        }
    }

    public void reset(AnnotationSet annotations, Document doc) {
        reset(annotations, doc, null, null);
    }

    public void reset(AnnotationSet annotations, Document doc, Node start, Node end) {
        annotationSet = annotations;

        AnnotationSet anns = annotations == null ? null : annotations.get(input.toStringHashSet());
        if (anns == null) this.annotations = new Annotation[0];
        else this.annotations = (Annotation[]) anns.toArray(new Annotation[anns.size()]);
        Arrays.sort(this.annotations, new OffsetComparator());
        cursor = 0;
        curStartOffset = -1;
        anns2classId.clear();
        scapeResultStack = new BlockStack(100);
        startNode = start;
        endNode = end;
        this.doc = doc;
    }

    public void reset(Annotation[] anns, Document doc, int startCursor, Node start, Node end) {
        this.annotations = anns;
        cursor = startCursor;
        curStartOffset = -1;
        anns2classId.clear();
        scapeResultStack = new BlockStack(100);
        startNode = start;
        this.doc = doc;
        endNode = end;
    }

    private ScapeResultForGate newScapeResultForGate() {
        if (scapeResultStack.isEmpty()) {
            return new ScapeResultForGate(this);
        } else {
            ScapeResultForGate r = (ScapeResultForGate) scapeResultStack.pop();
            r.phase = this;
            r.currentState = null;
            r.lastMatched = null;
            r.nBindings = 0;
            r.nRegexBindings = 0;
            r.start = null;
            r.end = null;
            r.size = 0;
            return r;
        }
    }

    private void freeScapeResultForGate(ScapeResultForGate e) {
        scapeResultStack.push(e);
    }

    public int getAnnotationClass(Annotation ann, Document doc) {
        Integer cls = anns2classId.get(ann);
        if (cls == null) {
            cls = allClassesTree.getAnnotationClass(ann, doc);
            anns2classId.put(ann, cls);
        }
        return cls;
    }

    public Annotation nextStartPoint(Node skipUntil) {
        while (!stack.isEmpty()) {
            freeScapeResultForGate((ScapeResultForGate) stack.pop());
        }

        ScapeDFSMState start = (ScapeDFSMState) dfsm.getStartState();
        if (start.transitions == null) {
            return null;
        }

        long skipUntilOffset = skipUntil != null ? skipUntil.getOffset() : -1;

        while (cursor < annotations.length) {
            Annotation ann = annotations[cursor++];
            long annStartOffset = ann.getStartNode().getOffset();
            if (startNode != null && annStartOffset < startNode.getOffset()) {
                continue;
            }

            if (endNode != null && annStartOffset > endNode.getOffset()) {
                break;
            }

            if (endNode != null && ann.getEndNode().getOffset() > endNode.getOffset()) {
                continue;
            }
            if (skipUntilOffset != -1) {
                if (curStartOffset == -1 || annStartOffset > curStartOffset) {
                    if (annStartOffset < skipUntilOffset) {
                        ann = null;
                        if (endNode == null || skipUntilOffset < endNode.getOffset()) {
                            while (cursor < annotations.length) {
                                ann = annotations[cursor++];
                                annStartOffset = ann.getStartNode().getOffset();

                                if (annStartOffset >= skipUntilOffset) {
                                    curStartOffset = annStartOffset;
                                    break;
                                }
                            }
                        }
                        if (ann == null)
                            break;
                    } else {
                        curStartOffset = annStartOffset;
                    }
                }
            } else {
                curStartOffset = annStartOffset;
            }

            if (endNode != null && ann.getEndNode().getOffset() > endNode.getOffset()) {
                continue;
            }

            int cls = getAnnotationClass(ann, doc);

            if (cls == -1) {
                continue;
            }

            ScapeDFSMPair[] pairs;
            if ((pairs = (ScapeDFSMPair[]) start.transitions.get(cls)) == null) {
                continue;
            }

            for (ScapeDFSMPair p : pairs) {
                ScapeResultForGate r = newScapeResultForGate();
                r.lastMatched = LinkedAnns.addItem(ann, null);
                r.start = r.lastMatched.ann.getStartNode();
                r.end = r.lastMatched.ann.getEndNode();
                r.size = 1;
                r.currentState = (ScapeDFSMState) p.s;
                r.addAnnToBindings(p.bindingSet, ann);
                stack.push(r);
            }
            return ann;
        }

        scapeResultStack = null;
        annotations = null;
        doc = null;
        anns2classId.clear();

        return null;
    }

    public ScapeResultForGate nextResultForGate() {
        while (!stack.isEmpty()) {
            ScapeResultForGate r = (ScapeResultForGate) stack.pop();
            boolean isFinal = r.currentState.isFinal();

            if (r.currentState.transitions == null && r.currentState.nPairs > 0) {
                ScapeBindedPair p = r.currentState.firstPair;
                if (!isFinal) {
                    r.currentState = (ScapeDFSMState) p.s;
                    stack.push(r);
                    p = p.next;
                }
                while (p != null) {
                    ScapeResultForGate nr = newScapeResultForGate();
                    nr.lastMatched = r.lastMatched;
                    nr.currentState = (ScapeDFSMState) p.s;
                    nr.start = r.start;
                    nr.end = r.end;
                    nr.size = r.size();
                    nr.importBindings(r);
                    stack.push(nr);
                    p = p.next;
                }
                continue;
            }

            if (r.currentState.nPairs > 0) {
                long rightEdge = -1;
                long followEnd = r.end.getOffset();
                int internCursor = cursor;

                Annotation todo = null;
                ScapeDFSMPair todoPair = null;

                IntFeatureMapStatic transitions = r.currentState.transitions;
                while (internCursor < annotations.length) {
                    Annotation ann = annotations[internCursor++];
                    long annStart = ann.getStartNode().getOffset();
                    if (annStart >= followEnd) {
                        if (rightEdge != -1 && annStart >= rightEdge)
                            break;
                        rightEdge = (ann.getEndNode().getOffset());
                    } else {
                        continue;
                    }

                    if (endNode != null && ann.getEndNode().getOffset() > endNode.getOffset()) {
                        continue;
                    }

                    int cls = getAnnotationClass(ann, doc);

                    if (cls == -1) {
                        continue;
                    }

                    ScapeDFSMPair[] pairs;
                    if ((pairs = (ScapeDFSMPair[]) transitions.get(cls)) == null) {
                        continue;
                    }

                    int i = 0;
                    if (!isFinal && todo == null) {
                        todo = ann;
                        todoPair = pairs[0];
                        i++;
                    }
                    for (; i < pairs.length; i++) {
                        ScapeResultForGate nr = newScapeResultForGate();
                        nr.lastMatched = LinkedAnns.addItem(ann, r.lastMatched);
                        nr.currentState = (ScapeDFSMState) pairs[i].s;
                        nr.start = r.start;
                        nr.end = ann.getEndNode();
                        nr.size = r.size() + 1;
                        nr.importBindings(r);
                        nr.addAnnToBindings(pairs[i].bindingSet, ann);
                        stack.push(nr);
                    }
                }
                if (todo != null) {
                    r.lastMatched = LinkedAnns.addItem(todo, r.lastMatched);
                    r.currentState = (ScapeDFSMState) todoPair.s;
                    r.end = todo.getEndNode();
                    r.size = r.size() + 1;
                    r.addAnnToBindings(todoPair.bindingSet, todo);
                    stack.push(r);
                } else if (!isFinal) {
                    freeScapeResultForGate(r);
                }
            }

            if (isFinal) {
                return r;
            }
        }
        return null;
    }

    public void orthoResetForGate(Document doc) {
        anns2classId.clear();
        scapeResultStack = new BlockStack(100);
        this.doc = doc;
    }

    public void match(Annotation first, Annotation second, ArrayList<ScapeRHSActionResult> result) {
        match(first, -2, second, -2, result);
    }

    public void match(Annotation first, int firstCls, Annotation second, int secondCls, ArrayList<ScapeRHSActionResult> result) {
        result.clear();

        while (!stack.isEmpty()) {
            freeScapeResultForGate((ScapeResultForGate) stack.pop());
        }

        if (firstCls == -2) {
            firstCls = getAnnotationClass(first, doc);
        }
        if (firstCls == -1) {
            return;
        }

        ScapeDFSMState start = (ScapeDFSMState) dfsm.getStartState();
        ScapeDFSMPair[] pairs;
        if ((pairs = (ScapeDFSMPair[]) start.transitions.get(firstCls)) == null) {
            return;
        }

        for (ScapeDFSMPair p : pairs) {
            ScapeResultForGate r = newScapeResultForGate();
            r.lastMatched = LinkedAnns.addItem(first, null);
            r.start = r.lastMatched.ann.getStartNode();
            r.end = r.lastMatched.ann.getEndNode();
            r.size = 1;
            r.currentState = (ScapeDFSMState) p.s;
            r.addAnnToBindings(p.bindingSet, first);

            stack.push(r);
        }

        while (!stack.isEmpty()) {
            ScapeResultForGate r = (ScapeResultForGate) stack.pop();

            if (r.size() == 1) {
                if (r.currentState.transitions == null && r.currentState.nPairs > 0) {
                    ScapeBindedPair p = r.currentState.firstPair;
                    r.currentState = (ScapeDFSMState) p.s;
                    stack.push(r);
                    p = p.next;
                    while (p != null) {
                        ScapeResultForGate nr = newScapeResultForGate();
                        nr.lastMatched = r.lastMatched;
                        nr.currentState = (ScapeDFSMState) p.s;
                        nr.start = r.start;
                        nr.end = r.end;
                        nr.size = r.size();
                        nr.importBindings(r);
                        stack.push(nr);
                        p = p.next;
                    }
                    continue;
                }
                if (secondCls == -2) {
                    secondCls = getAnnotationClass(second, doc);
                }
                if (secondCls == -1) {
                    return;
                }

                if ((pairs = (ScapeDFSMPair[]) r.currentState.transitions.get(secondCls)) == null) {
                    continue;
                }

                for (int i = 1; i < pairs.length; i++) {
                    ScapeResultForGate nr = newScapeResultForGate();
                    nr.lastMatched = LinkedAnns.addItem(second, r.lastMatched);
                    nr.currentState = (ScapeDFSMState) pairs[i].s;
                    nr.start = second.getStartNode().getOffset() < r.start.getOffset() ? second.getStartNode() : r.start;
                    nr.end = second.getEndNode().getOffset() > r.end.getOffset() ? second.getEndNode() : r.end;
                    nr.size = r.size() + 1;
                    nr.importBindings(r);
                    nr.addAnnToBindings(pairs[i].bindingSet, second);
                    stack.push(nr);
                }

                r.lastMatched = LinkedAnns.addItem(second, r.lastMatched);
                r.currentState = (ScapeDFSMState) pairs[0].s;
                r.start = second.getStartNode().getOffset() < r.start.getOffset() ? second.getStartNode() : r.start;
                r.end = second.getEndNode().getOffset() > r.end.getOffset() ? second.getEndNode() : r.end;
                r.size = r.size() + 1;
                r.addAnnToBindings(pairs[0].bindingSet, second);
                stack.push(r);
            } else if (r.size() == 2) {
                if (r.currentState.isFinal()) {
                    ScapeRuleSet rules = r.getRules();
                    r.activateBindings();
                    while (r.nextCombination()) {
                        for (int i = 0; i < rules.size(); i++) {
                            ScapeRule rule = rules.get(i);
                            if (rule.checkConstraints()) {
                                ArrayList<ScapeRHSAction> rhs = rule.getRhs();
                                for (ScapeRHSAction action : rhs) {
                                    if (action instanceof ScapeOrthoRHSAction &&
                                            !((ScapeOrthoRHSAction) action).match
                                            ) {
                                        result.clear();
                                        return;
                                    } else {
                                        result.add(action.buildResult());
                                    }
                                }
                            }
                        }
                    }
                    r.deactivateBindings();
                }
                freeScapeResultForGate(r);
            }
        }
    }

    public void match(Annotation first, int firstCls, ScapeInputSetByTypesForGate inputSet, ArrayList<ScapeRHSActionResult> results, ArrayList<Annotation> secondAnns) {
        results.clear();

        while (!stack.isEmpty()) {
            freeScapeResultForGate((ScapeResultForGate) stack.pop());
        }

        if (firstCls == -2) {
            firstCls = getAnnotationClass(first, doc);
        }
        if (firstCls == -1) {
            return;
        }

        ScapeDFSMState start = (ScapeDFSMState) dfsm.getStartState();
        ScapeDFSMPair[] pairs;
        if ((pairs = (ScapeDFSMPair[]) start.transitions.get(firstCls)) == null) {
            return;
        }

        for (ScapeDFSMPair p : pairs) {
            ScapeResultForGate r = newScapeResultForGate();
            r.lastMatched = LinkedAnns.addItem(first, null);
            r.start = r.lastMatched.ann.getStartNode();
            r.end = r.lastMatched.ann.getEndNode();
            r.size = 1;
            r.currentState = (ScapeDFSMState) p.s;
            r.addAnnToBindings(p.bindingSet, first);

            stack.push(r);
        }

        while (!stack.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) return;
            ScapeResultForGate r = (ScapeResultForGate) stack.pop();

            if (r.size() == 1) {
                if (r.currentState.transitions == null && r.currentState.nPairs > 0) {
                    ScapeBindedPair p = r.currentState.firstPair;
                    r.currentState = (ScapeDFSMState) p.s;
                    stack.push(r);
                    p = p.next;
                    while (p != null) {
                        ScapeResultForGate nr = newScapeResultForGate();
                        nr.lastMatched = r.lastMatched;
                        nr.currentState = (ScapeDFSMState) p.s;
                        nr.start = r.start;
                        nr.end = r.end;
                        nr.size = r.size();
                        nr.importBindings(r);
                        stack.push(nr);
                        p = p.next;
                    }
                    continue;
                }

                inputSet.reset(r.currentState.trnTypes);
                if (inputSet.next()) {
                    while (true) {
                        Annotation second = inputSet.getAnnotation();
                        int secondCls = inputSet.getClassId();

                        if (secondCls == -2) {
                            secondCls = getAnnotationClass(second, doc);
                        }
                        if (secondCls == -1 || (pairs = (ScapeDFSMPair[]) r.currentState.transitions.get(secondCls)) == null) {
                            if (!inputSet.next())
                                break;
                            continue;
                        }

                        boolean hasNext = inputSet.next();

                        for (int i = hasNext ? 0 : 1; i < pairs.length; i++) {
                            ScapeResultForGate nr = newScapeResultForGate();
                            nr.lastMatched = LinkedAnns.addItem(second, r.lastMatched);
                            nr.currentState = (ScapeDFSMState) pairs[i].s;
                            nr.start = second.getStartNode().getOffset() < r.start.getOffset() ? second.getStartNode() : r.start;
                            nr.end = second.getEndNode().getOffset() > r.end.getOffset() ? second.getEndNode() : r.end;
                            nr.size = r.size() + 1;
                            nr.importBindings(r);
                            nr.addAnnToBindings(pairs[i].bindingSet, second);
                            stack.push(nr);
                        }

                        if (!hasNext) {
                            r.lastMatched = LinkedAnns.addItem(second, r.lastMatched);
                            r.currentState = (ScapeDFSMState) pairs[0].s;
                            r.start = second.getStartNode().getOffset() < r.start.getOffset() ? second.getStartNode() : r.start;
                            r.end = second.getEndNode().getOffset() > r.end.getOffset() ? second.getEndNode() : r.end;
                            r.size = r.size() + 1;
                            r.addAnnToBindings(pairs[0].bindingSet, second);
                            stack.push(r);
                            break;
                        }
                    }
                }
            } else if (r.size() == 2) {
                if (r.currentState.isFinal()) {
                    ScapeRuleSet rules = r.getRules();
                    r.activateBindings();
                    while (r.nextCombination()) {
                        for (int i = 0; i < rules.size(); i++) {
                            ScapeRule rule = rules.get(i);
                            if (rule.checkConstraints()) {
                                ArrayList<ScapeRHSAction> rhs = rule.getRhs();
                                for (ScapeRHSAction action : rhs) {
                                    if (action instanceof ScapeOrthoRHSAction &&
                                            !((ScapeOrthoRHSAction) action).match
                                            ) {
                                    } else {
                                        results.add(action.buildResult());
                                        secondAnns.add(r.lastMatched.ann);
                                    }
                                }
                            }
                        }
                    }
                    r.deactivateBindings();
                }
                freeScapeResultForGate(r);
            }
        }
    }

    private class ScapeFSMIterator implements Iterator {
        int ruleN;

        ScapeFSMIterator() {
            ruleN = 0;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return ruleN < allRules.size;
        }

        public Object next() {
            return allRules.rules[ruleN++].fsm;
        }
    }

    class TypesIterator implements Iterator<TrnType> {
        TypesIterator(ScapeDFSMState state) {
            reset(state);
        }

        void reset(ScapeDFSMState state) {
            fmIt.reset(state != null ? state.transitions : null);
        }

        public void remove() {
        }

        public boolean hasNext() {
            return fmIt.hasNext();
        }

        public TrnType next() {
            NumeratedObject no = fmIt.next();
            return allClassesTree.getClassType(no.n);
        }
    }
}
