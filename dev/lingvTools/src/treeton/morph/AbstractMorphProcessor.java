/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.morph;

import treeton.core.*;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.util.BlockStack;
import treeton.core.util.LinkedTrns;
import treeton.core.util.UncoveredAreasIteratorPlug;
import treeton.dict.Dictionary;
import treeton.res.tokeniser.SimpleTokeniser;
import treeton.scape.ScapePhase;
import treeton.scape.ScapeResult;

import java.io.IOException;
import java.util.*;

public abstract class AbstractMorphProcessor {
    protected MorphInterface engine = null;
    protected Dictionary dictionary;
    protected SimpleTokeniser tokeniser;

    protected BlockStack stack = new BlockStack(5);
    protected BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);
    protected StringBuffer tbuf = new StringBuffer();
    protected ScapePhase predMorphPhase;
    protected Map<TrnType, Set<Integer>> filter = new HashMap<TrnType, Set<Integer>>();
    LinkedTrns.TrnsIterator linkedItr = LinkedTrns.newTrnsIterator(null);
    IntFeatureMap additionAttrs = new IntFeatureMapImpl();
    TreenotationsContext trnContext;
    boolean uncoveredAreasOnly = false;

    protected AbstractMorphProcessor(Dictionary dictionary, SimpleTokeniser tokeniser, MorphInterface engine, TreenotationsContext trnContext) throws ResourceInstantiationException {
        this.dictionary = dictionary;
        this.tokeniser = tokeniser;
        this.engine = engine;
        this.trnContext = trnContext;
    }

    public void init() throws ResourceInstantiationException {
        PredMorphPhaseBuilder builder = new PredMorphPhaseBuilder(trnContext, tokeniser, filter);
        try {
            predMorphPhase = builder.buildPredMorphPhase(dictionary);
        } catch (IOException e) {
            throw new ResourceInstantiationException("IOException when trying to build predmorph phase", e);
        } catch (TreetonModelException e) {
            throw new ResourceInstantiationException("TreetonModelException when trying to build predmorph phase", e);
        }
    }

    public void putAdditionAttr(String feature, Object value) {
        try {
            additionAttrs.put(dictionary.getType().getFeatureIndex(feature), value, dictionary.getType());
        } catch (TreetonModelException e) {
            //do nothing
        }
    }

    public void applyTo(TextMarkingStorage _storage) throws MorphException {
        TreenotationStorage storage = (TreenotationStorage) _storage;

        localBoard.clean();

        ArrayList<Treenotation> buffer = new ArrayList<Treenotation>();

        UncoveredAreasIterator unIt;

        if (isUncoveredAreasOnly()) {
            unIt = storage.uncoveredAreasIterator(dictionary.getType(), null, null);
        } else {
            unIt = new UncoveredAreasIteratorPlug(storage);
        }

        while (unIt.next()) {
            predMorphPhase.reset(storage, unIt.getStartToken(), unIt.getEndToken());
            Token rightEdge = null;

            while (predMorphPhase.nextStartPoint(rightEdge) != null) {
                rightEdge = null;
                ScapeResult r;

                while ((r = predMorphPhase.nextResult()) != null) {
                    linkedItr.reset(r.getLastMatched());

                    while (linkedItr.hasNext()) {
                        stack.push(linkedItr.nextTrn());
                    }

                    tbuf.setLength(0);

                    while (!stack.isEmpty()) {
                        Treenotation trn = (Treenotation) stack.pop();
                        tbuf.append(trn.getText());
                    }

                    String source = tbuf.toString();

                    Collection<Properties> l = engine.processOneWord(source, dictionary);

                    if (l == null || l.size() == 0) {
                        continue;
                    }

                    for (Object aL : l) {
                        fillBlackBoard(source, (Properties) aL);
                        Treenotation trn = TreetonFactory.newTreenotation(r.getStartToken(), r.getEndToken(), dictionary.getType(), localBoard);
                        List<Treenotation> trnList = postProcess(trn);
                        if (trnList == null) {
                            buffer.add(trn);
                        } else {
                            for (Treenotation t : trnList) {
                                buffer.add(t);
                            }
                        }

                    }
                }

                for (Treenotation aBuffer : buffer) {
                    storage.addPostFactum(aBuffer);
                }
                buffer.clear();
            }

            storage.applyPostFactumTrns();
        }
    }

    protected List<Treenotation> postProcess(Treenotation trn) {
        return null;
    }

    protected void fillBlackBoard(String source, Properties p) {
        additionAttrs.fillBlackBoard(localBoard);
        localBoard.fill(dictionary.getType(), p);
        addCustomFeatures(localBoard, p, source);
    }

    protected abstract void addCustomFeatures(BlackBoard board, Properties p, String source);

    public boolean isUncoveredAreasOnly() {
        return uncoveredAreasOnly;
    }

    public void setUncoveredAreasOnly(boolean uncoveredAreasOnly) {
        this.uncoveredAreasOnly = uncoveredAreasOnly;
    }

    public void deInit() {
        predMorphPhase = null;
    }

    public void addToFilter(TrnType tp, String featureName) throws TreetonModelException {
        int fn = tp.getFeatureIndex(featureName);
        if (fn == -1)
            return;
        Set<Integer> set = filter.get(tp);
        if (set == null) {
            set = new HashSet<Integer>();
            filter.put(tp, set);
        }
        set.add(fn);
    }
}