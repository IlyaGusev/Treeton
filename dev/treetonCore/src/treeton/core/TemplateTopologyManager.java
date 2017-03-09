/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnType;

import java.util.Map;

public class TemplateTopologyManager extends TopologyManager {
    public TemplateTopologyManager() {
        super();
    }

    public boolean mayBeAddedWeak(TreenotationImpl bounding, TreenotationImpl member) {
        if (target.isLocked())
            return false;

        if (bounding.isLocked())
            return false;

        if (member == bounding)
            return false;

        if (member == target)
            return false;

        if (parentsMap.get(member) != null) {
            return false;
        }

        if (bounding != target && boundingsMap.get(bounding) == null)
            return false;

        TreenotationImpl memberb = boundingsMap.get(member);

        if (memberb == bounding) {
            if (memberb.findNode(member).parentConnection == TreenotationImpl.PARENT_CONNECTION_WEAK)
                return false;
        }

        if (memberb == null) {
            if (!member.isLocked() && member.getContext() != null) {
                return false;
            }
            return checkCoverage(member);
        } else {
            if (isDescendant(bounding, memberb) && !isDescendant(bounding, member)) {
                return !memberb.isLocked();
            } else {
                return false;
            }
        }
    }

    public void addMemberWeak(TreenotationImpl bounding, TreenotationImpl member) {
        TreenotationImpl memberb = boundingsMap.get(member);

        if (memberb == bounding) {
            TreenotationImpl.Node memberNd = memberb.findNode(member);
            memberNd.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
            resizeDown(target);
            ensureVisibility();
            return;
        }

        super.addMemberWeak(bounding, member);
    }

    public boolean mayBeAddedPath(TreenotationImpl bounding, TreenotationImpl member) {
        if (bounding != target)
            return false;

        if (target.isLocked())
            return false;

        if (member == target)
            return false;

        if (parentsMap.get(member) != null) {
            return false;
        }

        TreenotationImpl memberb = boundingsMap.get(member);

        if (memberb == target) {
            return memberb.findNode(member).parentConnection != TreenotationImpl.PARENT_CONNECTION_PATH;
        }

        if (memberb == null) {
            if (!member.isLocked() && member.getContext() != null) {
                return false;
            }
            return checkCoverage(member);
        } else {
            return false;
        }
    }

    public void addMemberPath(TreenotationImpl bounding, TreenotationImpl member) {
        TreenotationImpl memberb = boundingsMap.get(member);

        if (memberb == bounding) {
            TreenotationImpl.Node memberNd = memberb.findNode(member);
            memberNd.parentConnection = TreenotationImpl.PARENT_CONNECTION_PATH;
            resizeDown(target);
            ensureVisibility();
            return;
        }

        if (memberb == null) {
            boundingsMap.put(member, bounding);
            incNumberOfContexts(member);
            if (bounding.nView > 0) {
                incNView(member, bounding.nView);
            }

            markTokens(member);
            TreenotationImpl.Node nd = bounding.addTree(member);
            nd.parentConnection = TreenotationImpl.PARENT_CONNECTION_PATH;
        }

        resizeDown(target);
        ensureVisibility();
    }

    public void aggregate(TreenotationImpl trn, TrnType tp, TreenotationImpl aggregate, boolean strong) {
        TreenotationImpl oldBnd = boundingsMap.get(trn);
        super.aggregate(trn, tp, aggregate, strong);
        if (oldBnd == null) {
            target.findNode(aggregate).parentConnection = TreenotationImpl.PARENT_CONNECTION_PATH;
        }
    }

    public void removeTreenotation(TreenotationImpl trn) {
        TreenotationImpl bounding = boundingsMap.get(trn);
        boundingsMap.remove(trn);
        TreenotationImpl parent = parentsMap.get(trn);

        if (parent != null) {
            parentsMap.remove(trn);
        }

        TreenotationImpl.Node nd;
        int oldPc;
        if (parent == null || parent == bounding) {
            nd = bounding.removeTree(trn);
            oldPc = nd.parentConnection;
        } else {
            TreenotationImpl.Node parentNd = bounding.findNode(parent);
            oldPc = bounding.findRootNode(parent).parentConnection;
            if (oldPc == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                oldPc = TreenotationImpl.PARENT_CONNECTION_WEAK;
            }
            nd = parentNd.removeRelation(trn);
        }

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        child.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
                        bounding.addTree(child);
                        if (bounding == target) {
                            child.parentConnection = oldPc;
                        }
                        parentsMap.remove(child.trn);
                    }
                }
            }
        }

        if (trn.trees != null) {
            for (TreenotationImpl.Node tree : trn.trees) {
                if (tree == null)
                    continue;
                tree.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
                bounding.addTree(tree);
                if (trn.isAdded()) {
                    storage.decNView(tree.trn, 1);
                }

                if (bounding == target) {
                    tree.parentConnection = oldPc;
                }
                if (parentsMap.containsKey(tree.trn)) {
                    parentsMap.remove(tree.trn);
                }
                for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMap.entrySet()) {
                    if (entry.getValue() == trn) {
                        entry.setValue(bounding);
                    }
                }
            }
        }

        decNumberOfContexts(trn);
        trn.trees = null;

        resizeDown(target);
        ensureVisibility();
    }

    public void removeIncomingLink(TreenotationImpl slave) {
        TreenotationImpl bounding = boundingsMap.get(slave);

        TreenotationImpl parent = parentsMap.get(slave);

        parentsMap.remove(slave);

        if (parent == bounding) {
            TreenotationImpl.Node nd;
            nd = bounding.findNode(slave);
            nd.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
        } else {
            TreenotationImpl.Node parentNd = bounding.findNode(parent);
            int oldPc = -1;
            if (bounding == target) {
                oldPc = bounding.findRootNode(parent).parentConnection;
                if (oldPc == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                    oldPc = TreenotationImpl.PARENT_CONNECTION_WEAK;
                }
            }
            TreenotationImpl.Node nd = parentNd.removeRelation(slave);
            nd.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
            bounding.addTree(nd);
            if (bounding == target) {
                nd.parentConnection = oldPc;
            }
        }

        resizeDown(target);
        ensureVisibility();

    }
}
