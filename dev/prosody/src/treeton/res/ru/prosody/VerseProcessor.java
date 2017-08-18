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

import java.util.ArrayList;
import java.util.Map;

public class VerseProcessor extends Resource {
    public String process(String text, TextMarkingStorage _storage, Map<String, Object> runtimeParameters) throws ExecutionException {
        assert _storage instanceof TreenotationStorage;

        TreenotationStorage storage = (TreenotationStorage) _storage;

        try {
            TypeIteratorInterface tit = storage.typeIterator(verseTp);
            avtit = storage.typeIterator(accvarTp);
            fit = storage.followIterator(followInputTpSet,accvarTpSet,null);

            while (tit.hasNext()) {
                Treenotation trn = (Treenotation) tit.next();

                trn.put(meter_feature,TreetonFactory.newTString("iambus"));

                processVerse(storage,trn,matchPattern_Iambus);
            }
            avtit.reset(null,null,null,null);
            avtit = null;
        } catch (Exception e) {
            processTerminated();
            throw new ExecutionException("Exception during AccentGenerator execution: "+e.getMessage(),e);
        }
        return null;
    }

    TypeIteratorInterface avtit;
    FollowIteratorInterface fit;

    private void processVerse(TreenotationStorage storage, Treenotation verse, PatternNode[] pattern) {
        Token start = verse.getStartToken();
        Token end = verse.getEndToken();
        avtit.reset(start,end);
        Treenotation first = null;
        boolean recognized = false;
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
                recognized = matchPattern(storage, (TreenotationImpl) av, pattern, 0, start, end, accentVariants) || recognized;
            } else {
                break;
            }
        }

        avtit.reset(null,null);
        fit.reset(null,null);
        if (!sylFound) {
            storage.forget(verse);
        } else if (!recognized) {
            verse.put(nVariants_feature,0);
            verse.put(meter_feature,"unknown");
            verse.put(form_feature,"unknown");
        } else {
            verse.put(AccentVariantArr_feature, accentVariants.toArray());
            verse.put(nVariants_feature,accentVariants.size());
            String form = detectForm((TreenotationImpl) verse);
            if ("unknown".equals(form))
                verse.put(meter_feature,"unknown");
            verse.put(form_feature,form);
        }
    }

    private String detectForm(TreenotationImpl verse) {
        String form = null;

        Object[] nodes = (Object[]) verse.get(AccentVariantArr_feature);
        for (Object node : nodes) {
            if (node == null)
                continue;
            TreenotationImpl accVar = (TreenotationImpl) node;
            TreenotationImpl.Node[] children = accVar.getTrees();
            boolean s2=false,s4=false,s6=false,s8=false;
            int cnt = 0;

            for (TreenotationImpl.Node child : children) {
                if (child == null)
                    continue;
                TreenotationImpl accVar1 = (TreenotationImpl) child.getTrn();
                TreenotationImpl.Node[] children1 = accVar1.getTrees();
                for (TreenotationImpl.Node child1 : children1) {
                    if (child1 == null)
                        continue;
                    cnt++;
                    if (cnt==2) {
                        s2 = child1.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG;
                    } else if (cnt==4) {
                        s4 = child1.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG;
                    } else if (cnt==6) {
                        s6 = child1.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG;
                    } else if (cnt==8) {
                        s8 = child1.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG;
                    }
                }
            }
            String nForm;
            if (s2 && s4 && s6 && s8) {
                nForm = "I";
            } else if (!s2 && s4 && s6 && s8) {
                nForm = "II";
            } else if (s2 && !s4 && s6 && s8) {
                nForm = "III";
            } else if (s2 && s4 && !s6 && s8) {
                nForm = "IV";
            } else if (!s2 && !s4 && s6 && s8) {
                nForm = "V";
            } else if (!s2 && s4 && !s6 && s8) {
                nForm = "VI";
            } else if (s2 && !s4 && !s6 && s8) {
                nForm = "VII";
            } else if (!s2 && !s4 && !s6 && s8) {
                nForm = "VIII";
            } else {
                nForm = "unknown";
            }
            if (form != null && !nForm.equals(form)) {
                return "ambig";
            }
            form = nForm;
        }
        if (form == null) {
            form = "unknown";
        }
        return form;
    }

    private boolean matchPattern(TreenotationStorage storage, TreenotationImpl accVariant, PatternNode[] pattern, int index, Token start, Token end, ArrayList<Treenotation> accentVariants) {
        int nextIndex = match(accVariant,pattern,index);
        if (nextIndex < 0)
            return false;
        tarr.add(accVariant);

        boolean recognized=false;

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
            recognized = matchPattern(storage,av,pattern,nextIndex,start,end,accentVariants) || recognized;
        }
        if (arr.size()==0 && (nextIndex == pattern.length || (nextIndex == pattern.length - 1 && pattern[pattern.length-1].multiple))) {
            TreenotationSyntax newAccentVariant = (TreenotationSyntax) TreetonFactory.newSyntaxTreenotation(storage,start,end,accvarTp);
            for (TreenotationImpl trn : tarr) {
                newAccentVariant.addTree(new TreenotationImpl.Node(TreenotationImpl.PARENT_CONNECTION_WEAK, trn));
            }
            recognized = true;
            accentVariants.add(newAccentVariant);
            storage.add(newAccentVariant);
        }

        tarr.remove(accVariant);
        return recognized;
    }

    private int match(Treenotation accVariant, PatternNode[] pattern, int index) {
        TreenotationImpl av = (TreenotationImpl) accVariant;
        TreenotationImpl.Node[] arr = av.getTrees();
        int nSyl = av.getNumberOfTrees();

        int i=0;
        while (i<arr.length && arr[i]==null) i++;
        TreenotationImpl.Node cur=arr[i++];
        while (true) {
            if (pattern[index].accent) {
                if (pattern[index].multiple) {
                    if (
                            cur.getParentConection()==TreenotationImpl.PARENT_CONNECTION_STRONG ||
                                    !pattern[index].exact && cur.getParentConection()==TreenotationImpl.PARENT_CONNECTION_WEAK
                            ) {
                        while (i < arr.length && arr[i]==null) i++;
                        if (i == arr.length) {
                            return index;
                        }
                        cur = arr[i++];
                    } else  {
                        index++;
                    }
                } else {
                    if (
                            cur.getParentConection()==TreenotationImpl.PARENT_CONNECTION_STRONG ||
                                    !pattern[index].exact && cur.getParentConection()==TreenotationImpl.PARENT_CONNECTION_WEAK
                            ) {
                        index++;
                        while (i < arr.length && arr[i]==null) i++;
                        if (i == arr.length) {
                            return index;
                        }
                        cur = arr[i++];
                    } else  {
                        return -1; //index++
                    }
                }
            } else {
                if (pattern[index].multiple) {
                    if (
                            cur.getParentConection()==TreenotationImpl.PARENT_CONNECTION_WEAK ||
                                    !pattern[index].exact && cur.getParentConection()==TreenotationImpl.PARENT_CONNECTION_STRONG && nSyl==1
                            ) {
                        while (i < arr.length && arr[i]==null) i++;
                        if (i == arr.length) {
                            return index;
                        }
                        cur = arr[i++];
                    } else  {
                        return -1;
                    }
                } else {
                    if (
                            cur.getParentConection()==TreenotationImpl.PARENT_CONNECTION_WEAK ||
                                    !pattern[index].exact && cur.getParentConection()==TreenotationImpl.PARENT_CONNECTION_STRONG && nSyl==1
                            ) { //на нечетной позиции ударятся может только односложник
                        index++;
                        while (i < arr.length && arr[i]==null) i++;
                        if (i == arr.length) {
                            return index;
                        }
                        cur = arr[i++];
                    } else  {
                        return -1;
                    }
                }
            }
        }
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

    ArrayList<TreenotationImpl> tarr;

    class PatternNode {
        public PatternNode(boolean accent, boolean exact, boolean multiple) {
            this.accent = accent;
            this.exact = exact;
            this.multiple = multiple;
        }

        boolean accent;
        boolean exact;
        boolean multiple;
    }

    PatternNode[] matchPattern_Iambus = {
            new PatternNode(false,false,false),
            new PatternNode(true,false,false),
            new PatternNode(false,false,false),
            new PatternNode(true,false,false),
            new PatternNode(false,false,false),
            new PatternNode(true,false,false),
            new PatternNode(false,false,false),
            new PatternNode(true,true,false),
            new PatternNode(false,true,true),
    };


    public void init() throws ResourceInstantiationException {
        try {
            verseTp = getTrnContext().getType("Verse");
            syllTp = getTrnContext().getType("Syllable");
            accvarTp = getTrnContext().getType("AccVariant");

            meter_feature = verseTp.getFeatureIndex("meter");
            form_feature = verseTp.getFeatureIndex("form");
            nVariants_feature = verseTp.getFeatureIndex("nVariants");
            AccentVariantArr_feature = verseTp.getFeatureIndex("AccentVariantArr");

            TrnTypeSetFactory factory = new TrnTypeSetFactory();
            accvarTpSet = factory.newTrnTypeSet(new TrnType[]{accvarTp},1);
            followInputTpSet = factory.newTrnTypeSet(new TrnType[]{accvarTp},1);

            tarr = new ArrayList<TreenotationImpl>();

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
