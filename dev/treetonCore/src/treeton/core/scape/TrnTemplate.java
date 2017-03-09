/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape;

import treeton.core.IntFeatureMapImpl;
import treeton.core.Treenotation;
import treeton.core.TreenotationUtil;
import treeton.core.TreetonFactory;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.fsm.logicset.LogicFSM;
import treeton.core.fsm.logicset.RegExpParseTree;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.util.nu;
import treeton.core.util.sut;

import java.util.*;

public class TrnTemplate implements Iterable<Treenotation> {
    public List<Treenotation> trns;
    public HashMap<LogicFSM, String> fsmNames;

    private HashMap<TrnType, HashSet<Integer>> caseInsensitiveFeatures;

    public TrnTemplate() {
        trns = new ArrayList<Treenotation>();
        fsmNames = new HashMap<LogicFSM, String>();
    }

    public int readIn(TreenotationsContext context, char s[], int pl, int endpl, HashMap<TrnType, HashSet<Integer>> caseInsensitiveFeatures) throws ParseException, TreetonModelException {
        return readIn(context.getTypes(), s, pl, endpl, caseInsensitiveFeatures);
    }

    public int readIn(TrnTypeStorage storage, char s[], int pl, int endpl, HashMap<TrnType, HashSet<Integer>> caseInsensitiveFeatures) throws ParseException {
        this.caseInsensitiveFeatures = caseInsensitiveFeatures;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }

        boolean first = true;
        while (pl <= endpl && (s[pl] == '|' || first)) {
            first = false;
            pl++;

            pl = sut.skipSpacesEndls(s, pl, endpl);
            int i = pl;
            pl = sut.skipVarName(s, pl, endpl);
            if (pl == i) {
                throw new ParseException("missing type name", null, s, pl, endpl);
            }
            String t = new String(s, i, pl - i);
            TrnType tp;
            try {
                if ((tp = storage.get(t)) == null) {
                    throw new ParseException("unregistered type " + t, null, s, pl, endpl);
                }
            } catch (TreetonModelException e) {
                throw new ParseException("unregistered type " + t, null, s, pl, endpl);
            }

            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] != ':' && s[pl] != '}' && s[pl] != '|') {
                throw new ParseException("missing ':','}' or '|'", null, s, pl, endpl);
            }
            trns.add(TreetonFactory.newTreenotation(null, null, tp));

            if (s[pl] == '}') {
                pl++;
                this.caseInsensitiveFeatures = null;
                return pl;
            } else if (s[pl] == ':') {
                pl++;
                break;
            }
        }

        pl = readTrnTemplate(s, pl, endpl, trns, 0, false);
        if (s[pl] != '}') {
            throw new ParseException("missing '}'", null, s, pl, endpl);
        }
        pl++;
        this.caseInsensitiveFeatures = null;
        return pl;
    }

    public int readIn(char s[], int pl, int endpl, TrnType tp, HashMap<TrnType, HashSet<Integer>> caseInsensitiveFeatures) throws ParseException {
        this.caseInsensitiveFeatures = caseInsensitiveFeatures;

        trns.add(TreetonFactory.newTreenotation(null, null, tp));
        pl = readTrnTemplate(s, pl, endpl, trns, 0, true) + 1;
        this.caseInsensitiveFeatures = null;
        return pl;
    }

    protected boolean isCaseInsensetive(int feature, TrnType tp) {
        if (caseInsensitiveFeatures == null) {
            return false;
        }
        HashSet<Integer> h = caseInsensitiveFeatures.get(tp);
        return h != null && h.contains(feature);
    }

    int readEqual(char s[], int pl, int endpl, List<Treenotation> variants, int start, boolean extBraces) throws ParseException {
        int beg;
        int i, j, numStartVariants = variants.size() - start;
        boolean gotIt, first;
        String N;
        Object V = null;
        int feature;

        pl = sut.skipSpacesEndls(s, pl, endpl);
        beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing feature name", null, s, pl, endpl);
        }
        N = new String(s, beg, pl - beg);
        for (i = variants.size() - numStartVariants; i < variants.size(); i++) {
            Treenotation trn = variants.get(i);
            try {
                feature = trn.getType().getFeatureIndex(N);
            } catch (TreetonModelException e) {
                feature = -1;
            }
            if (feature == -1) {
                throw new ParseException("unregistered feature " + N + "(type " + trn.getType() + ")", null, s, pl, endpl);
            }
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '=') {
            throw new ParseException("missing '='", null, s, pl, endpl);
        }
        i = 0;
        first = true;
        gotIt = false;
        while (pl <= endpl && (s[pl] == '|' || first) && !gotIt) {
            first = false;
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            String regstr = null;
            String bindingName = null;
            if (s[pl] == '<') {
                pl++;
                beg = pl;
                while (pl <= endpl) {
                    if (s[pl] == '\\') { //todo этого мало, потенциально возможны ошибки
                        pl += 2;
                    } else if (s[pl] == '>') {
                        break;
                    } else {
                        pl++;
                    }
                }
                if (pl > endpl) {
                    throw new ParseException("missing '>'", null, s, pl, endpl);
                }
                regstr = new String(s, beg, pl - beg);
                pl++;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                sut.checkEndOfStream(s, pl, endpl);
                if (s[pl] == ':') {
                    pl++;
                    pl = sut.skipSpacesEndls(s, pl, endpl);
                    beg = pl;
                    pl = sut.skipVarName(s, pl, endpl);
                    if (pl == beg) {
                        throw new ParseException("missing binding name", null, s, pl, endpl);
                    }
                    bindingName = new String(s, beg, pl - beg);
                }
            } else {
                beg = pl;
                pl = sut.skipVarValue(s, pl, endpl);
                if (pl == beg) {
                    throw new ParseException("missing feature value", null, s, pl, endpl);
                }
                if (pl - beg == 4 && s[beg] == 'n' && s[beg + 1] == 'u' && s[beg + 2] == 'l' && s[beg + 3] == 'l') {
                    V = nu.ll;
                } else {
                    if (s[beg] == '"') {
                        V = sut.extractTString(s, beg + 1, pl - beg - 2);
                    } else {
                        V = sut.extractTString(s, beg, pl - beg);
                    }
                }
            }
            for (i = variants.size() - numStartVariants; i < variants.size(); i++) {
                Treenotation cur = variants.get(i);
                TrnType tp = cur.getType();
                try {
                    feature = tp.getFeatureIndex(N);
                } catch (TreetonModelException e) {
                    feature = -1;
                }
                if (regstr != null) {
                    RegExpParseTree root;
                    try {
                        if (isCaseInsensetive(feature, tp)) {
                            root = RegExpParseTree.parseRegExp(regstr, false);
                        } else {
                            root = RegExpParseTree.parseRegExp(regstr, true);
                        }
                        V = root.createNonDeterminedLogicFSM();
                    } catch (Exception e) {
                        throw new ParseException(e.getMessage(), null, s, pl, endpl);
                    }
                    if (bindingName != null) {
                        fsmNames.put((LogicFSM) V, bindingName);
                    }
                }

                Object oldV = cur.get(feature);
                if (oldV == null) {
                    cur.put(feature, V, tp);
                } else {
                    Object[] arr;
                    if (oldV instanceof Object[]) {
                        Object[] oldArr = (Object[]) oldV;
                        arr = new Object[oldArr.length + 1];
                        System.arraycopy(oldArr, 0, arr, 0, oldArr.length);
                        arr[arr.length - 1] = V;
                    } else {
                        arr = new Object[2];
                        arr[0] = oldV;
                        arr[1] = V;
                    }
                    cur.put(feature, arr, tp);
                }
            }
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] == '|') {
                j = pl + 1;
                while (j <= endpl && s[j] != (extBraces ? ')' : '}') && s[j] != ',' && s[j] != '|') {
                    if (s[j] == '=') {
                        gotIt = true;
                        break;
                    }
                    j++;
                }

                if (!gotIt) {
                    for (i = 0; i < numStartVariants; i++) {
                        Treenotation trn = (Treenotation) (variants.get(i + start)).clone();
                        try {
                            feature = trn.getType().getFeatureIndex(N);
                        } catch (TreetonModelException e) {
                            feature = -1;
                        }
                        ((IntFeatureMapImpl) trn).removeLight(feature);
                        variants.add(trn);
                    }
                }
            }
        }
        sut.checkEndOfStream(s, pl, endpl);
        if ((s[pl] == ',' || s[pl] == (extBraces ? ')' : '}') || s[pl] == '|') && i == variants.size()) {
            return pl;
        }
        throw new ParseException("wrong equation syntax", null, s, pl, endpl);
    }

    int readTrnTemplate(char s[], int pl, int endpl, List<Treenotation> _variants, int _start, boolean extBraces) throws ParseException {
        ArrayList<Treenotation> variants = new ArrayList<Treenotation>();
        int i;
        for (i = 0; i < _variants.size(); i++) {
            variants.add((Treenotation) _variants.get(i).clone());
        }
        int start = _start;
        pl = sut.skipSpacesEndls(s, pl, endpl);

        if (extBraces) {
            if (s[pl] != '(') {
                throw new ParseException("missing '('", null, s, pl, endpl);
            }
            pl++;
        }

        while (pl <= endpl) {
            pl = sut.skipSpacesEndls(s, pl, endpl);
            if (s[pl] == (extBraces ? ')' : '}')) {
                break;
            }
            if (s[pl] == '(') {
                pl = readTrnTemplate(s, pl, endpl, variants, start, true);
                pl++;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                if (s[pl] == '|') {
                    start = variants.size();
                    for (i = 0; i < _variants.size() - _start; i++) {
                        variants.add((Treenotation) (_variants.get(i + _start)).clone());
                    }
                    pl++;
                } else if (s[pl] == (extBraces ? ')' : '}')) {
                    break;
                } else if (s[pl] == ',') {
                    pl++;
                }
            } else {
                pl = readEqual(s, pl, endpl, variants, start, extBraces);
                if (s[pl] == '|') {
                    start = variants.size();
                    for (i = 0; i < _variants.size() - _start; i++) {
                        variants.add((Treenotation) (_variants.get(i + _start)).clone());
                    }
                    pl++;
                } else if (s[pl] == (extBraces ? ')' : '}')) {
                    break;
                } else if (s[pl] == ',') {
                    pl++;
                }
            }
        }
        if (pl <= endpl && s[pl] == (extBraces ? ')' : '}')) {
            _variants.clear();
            for (i = 0; i < variants.size(); i++) {
                _variants.add(variants.get(i));
            }
            return pl;
        }
        throw new ParseException("wrong TrnTemplate syntax", null, s, pl, endpl);
    }

    public boolean match(Treenotation value) {
        for (Treenotation trn : trns) {
            if (TreenotationUtil.match(value, trn)) {
                return true;
            }
        }
        return false;
    }

    public boolean match(ScapeVariable value) {
        for (Treenotation trn : trns) {
            if (TreenotationUtil.match(value, trn)) {
                return true;
            }
        }
        return false;
    }

  /*public static void main(String[] args) {
   if (!Session.getInstance().isOpenOffline()) {
     Session.getInstance().close();
     Session.getInstance().openOffline();
   }

   TrnTemplate templ = new TrnTemplate();
   String s = "{Morph: length = 1, length = 3, POS = N, CAS = nom, (GEND = m | f), POS = A, base=<.*>:r1 }";
   try {
     templ.readIn(s.toCharArray(),0,s.length()-1,null);
   } catch (ParseException e) {
     e.printStackTrace();
   }
   System.out.println("Success!");

   try {
     LogicFSM au01 = RegExpParseTree.parseRegExp("(a|b|c)(d|g|f)",false).createNonDeterminedLogicFSM();
     LogicFSM au02= RegExpParseTree.parseRegExp("(a|b)(d|g)",false).createNonDeterminedLogicFSM();
     FSMFrame.showFSMFrame(au01);
     //LogicFSM au03= RegExpParseTree.parseRegExp("[edfg]*").createNonDeterminedLogicFSM();
     FSMFrame.showFSMFrame(au02);
     ArrayList<LogicFSM> arr = new ArrayList<LogicFSM>();
     arr.add(au01);
     au01.assignData("Data1");
     arr.add(au02);
     au02.assignData("Data2");
     //arr.add(au03);
     FSMFrame.showFSMFrame(au01);
     FSMFrame.showFSMFrame(au02);

     FSMFrame.showFSMFrame(LogicFSM.multipleUnion(arr.iterator()));
   } catch (Exception e) {
     e.printStackTrace();
   }

   try {
     String path = UInputStream.getResourcePath("res:/treeton/tests/test3.scape");
     char[] f = FileMapper.map2memory(path);
     ScapePhase phase = new ScapePhase();
     phase.makeFeatureCaseInsensitive(TrnType.get("Morph"),"base");
     phase.readIn(f,0,f.length-1);
     phase.initialize();
     phase.showAll();
   } catch (FileNotFoundException e) {
     e.printStackTrace();
   } catch (IOException e) {
     e.printStackTrace();
   } catch (ParseException e) {
     e.printStackTrace();
   }

 } */

    public Iterator<Treenotation> iterator() {
        return trns.iterator();
    }

    public void addRelevantFeaturesForType(TrnType tp, HashSet<Integer> set) {
        for (Treenotation t : trns) {
            if (t.getType().equals(tp)) {
                int sz = t.size();
                for (int i = 0; i < sz; i++) {
                    set.add(t.getKey(i));
                }
            }
        }
    }

}


