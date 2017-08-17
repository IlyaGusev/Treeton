/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody;

import com.sun.tools.javac.util.Pair;
import treeton.core.*;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextUtil;
import treeton.core.config.context.resources.ResourceChain;
import treeton.core.config.context.resources.ResourceUtils;
import treeton.core.config.context.resources.xmlimpl.ResourcesContextXMLImpl;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;

import java.util.*;

public class VerseProcessingUtilities {
    public VerseProcessingUtilities( TreenotationsContext trnContext ) throws TreetonModelException {
        accVariantTp = trnContext.getType("AccVariant");
        syllableTp = trnContext.getType("Syllable");
        verseTp = trnContext.getType("Verse");
        phonWordTp = trnContext.getType("PhonWord");
        userVariantFeatureId = accVariantTp.getFeatureIndex("userVariant");
    }

    private TrnType accVariantTp;
    private TrnType syllableTp;
    private TrnType verseTp;
    private TrnType phonWordTp;
    private int userVariantFeatureId;

    public void collectUserStresses(TreenotationStorageImpl storage, HashSet<Treenotation> forceStressed, HashSet<Treenotation> forceUnstressed ) {
        TypeIteratorInterface iterator = storage.typeIterator( accVariantTp, storage.firstToken(), storage.lastToken() );

        while( iterator.hasNext() ) {
            TreenotationImpl accVariant = (TreenotationImpl) iterator.next();
            if( !Boolean.TRUE.equals(accVariant.get(userVariantFeatureId) ) ) {
                continue;
            }

            TreenotationImpl.Node[] syllables = accVariant.getTrees();

            for (TreenotationImpl.Node syllableNode : syllables) {
                if( syllableNode.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG ) {
                    forceStressed.add( syllableNode.getTrn() );
                } else {
                    forceUnstressed.add( syllableNode.getTrn() );
                }
            }
        }
    }

    public List<Treenotation> getVerseSyllables( TreenotationStorageImpl storage, Treenotation verse ) {
        List<Treenotation> syllables = new ArrayList<>();

        TypeIteratorInterface phonWordIterator = storage.typeIterator(phonWordTp, verse.getStartToken(), verse.getEndToken());

        while (phonWordIterator.hasNext()) {
            Treenotation phonWordTrn = (Treenotation) phonWordIterator.next();
            if (phonWordTrn.getStartToken().compareTo(verse.getStartToken()) < 0 ||
                    phonWordTrn.getEndToken().compareTo(verse.getEndToken()) > 0) {
                continue;
            }

            TypeIteratorInterface sylIterator = storage.typeIterator(syllableTp, phonWordTrn.getStartToken(), phonWordTrn.getEndToken());
            while (sylIterator.hasNext()) {
                syllables.add((Treenotation) sylIterator.next());
            }
        }

        return syllables;
    }

    private List<Treenotation> getAllSyllables(TreenotationStorageImpl storage) {
        List<Treenotation> syllables = new ArrayList<>();

        TypeIteratorInterface phonWordIterator = storage.typeIterator(phonWordTp);

        while (phonWordIterator.hasNext()) {
            Treenotation phonWordTrn = (Treenotation) phonWordIterator.next();

            TypeIteratorInterface sylIterator = storage.typeIterator(syllableTp, phonWordTrn.getStartToken(), phonWordTrn.getEndToken());
            while (sylIterator.hasNext()) {
                syllables.add((Treenotation) sylIterator.next());
            }
        }

        return syllables;
    }


    public ArrayList<SyllableInfo> generateSyllableInfo(TreenotationStorageImpl storage,
                                                         HashSet<Treenotation> forceStressed,
                                                         HashSet<Treenotation> forceUnstressed, Treenotation verse ) {
        ArrayList<SyllableInfo> result = new ArrayList<>();
        Map<Treenotation,SyllableInfo.StressStatus> stressInfoMap = new HashMap<>();

        List<Treenotation> allSyllables = verse == null ? getAllSyllables(storage) : getVerseSyllables(storage,verse);
        for( Treenotation syll : allSyllables ) {
            SyllableInfo.StressStatus status = null;

            if( forceStressed != null && forceStressed.contains(syll) ) {
                status = SyllableInfo.StressStatus.STRESSED;
            } else if(  forceUnstressed != null && forceUnstressed.contains(syll) ) {
                status = SyllableInfo.StressStatus.UNSTRESSED;
            }

            stressInfoMap.put(syll,status);
        }

        TypeIteratorInterface iterator = storage.typeIterator(accVariantTp, verse == null ? storage.firstToken() : verse.getStartToken(),
                verse == null ? storage.lastToken() : verse.getEndToken() );

        while (iterator.hasNext()) {
            TreenotationImpl accVariant = (TreenotationImpl) iterator.next();

            if (Boolean.TRUE.equals(accVariant.get(userVariantFeatureId))) {
                continue;
            }

            TreenotationImpl.Node[] syllables = accVariant.getTrees();

            if(forceUnstressed != null || forceStressed != null ) {
                boolean skip = false;
                for (TreenotationImpl.Node syllableNode : syllables) {
                    TreenotationImpl syllable = (TreenotationImpl) syllableNode.getTrn();

                    assert stressInfoMap.containsKey(syllable);

                    if (syllableNode.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                        if (forceUnstressed != null && forceUnstressed.contains(syllable)) {
                            skip = true;
                            break;
                        }
                    } else {
                        if (forceStressed != null && forceStressed.contains(syllable)) {
                            skip = true;
                            break;
                        }
                    }
                }

                if( skip ) {
                    continue;
                }
            }


            for (TreenotationImpl.Node syllableNode : syllables) {
                TreenotationImpl syllable = (TreenotationImpl) syllableNode.getTrn();
                assert stressInfoMap.containsKey(syllable);

                if (forceStressed != null && forceStressed.contains(syllable)) {
                    continue;
                }
                if (forceUnstressed != null && forceUnstressed.contains(syllable)) {
                    continue;
                }

                SyllableInfo.StressStatus status = stressInfoMap.get(syllable);

                if (syllableNode.getParentConection() == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                    if (status == null) {
                        status = SyllableInfo.StressStatus.STRESSED;
                        stressInfoMap.put(syllable, status);
                    } else if (status == SyllableInfo.StressStatus.UNSTRESSED) {
                        status = SyllableInfo.StressStatus.AMBIGUOUS;
                        stressInfoMap.put(syllable, status);
                    }
                } else {
                    if (status == null) {
                        status = SyllableInfo.StressStatus.UNSTRESSED;
                        stressInfoMap.put(syllable, status);
                    } else if (status == SyllableInfo.StressStatus.STRESSED) {
                        status = SyllableInfo.StressStatus.AMBIGUOUS;
                        stressInfoMap.put(syllable, status);
                    }
                }
            }
        }

        for( Treenotation syllable : allSyllables ) {
            SyllableInfo.StressStatus stressStatus = stressInfoMap.get(syllable);
            if( stressStatus == null ) {
                stressStatus = SyllableInfo.StressStatus.UNSTRESSED;
            }

            int s = syllable.getStartToken().getStartNumerator();
            result.add(new SyllableInfo(s - (verse == null ? 0 : verse.getStartToken().getStartNumerator()),
                    syllable.getEndToken().getEndNumerator()-s,stressStatus));
        }

        return result;
    }

    public void injectSyllableInfo(TreenotationStorageImpl storage, Collection<StressDescription> externalStressInfo) {
        HashSet<Treenotation> stub = new HashSet<>();
        injectSyllableInfo(storage, stub, stub, externalStressInfo);
    }

    public void injectSyllableInfo(TreenotationStorageImpl storage,
                                                      HashSet<Treenotation> forceStressed,
                                                      HashSet<Treenotation> forceUnstressed,
                                                      Collection<StressDescription> externalStressInfo) {
        Map<Integer, TreenotationImpl> syllablesMap = new HashMap<>();
        TypeIteratorInterface syllablesIterator = storage.typeIterator(syllableTp, storage.firstToken(), storage.lastToken());

        while (syllablesIterator.hasNext()) {
            TreenotationImpl syllable = (TreenotationImpl) syllablesIterator.next();
            syllablesMap.put(syllable.getStartNumerator(), syllable);
        }

        TypeIteratorInterface verseIterator = storage.typeIterator(verseTp, storage.firstToken(), storage.lastToken());

        for (StressDescription externalStressDescription : externalStressInfo) {
            assert verseIterator.hasNext();
            Treenotation verse = (Treenotation) verseIterator.next();
            assert verse.getStartDenominator() == 1;
            for (SyllableInfo syllableInfo : externalStressDescription) {
                if (syllableInfo.stressStatus == SyllableInfo.StressStatus.AMBIGUOUS) {
                    continue;
                }

                TreenotationImpl syllable = syllablesMap.get(verse.getStartNumerator() + syllableInfo.startOffset);
                if (syllable == null) { //TODO надо бы варнинги куда-то писать
                    continue;
                }
                int length = syllable.getEndNumerator() - syllable.getStartNumerator();
                if (length != syllableInfo.length) { //TODO надо бы варнинги куда-то писать
                    continue;
                }

                TreenotationImpl accVariant = (TreenotationImpl) TreetonFactory.newSyntaxTreenotation(storage, syllable.getStartToken(),
                        syllable.getEndToken(), accVariantTp);
                accVariant.put(userVariantFeatureId, Boolean.TRUE);
                accVariant.addTree(new TreenotationImpl.Node(
                        syllableInfo.stressStatus == SyllableInfo.StressStatus.STRESSED ?
                                TreenotationImpl.PARENT_CONNECTION_STRONG :
                                TreenotationImpl.PARENT_CONNECTION_WEAK, syllable));
                if (syllableInfo.stressStatus == SyllableInfo.StressStatus.STRESSED) {
                    forceStressed.add(syllable);
                } else {
                    forceUnstressed.add(syllable);
                }

                storage.add(accVariant);
            }
        }
    }

    public static void parseFormattedVerses(ArrayList<String> formattedInput, ArrayList<String> plainOutput, ArrayList<StressDescription> stressDescriptions) throws Exception {
        TreenotationsContext trnContext = ContextConfiguration.trnsManager().get("Common.Russian.Prosody");
        ResourcesContextXMLImpl resContext = (ResourcesContextXMLImpl)
                ContextConfiguration.resourcesManager().get(ContextUtil.getFullName(trnContext));

        ResourceChain syllabizatorChain = new ResourceUtils().createResourceChain(resContext.getResourceChainModel("OnlySyllabizatorChain", true));
        syllabizatorChain.initialize(trnContext);

        TreenotationStorageImpl storage = (TreenotationStorageImpl) TreetonFactory.newTreenotationStorage(trnContext);
        TrnType syllableTp = trnContext.getType("Syllable");

        for (String verse : formattedInput) {
            ArrayList<Integer> stressPlaces = new ArrayList<>();
            ArrayList<Pair<Integer, Integer>> unstressedZones = new ArrayList<>();

            String pureString = locateUserAccentMarkup(verse, stressPlaces, unstressedZones);

            plainOutput.add(pureString);

            syllabizatorChain.execute(pureString, storage, new HashMap<>());

            Map<Integer, TreenotationImpl> syllablesCoverage = new HashMap<>();

            TypeIteratorInterface syllablesIterator = storage.typeIterator(syllableTp, storage.firstToken(), storage.lastToken());

            while (syllablesIterator.hasNext()) {
                TreenotationImpl syllable = (TreenotationImpl) syllablesIterator.next();

                for (int i = syllable.getStartNumerator(); i < syllable.getEndNumerator(); i++) {
                    syllablesCoverage.put(i, syllable);
                }
            }

            HashSet<TreenotationImpl> userStressedSyllables = new HashSet<>();

            for (Integer stressPlace : stressPlaces) {
                TreenotationImpl syllable = syllablesCoverage.get(stressPlace);

                if (syllable == null) {
                    throw new RuntimeException("No syllable for position " + stressPlace + ", verse " + verse);
                }

                userStressedSyllables.add(syllable);
            }

            HashSet<TreenotationImpl> userUnstressedSyllables = new HashSet<>();

            for (Pair<Integer, Integer> unstressedZone : unstressedZones) {
                boolean found = false;
                for (int j = unstressedZone.fst; j < unstressedZone.snd; j++) {
                    TreenotationImpl syllable = syllablesCoverage.get(j);

                    if (syllable == null) {
                        continue;
                    }

                    found = true;

                    if (userStressedSyllables.contains(syllable)) {
                        throw new RuntimeException("Ambiguous user accent markup, syllable " + syllable.getText() + ", verse " + verse);
                    }

                    userUnstressedSyllables.add(syllable);
                }

                if (!found) {
                    System.err.println("No syllable for zone [" + unstressedZone.fst + "," + unstressedZone.snd + "], verse " + verse);
                    return;
                }
            }

            ArrayList<SyllableInfo> sylInfo = new ArrayList<>();

            for (TreenotationImpl syllable : userStressedSyllables) {
                sylInfo.add(new SyllableInfo(syllable.getStartNumerator(),
                        syllable.getEndNumerator() - syllable.getStartNumerator(), SyllableInfo.StressStatus.STRESSED));
            }

            for (TreenotationImpl syllable : userUnstressedSyllables) {
                sylInfo.add(new SyllableInfo(syllable.getStartNumerator(),
                        syllable.getEndNumerator() - syllable.getStartNumerator(), SyllableInfo.StressStatus.UNSTRESSED));
            }

            stressDescriptions.add(new StressDescription(sylInfo));
        }
    }

    private static String locateUserAccentMarkup(String verse, ArrayList<Integer> stressPlaces, ArrayList<Pair<Integer, Integer>> unstressedZones) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < verse.length(); i++) {
            char c = verse.charAt(i);

            if (c == '{') {
                int j = i + 1;
                for (; j < verse.length(); j++) {
                    c = verse.charAt(j);
                    if (c == '}') {
                        break;
                    }
                }
                if (j == verse.length()) {
                    throw new RuntimeException("Unclosed '{' in the input file, verse" + verse);
                }

                String syl = verse.substring(i + 1, j);
                int sylPlace = syl.indexOf('\'');
                if (sylPlace == -1) {
                    unstressedZones.add(new Pair<>(sb.length(), sb.length() + syl.length()));
                    sb.append(syl);
                    i = j;
                } else if (sylPlace == 0) {
                    throw new RuntimeException("Accent sign right after '{', verse" + verse);
                } else {
                    if (!Character.isLetter(syl.charAt(sylPlace - 1))) {
                        throw new RuntimeException("Accent sign not after letter, verse" + verse);
                    }
                    stressPlaces.add(sb.length() + sylPlace - 1);
                    sb.append(syl.substring(0, sylPlace)).append(syl.substring(sylPlace + 1));
                    i = j;
                }
            } else if (c == '\\') {
                i++;
                if (i == verse.length()) {
                    break;
                }
                sb.append(verse.charAt(i));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

}
