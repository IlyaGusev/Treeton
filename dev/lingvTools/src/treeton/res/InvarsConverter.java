/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res;

import treeton.core.*;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.model.dclimpl.MarkInIntroduction;
import treeton.core.model.dclimpl.TrnTypeDclImpl;
import treeton.core.model.dclimpl.ValidationTree;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class InvarsConverter extends Resource {
    TrnTypeDclImpl type;
    String typeName;
    ValidationTree tree;
    HashSet<MarkInIntroduction> inflHash;

    BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);

    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;

        localBoard.clean();
        inflHash = new HashSet<MarkInIntroduction>();
        inflHash.add(type.getDeclaredMark("invar_excl"));
        inflHash.add(type.getDeclaredMark("stringMark"));
        inflHash.add(type.getDeclaredMark("orthmMark"));
        tree = type.getHierarchy().refactor(inflHash, ValidationTree.REMOVE_HASHED_MARKS);
        inflHash = new HashSet<MarkInIntroduction>();
        inflHash.add(type.getDeclaredMark("infl"));

        Iterator it = storage.typeIterator(type);

        int INVAR_feature;
        try {
            INVAR_feature = type.getFeatureIndex("INVAR");
        } catch (TreetonModelException e) {
            throw new ExecutionException("Unable to locate feature INVAR", e);
        }
        TString invarValue = TreetonFactory.newTString("invar");

        while (it.hasNext()) {
            Treenotation trn = (Treenotation) it.next();

            boolean isInvar = invarValue.equals(
                    trn.get(INVAR_feature));
            if (isInvar) {
                Treenotation[] trns = tree.vary(trn.getStartToken(), trn.getEndToken(), trn, inflHash, false);
                if (trns != null && trns.length > 0 && (trns.length > 1 || trns[0].size() != trn.size())) {
                    for (Treenotation t : trns) {
                        storage.addPostFactum(t);
                    }
                    storage.remove(trn);
                }
            }
        }
        storage.applyPostFactumTrns();
        return null;
    }

    protected void stop() {
    }

    protected void init() throws ResourceInstantiationException {
        try {
            String typeName = (String) getInitialParameters().get("inputType");
            type = (TrnTypeDclImpl) getTrnContext().getType(typeName);
            this.typeName = typeName;
        } catch (Exception e) {
            throw new ResourceInstantiationException(null, "Exception during InvarsConverter instantiation: " + e.getMessage(), e);
        }
    }

    protected void deInit() {
    }

    protected void processTerminated() {
    }
}
