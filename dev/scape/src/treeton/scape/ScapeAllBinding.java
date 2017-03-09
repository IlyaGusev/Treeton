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
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeUtils;
import treeton.core.scape.ScapeVariable;
import treeton.core.util.LinkedTrns;

import java.util.Map;

public class ScapeAllBinding extends ScapeBinding implements ScapeVariable {
    ScapeResult result;
    ScapeResultForGate gateResult;
    Document doc;

    public TrnType getType() {
        if (result != null)
            return result.lastMatched.getTrn().getType();
        if (gateResult != null)
            try {
                return gateResult.phase.types.get(gateResult.lastMatched.ann.getType());
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

        TrnType tp;
        String fName;
        try {
            tp = gateResult.phase.types.get(ann.getType());
            fName = tp.getFeatureNameByIndex(feature);
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
            if (result != null) {
                return result.start.getStartNumerator();
            }
            if (gateResult != null) {
                return gateResult.start.getOffset().intValue();
            }
            return null;
        } else if (feature == TrnType.end_FEATURE) {
            if (result != null) {
                return result.end.getEndNumerator();
            }
            if (gateResult != null) {
                return gateResult.end.getOffset().intValue();
            }
            return null;
        }

        if (result != null)
            return getValue(result.lastMatched.getTrn(), feature);
        if (gateResult != null)
            return getValue(gateResult.lastMatched.ann, feature);
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

        for (Map.Entry<String, Object> entry : ((Map<String, Object>) fm).entrySet()) {
            String f = entry.getKey();
            Object v = entry.getValue();
            int fn;
            try {
                fn = tp.getFeatureIndex(f);
            } catch (TreetonModelException e) {
                fn = -1;
            }
            if (v != null) {
                v = TrnTypeUtils.treatFeatureValueForGate(tp, fn, v);
                board.put(fn, v);
            }
        }
    }

    public void fillBlackBoard(BlackBoard board) {
        if (result != null)
            fillBlackBoard(result.lastMatched.getTrn(), board);
        if (gateResult != null)
            fillBlackBoard(gateResult.lastMatched.ann, board);
    }

    public TrnType getType(int n) {
        if (result != null) {
            LinkedTrns.Item cur = result.lastMatched;
            int cnt = result.size - 1;
            while (cur != null) {
                if (cnt == n) {
                    return cur.getTrn().getType();
                }
                cur = cur.getPrevious();
                cnt--;
            }
        }
        if (gateResult != null) {
            LinkedAnns.Item cur = gateResult.lastMatched;
            int cnt = gateResult.size - 1;
            while (cur != null) {
                if (cnt == n) {
                    try {
                        return gateResult.phase.types.get(cur.ann.getType());
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
        if (result != null) {
            LinkedTrns.Item cur = result.lastMatched;
            int cnt = result.size - 1;
            while (cur != null) {
                if (cnt == n) {
                    return getValue(cur.getTrn(), feature);
                }
                cur = cur.getPrevious();
                cnt--;
            }
        }
        if (gateResult != null) {
            LinkedAnns.Item cur = gateResult.lastMatched;
            int cnt = gateResult.size - 1;
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
        int feature;
        try {
            feature = tp.getFeatureIndex(featureName);
        } catch (TreetonModelException e) {
            feature = -1;
        }
        return getValue(n, feature);
    }

    public void fillBlackBoard(int n, BlackBoard board) {
        if (result != null) {
            LinkedTrns.Item cur = result.lastMatched;
            int cnt = result.size - 1;
            while (cur != null) {
                if (cnt == n) {
                    fillBlackBoard(cur.getTrn(), board);
                }
                cur = cur.getPrevious();
                cnt--;
            }
        }
        if (gateResult != null) {
            LinkedAnns.Item cur = gateResult.lastMatched;
            int cnt = gateResult.size - 1;
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
        LinkedTrns.Item cur = result.lastMatched;
        int cnt = result.size - 1;
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
        LinkedAnns.Item cur = gateResult.lastMatched;
        int cnt = gateResult.size - 1;
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
        if (result == null)
            return null;
        return result.start;
    }

    public Token getEndToken() {
        if (result == null)
            return null;
        return result.end;
    }

    public Node getStartNode() {
        if (gateResult == null)
            return null;
        return gateResult.start;
    }

    public Node getEndNode() {
        if (gateResult == null)
            return null;
        return gateResult.end;
    }

    public int getSize() {
        if (result != null)
            return result.size();
        if (gateResult != null)
            return gateResult.size();
        return -1;
    }

    public Treenotation[] toTrnArray() {
        if (result.size <= 0)
            return null;
        Treenotation[] arr = new Treenotation[result.size];
        LinkedTrns.Item cur = result.lastMatched;
        int cnt = result.size - 1;
        while (cur != null) {
            arr[cnt] = cur.getTrn();
            cur = cur.getPrevious();
            cnt--;
        }
        return arr;
    }

    public Annotation[] toAnnArray() {
        if (gateResult.size <= 0)
            return null;
        Annotation[] arr = new Annotation[gateResult.size];
        LinkedAnns.Item cur = gateResult.lastMatched;
        int cnt = gateResult.size - 1;
        while (cur != null) {
            arr[cnt] = cur.ann;
            cur = cur.previous;
            cnt--;
        }
        return arr;
    }

    public void attach(ScapeResult r) {
        result = r;
        gateResult = null;
    }

    public void attach(ScapeResultForGate r) {
        result = null;
        gateResult = r;
        doc = r.phase.doc;
    }

    public void detach() {
        result = null;
        gateResult = null;
        doc = null;
    }
}
