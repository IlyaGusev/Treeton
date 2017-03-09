/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.fsm.logicset.LogicFSM;
import treeton.core.model.TrnType;
import treeton.core.scape.ScapeVariable;
import treeton.core.util.nu;
import treeton.core.util.sut;

public class TreenotationUtil {
    public static boolean match(IntFeatureMap map, IntFeatureMap template, boolean excludeSystemFeatures) {
        if (template == null) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < template.size(); i++) {
            Object o = template.getByIndex(i);
            if (o != null) {
                int other_feature = template.getKey(i);
                if (excludeSystemFeatures && (other_feature < TrnType.getNumberOfSystemFeatures()))
                    continue;
                Object o1 = map.get(other_feature);
                if (o instanceof LogicFSM) {
                    LogicFSM fsm = (LogicFSM) o;
                    if (o1 == null || fsm.match(o1.toString()) == null) {
                        return false;
                    }
                } else {
                    if (o == nu.ll) {
                        if (o1 != null) {
                            return false;
                        }
                    } else {
                        if (o1 == null) {
                            return false;
                        }
                        if (!o.equals(o1)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public static boolean match(ScapeVariable var, IntFeatureMap template, boolean excludeSystemFeatures) {
        if (template == null) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < template.size(); i++) {
            Object o = template.getByIndex(i);
            if (o != null) {
                int other_feature = template.getKey(i);
                if (excludeSystemFeatures && (other_feature < TrnType.getNumberOfSystemFeatures()))
                    continue;
                Object o1 = var.getValue(other_feature);
                if (o instanceof LogicFSM) {
                    LogicFSM fsm = (LogicFSM) o;
                    if (o1 == null || fsm.match(o1.toString()) == null) {
                        return false;
                    }
                } else {
                    if (o == nu.ll) {
                        if (o1 != null) {
                            return false;
                        }
                    } else {
                        if (o1 == null) {
                            return false;
                        }
                        if (!o.equals(o1)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public static boolean match(Treenotation trn, Treenotation template) {
        if (template == null) {
            return false;
        }
        if (!template.getType().equals(trn.getType())) {
            return false;
        }
        String s = null;
        Object o = template.get(TrnType.string_FEATURE);
        if (o != null) {
            s = trn.getText();
            if (!o.toString().equals(s)) {
                return false;
            }
        }
        o = template.get(TrnType.length_FEATURE);
        if (o != null) {
            if (s == null) {
                s = trn.getText();
            }
            if (!o.equals(s.length())) {
                return false;
            }
        }
        o = template.get(TrnType.orthm_FEATURE);
        if (o != null) {
            if (s == null) {
                s = trn.getText();
            }
            if (!sut.matchTemplate(s, o.toString())) {
                return false;
            }
        }
        o = template.get(TrnType.start_FEATURE);
        if (o != null) {
            if (!o.equals(trn.getStartNumerator())) {
                return false;
            }
        }

        o = template.get(TrnType.end_FEATURE);
        if (o != null) {
            if (!o.equals(trn.getEndNumerator())) {
                return false;
            }
        }

        return match(trn, template, true);
    }

    public static boolean match(ScapeVariable variable, Treenotation template) {
        return template != null && template.getType().equals(variable.getType()) && match(variable, template, false);
    }
}
