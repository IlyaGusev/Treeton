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
import treeton.core.model.TrnType;
import treeton.core.scape.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UncoveredTrnsProcessor extends Resource {
    DefaultScapeVariable variable = new DefaultScapeVariable();
    BlackBoard localBoard;
    private TrnType inputType;
    private TrnTemplate inputTemplate;
    private AssignmentVector vector;
    private List<TrnType> coverTypes;

    protected DefaultScapeVariable getVariable() {
        return variable;
    }

    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        try {
            TypeIteratorInterface tit = storage.typeIterator(inputType);
            while (tit.hasNext()) {
                Treenotation trn = (Treenotation) tit.next();
                if (inputTemplate.match(trn)) {
                    TokenImpl cur = (TokenImpl) trn.getStartToken();

                    boolean covered = false;
                    while (cur != trn.getEndToken()) {
                        if (hasParentOfTypes(cur, coverTypes)) {
                            covered = true;
                            break;
                        }
                        cur = (TokenImpl) cur.getNextToken();
                    }

                    if (hasParentOfTypes(cur, coverTypes)) {
                        covered = true;
                    }
                    if (!covered) {
                        variable.setValue(trn);
                        vector.assign(localBoard);
                        Treenotation newTrn = TreetonFactory.newTreenotation(trn.getStartToken(), trn.getEndToken(), vector.getType(), localBoard);
                        storage.add(newTrn);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            processTerminated();
            throw new ExecutionException("Exception during UncoveredTrnsProcessor execution: " + e.getMessage(), e);
        }
    }

    private boolean hasParentOfTypes(TokenImpl cur, List<TrnType> coverTypes) {
        for (TrnType tp : coverTypes) {
            TreenotationImpl[] treenotations = cur.getParent(tp);
            if (treenotations != null)
                return true;
        }
        return false;
    }

    protected TrnType getInputType() {
        return inputType;
    }

    protected TrnTemplate getInputTemplate() {
        return inputTemplate;
    }

    protected AssignmentVector getVector() {
        return vector;
    }

    protected List<TrnType> getCoverType() {
        return coverTypes;
    }

    public void init() throws ResourceInstantiationException {
        try {
            String templateString = (String) getInitialParameters().get("inputTemplate");
            inputTemplate = new TrnTemplate();
            inputTemplate.readIn(getTrnContext(), templateString.toCharArray(), 0, templateString.length() - 1, null);
            inputType = null;
            for (Treenotation trn : inputTemplate) {
                if (inputType == null) {
                    inputType = trn.getType();
                } else if (!inputType.equals(trn.getType())) {
                    throw new ResourceInstantiationException("Input must contain treenotations of same TrnType");
                }
            }

            vector = new AssignmentVector(getTrnContext().getTypes(), getTrnContext().getTypes());
            String assignments = (String) getInitialParameters().get("assignments");
            HashMap<String, ScapeVariable> bindings = new HashMap<String, ScapeVariable>();
            bindings.put("@", variable);
            vector.readIn(assignments.toCharArray(), 0, assignments.length() - 1, bindings, null);
            coverTypes = new ArrayList<TrnType>();
            for (Object o : (List) getInitialParameters().get("coverTypes")) {
                String s = (String) o;
                coverTypes.add(getTrnContext().getType(s));
            }

        } catch (TreetonModelException e) {
            throw new ResourceInstantiationException("Error with model", e);
        } catch (ParseException e) {
            throw new ResourceInstantiationException("Parse error", e);
        }
        localBoard = TreetonFactory.newBlackBoard(50, false);
    }

    public void deInit() {
        localBoard = null;
    }

    public void stop() {
    }

    public void processTerminated() {
        localBoard.clean();
    }
}
