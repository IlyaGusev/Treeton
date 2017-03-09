/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res;

import treeton.core.*;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.scape.trnmapper.TrnMapperRule;
import treeton.core.scape.trnmapper.TrnMapperRuleStorage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TrnMapper extends Resource {
    private TrnMapperRuleStorage trnMapperRuleStorage;
    private TrnType[] inputTypes;
    private BlackBoard localBoard;

    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        try {
            TypeIteratorInterface tit = storage.typeIterator(inputTypes);
            while (tit.hasNext()) {
                Treenotation trn = (Treenotation) tit.next();
                TrnMapperRule mapperRule = trnMapperRuleStorage.getRule(trn);
                if (mapperRule != null) {
                    mapperRule.bind(trn);
                    mapperRule.assign(localBoard);
                    trn.removeAll();
                    if (!mapperRule.getType().equals(trn.getType())) {
                        storage.changeType(trn, mapperRule.getType());
                    }
                    trn.put(localBoard);
                }
            }
            return null;
        } catch (Exception e) {
            processTerminated();
            throw new ExecutionException("Exception during UncoveredTrnsProcessor execution: " + e.getMessage(), e);
        }
    }

    protected TrnType[] getInputType() {
        return inputTypes;
    }


    public void init() throws ResourceInstantiationException {
        try {
            String mp = (String) getInitialParameters().get("path");
            if (mp != null) {
                URL mapperPath = new URL(getResContext().getFolder(), mp);
                trnMapperRuleStorage = new TrnMapperRuleStorage(getTrnContext().getTypes(), getTrnContext().getTypes());
                trnMapperRuleStorage.readInFromFile(mapperPath.getPath());
            } else {
                throw new ResourceInstantiationException("No mapper path specified");
            }
            Set<TrnType> types = new HashSet<TrnType>();

            for (TrnMapperRule rule : trnMapperRuleStorage.getRules()) {
                for (Treenotation trn : rule.getTemplate()) {
                    types.add(trn.getType());
                }
            }

            inputTypes = types.toArray(new TrnType[types.size()]);
        } catch (TreetonModelException e) {
            throw new ResourceInstantiationException("Error with model", e);
        } catch (ParseException e) {
            throw new ResourceInstantiationException("Parse error", e);
        } catch (MalformedURLException e) {
            throw new ResourceInstantiationException("Problem", e);
        } catch (IOException e) {
            throw new ResourceInstantiationException("Problem", e);
        } catch (ContextException e) {
            throw new ResourceInstantiationException("Problem", e);
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