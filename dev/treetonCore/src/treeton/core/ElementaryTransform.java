/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnRelationType;
import treeton.core.model.TrnType;

public class ElementaryTransform {
    TransformType type;
    TreenotationImpl trn1;
    TreenotationImpl trn2;
    TrnRelationType relType;
    TrnType tp;

    public ElementaryTransform(TransformType type, TreenotationImpl trn1, TreenotationImpl trn2, TrnRelationType relType, TrnType tp) {
        this.type = type;
        this.trn1 = trn1;
        this.trn2 = trn2;
        this.relType = relType;
        this.tp = tp;
    }

    public TransformType getType() {
        return type;
    }

    public Treenotation getFirstTrn() {
        return trn1;
    }

    public Treenotation getSecondTrn() {
        return trn2;
    }

    public TrnRelationType getRelType() {
        return relType;
    }

    public TrnType getAggroType() {
        return tp;
    }

    public void perform(TopologyManager manager) {
        if (type == TransformType.TRANSFORM_LINK) {
            manager.link(trn1, trn2, relType);
        } else if (type == TransformType.TRANSFORM_AGGREGATE_STRONG) {
            if (trn2 == null) {
                trn2 = manager.aggregate(trn1, tp, true);
            } else {
                manager.aggregate(trn1, tp, trn2, true);
            }
        } else if (type == TransformType.TRANSFORM_AGGREGATE_WEAK) {
            if (trn2 == null) {
                trn2 = manager.aggregate(trn1, tp, false);
            } else {
                manager.aggregate(trn1, tp, trn2, false);
            }
        } else if (type == TransformType.TRANSFORM_ADDMEMBER_STRONG) {
            manager.addMemberStrong(trn1, trn2);
        } else if (type == TransformType.TRANSFORM_ADDMEMBER_WEAK) {
            manager.addMemberWeak(trn1, trn2);
        } else if (type == TransformType.TRANSFORM_ADDMEMBER_PATH) {
            ((TemplateTopologyManager) manager).addMemberPath(trn1, trn2);
        }
    }

    public boolean mayBePerformed(TopologyManager manager) {
        if (type == TransformType.TRANSFORM_LINK) {
            return manager.mayBeLinked(trn1, trn2);
        } else if (type == TransformType.TRANSFORM_AGGREGATE_STRONG) {
            return manager.mayBeAggregated(trn1);
        } else if (type == TransformType.TRANSFORM_AGGREGATE_WEAK) {
            return manager.mayBeAggregated(trn1);
        } else if (type == TransformType.TRANSFORM_ADDMEMBER_STRONG) {
            return manager.mayBeAddedStrong(trn1, trn2);
        } else if (type == TransformType.TRANSFORM_ADDMEMBER_WEAK) {
            return manager.mayBeAddedWeak(trn1, trn2);
        } else if (type == TransformType.TRANSFORM_ADDMEMBER_PATH) {
            return ((TemplateTopologyManager) manager).mayBeAddedPath(trn1, trn2);
        }
        return false;
    }

    public TreenotationImpl getAggro() {
        if (type != TransformType.TRANSFORM_AGGREGATE_STRONG && type != TransformType.TRANSFORM_AGGREGATE_WEAK)
            return null;
        return trn2;
    }

    public enum TransformType {
        TRANSFORM_LINK,
        TRANSFORM_ADDMEMBER_STRONG,
        TRANSFORM_ADDMEMBER_WEAK,
        TRANSFORM_ADDMEMBER_PATH,
        TRANSFORM_AGGREGATE_STRONG,
        TRANSFORM_AGGREGATE_WEAK
    }
}
