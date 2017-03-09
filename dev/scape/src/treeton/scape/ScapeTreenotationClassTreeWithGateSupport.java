/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.Annotation;
import gate.Document;
import gate.FeatureMap;
import treeton.core.fsm.ScapeTreenotationClassTree;
import treeton.core.fsm.logicset.LogicState;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.model.TrnTypeUtils;
import treeton.core.util.nu;

public class ScapeTreenotationClassTreeWithGateSupport extends ScapeTreenotationClassTree {
    public ScapeTreenotationClassTreeWithGateSupport(TrnTypeStorage types) {
        super(types);
    }

    public int getAnnotationClass(Annotation ann, Document doc) {
        FinalNode fn = getFinalState(ann, doc);
        if (fn == null) {
            return -1;
        } else {
            return fn.unsafeGetId();
        }
    }

    private FinalNode getFinalState(Annotation ann, Document doc) {
        try {
            curType = types.get(ann.getType());
        } catch (TreetonModelException e) {
            curType = null;
        }
        if (root == null || root.map == null)
            return null;
        Node cur = (Node) root.map.get(curType);

        if (cur == null)
            return null;

        FeatureMap fm = ann.getFeatures();

        while (true) {
            if (cur instanceof LogicFSMDistributer) {
                LogicFSMDistributer d = (LogicFSMDistributer) cur;
                if (TrnType.string_FEATURE == d.key || TrnType.orthm_FEATURE == d.key) {
                    String s = ScapeBinding.getText(doc, ann);
                    LogicState state = d.map.match(s);
                    if (state == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    } else {
                        cur = (Node) state.getData();
                    }
                } else {
                    Object o;
                    try {
                        o = fm.get(curType.getFeatureNameByIndex(d.key));
                    } catch (TreetonModelException e) {
                        o = null;
                    }
                    if (o == null) {
                        if (d.nullTransition != null) {
                            cur = d.nullTransition;
                            continue;
                        }
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    } else {
                        String s = o.toString();
                        LogicState state = d.map.match(s);
                        if (state == null) {
                            if (d.otherTransition != null) {
                                cur = d.otherTransition;
                                continue;
                            }
                            return null;
                        } else {
                            cur = (Node) state.getData();
                        }
                    }
                }
            } else if (cur instanceof Distributer) {
                Distributer d = (Distributer) cur;

                if (TrnType.string_FEATURE == d.key || TrnType.orthm_FEATURE == d.key) {
                    String s = ScapeBinding.getText(doc, ann);
                    if (isCaseInsensetive(d.key)) {
                        s = s.toLowerCase();
                    }
                    cur = (Node) d.map.get(s);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                } else if (TrnType.length_FEATURE == d.key) {
                    Integer i = ScapeBinding.getText(doc, ann).length();
                    cur = (Node) d.map.get(i);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                } else if (TrnType.start_FEATURE == d.key) {
                    Integer i = ann.getStartNode().getOffset().intValue();
                    cur = (Node) d.map.get(i);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                } else if (TrnType.end_FEATURE == d.key) {
                    Integer i = ann.getEndNode().getOffset().intValue();
                    cur = (Node) d.map.get(i);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                } else {
                    Object o;
                    try {
                        o = fm.get(curType.getFeatureNameByIndex(d.key));
                    } catch (TreetonModelException e) {
                        o = null;
                    }
                    if (o == null) {
                        o = nu.ll;
                    } else if (isCaseInsensetive(d.key)) {
                        o = o.toString().toLowerCase();
                    } else {
                        o = TrnTypeUtils.treatFeatureValue(curType, d.key, o);
                    }
                    cur = (Node) d.map.get(o);
                    if (cur == null) {
                        if (d.otherTransition != null) {
                            cur = d.otherTransition;
                            continue;
                        }
                        return null;
                    }
                }
            } else if (cur instanceof SequenceReader) {
                SequenceReader sr = (SequenceReader) cur;
                for (int i = 0; i < sr.map.size(); i++) {
                    int key = sr.map.getIndexByNumber(i);

                    if (key == TrnType.string_FEATURE || key == TrnType.orthm_FEATURE) {
                        String s = ScapeBinding.getText(doc, ann);
                        if (isCaseInsensetive(key)) {
                            s = s.toLowerCase();
                        }
                        if (!s.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    } else if (key == TrnType.length_FEATURE) {
                        Integer in = ScapeBinding.getText(doc, ann).length();
                        if (!in.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    } else if (key == TrnType.start_FEATURE) {
                        Integer in = ann.getStartNode().getOffset().intValue();
                        if (!in.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    } else if (key == TrnType.end_FEATURE) {
                        Integer in = ann.getEndNode().getOffset().intValue();
                        if (!in.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    } else {
                        Object o;
                        try {
                            o = fm.get(curType.getFeatureNameByIndex(key));
                        } catch (TreetonModelException e) {
                            o = null;
                        }
                        if (o == null) {
                            o = nu.ll;
                        } else if (isCaseInsensetive(key)) {
                            o = o.toString().toLowerCase();
                        } else {
                            o = TrnTypeUtils.treatFeatureValue(curType, key, o);
                        }
                        if (!o.equals(sr.map.getByNumber(i))) {
                            return null;
                        }
                    }
                }
                cur = sr.nd;
            } else if (cur instanceof FinalNode) {
                return (FinalNode) cur;
            }
        }
    }


}
