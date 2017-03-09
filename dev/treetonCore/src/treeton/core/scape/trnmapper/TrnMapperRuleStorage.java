/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape.trnmapper;

import treeton.core.Treenotation;
import treeton.core.fsm.ScapeTreenotationClassTree;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnTypeStorage;
import treeton.core.scape.ParseException;
import treeton.core.util.FileMapper;
import treeton.core.util.sut;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class TrnMapperRuleStorage {
    ScapeTreenotationClassTree trnIndex;
    TrnTypeStorage sourceTypes, targetTypes;
    HashMap<Treenotation, TrnMapperRule> rules = new HashMap<Treenotation, TrnMapperRule>();

    public TrnMapperRuleStorage(TrnTypeStorage pSourceTypes, TrnTypeStorage pTargetTypes) {
        sourceTypes = pSourceTypes;
        targetTypes = pTargetTypes;
    }

    public TrnMapperRule getRule(Treenotation trn) {
        ScapeTreenotationClassTree.FinalNode nd = trnIndex.getFinalState(trn);
        if (nd == null)
            return null;
        return (TrnMapperRule) nd.getValues()[0];
    }

    public ArrayList<String> getTypesWithRules() {
        ArrayList<String> list = new ArrayList<String>();
        Collection<TrnMapperRule> col = rules.values();
        for (TrnMapperRule r : col) {
            try {
                list.add(r.assignments.getType().getName());
            } catch (TreetonModelException e) {
                //do nothing
            }
        }
        return list;
    }

    public void readInFromFile(String path) throws ParseException, IOException {
        char[] s = FileMapper.map2memory(path, "UTF8");
        int pl = 0, endpl = s.length - 1;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        while (true) {
            if (pl > endpl)
                break;
            TrnMapperRule r = new TrnMapperRule(sourceTypes, targetTypes);
            pl = r.readIn(s, pl, endpl);
            for (Treenotation t : r.trntemp.trns) {
                rules.put(t, r);
            }
        }
        trnIndex = new ScapeTreenotationClassTree(targetTypes);
        trnIndex.build(rules.keySet().iterator(), rules.values().iterator());
//    JTreeFrame.showJTreeFrame(trnIndex,ScapeTreenotationClassTree.newJTreeCellRenderer());
    }

  /*private boolean checkAmbiguity() {
    Iterator<ScapeTreenotationClassTree.FinalNode> it = trnIndex.finalStatesIterator();
    while (it.hasNext()) {
      ScapeTreenotationClassTree.FinalNode nd = it.next();
      if (nd.getValues().length>1) {
        return false;
      }
    }
    return true;
  } */

    public Collection<TrnMapperRule> getRules() {
        return rules.values();
    }
}
