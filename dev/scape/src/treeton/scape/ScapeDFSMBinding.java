/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.Document;
import gate.FeatureMap;
import gate.Node;
import treeton.core.BlackBoard;
import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.fsm.logicset.LogicFSM;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeUtils;
import treeton.core.scape.ScapeRegexBinding;
import treeton.core.scape.ScapeVariable;
import treeton.core.util.LinkedTrns;

import java.util.Iterator;
import java.util.Map;

public class ScapeDFSMBinding extends ScapeBinding implements ScapeVariable {
    ScapePhase phase;

    int nOldBindings;
    ScapeFSMBinding[] oldBindings; //for debug only
    ScapeRegexBinding regexBinding;
    int regexFeature;
    LogicFSM regexFSM;

    LinkedTrns.Item trnItem;
    Token startToken;
    Token endToken;

    LinkedAnns.Item annItem;
    Node startNode;
    Node endNode;
    Document doc;

    int size;

    ScapeDFSMBinding(ScapePhase phase) {
        super();
        this.phase = phase;
        oldBindings = null;
        nOldBindings = 0;
        regexBinding = null;
        regexFSM = null;
    }

    public TrnType getType() {
        if (trnItem != null)
            return trnItem.getTrn().getType();
        if (annItem != null)
            try {
                return phase.types.get(annItem.ann.getType());
            } catch (TreetonModelException e) {
                return null;
            }
        return null;
    }

    private Object getValue(Treenotation trn, int feature) {
        if (feature == TrnType.string_FEATURE || TrnType.orthm_FEATURE == feature) {
            return trn.getText();
        } else if (feature == TrnType.length_FEATURE) {
            return trn.getText().length();
        } else if (feature == TrnType.start_FEATURE) {
            return trn.getStartToken().getStartNumerator();
        } else if (feature == TrnType.end_FEATURE) {
            return trn.getEndToken().getEndNumerator();
        }

        return trn.get(feature);
    }

    private Object getValue(Annotation ann, int feature) {
        if (feature == TrnType.string_FEATURE || TrnType.orthm_FEATURE == feature) {
            return getText(doc, ann);
        } else if (feature == TrnType.length_FEATURE) {
            return getText(doc, ann).length();
        } else if (feature == TrnType.start_FEATURE) {
            return ann.getStartNode().getOffset().intValue();
        } else if (feature == TrnType.end_FEATURE) {
            return ann.getEndNode().getOffset().intValue();
        }

        TrnType tp = null;
        String fName = null;
        try {
            tp = phase.types.get(ann.getType());
            fName = tp.getFeatureNameByIndex(feature).toString();
        } catch (TreetonModelException e) {
            return null;
        }
        Object o = ann.getFeatures().get(fName);
        if (o == null)
            return null;
        return TrnTypeUtils.treatFeatureValueForGate(tp, feature, o);
    }

    public Object getValue(int feature) {
        if (feature < 0)
            return null;
        if (feature == TrnType.start_FEATURE) {
            if (trnItem != null) {
                return startToken.getStartNumerator();
            }
            if (annItem != null) {
                return startNode.getOffset().intValue();
            }
            return null;
        } else if (feature == TrnType.end_FEATURE) {
            if (trnItem != null) {
                return endToken.getEndNumerator();
            }
            if (annItem != null) {
                return endNode.getOffset().intValue();
            }
            return null;
        }

        if (trnItem != null)
            return getValue(trnItem.getTrn(), feature);
        if (annItem != null)
            return getValue(annItem.ann, feature);
        return null;
    }

    public Object getValue(String featureName) {
        TrnType tp = getType();
        int feature;
        try {
            feature = tp.getFeatureIndex(featureName);
        } catch (TreetonModelException e) {
            feature = -1;
        }
        return getValue(feature);
    }

    private void fillBlackBoard(Treenotation trn, BlackBoard board) {
        trn.fillBlackBoard(board);
    }

    private void fillBlackBoard(Annotation ann, BlackBoard board) {
        FeatureMap fm = ann.getFeatures();
        TrnType tp = getType();

        Iterator it = fm.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            String f = (String) e.getKey();
            Object v = e.getValue();
            int fn;
            try {
                fn = tp.getFeatureIndex(f);
            } catch (TreetonModelException e1) {
                fn = -1;
            }
            if (v != null) {
                v = TrnTypeUtils.treatFeatureValueForGate(tp, fn, v);
                board.put(fn, v);
            }
        }
    }

    public void fillBlackBoard(BlackBoard board) {
        if (trnItem != null)
            fillBlackBoard(trnItem.getTrn(), board);
        if (annItem != null)
            fillBlackBoard(annItem.ann, board);
    }

    public TrnType getType(int n) {
        if (trnItem != null) {
            LinkedTrns.Item cur = trnItem;
            int cnt = size - 1;
            while (cur != null) {
                if (cnt == n) {
                    return cur.getTrn().getType();
                }
                cur = cur.getPrevious();
                cnt--;
            }
        }
        if (annItem != null) {
            LinkedAnns.Item cur = annItem;
            int cnt = size - 1;
            while (cur != null) {
                if (cnt == n) {
                    try {
                        return phase.types.get(cur.ann.getType());
                    } catch (TreetonModelException e) {
                        return null;
                    }
                }
                cur = cur.previous;
                cnt--;
            }
        }
        return null;
    }

    public Object getValue(int n, int feature) {
        if (feature < 0)
            return null;
        if (trnItem != null) {
            LinkedTrns.Item cur = trnItem;
            int cnt = size - 1;
            while (cur != null) {
                if (cnt == n) {
                    return getValue(cur.getTrn(), feature);
                }
                cur = cur.getPrevious();
                cnt--;
            }
        }
        if (annItem != null) {
            LinkedAnns.Item cur = annItem;
            int cnt = size - 1;
            while (cur != null) {
                if (cnt == n) {
                    return getValue(cur.ann, feature);
                }
                cur = cur.previous;
                cnt--;
            }
        }
        return null;
    }

    public Object getValue(int n, String featureName) {
        TrnType tp = getType(n);
        int feature = 0;
        try {
            feature = tp.getFeatureIndex(featureName);
        } catch (TreetonModelException e) {
            return null;
        }
        return getValue(n, feature);
    }

    public void fillBlackBoard(int n, BlackBoard board) {
        if (trnItem != null) {
            LinkedTrns.Item cur = trnItem;
            int cnt = size - 1;
            while (cur != null) {
                if (cnt == n) {
                    fillBlackBoard(cur.getTrn(), board);
                }
                cur = cur.getPrevious();
                cnt--;
            }
        }
        if (annItem != null) {
            LinkedAnns.Item cur = annItem;
            int cnt = size - 1;
            while (cur != null) {
                if (cnt == n) {
                    fillBlackBoard(cur.ann, board);
                }
                cur = cur.previous;
                cnt--;
            }
        }
    }

    public Treenotation getTrn(int n) {
        LinkedTrns.Item cur = trnItem;
        int cnt = size - 1;
        while (cur != null) {
            if (cnt == n) {
                return cur.getTrn();
            }
            cur = cur.getPrevious();
            cnt--;
        }
        return null;
    }

    public Annotation getAnn(int n) {
        LinkedAnns.Item cur = annItem;
        int cnt = size - 1;
        while (cur != null) {
            if (cnt == n) {
                return cur.ann;
            }
            cur = cur.previous;
            cnt--;
        }
        return null;
    }

    public Token getStartToken() {
        return startToken;
    }

    public Token getEndToken() {
        return endToken;
    }

    public Node getStartNode() {
        return startNode;
    }

    public Node getEndNode() {
        return endNode;
    }

    public int getSize() {
        return size;
    }

    public Treenotation[] toTrnArray() {
        if (size <= 0)
            return new Treenotation[0];
        Treenotation[] arr = new Treenotation[size];
        LinkedTrns.Item cur = trnItem;
        int cnt = size - 1;
        while (cur != null) {
            arr[cnt] = cur.getTrn();
            cur = cur.getPrevious();
            cnt--;
        }
        return arr;
    }

    public Annotation[] toAnnArray() {
        if (size <= 0)
            return null;
        Annotation[] arr = new Annotation[size];
        LinkedAnns.Item cur = annItem;
        int cnt = size - 1;
        while (cur != null) {
            arr[cnt] = cur.ann;
            cur = cur.previous;
            cnt--;
        }
        return arr;
    }

}
