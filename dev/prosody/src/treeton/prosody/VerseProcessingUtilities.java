/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody;

import treeton.core.Treenotation;
import treeton.core.TreenotationImpl;
import treeton.core.TreenotationStorageImpl;
import treeton.core.TypeIteratorInterface;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;

import java.util.*;

public class VerseProcessingUtilities {
    public VerseProcessingUtilities( TreenotationsContext trnContext ) throws TreetonModelException {
        accVariantTp = trnContext.getType("AccVariant");
        syllableTp = trnContext.getType("Syllable");
        phonWordTp = trnContext.getType("PhonWord");
        userVariantFeatureId = accVariantTp.getFeatureIndex("userVariant");
    }

    private TrnType accVariantTp;
    private TrnType syllableTp;
    private TrnType phonWordTp;
    private int userVariantFeatureId;

    public void collectUserStresses(TreenotationStorageImpl storage, HashSet<Treenotation> forceStressed, HashSet<Treenotation> forceUnstressed ) {
        TypeIteratorInterface iterator = storage.typeIterator( accVariantTp, storage.firstToken(), storage.lastToken() );

        int status = -2;

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

    public List<Treenotation> getAllSyllables( TreenotationStorageImpl storage ) {
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
}
