/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model.dclimpl;

import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.config.context.treenotations.xmlimpl.TreenotationsContextXMLImpl;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.scape.ParseException;
import treeton.core.util.sut;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TrnTypeStorageDclImpl implements TrnTypeStorage {
    public static final int string_FEATURE = 0;
    public static final int length_FEATURE = 1;
    public static final int orthm_FEATURE = 2;
    public static final int start_FEATURE = 3;
    public static final int end_FEATURE = 4;
    private static char[][] keywords = {
            "type".toCharArray(),
            "tokentype".toCharArray(),
            "extends".toCharArray(),
    };
    private TreenotationsContext context;
    private HashMap<String, TrnTypeDclImpl> types = new HashMap<String, TrnTypeDclImpl>();
    private List<TrnTypeDclImpl> typesByIndex = new ArrayList<TrnTypeDclImpl>();
    private int edge = -1;
    private int start;


    public TrnTypeStorageDclImpl() throws TreetonModelException {
    }

    public TrnTypeStorageDclImpl(TreenotationsContextXMLImpl context, TrnTypeStorageDclImpl parent) throws TreetonModelException {
        this.context = context;
        if (parent == null) {
            //register("DEFAULT_TOKEN",new String[0],new Class[0],false,true);
            //register("System",new String[]{"name"},new Class[]{TString.class});
        } else {
            for (TrnTypeDclImpl ot : parent.typesByIndex) {
                TrnTypeDclImpl oldBasis = ot.basisType;
                TrnTypeDclImpl newBasis = null;
                if (oldBasis != null) {
                    newBasis = typesByIndex.get(oldBasis.index);
                }
                TrnTypeDclImpl nt = new TrnTypeDclImpl(this, ot, newBasis);
                typesByIndex.add(nt);
                types.put(nt.getName(), nt);
            }
            edge = typesByIndex.size();
        }
    }

    public TrnType get(String s) {
        TrnType t;
        t = types.get(s);
        return t;
    }

    public TrnType get(int i) {
        return typesByIndex.get(i);
    }

    public TrnTypeDclImpl register(String s, String[] fNames, Class[] fTypes) {
        return register(s, fNames, fTypes, false, false);
    }

    public TrnTypeDclImpl register(String s, String[] fNames, Class[] fTypes, boolean auto, boolean token) {
        TrnTypeDclImpl t;
        t = types.get(s);
        if (t != null) {
            throw new IllegalArgumentException(s);
        }
        float[] hsb = new float[3];
        hsb[0] = (float) Math.random() * 0.8f + 0.1f;
        hsb[1] = (float) Math.random() / 2f + 0.4f;
        hsb[2] = (float) Math.random() / 2f + 0.4f;
        t = new TrnTypeDclImpl(this, s, typesByIndex.size(), fNames, fTypes, auto, token, new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])));
        types.put(s, t);
        typesByIndex.add(t);
        return t;
    }

    public TrnType register(String s, String[] fNames, Class[] fTypes, boolean auto, boolean token, Color c) {
        TrnTypeDclImpl t;
        t = types.get(s);
        if (t != null) {
            throw new IllegalArgumentException(s);
        }
        t = new TrnTypeDclImpl(this, s, typesByIndex.size(), fNames, fTypes, auto, token, c);
        types.put(s, t);
        typesByIndex.add(t);
        return t;
    }

    public TrnType[] getAllTypes() {
        TrnType[] res = new TrnType[types.size()];
        types.values().toArray(res);
        return res;
    }

    private void clear() {
        types.clear();
        typesByIndex.clear();
    }

    public int readIn(char[] s, int pl, int endpl) throws ParseException, TreetonModelException {
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        boolean tokenType = false;
        if (n == 1) { //tokentype
            tokenType = true;
        } else if (n != 0) {
            throw new ParseException("wrong syntax", null, s, pl, endpl);
        }
        pl += keywords[n].length;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing type name", null, s, pl, endpl);
        }
        String N = new String(s, beg, pl - beg);

        TrnTypeDclImpl tp;
        tp = (TrnTypeDclImpl) get(N);
        boolean fromParentDomain = false;
        if (tp == null) {
            tp = register(N, null, null, true, tokenType);
        } else if (tp.index >= edge) {
            throw new ParseException("type " + N + "is already defined in this scope", null, s, pl, endpl);
        } else {
            fromParentDomain = true;
            tp.autoFill = true;
        }

        pl = sut.skipSpacesEndls(s, pl, endpl);

        n = sut.checkDelims(s, pl, endpl, keywords);

        TrnTypeDclImpl basisTp = null;
        Class oldAutofillType = tp.defaultAutoFillType;
        tp.defaultAutoFillType = null;


        if (n == 2) { //extends
            if (fromParentDomain) {
                throw new ParseException("\"extends\" keyword can not be used here because the type is defined in the parent domain", null, s, pl, endpl);
            }
            pl += keywords[n].length;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            beg = pl;
            pl = sut.skipVarName(s, pl, endpl);
            if (pl == beg) {
                throw new ParseException("missing type name", null, s, pl, endpl);
            }
            N = new String(s, beg, pl - beg);
            if ((basisTp = (TrnTypeDclImpl) get(N)) == null) {
                throw new ParseException("type " + N + " is undeclared", null, s, pl, endpl);
            }
            tp.introduction = new IntroductionBlock(tp);
            tp.basisType = basisTp;
            IntroductionBlock.copyIntroductionBlock(tp.introduction, basisTp.introduction);

            pl = sut.skipSpacesEndls(s, pl, endpl);
        }

        if (fromParentDomain) {
            basisTp = tp.basisType;
        }

        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }
        pl++;
        if (tp.introduction == null) {
            tp.introduction = new IntroductionBlock(tp);
            tp.introduction.registerBasis();
        }
        pl = tp.introduction.readIn(s, pl, endpl);
        pl++;
        tp.defaultAutoFillType = oldAutofillType;
        tp.autoFill = false;

        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '}') {
            ValidationTree vtree = new ValidationTree(tp, basisTp);
            pl = vtree.readIn(s, pl, endpl, fromParentDomain);
            if (fromParentDomain) {
                tp.mainHierarchy.root.mergeWith(vtree.root);
            } else {
                tp.hierarchies.put(vtree.name, vtree);
                if (tp.mainHierarchy == null) {
                    tp.mainHierarchy = vtree;
                } else {
                    throw new UnsupportedOperationException("Multiple hierachies are not supported");
                }
            }
            pl++;
            pl = sut.skipSpacesEndls(s, pl, endpl);
            sut.checkEndOfStream(s, pl, endpl);
            if (s[pl] != '}') {
                throw new UnsupportedOperationException("Missing \"}\"");
            }
        }

        return pl;
    }

    public int readInFromChars(char[] arr, int _start) {
        clear();
        start = _start;
        int n = arr[start++];
        ArrayList<TrnTypeDclImpl> list = new ArrayList<TrnTypeDclImpl>();

        for (int i = 0; i < n; i++) {
            TrnTypeDclImpl tp = readInTrnType(arr);
            int id = tp.getIndex();
            while (list.size() <= id) list.add(null);
            list.set(id, tp);
            types.put(tp.getName(), tp);
        }
        typesByIndex = list;
//    typesByIndex=Arrays.asList(byIndex);
        return start;
    }

    private TrnTypeDclImpl readInTrnType(char[] arr) {
        int n = arr[start++];
        String name = new String(arr, start, n);
        start += n;
        boolean autoFill = arr[start++] != 0;
        boolean tokenType = arr[start++] != 0;
        int index = arr[start++];
        int nFeatures = arr[start++];
        List<String> names = new ArrayList<String>();
        List<Class> types = new ArrayList<Class>();
        for (int i = 0; i < nFeatures; i++) {
            n = arr[start++];
            String className = new String(arr, start, n);
            Class c = null;
            try {
                c = Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            start += n;
            n = arr[start++];
            String nm = new String(arr, start, n);
            start += n;

            if (!TrnType.isSystemFeature(nm)) {  //for backward compatibility
                names.add(nm);
                types.add(c);
            }
        }
        int r = arr[start++];
        int g = arr[start++];
        int b = arr[start++];
        int a = arr[start++];

        return new TrnTypeDclImpl(this, name, index, names.toArray(new String[names.size()]), types.toArray(new Class[types.size()]), autoFill, tokenType, new Color(r, g, b, a));
    }

    public int size() {
        return types.size();
    }


    public int hashCode() {
        return context.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof TrnTypeStorageDclImpl && context.equals(((TrnTypeStorageDclImpl) obj).context);
    }
}
