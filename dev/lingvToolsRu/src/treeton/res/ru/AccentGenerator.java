/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.*;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;

import java.util.*;

public class AccentGenerator extends Resource {
    Treenotation[] mappingArr;
    TypeIteratorInterface stit;
    TrnType morphTp;
    TrnType syllTp;
    TrnType accvarTp;
    TrnType phonWordTp;
    int ACCPL_feature;
    int POS_feature;
    int Klitik_feature;
    int MorphArr_feature;
    ArrayList<Treenotation> tarr;
    private Set<TString> klitiks;

    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        mappingArr = new Treenotation[200];
        ArrayList<Treenotation> arr = new ArrayList<Treenotation>();
        TypeIteratorInterface tit = storage.typeIterator(morphTp);
        stit = storage.typeIterator(syllTp);

        Token lastEnd = null;
        while (tit.hasNext()) {
            Treenotation trn = (Treenotation) tit.next();

            if (arr.size() > 0 && (trn.getStartToken() != arr.get(0).getStartToken() || trn.getEndToken() != arr.get(0).getEndToken())) {
                generateAccentVariants(storage, arr);
                lastEnd = arr.get(0).getEndToken();
                arr.clear();
            }

            if (lastEnd != null && trn.getStartToken().compareTo(lastEnd) <= 0) {
                continue;
            }

            arr.add(trn);
        }
        if (arr.size() > 0) {
            generateAccentVariants(storage, arr);
            arr.clear();
        }
        stit.reset(null, null, null, null);
        stit = null;
        mappingArr = null;
        return null;
    }

    protected void generateAccentVariants(TreenotationStorage storage, ArrayList<Treenotation> arr) {
        Treenotation trn = arr.get(0);
        char[] form = trn.getText().toCharArray();
        ensureMappingCapacity(form);
        for (int i = 0; i < form.length; i++) {
            mappingArr[i] = null;
        }
        stit.reset(trn.getStartToken(), trn.getEndToken());
        int shift = 0;
        int nSylls = 0;
        while (stit.hasNext()) {
            Treenotation syll = (Treenotation) stit.next();
            nSylls++;
            int startOffs = syll.getStartNumerator() / syll.getStartDenominator();
            int endOffs = syll.getEndNumerator() / syll.getEndDenominator();
            int i = startOffs;
            while (i < endOffs) {
                mappingArr[i - startOffs + shift] = syll;
                i++;
            }
            shift += endOffs - startOffs;
        }

        if (nSylls == 0) {
            return;
        }

        boolean klitikFound = false;
        boolean noAccentFound = false;
        for (Treenotation t : arr) {
            Integer accpl = (Integer) t.get(ACCPL_feature);
            if (accpl != null && accpl >= 0 && accpl < form.length) {
                form[accpl] = 0x0;
            } else if (accpl != null && accpl == -1) {
                noAccentFound = true;
            }

            if (klitikFound) {
                continue;
            }

            if (t.get(Klitik_feature) != null) {
                klitikFound = true;
            }

            if (klitikFound) {
                continue;
            }

            Object pos = t.get(POS_feature);
            if (isKlitik(pos)) {
                klitikFound = true;
            }
        }

        boolean variantFound = false;
        for (int i = 0; i < form.length; i++) {
            if (form[i] == 0x0) {
                TreenotationImpl variant = (TreenotationImpl) TreetonFactory.newSyntaxTreenotation(storage, trn.getStartToken(), trn.getEndToken(), accvarTp);
                Treenotation accented = mappingArr[i];
                for (int j = 0; j < form.length; j++) {
                    Treenotation syl = mappingArr[j];
                    if (syl == null) {
                        continue;
                    }
                    if (j == 0 || syl != mappingArr[j - 1]) {
                        variant.addTree(new TreenotationImpl.Node(
                                accented == syl ? TreenotationImpl.PARENT_CONNECTION_STRONG : TreenotationImpl.PARENT_CONNECTION_WEAK,
                                (TreenotationImpl) syl));
                    }
                }
                tarr.clear();
                for (Treenotation t : arr) {
                    Integer accpl = (Integer) t.get(ACCPL_feature);
                    if (accpl != null && accpl == i) {
                        tarr.add(t);
                    }
                }
                variant.put(MorphArr_feature, tarr.toArray());
                variantFound = true;
                variant.lock();
                storage.add(variant);
            }
        }
        if (nSylls == 1 && klitikFound || noAccentFound && !variantFound) {
            TreenotationImpl variant = (TreenotationImpl) TreetonFactory.newSyntaxTreenotation(storage, trn.getStartToken(), trn.getEndToken(), accvarTp);

            for (int k = 0; k < form.length; k++) {
                Treenotation syl = mappingArr[k];
                if (syl == null) {
                    continue;
                }
                if (k == 0 || syl != mappingArr[k - 1]) {
                    variant.addTree(new TreenotationImpl.Node(
                            TreenotationImpl.PARENT_CONNECTION_WEAK,
                            (TreenotationImpl) syl));
                }
            }

            variantFound = true;
            tarr.clear();
            for (Treenotation t : arr) {
                Integer accpl = (Integer) t.get(ACCPL_feature);
                Object pos = t.get(POS_feature);
                if (accpl == null || accpl == -1 || isKlitik(pos) || t.get(Klitik_feature) != null) {
                    tarr.add(t);
                }
            }
            variant.put(MorphArr_feature, tarr.toArray());

            variant.lock();
            storage.add(variant);
        }
        if (!variantFound) {
            for (int j = 0; j < form.length; j++) {
                Treenotation accented = mappingArr[j];
                if (j == 0 || accented != mappingArr[j - 1]) {
                    TreenotationImpl variant = (TreenotationImpl) TreetonFactory.newSyntaxTreenotation(storage, trn.getStartToken(), trn.getEndToken(), accvarTp);
                    for (int k = 0; k < form.length; k++) {
                        Treenotation syl = mappingArr[k];
                        if (syl == null) {
                            continue;
                        }
                        if (k == 0 || syl != mappingArr[k - 1]) {
                            variant.addTree(new TreenotationImpl.Node(
                                    accented == syl ? TreenotationImpl.PARENT_CONNECTION_STRONG : TreenotationImpl.PARENT_CONNECTION_WEAK,
                                    (TreenotationImpl) syl));
                        }
                    }
                    variant.put(MorphArr_feature, arr.toArray());
                    variant.lock();
                    storage.add(variant);
                }
            }
        }

        Treenotation phonWord = TreetonFactory.newTreenotation(trn.getStartToken(), trn.getEndToken(), phonWordTp);
        storage.add(phonWord);
    }

    private boolean isKlitik(Object pos) {
        //noinspection SuspiciousMethodCalls
        return klitiks.contains(pos);
    }

    private void ensureMappingCapacity(char[] form) {
        if (form.length > mappingArr.length) {
            mappingArr = new Treenotation[form.length];
        }
    }

    protected void init() throws ResourceInstantiationException {
        ACCPL_feature = -1;
        POS_feature = -1;
        MorphArr_feature = -1;
        Klitik_feature = -1;
        try {
            morphTp = getTrnContext().getType((String) getInitialParameters().get("Morph_type"));
            ACCPL_feature = morphTp.getFeatureIndex((String) getInitialParameters().get("ACCPL_feature"));
            POS_feature = morphTp.getFeatureIndex((String) getInitialParameters().get("POS_feature"));
            Klitik_feature = morphTp.getFeatureIndex((String) getInitialParameters().get("Klitik_feature"));
            accvarTp = getTrnContext().getType((String) getInitialParameters().get("AccVariant_type"));
            MorphArr_feature = accvarTp.getFeatureIndex((String) getInitialParameters().get("MorphArr_feature"));
            syllTp = getTrnContext().getType((String) getInitialParameters().get("Syllable_type"));
            phonWordTp = getTrnContext().getType((String) getInitialParameters().get("PhonWord_type"));

            klitiks = new HashSet<TString>();

            List list = (List) getInitialParameters().get("klitik_POSes");
            if (list != null) {
                for (Object s : list) {
                    klitiks.add(TreetonFactory.newTString((String) s));
                }
            }
        } catch (TreetonModelException e) {
            throw new ResourceInstantiationException("Error with model", e);
        }

        tarr = new ArrayList<Treenotation>();
    }

    public void deInit() {
        tarr = null;
        mappingArr = null;
    }


    public void stop() {
    }

    public void processTerminated() {
        stit.reset(null, null, null, null);
        tarr = new ArrayList<Treenotation>();
        mappingArr = null;
        stit = null;
    }
}
