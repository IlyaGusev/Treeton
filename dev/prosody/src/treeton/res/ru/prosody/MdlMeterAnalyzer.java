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
import treeton.prosody.mdlcompiler.MdlEngine;
import treeton.prosody.mdlcompiler.fsm.Meter;
import treeton.prosody.mdlcompiler.fsm.MeterStatistics;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class MdlMeterAnalyzer extends Resource {
    public String process(String text, TextMarkingStorage _storage, Map<String, Object> runtimeParameters) throws ExecutionException {
        assert _storage instanceof TreenotationStorage;

        TreenotationStorage storage = (TreenotationStorage) _storage;

        try {
            TypeIteratorInterface tit = storage.typeIterator(verseTp);
            avtit = storage.typeIterator(accvarTp);
            fit = storage.followIterator(followInputTpSet,accvarTpSet,null);

            MeterStatistics stats = new MeterStatistics();

            while (tit.hasNext()) {
                Treenotation trn = (Treenotation) tit.next();

                MeterStatistics s = processVerse(storage,trn);
                stats.merge(s, MeterStatistics.MergeMode.AVERAGE);
            }

            Map<Meter, Set<Integer>> recognizedMeters = analyzeMeterStatistics(stats);
            ObjectPair<Meter,Integer> mainMeter = chooseMainMeter(stats,recognizedMeters);

            String mainMeterString = null;

            if (mainMeter != null) {
                Set<Integer> s = recognizedMeters.get(mainMeter.getFirst());
                s.remove(mainMeter.getSecond());

                if (s.size()==0) {
                    recognizedMeters.remove(mainMeter.getFirst());
                }

                mainMeterString = mainMeter.getFirst().getName() + ":" + mainMeter.getSecond();
            }



            StringBuffer meter = new StringBuffer();

            boolean firstTime = true;
            for (Map.Entry<Meter, Set<Integer>> entry : recognizedMeters.entrySet()) {
                for (Integer nFoots : entry.getValue()) {
                    if (!firstTime) {
                        meter.append("/ ");
                    } else {
                        firstTime = false;
                    }

                    meter.append(entry.getKey().getName()).append(":").append(nFoots);
                }
            }

            showResultsFrame(mainMeterString,meter.toString(),stats);

            tit = storage.typeIterator(verseTp);

            while (tit.hasNext()) {
                Treenotation trn = (Treenotation) tit.next();
                trn.put(meter_feature, TreetonFactory.newTString(mainMeterString != null ? mainMeterString : meter.toString()));
            }

            avtit.reset(null,null,null,null);
            avtit = null;
//
//            if (stats != null) {
//                System.out.println("Statistics for meter analyzer:\n"+stats);
//            }
        } catch (Exception e) {
            processTerminated();
            throw new ExecutionException("Exception during AccentGenerator execution: "+e.getMessage(),e);
        }
        return null;
    }

    private ObjectPair<Meter, Integer> chooseMainMeter(MeterStatistics stats, Map<Meter, Set<Integer>> recognizedMeters) {
        Map<Meter, Set<Integer>> temp = new HashMap<Meter, Set<Integer>>();

        double minUnstressed=100;

        for (Map.Entry<Meter, Set<Integer>> entry : recognizedMeters.entrySet()) {
            for (Integer integer : entry.getValue()) {
                Double u = stats.getUnstressed(entry.getKey(),integer);
                if (u != null && u < minUnstressed) {
                    minUnstressed = u;
                }
            }
        }

        for (Map.Entry<Meter, Set<Integer>> entry : recognizedMeters.entrySet()) {
            Meter m = entry.getKey();

            for (Integer integer : entry.getValue()) {
                Double u = stats.getUnstressed(entry.getKey(),integer);
                if ( u != null && Math.abs(u-minUnstressed) <= 0.03 ) {
                    Set<Integer> set = temp.get(m);
                    if (set == null) {
                        set = new HashSet<Integer>();
                        temp.put(m,set);
                    }
                    set.add(integer);
                }
            }
        }

        double minOverstressed=100;

        for (Map.Entry<Meter, Set<Integer>> entry : temp.entrySet()) {
            for (Integer integer : entry.getValue()) {
                Double u = stats.getOverstressed(entry.getKey(),integer);
                if (u != null && u < minOverstressed) {
                    minOverstressed = u;
                }
            }
        }

        Set<Meter> toDelete = new HashSet<Meter>();

        for (Map.Entry<Meter, Set<Integer>> entry : temp.entrySet()) {
            Iterator<Integer> it = entry.getValue().iterator();
            while (it.hasNext()) {
                Integer i = it.next();
                Double u = stats.getOverstressed(entry.getKey(),i);

                if (u == null || Math.abs(u-minOverstressed) > 0.03 ) {
                    it.remove();
                }
            }

            if (entry.getValue().size()==0)
                toDelete.add(entry.getKey());
        }

        for (Meter meter : toDelete) {
            temp.remove(meter);
        }

        if (temp.size()==1) {
            Map.Entry<Meter, Set<Integer>> e = temp.entrySet().iterator().next();

            if (e.getValue().size()==1) {
                return new ObjectPair<Meter, Integer>(e.getKey(),e.getValue().iterator().next());
            } else {
                return null;
            }
        }

        return null;
    }

    private void showResultsFrame(String result, String alternatives, MeterStatistics stats) {
        JFrame frame = new JFrame("Analysis results");

        JScrollPane jscr = new  JScrollPane();
        jscr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jscr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JTextPane text = new JTextPane();
        text.setContentType("text/html");
        text.setEditable(false);

        jscr.getViewport().add(text,null);

        stats.setToStringFontSize(7);

        StringBuffer buf = new StringBuffer();

        buf.append("<table align=\"center\"><tr><td><font size=\"8\">Результат: <b>");
        if (result != null) {
            buf.append(result);
        } else {
            buf.append(alternatives);
        }
        buf.append("</b></font></td></tr></table><br>");

        if (result != null && alternatives.length() > 0) {
            buf.append("<table align=\"center\"><tr><td><font size=\"8\">Альтернативные варианты: <b>");
            buf.append(alternatives);
            buf.append("</b></font></td></tr></table><br>");
        }

        buf.append(stats);

        text.setText(buf.toString());

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setContentPane(jscr);
        frame.pack();
        frame.setResizable(true);
        frame.setBounds(0, 0, screenSize.width, screenSize.height);

        frame.setVisible(true);
    }

    TypeIteratorInterface avtit;
    FollowIteratorInterface fit;

//    MeterRecognizer externalMeterRecognizer;

//    private class Statistics {
//        int hits;
//        int misses;
//        int partialHits;
//        int total;
//
//        public String toString() {
//            return "Hits: "+hits+"; Misses: "+misses+"; Partial hits: "+partialHits+"; Total: "+total+". Precision is "+
//                    (total == 0 ? 0 : ((double)hits+((double)partialHits)/2)/total);
//        }
//    }

    private MeterStatistics processVerse(TreenotationStorage storage, Treenotation verse) {
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

        MeterStatistics stats = new MeterStatistics();

        avtit.reset(null,null);
        fit.reset(null,null);
        if (!sylFound) {
            storage.forget(verse);
        } else {
            for (Treenotation accentVariant : accentVariants) {
                MeterStatistics accVarStats = countMetersProbabilities(accentVariant);
                accentVariant.put(stats_feature,accVarStats);
                stats.merge(accVarStats, MeterStatistics.MergeMode.AVERAGE);
            }

            verse.put(stats_feature_verse,stats);

//            MeterType correctMeter = null;
//            if (externalMeterRecognizer != null) {
//                correctMeter = externalMeterRecognizer.recognize(verse.getText());
//            }


//            if (correctMeter != null) {
//                stats.total++;
//                if (recognizedMeters.contains(correctMeter)) {
//                    if (recognizedMeters.size()==1) {
//                        stats.hits++;
//                    } else {
//                        stats.partialHits++;
//                    }
//                } else {
//                    stats.misses++;
//                }
//            }

        }

        return stats;
    }

    private Map<Meter, Set<Integer>> analyzeMeterStatistics(MeterStatistics stats) {
        Map<Meter, Set<Integer>> map = stats.findMetersAboveThreshold();
        if (map.size()==0) {
            map = stats.findBestMeters();
        }

        int minPriority = -1;

        for (Meter meter : map.keySet()) {
            if (minPriority == -1 || meter.getPriority() < minPriority) {
                minPriority = meter.getPriority();
            }
        }

        Iterator<Map.Entry<Meter, Set<Integer>>> it = map.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Meter, Set<Integer>> e = it.next();

            if (e.getKey().getPriority() != minPriority) {
                it.remove();
            }
        }

        return map;
    }

    private MeterStatistics countMetersProbabilities(Treenotation accentVariant) {
        MeterStatistics result = new MeterStatistics();

        TreenotationImpl av = (TreenotationImpl) accentVariant;
        TreenotationImpl.Node[] words = av.getTrees();

        int nSyllables = 0;

        for (TreenotationImpl.Node cur : words) {
            if (cur == null)
                continue;

            TreenotationImpl wordAcc = (TreenotationImpl) cur.getTrn();

            TreenotationImpl.Node[] syllables = wordAcc.getTrees();

            for (TreenotationImpl.Node syllable : syllables) {
                if (syllable == null)
                    continue;

                nSyllables++;
            }
        }

        final boolean[] stresses = new boolean[nSyllables];
        final int[] wordNumbers = new int[nSyllables];

        int i=0,w=0;

        for (TreenotationImpl.Node cur : words) {
            if (cur == null)
                continue;

            TreenotationImpl wordAcc = (TreenotationImpl) cur.getTrn();

            TreenotationImpl.Node[] syllables = wordAcc.getTrees();

            for (TreenotationImpl.Node syllable : syllables) {
                if (syllable == null)
                    continue;

                stresses[i]=syllable.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG;
                wordNumbers[i++]=w;
            }
            w++;
        }


        for (Meter meter : mdlEngine) {
            //TODO перейти здесь на новый Input

            /*MeterMatcher matcher = new MeterMatcher(meter,input,stressRestrictionViolationWeight,reaccentuationRestrictionViolationWeight,10,10,mdlEngine.isReverseAutomata());
            int n = matcher.getNumberOfResults();
            for ( int j=0;j<n;j++ ) {
                double p = 1 - matcher.getPenalty(j);
                result.add(meter,matcher.getNumberOfStressedSyllables(j), p * 100,matcher.getOverStressed(j) * p, matcher.getUnstressed(j) * p);
            }*/
        }

        return result;
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
    int stats_feature;
    int stats_feature_verse;
    int MorphArr_feature;

    double stressRestrictionViolationWeight;
    double reaccentuationRestrictionViolationWeight;

    ArrayList<TreenotationImpl> tarr;
    MdlEngine mdlEngine;

    public void init() throws ResourceInstantiationException {
        try {
            verseTp = getTrnContext().getType("Verse");
            syllTp = getTrnContext().getType("Syllable");
            accvarTp = getTrnContext().getType("AccVariant");

            meter_feature = verseTp.getFeatureIndex("meter");
            form_feature = verseTp.getFeatureIndex("form");
            stats_feature_verse = verseTp.getFeatureIndex("stats");

            MorphArr_feature = accvarTp.getFeatureIndex("MorphArr");
            stats_feature = accvarTp.getFeatureIndex("stats");

            TrnTypeSetFactory factory = new TrnTypeSetFactory();
            accvarTpSet = factory.newTrnTypeSet(new TrnType[]{accvarTp},1);
            followInputTpSet = factory.newTrnTypeSet(new TrnType[]{accvarTp},1);

            tarr = new ArrayList<TreenotationImpl>();

            URL path;
            try {
                path = new URL(getResContext().getFolder(), (String) getInitialParameters().get("grammarPath"));
            } catch (MalformedURLException e) {
                throw new ResourceInstantiationException("Malformed url exception during MdlMeterAnalyzer instantiation",e);
            }

            Object o = getInitialParameters().get("stressRestrictionViolationWeight");
            stressRestrictionViolationWeight = Double.valueOf(o.toString());

            o = getInitialParameters().get("reaccentuationRestrictionViolationWeight");
            reaccentuationRestrictionViolationWeight = Double.valueOf(o.toString());


            mdlEngine = new MdlEngine(path.getPath(),true);

//            try {
//                String spec = (String) getInitialParameters().get("benchmarkPath");
//                if (spec != null) {
//                    URL path = new URL(getResContext().getFolder(), spec);
//                    externalMeterRecognizer = new MeterRecognizerFileImpl(new File(path.getPath()),";");
//                    stats = new Statistics();
//                }
//            } catch (MalformedURLException e) {
//                throw new ResourceInstantiationException("Malformed url exception during StarlingMorphApplier instantiation",e);
//            }

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