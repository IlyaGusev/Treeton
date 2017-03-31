/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru.prosody;

import treeton.core.*;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeSet;
import treeton.core.model.TrnTypeSetFactory;
import treeton.core.util.ObjectPair;

import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;

public class MeterAnalyzer extends Resource {
    public String process(String text, TextMarkingStorage _storage, Map<String, Object> runtimeParameters) throws ExecutionException {
        assert _storage instanceof TreenotationStorage;

        TreenotationStorage storage = (TreenotationStorage) _storage;

        try {
            TypeIteratorInterface tit = storage.typeIterator(verseTp);
            avtit = storage.typeIterator(accvarTp);
            fit = storage.followIterator(followInputTpSet,accvarTpSet,null);

            while (tit.hasNext()) {
                Treenotation trn = (Treenotation) tit.next();

                processVerse(storage,trn);
            }
            avtit.reset(null,null,null,null);
            avtit = null;

            if (stats != null) {
                System.out.println("Statistics for meter analyzer:\n"+stats);
            }
        } catch (Exception e) {
            processTerminated();
            throw new ExecutionException("Exception during AccentGenerator execution: "+e.getMessage(),e);
        }
        return null;
    }

    TypeIteratorInterface avtit;
    FollowIteratorInterface fit;

    MeterRecognizer externalMeterRecognizer;
    Statistics stats;

    private class Statistics {
        int hits;
        int misses;
        int partialHits;
        int total;

        public String toString() {
            return "Hits: "+hits+"; Misses: "+misses+"; Partial hits: "+partialHits+"; Total: "+total+". Precision is "+
                    (total == 0 ? 0 : ((double)hits+((double)partialHits)/2)/total);
        }
    }

    private void processVerse(TreenotationStorage storage, Treenotation verse) {
        Token start = verse.getStartToken();
        Token end = verse.getEndToken();
        avtit.reset(start,end);
        Treenotation first = null;
        boolean sylFound = false;
        ArrayList<Treenotation> accentVariants = new ArrayList<Treenotation>();
        while (avtit.hasNext()) {
            Treenotation av = (Treenotation) avtit.next();

            if (!(av.getStartToken().compareTo(start)>=0 && av.getEndToken().compareTo(end)<=0)) {
                continue;
            }

            if (first == null || first.getStartToken() == av.getStartToken() && first.getEndToken() == av.getEndToken()) {
                first = av;
                tarr.clear();
                sylFound = true;
                matchPattern(storage, (TreenotationImpl) av, start, end, accentVariants);
            } else {
                break;
            }
        }

        avtit.reset(null,null);
        fit.reset(null,null);
        if (!sylFound) {
            storage.forget(verse);
        } else {
            Map<Integer,Double> dolnikVer = new HashMap<Integer, Double>();
            int iambusVer = 0;
            int trocheusVer = 0;
            int amphibracheusVer = 0;
            int dactilusVer = 0;
            int anapaestusVer = 0;
            for (Treenotation accentVariant : accentVariants) {
                countMetersProbabilities(accentVariant);

                dolnikVer = sum(dolnikVer, (Map<Integer, Double>) accentVariant.get(dolnikVer_feature));
                iambusVer += (Integer) accentVariant.get(iambusVer_feature);
                trocheusVer += (Integer) accentVariant.get(trocheusVer_feature);
                amphibracheusVer += (Integer) accentVariant.get(amphibracheusVer_feature);
                dactilusVer += (Integer) accentVariant.get(dactilusVer_feature);
                anapaestusVer += (Integer) accentVariant.get(anapaestusVer_feature);
            }

            int maxDolnik = 0;
            for (Map.Entry<Integer, Double> entry : dolnikVer.entrySet()) {
                double m = (entry.getValue() * 100) / accentVariants.size();
                entry.setValue(m);
                if (m > maxDolnik)
                    maxDolnik = (int) m;
            }
            iambusVer /= accentVariants.size();
            trocheusVer /= accentVariants.size();
            amphibracheusVer /= accentVariants.size();
            dactilusVer /= accentVariants.size();
            anapaestusVer /= accentVariants.size();

            verse.put(dolnikVer_feature_verse,dolnikVer.toString());
            verse.put(iambusVer_feature_verse,iambusVer);
            verse.put(trocheusVer_feature_verse,trocheusVer);
            verse.put(dactilusVer_feature_verse,dactilusVer);
            verse.put(amphibracheusVer_feature_verse,amphibracheusVer);
            verse.put(anapaestusVer_feature_verse,anapaestusVer);

            int maxVer = Math.max(maxDolnik,Math.max(iambusVer, Math.max(trocheusVer, Math.max(amphibracheusVer, Math.max(dactilusVer, anapaestusVer)))));

            MeterType correctMeter = null;
            if (externalMeterRecognizer != null) {
                correctMeter = externalMeterRecognizer.recognize(verse.getText());
            }

            List<MeterType> recognizedMeters = new ArrayList<MeterType>();
            String meter = "";
            if (maxDolnik == maxVer) {
                recognizedMeters.add(MeterType.dolnik);
            }
            if (iambusVer == maxVer) {
                recognizedMeters.add(MeterType.iambus);
            }
            if (trocheusVer == maxVer) {
                recognizedMeters.add(MeterType.trocheus);
            }

            if (amphibracheusVer == maxVer) {
                recognizedMeters.add(MeterType.amphibracheus);
            }

            if (dactilusVer == maxVer) {
                recognizedMeters.add(MeterType.dactilus);
            }

            if (anapaestusVer == maxVer) {
                recognizedMeters.add(MeterType.anapaestus);
            }

            boolean firstTime = true;
            for (MeterType recognizedMeter : recognizedMeters) {
                if (!firstTime) {
                    meter += "/ ";
                } else {
                    firstTime = false;
                }

                meter += recognizedMeter;
            }

            if (correctMeter != null) {
                stats.total++;
                if (recognizedMeters.contains(correctMeter)) {
                    if (recognizedMeters.size()==1) {
                        stats.hits++;
                    } else {
                        stats.partialHits++;
                    }
                } else {
                    stats.misses++;
                }
            }

            verse.put(meter_feature, TreetonFactory.newTString(meter));
        }
    }

    private void countMetersProbabilities(Treenotation accentVariant) {
        TreenotationImpl av = (TreenotationImpl) accentVariant;
        TreenotationImpl.Node[] words = av.getTrees();

        double iambusVer = 1;
        double trocheusVer = 1;
        double amphibracheusVer = 1;
        double dactilusVer = 1;
        double anapaestusVer = 1;

        int lastAccent = -1;

        int sylCnt = 1;
        for (TreenotationImpl.Node cur : words) {
            if (cur == null)
                continue;
            TreenotationImpl wordAcc = (TreenotationImpl) cur.getTrn();
            int nSyl = wordAcc.getNumberOfTrees();
            if (nSyl > 1) {
                TreenotationImpl.Node[] syllables = wordAcc.getTrees();

                int sylCntBefore = sylCnt;

                for (TreenotationImpl.Node syllable : syllables) {
                    if (syllable == null)
                        continue;

                    if (syllable.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                        lastAccent = sylCnt;

                        int d = sylCnt % 2;
                        if (d == 0) {
                            trocheusVer /= 2;
                        } else {
                            iambusVer /= 2;
                        }

                        boolean mayBeAmphi = false;
                        boolean mayBeDact = false;
                        boolean mayBeAnapaest = false;
                        if (nSyl == 2) {
                            int nWeakSyl = sylCntBefore;
                            for (TreenotationImpl.Node syll : syllables) {
                                if (syll == null)
                                    continue;
                                if (syll.getParentConection() != TreenotationImpl.PARENT_CONNECTION_STRONG) {
                                    break;
                                }
                                nWeakSyl++;
                            }
                            int d1 = nWeakSyl % 3;
                            if (d1 == 0) {
                                mayBeDact = true;
                                mayBeAmphi = true;
                            }
                            if (d1 == 1) {
                                mayBeAnapaest = true;
                                mayBeAmphi = true;
                            }
                            if (d1 == 2) {
                                mayBeDact = true;
                                mayBeAnapaest = true;
                            }
                        }

                        d = sylCnt % 3;
                        if (d == 0) {
                            mayBeAnapaest = true;
                        }
                        if (d == 1) {
                            mayBeDact = true;
                        }
                        if (d == 2) {
                            mayBeAmphi = true;
                        }

                        if (!mayBeAmphi) {
                            amphibracheusVer /= 2;
                        }
                        if (!mayBeDact) {
                            dactilusVer /= 2;
                        }
                        if (!mayBeAnapaest) {
                            anapaestusVer /= 2;
                        }
                    }

                    sylCnt++;
                }

            } else {
                TreenotationImpl.Node[] syllables = wordAcc.getTrees();

                for (TreenotationImpl.Node syllable : syllables) {
                    if (syllable == null)
                        continue;

                    if (syllable.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                        lastAccent = sylCnt;
                    }
                }

                sylCnt += wordAcc.getNumberOfTrees();
            }
        }

        int d = lastAccent % 2;

        if (d == 0) {
            trocheusVer /= 2;
        } else {
            iambusVer /= 2;
        }

        d = lastAccent % 3;

        if (d == 0) {
            dactilusVer/=2;
            amphibracheusVer/=2;
        } else if (d == 1) {
            anapaestusVer/=2;
            amphibracheusVer/=2;
        } else {
            dactilusVer/=2;
            anapaestusVer/=2;
        }


        accentVariant.put(iambusVer_feature,(int)(iambusVer*100));
        accentVariant.put(trocheusVer_feature,(int)(trocheusVer*100));
        accentVariant.put(anapaestusVer_feature,(int)(anapaestusVer*100));
        accentVariant.put(dactilusVer_feature,(int)(dactilusVer*100));
        accentVariant.put(amphibracheusVer_feature,(int)(amphibracheusVer*100));

        choosedAccents.clear();
        ObjectPair<Integer, Integer> pair = findLast(words);
        if (pair != null) {
            choosedAccents.push(pair);
        }
        accentVariant.put(dolnikVer_feature,pair == null ? new HashMap<Integer,Double>() : countDolnikProbability(words,pair.getFirst(),pair.getSecond()));
    }

    private ObjectPair<Integer,Integer> findLast(TreenotationImpl.Node[] words) {
        int wordNumber = words.length - 1;
        int syllNumber = -1;

        while (wordNumber >= 0) {
            TreenotationImpl wordAcc = (TreenotationImpl) words[wordNumber].getTrn();
            if (syllNumber == -1) {
                syllNumber = wordAcc.getNumberOfTrees()-1;
            }

            TreenotationImpl.Node syll = wordAcc.getTrees()[syllNumber];

            if (syll.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                return new ObjectPair<Integer, Integer>(wordNumber,syllNumber);
            }

            if (syllNumber > 0) {
                syllNumber--;
            } else {
                wordNumber--;
                syllNumber=-1;
            }
        }
        return null;
    }

    private Stack<ObjectPair<Integer,Integer>> choosedAccents = new Stack<ObjectPair<Integer,Integer>>();

    private Map<Integer,Double> countDolnikProbability(TreenotationImpl.Node[] words, int wordNumber, int syllNumber) {
        if (syllNumber > 0) {
            syllNumber--;
        } else {
            wordNumber--;
            syllNumber=-1;
        }
        if (wordNumber<0) {
            return finalizeDolnik(words);
        } else {
            TreenotationImpl wordAcc = (TreenotationImpl) words[wordNumber].getTrn();
            if (syllNumber == -1) {
                syllNumber = wordAcc.getNumberOfTrees()-1;
            }

            if (syllNumber > 0) {
                syllNumber--;
            } else {
                wordNumber--;
                syllNumber=-1;
            }
            if (wordNumber<0) {
                return finalizeDolnik(words);
            }

            wordAcc = (TreenotationImpl) words[wordNumber].getTrn();
            if (syllNumber == -1) {
                syllNumber = wordAcc.getNumberOfTrees()-1;
            }

            TreenotationImpl.Node syll = wordAcc.getTrees()[syllNumber];
            boolean isAcc = syll.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG;

            Map<Integer, Double> res = null;
            if (isAcc) {
                choosedAccents.push(new ObjectPair<Integer, Integer>(wordNumber,syllNumber));
                res = countDolnikProbability(words,wordNumber,syllNumber);
                choosedAccents.pop();
//                if (wordAcc.getTrees().length>1)
//                    return res;
            }
            int syllNumber1;
            int wordNumber1;
            if (syllNumber > 0) {
                syllNumber1=syllNumber-1;
                wordNumber1=wordNumber;
            } else {
                wordNumber1=wordNumber-1;
                syllNumber1=-1;
            }

            if (wordNumber1<0) {
                choosedAccents.push(new ObjectPair<Integer, Integer>(wordNumber,syllNumber));
                Map<Integer, Double> res1 = countDolnikProbability(words,wordNumber,syllNumber);
                choosedAccents.pop();
                Map<Integer, Double> res2 = finalizeDolnik(words);
                return merge(merge(res,res1),res2);
            } else {
                TreenotationImpl wordAcc1 = (TreenotationImpl) words[wordNumber1].getTrn();
                if (syllNumber1 == -1) {
                    syllNumber1 = wordAcc1.getNumberOfTrees()-1;
                }

                TreenotationImpl.Node syll1 = wordAcc1.getTrees()[syllNumber1];
                isAcc = syll1.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG;

                if (isAcc) {
                    choosedAccents.push(new ObjectPair<Integer, Integer>(wordNumber1,syllNumber1));
                    Map<Integer, Double> res1 = countDolnikProbability(words,wordNumber1,syllNumber1);
                    choosedAccents.pop();
                    return merge(res,res1);
                } else {
                    choosedAccents.push(new ObjectPair<Integer, Integer>(wordNumber,syllNumber));
                    Map<Integer, Double> res1 = countDolnikProbability(words,wordNumber,syllNumber);
                    choosedAccents.pop();
                    choosedAccents.push(new ObjectPair<Integer, Integer>(wordNumber1,syllNumber1));
                    Map<Integer, Double> res2 = countDolnikProbability(words,wordNumber1,syllNumber1);
                    choosedAccents.pop();
                    return merge(merge(res1,res2),res);
                }
            }
        }
    }

    private Map<Integer, Double> merge(Map<Integer, Double> res1, Map<Integer, Double> res2) {
        if (res1 == null)
            return res2;
        if (res2 == null)
            return res1;

        Map<Integer, Double> res = new HashMap<Integer, Double>();

        for (Map.Entry<Integer, Double> entry : res1.entrySet()) {
            Double d = res2.get(entry.getKey());
            if (d != null) {
                res.put(entry.getKey(),Math.max(entry.getValue(),d));
            } else {
                res.put(entry.getKey(),entry.getValue());
            }
        }

        for (Map.Entry<Integer, Double> entry : res2.entrySet()) {
            if (!res1.containsKey(entry.getKey())) {
                res.put(entry.getKey(),entry.getValue());
            }
        }

        return res;
    }

    private Map<Integer, Double> sum(Map<Integer, Double> res1, Map<Integer, Double> res2) {
        Map<Integer, Double> res = new HashMap<Integer, Double>();

        for (Map.Entry<Integer, Double> entry : res1.entrySet()) {
            Double d = res2.get(entry.getKey());
            if (d != null) {
                res.put(entry.getKey(),entry.getValue()+d);
            } else {
                res.put(entry.getKey(),entry.getValue());
            }
        }

        for (Map.Entry<Integer, Double> entry : res2.entrySet()) {
            if (!res1.containsKey(entry.getKey())) {
                res.put(entry.getKey(),entry.getValue());
            }
        }

        return res;
    }

    private Map<Integer,Double> finalizeDolnik(TreenotationImpl.Node[] words) {
        Set<ObjectPair<Integer,Integer>> set = new HashSet<ObjectPair<Integer, Integer>>();

        Map<Integer,Double> res = new HashMap<Integer, Double>();

        double d = 1;

        for (ObjectPair<Integer, Integer> choosedAccent : choosedAccents) {
            TreenotationImpl wordAcc = (TreenotationImpl) words[choosedAccent.getFirst()].getTrn();
            if (wordAcc.getTrees().length==1)
                continue;

            for (int i=0;i<wordAcc.getTrees().length;i++) {
                TreenotationImpl.Node syll = wordAcc.getTrees()[i];
                if(syll.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                    set.add(new ObjectPair<Integer, Integer>(choosedAccent.getFirst(),i));
                }
            }
        }

        set.removeAll(choosedAccents);

        for (int i=0;i<set.size();i++) {
            d /= 2;
        }

        res.put(choosedAccents.size(),d);

        return res;
    }

    private void matchPattern(TreenotationStorage storage, TreenotationImpl accVariant, Token start, Token end, ArrayList<Treenotation> accentVariants) {
        tarr.add(accVariant);

        fit.reset(accvarTpSet,accVariant.getEndToken());
        ArrayList<TreenotationImpl> arr = new ArrayList<TreenotationImpl>();
        while (fit.hasNext()) {
            TreenotationImpl av = (TreenotationImpl) fit.next();
            if (!(av.getStartToken().compareTo(start)>=0 && av.getEndToken().compareTo(end)<=0)) {
                continue;
            }
            arr.add(av);
        }
        for (TreenotationImpl av : arr) {
            matchPattern(storage,av,start,end,accentVariants);
        }
        if (arr.size()==0) {
            TreenotationSyntax newAccentVariant = (TreenotationSyntax) TreetonFactory.newSyntaxTreenotation(storage,start,end,accvarTp);
            for (TreenotationImpl trn : tarr) {
                newAccentVariant.addTree(new TreenotationImpl.Node(TreenotationImpl.PARENT_CONNECTION_WEAK, trn));
            }
            accentVariants.add(newAccentVariant);
            storage.add(newAccentVariant);
        }

        tarr.remove(accVariant);
    }

    TrnType verseTp;
    TrnType syllTp;
    TrnType accvarTp;
    TrnTypeSet accvarTpSet;
    TrnTypeSet followInputTpSet;

    int meter_feature;
    int form_feature;
    int nVariants_feature;
    int AccentVariantArr_feature;
    int MorphArr_feature;

    int dolnikVer_feature;
    int iambusVer_feature;
    int trocheusVer_feature;
    int amphibracheusVer_feature;
    int dactilusVer_feature;
    int anapaestusVer_feature;

    int dolnikVer_feature_verse;
    int iambusVer_feature_verse;
    int trocheusVer_feature_verse;
    int amphibracheusVer_feature_verse;
    int dactilusVer_feature_verse;
    int anapaestusVer_feature_verse;

    ArrayList<TreenotationImpl> tarr;

    public void init() throws ResourceInstantiationException {
        try {
            verseTp = getTrnContext().getType("Verse");
            syllTp = getTrnContext().getType("Syllable");
            accvarTp = getTrnContext().getType("AccVariant");

            dolnikVer_feature = accvarTp.getFeatureIndex(MeterType.dolnik+"Ver");
            iambusVer_feature = accvarTp.getFeatureIndex(MeterType.iambus+"Ver");
            trocheusVer_feature = accvarTp.getFeatureIndex(MeterType.trocheus+"Ver");
            amphibracheusVer_feature = accvarTp.getFeatureIndex(MeterType.amphibracheus+"Ver");
            dactilusVer_feature = accvarTp.getFeatureIndex(MeterType.dactilus+"Ver");
            anapaestusVer_feature = accvarTp.getFeatureIndex(MeterType.anapaestus+"Ver");

            dolnikVer_feature_verse = verseTp.getFeatureIndex(MeterType.dolnik+"Ver");
            iambusVer_feature_verse = verseTp.getFeatureIndex(MeterType.iambus+"Ver");
            trocheusVer_feature_verse = verseTp.getFeatureIndex(MeterType.trocheus+"Ver");
            amphibracheusVer_feature_verse = verseTp.getFeatureIndex(MeterType.amphibracheus+"Ver");
            dactilusVer_feature_verse = verseTp.getFeatureIndex(MeterType.dactilus+"Ver");
            anapaestusVer_feature_verse = verseTp.getFeatureIndex(MeterType.anapaestus+"Ver");

            meter_feature = verseTp.getFeatureIndex("meter");
            form_feature = verseTp.getFeatureIndex("form");
            nVariants_feature = verseTp.getFeatureIndex("nVariants");
            AccentVariantArr_feature = verseTp.getFeatureIndex("AccentVariantArr");

            MorphArr_feature = accvarTp.getFeatureIndex("MorphArr");

            TrnTypeSetFactory factory = new TrnTypeSetFactory();
            accvarTpSet = factory.newTrnTypeSet(new TrnType[]{accvarTp},1);
            followInputTpSet = factory.newTrnTypeSet(new TrnType[]{accvarTp},1);

            tarr = new ArrayList<TreenotationImpl>();

            try {
                String spec = (String) getInitialParameters().get("benchmarkPath");
                if (spec != null) {
                    URL path = new URL(getResContext().getFolder(), spec);
                    externalMeterRecognizer = new MeterRecognizerFileImpl(new File(path.getPath()),";");
                    stats = new Statistics();
                }
            } catch (MalformedURLException e) {
                throw new ResourceInstantiationException("Malformed url exception during StarlingMorphApplier instantiation",e);
            }

        } catch (Exception e) {
            deInit();
            throw new ResourceInstantiationException(null,"Exception during AccentGenerator instantiation: "+e.getMessage(),e);
        }
    }

    public void deInit() {
        tarr = null;
    }

    public void stop() {
    }

    public void processTerminated() {
        avtit.reset(null,null,null,null);
        fit.reset(null,null,null);
        fit = null;
        tarr = new ArrayList<TreenotationImpl>();
        avtit = null;
    }
}