/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.scape.trnmapper;

import treeton.core.BlackBoard;
import treeton.core.Treenotation;
import treeton.core.fsm.ScapeTreenotationClassTree;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.scape.ParseException;
import treeton.core.util.FileMapper;
import treeton.core.util.LonelyTreenotation;
import treeton.core.util.sut;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class StringToTrnMapperRuleStorage {
    LonelyTreenotation trn;
    TrnType targetType;
    ScapeTreenotationClassTree trnIndex;
    TrnTypeStorage targetTypes;
    HashMap<Treenotation, StringToTrnMapperRule> rules = new HashMap<Treenotation, StringToTrnMapperRule>();

    public StringToTrnMapperRuleStorage(TrnTypeStorage pTargetTypes, TrnType targetType) {
        targetTypes = pTargetTypes;
        this.targetType = targetType;
        try {
            trn = new LonelyTreenotation(null, null, pTargetTypes.get("Atom"), null);
        } catch (TreetonModelException e) {
            //do nothing
        }
    }

    public StringToTrnMapperRuleStorage(TrnTypeStorage pTargetTypes) {
        targetTypes = pTargetTypes;
        try {
            trn = new LonelyTreenotation(null, null, pTargetTypes.get("Atom"), null);
        } catch (TreetonModelException e) {
            //do nothing
        }
    }

    public StringToTrnMapperRule getRule(String s) {
        trn.setText(s);
        ScapeTreenotationClassTree.FinalNode nd = trnIndex.getFinalState(trn);
        if (nd == null)
            return null;
        return (StringToTrnMapperRule) nd.getValues()[0];
    }

    public Object[] getRules(String s) {
        trn.setText(s);
        ScapeTreenotationClassTree.FinalNode nd = trnIndex.getFinalState(trn);
        if (nd == null)
            return null;
        return nd.getValues();
    }

    public int assign(BlackBoard board, String s) {
        Object[] arr = getRules(s);
        if (arr != null && arr.length > 0) {
            for (Object o : arr) {
                if (o == null)
                    continue;
                StringToTrnMapperRule rule = (StringToTrnMapperRule) o;
                rule.bind(s);
                while (rule.next()) {
                    rule.assign(board);
                }
                rule.unbind();
            }
            return 1;
        } else {
            return -1;
        }
    }

    public int assign(Treenotation dest, String s) {
        Object[] arr = getRules(s);
        if (arr != null && arr.length > 0) {
            for (Object o : arr) {
                StringToTrnMapperRule rule = (StringToTrnMapperRule) o;
                rule.bind(s);
                while (rule.next()) {
                    rule.assign(dest);
                }
                rule.unbind();
            }
            return 1;
        } else {
            return -1;
        }
    }

    public void readInFromFile(String path) throws ParseException, IOException {
        char[] s = FileMapper.map2memory(path, "UTF8");
        int pl = 0, endpl = s.length - 1;
        pl = sut.skipSpacesEndls(s, pl, endpl);
        while (true) {
            if (pl > endpl)
                break;
            StringToTrnMapperRule r = targetType == null ? new StringToTrnMapperRule(targetTypes) : new StringToTrnMapperRule(targetTypes, targetType);
            pl = r.readIn(s, pl, endpl);
            for (Treenotation t : r.trntemp.trns) {
                rules.put(t, r);
            }
        }
        trnIndex = new ScapeTreenotationClassTree(targetTypes);
        trnIndex.build(rules.keySet().iterator(), rules.values().iterator());
//    JTreeFrame.showJTreeFrame(trnIndex,ScapeTreenotationClassTree.newJTreeCellRenderer());
    }

    public boolean checkAmbiguity() {
        Iterator<ScapeTreenotationClassTree.FinalNode> it = trnIndex.finalStatesIterator();
        while (it.hasNext()) {
            ScapeTreenotationClassTree.FinalNode nd = it.next();
            if (nd.getValues().length > 1) {
                return false;
            }
        }
        return true;
    }

    public TrnType getTargetType() {
        return targetType;
    }
}
