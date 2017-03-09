/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnRelationType;
import treeton.core.model.TrnType;
import treeton.core.util.BlockStack;
import treeton.core.util.MutableInteger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

public class TopologyManager {
    protected TreenotationStorageImpl storage;
    protected HashMap<TreenotationImpl, TreenotationImpl> boundingsMap = new HashMap<TreenotationImpl, TreenotationImpl>();
    protected HashMap<TreenotationImpl, TreenotationImpl> parentsMap = new HashMap<TreenotationImpl, TreenotationImpl>();
    protected HashMap<TreenotationImpl, TreenotationImpl> boundingsMapVirtual = new HashMap<TreenotationImpl, TreenotationImpl>();
    protected HashMap<TreenotationImpl, TreenotationImpl> parentsMapVirtual = new HashMap<TreenotationImpl, TreenotationImpl>();
    protected TreenotationImpl target;
    short bar = -1;
    private boolean removeEmpty = true;
    private boolean checkCoverage = true;
    private long virtualStackpos = -1;
    private BlockStack markStack = null;
    private boolean targetVisible;
    private Stack<Change> changes;
    private boolean undoEnabled = false;

    //todo доделать forget
    public boolean isCheckCoverage() {
        return checkCoverage;
    }

    public void setCheckCoverage(boolean checkCoverage) {
        this.checkCoverage = checkCoverage;
    }

    public void clean() {
        if (!isConnected())
            return;

        for (Map.Entry<TreenotationImpl, TreenotationImpl> e : boundingsMap.entrySet()) {
            TreenotationImpl t = e.getKey();
            decNumberOfContexts(t);
            if (!t.isLocked()) {
                t.trees = null;
                if (t.isAdded()) {
                    storage.decNView(t, t.nView - 1);
                    t.nView = 1;
                } else {
                    storage.decNView(t, t.nView);
                }
            } else {
                TreenotationImpl b = e.getValue();
                if (b.nView > 0) {
                    storage.decNView(t, b.nView);
                }
            }
        }
        target.trees = null;
        ensureVisibility();

        while (!markStack.isEmpty()) {
            TreenotationImpl trn = (TreenotationImpl) markStack.pop();
            trn.unmark(bar);
        }

        boundingsMap.clear();
        parentsMap.clear();

        if (undoEnabled) {
            changes.clear();
        }
    }

    public boolean isTargetVisible() {
        return targetVisible;
    }

    public void setTargetVisible(boolean targetVisible) {
        this.targetVisible = targetVisible;
        ensureVisibility();
    }

    protected void ensureVisibility() {
        if (targetVisible) {
            if (target.isEmpty() && removeEmpty) {
                if (target.isAdded()) {
                    storage._remove(target);
                }
                this.target.startToken = this.target.endToken = null;
            } else if (!target.isAdded()) {
                storage.add(target);
            }
        } else {
            if (target.isEmpty() && removeEmpty) {
                if (target.isAdded()) {
                    storage._remove(target);
                }
                this.target.startToken = this.target.endToken = null;
            } else if (target.isAdded()) {
                storage._remove(target);
            }
        }
    }

    public void connect(Treenotation target) {
        if (target.getContext() != null) {
            throw new RuntimeException("The target treenotation already belongs to some context");
        }
        if (!(target instanceof TreenotationSyntax)) {
            throw new RuntimeException("The target treenotation must be the instance of the TreenotationSyntax class");
        }
        this.target = (TreenotationImpl) target;
        this.target.context = this;
        this.storage = (TreenotationStorageImpl) target.getStorage();
        targetVisible = target.isAdded();
        if (storage == null) {
            throw new RuntimeException("The target doesn't belong to any TreenotationStorage");
        }
        bar = this.storage.getChannel();
        markStack = this.storage.getStack();
        slurpContents((TreenotationImpl) target);
        resizeDown(this.target);
        ensureVisibility();
    }

    public boolean isConnected() {
        return storage != null;
    }

    public void disconnect() {
        if (!isConnected())
            return;
        while (!markStack.isEmpty()) {
            TreenotationImpl trn = (TreenotationImpl) markStack.pop();
            trn.unmark(bar);
        }
        storage.freeStack(markStack);
        storage.freeChannel(bar);
        storage = null;
        bar = -1;
        target.context = null;
        target = null;

        boundingsMap.clear();
        parentsMap.clear();

        if (undoEnabled) {
            changes.clear();
        }
    }

    private void slurpContents(TreenotationImpl trn) {
        if (trn.trees == null)
            return;
        for (TreenotationImpl.Node tree : trn.trees) {
            if (tree == null)
                continue;
            slurp(tree,
                    tree.parentConnection == TreenotationImpl.PARENT_CONNECTION_STRONG
                            ? trn : null, trn);
        }
    }

    protected void decNumberOfContexts(TreenotationImpl trn) {
        Object o = trn.getContext();
        if (o == null) {
        } else if (o instanceof Treenotation) {
            trn.context = null;
        } else {
            MutableInteger m = (MutableInteger) o;
            if (m.value == 1) {
                trn.context = null;
            } else {
                m.value--;
            }
        }
    }

    protected void incNumberOfContexts(TreenotationImpl trn) {
        logChange_INC_CONTEXT(trn);

        Object o = trn.getContext();
        if (o == null) {
            trn.context = target;
        } else if (o instanceof Treenotation) {
            trn.context = new MutableInteger(2);
        } else {
            ((MutableInteger) o).value++;
        }
    }

    private void slurp(TreenotationImpl.Node nd, TreenotationImpl parent, TreenotationImpl bounding) {
        TreenotationImpl trn = nd.trn;

        if (parent != null) {
            parentsMap.put(trn, parent);
        }

        if (bounding != null) {
            boundingsMap.put(trn, bounding);
        }

        if (trn.isEmpty() && trn.isLocked()) {
            markTokens(trn);
        } else if (!trn.isEmpty()) {
            slurpContents(trn);
        }

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        slurp(child, trn, bounding);
                    }
                }
            }
        }
    }

    protected void markTokens(TreenotationImpl cur) {
        TokenImpl start = (TokenImpl) cur.getStartToken();
        TokenImpl end = (TokenImpl) cur.getEndToken();
        while (start != end) {
            if (!start.isMarked(bar)) {
                start.mark(bar);
                markStack.push(start);
            }
            start = (TokenImpl) start.getNextToken();
        }
        if (!start.isMarked(bar)) {
            start.mark(bar);
            markStack.push(start);
        }
    }

    public boolean checkCoverage(TreenotationImpl trn) {
        if (!trn.isEmpty() || !trn.isLocked()) { //todo: implement without these constraints
            return false;
        }

        if (!checkCoverage)
            return true;

        TokenImpl start = (TokenImpl) trn.getStartToken();
        TokenImpl end = (TokenImpl) trn.getEndToken();
        while (start != end) {
            if (start.isMarked(bar)) {
                return false;
            }
            start = (TokenImpl) start.getNextToken();
        }
        return !start.isMarked(bar);
    }

    private boolean checkCoverageBetween(TreenotationImpl t1, TreenotationImpl t2) {
        if (!t1.isEmpty() || !t1.isLocked()) { //todo: implement without these constraints
            return false;
        }
        if (!t2.isEmpty() || !t2.isLocked()) { //todo: implement without these constraints
            return false;
        }

        return !t1.intersects(t2);
    }

    public boolean mayBeLinked(TreenotationImpl host, TreenotationImpl slave) {
        if (target.isLocked())
            return false;

        if (host == slave)
            return false;

        if (host == target || slave == target)
            return false;

        if (parentsMap.get(slave) != null) {
            return false;
        }

        TreenotationImpl hostb = boundingsMap.get(host);
        TreenotationImpl slaveb = boundingsMap.get(slave);

        if (hostb == null) {
            if (!host.isLocked() && host.getContext() != null) {
                return false;
            }
            if (slaveb == null) {
                if (!slave.isLocked() && slave.getContext() != null) {
                    return false;
                }

                return checkCoverage(host) && checkCoverage(slave) && checkCoverageBetween(host, slave);
            } else {
                if (slaveb != target)
                    return false;
                if (slaveb.isLocked())
                    return false;
                return checkCoverage(host);
            }
        } else {
            if (slaveb == null) {
                if (hostb.isLocked())
                    return false;
                return checkCoverage(slave);
            } else {
                if (isDescendant(hostb, slaveb) && !isDescendant(host, slave)) {
                    return !hostb.isLocked();
                } else {
                    return false;
                }
            }
        }
    }

    public boolean linkVirtual(TreenotationImpl host, TreenotationImpl slave) {
        if (target.isLocked())
            return false;

        if (host == slave)
            return false;

        if (host == target || slave == target)
            return false;

        if (parentsMapVirtual.get(slave) != null || parentsMap.get(slave) != null) {
            return false;
        }

        TreenotationImpl hostb = boundingsMapVirtual.get(host);
        if (hostb == null) {
            hostb = boundingsMap.get(host);
        }

        TreenotationImpl slaveb = boundingsMapVirtual.get(slave);
        if (slaveb == null) {
            slaveb = boundingsMap.get(slave);
        }

        if (hostb == null) {
            if (!host.isLocked() && host.getContext() != null) {
                return false;
            }

            if (slaveb == null) {
                if (!slave.isLocked() && slave.getContext() != null) {
                    return false;
                }

                if (checkCoverage(host) && checkCoverage(slave) && checkCoverageBetween(host, slave)) {
                    parentsMapVirtual.put(slave, host);

                    boundingsMapVirtual.put(host, target);
                    boundingsMapVirtual.put(slave, target);

                    markTokens(host);
                    markTokens(slave);
                } else {
                    return false;
                }
            } else {
                if (slaveb != target)
                    return false;
                if (slaveb.isLocked())
                    return false;
                if (checkCoverage(host)) {
                    parentsMapVirtual.put(slave, host);

                    boundingsMapVirtual.put(host, target);
                    markTokens(host);
                } else {
                    return false;
                }
            }
        } else {
            if (slaveb == null) {
                if (hostb.isLocked())
                    return false;
                if (checkCoverage(slave)) {
                    parentsMapVirtual.put(slave, host);

                    boundingsMapVirtual.put(slave, hostb);
                    markTokens(slave);
                } else {
                    return false;
                }
            } else {
                if (isDescendant(hostb, slaveb) && !isDescendant(host, slave)) {
                    if (hostb.isLocked())
                        return false;
                    for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMapVirtual.entrySet()) {
                        if (entry.getValue() == slaveb) {
                            if (isDescendant(entry.getKey(), slave)) {
                                entry.setValue(hostb);
                            }
                        }
                    }
                    for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMap.entrySet()) {
                        if (entry.getValue() == slaveb) {
                            if (isDescendant(entry.getKey(), slave)) {
                                boundingsMapVirtual.put(entry.getKey(), hostb);
                            }
                        }
                    }
                    parentsMapVirtual.put(slave, host);

                } else {
                    return false;
                }
            }
        }
        return true;
    }

    protected void logTransformStart() {
        if (undoEnabled) {
            changes.push(new TRANSFORM_STARTED(markStack.getPosition()));
        }
    }

    protected void logChange_HASHMAP(HashMap<TreenotationImpl, TreenotationImpl> map, TreenotationImpl key, TreenotationImpl oldV, TreenotationImpl newV) {
        if (undoEnabled) {
            changes.push(new HASHMAP_Change(map, key, oldV, newV));
        }
    }

    protected void logChange_INC_CONTEXT(TreenotationImpl trn) {
        if (undoEnabled) {
            changes.push(new INC_CONTEXT_Change(trn));
        }
    }

    private void logChange_INC_VIEW(TreenotationImpl trn, int i) {
        if (undoEnabled) {
            changes.push(new INC_VIEW_Change(trn, i));
        }

    }

    protected void logChange_ADD_TREE(TreenotationImpl bounding, TreenotationImpl.Node nd) {
        if (undoEnabled) {
            changes.push(new ADD_TREE_Change(bounding, nd));
        }
    }

    protected void logChange_REMOVE_TREE(TreenotationImpl bounding, TreenotationImpl.Node nd) {
        if (undoEnabled) {
            changes.push(new REMOVE_TREE_Change(bounding, nd));
        }
    }

    protected void logChange_ADD_RELATION(TreenotationImpl.Node host, TreenotationImpl.Node slave) {
        if (undoEnabled) {
            changes.push(new ADD_RELATION_Change(host, slave));
        }
    }

    protected void logChange_START_OFFSET(TreenotationImpl trn, TokenImpl oldV, TokenImpl newV) {
        if (undoEnabled) {
            changes.push(new START_OFFSET_Change(trn, oldV, newV));
        }
    }

    protected void logChange_END_OFFSET(TreenotationImpl trn, TokenImpl oldV, TokenImpl newV) {
        if (undoEnabled) {
            changes.push(new END_OFFSET_Change(trn, oldV, newV));
        }
    }

    public void setUndoEnabled(boolean undoEnabled) {
        if (undoEnabled == this.undoEnabled) {
            return;
        }

        this.undoEnabled = undoEnabled;
        if (undoEnabled) {
            changes = new Stack<Change>();
        } else {
            changes = null;
        }
    }

    public void link(TreenotationImpl host, TreenotationImpl slave, TrnRelationType tp) {
        logTransformStart();

        logChange_HASHMAP(parentsMap, slave, null, host);
        parentsMap.put(slave, host);

        TreenotationImpl hostb = boundingsMap.get(host);
        TreenotationImpl slaveb = boundingsMap.get(slave);

        if (hostb == null) {
            logChange_HASHMAP(boundingsMap, host, null, target);
            boundingsMap.put(host, target);
            incNumberOfContexts(host);
            if (target.nView > 0) {
                incNView(host, target.nView);
            }
            if (slaveb == null) {
                logChange_HASHMAP(boundingsMap, slave, null, target);
                boundingsMap.put(slave, target);
                incNumberOfContexts(slave);
                if (target.nView > 0) {
                    incNView(slave, target.nView);
                }


                TreenotationImpl.Node hostnd = target.addTree(host);
                logChange_ADD_TREE(target, hostnd);
                TreenotationImpl.Node slavend = hostnd.addRelation(slave, tp, null);
                logChange_ADD_RELATION(hostnd, slavend);
                markTokens(host);
                markTokens(slave);
                resizeUp(slave);
            } else {
                TreenotationImpl.Node oldNd = slaveb.removeTree(slave);
                logChange_REMOVE_TREE(slaveb, oldNd);
                TreenotationImpl.Node hostnd = target.addTree(host);
                logChange_ADD_TREE(target, hostnd);
                TreenotationImpl.Node slavend = hostnd.addRelation(slave, tp, oldNd);
                logChange_ADD_RELATION(hostnd, slavend);
                markTokens(host);
                resizeUp(host);
            }
        } else {
            if (slaveb == null) {
                logChange_HASHMAP(boundingsMap, slave, null, hostb);
                boundingsMap.put(slave, hostb);
                incNumberOfContexts(slave);
                if (hostb.nView > 0) {
                    incNView(slave, hostb.nView);
                }

                TreenotationImpl.Node nd = hostb.findNode(host);
                markTokens(slave);
                TreenotationImpl.Node slavend = nd.addRelation(slave, tp, null);
                logChange_ADD_RELATION(nd, slavend);
                resizeUp(slave);
            } else {
                TokenImpl min = (TokenImpl) slave.getStartToken();
                TokenImpl max = (TokenImpl) slave.getEndToken();
                int d = hostb.nView - slaveb.nView;
                for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMap.entrySet()) {
                    if (entry.getValue() == slaveb) {
                        if (isDescendant(entry.getKey(), slave)) {
                            TokenImpl pmin = (TokenImpl) entry.getKey().getStartToken();
                            TokenImpl pmax = (TokenImpl) entry.getKey().getEndToken();
                            if (pmin != null && pmin.compareTo(min) < 0) {
                                min = pmin;
                            }
                            if (pmax != null && pmax.compareTo(max) > 0) {
                                max = pmax;
                            }
                            logChange_HASHMAP(boundingsMap, entry.getKey(), slaveb, hostb);
                            entry.setValue(hostb);
                            if (d > 0) {
                                incNView(entry.getKey(), d);
                            }
                        }
                    }
                }

                TreenotationImpl.Node hostNd = hostb.findNode(host);
                TreenotationImpl.Node slaveNd = slaveb.removeTree(slave);
                logChange_REMOVE_TREE(slaveb, slaveNd);
                hostNd.addRelation(slave, tp, slaveNd);
                logChange_ADD_RELATION(hostNd, slaveNd);
                ensureBoundings(hostb, min, max);
                resizeUp(hostb);
            }
        }

        if (!removeEmpty)
            resizeDown(target);

    }

    protected void incNView(TreenotationImpl host, int i) {
        logChange_INC_VIEW(host, i);
        storage.incNView(host, i);
    }

    private void decNView(TreenotationImpl host, int i) {
        storage.decNView(host, i);
    }

    public boolean mayBeAddedStrong(TreenotationImpl bounding, TreenotationImpl member) {
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

    public boolean addMemberStrongVirtual(TreenotationImpl bounding, TreenotationImpl member) {
        if (target.isLocked())
            return false;

        if (bounding.isLocked())
            return false;

        if (member == bounding)
            return false;

        if (member == target)
            return false;

        if (parentsMapVirtual.get(member) != null || parentsMap.get(member) != null) {
            return false;
        }

        if (bounding != target && (boundingsMapVirtual.get(bounding) == null && boundingsMap.get(bounding) == null))
            return false;

        TreenotationImpl memberb = boundingsMapVirtual.get(member);
        if (memberb == null) {
            memberb = boundingsMap.get(member);
        }

        if (memberb == bounding) {
            return true;
        }

        if (memberb == null) {
            if (!member.isLocked() && member.getContext() != null) {
                return false;
            }
            if (checkCoverage(member)) {
                boundingsMapVirtual.put(member, bounding);
                parentsMapVirtual.put(member, bounding);
                markTokens(member);
            }
        } else {
            if (isDescendant(bounding, memberb) && !isDescendant(bounding, member)) {
                if (memberb.isLocked())
                    return false;
                for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMapVirtual.entrySet()) {
                    if (entry.getValue() == memberb) {
                        if (isDescendant(entry.getKey(), member)) {
                            entry.setValue(bounding);
                        }
                    }
                }

                for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMap.entrySet()) {
                    if (entry.getValue() == memberb) {
                        if (isDescendant(entry.getKey(), member)) {
                            boundingsMapVirtual.put(entry.getKey(), bounding);
                        }
                    }
                }
                parentsMapVirtual.put(member, bounding);
            } else {
                return false;
            }
        }
        return true;
    }

    public void addMemberStrong(TreenotationImpl bounding, TreenotationImpl member) {
        logTransformStart();

        logChange_HASHMAP(parentsMap, member, null, bounding);
        parentsMap.put(member, bounding);

        TreenotationImpl memberb = boundingsMap.get(member);

        if (memberb == null) {
            logChange_HASHMAP(boundingsMap, member, memberb, bounding);
            boundingsMap.put(member, bounding);
            incNumberOfContexts(member);
            if (bounding.nView > 0) {
                incNView(member, bounding.nView);
            }
            markTokens(member);
            TreenotationImpl.Node nd = bounding.addTree(member);
            logChange_ADD_TREE(bounding, nd);
            nd.parentConnection = TreenotationImpl.PARENT_CONNECTION_STRONG;
            resizeUp(member);
        } else {
            TokenImpl min = (TokenImpl) member.getStartToken();
            TokenImpl max = (TokenImpl) member.getEndToken();
            int d = bounding.nView - memberb.nView;
            for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMap.entrySet()) {
                if (entry.getValue() == memberb) {
                    if (isDescendant(entry.getKey(), member)) {
                        TokenImpl pmin = (TokenImpl) entry.getKey().getStartToken();
                        TokenImpl pmax = (TokenImpl) entry.getKey().getEndToken();
                        if (pmin != null && pmin.compareTo(min) < 0) {
                            min = pmin;
                        }
                        if (pmax != null && pmax.compareTo(max) > 0) {
                            max = pmax;
                        }
                        logChange_HASHMAP(boundingsMap, entry.getKey(), memberb, bounding);
                        entry.setValue(bounding);
                        if (d > 0) {
                            incNView(entry.getKey(), d);
                        }
                    }
                }
            }

            TreenotationImpl.Node memberNd = memberb.removeTree(member);
            logChange_REMOVE_TREE(memberb, memberNd);
            memberNd.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
            bounding.addTree(memberNd);
            logChange_ADD_TREE(bounding, memberNd);
            memberNd.parentConnection = TreenotationImpl.PARENT_CONNECTION_STRONG;
            ensureBoundings(bounding, min, max);
            resizeUp(bounding);
        }

        if (!removeEmpty)
            resizeDown(target);

    }

    public boolean mayBeAggregated(TreenotationImpl trn) {
        if (target.isLocked())
            return false;

        if (trn == target)
            return false;

        if (parentsMap.get(trn) != null) {
            return false;
        }

        TreenotationImpl bnd = boundingsMap.get(trn);

        if (bnd == null) {
            if (!trn.isLocked() && trn.getContext() != null) {
                return false;
            }
            return checkCoverage(trn);
        }

        return !bnd.isLocked();
    }

    public TreenotationImpl aggregateVirtual(TreenotationImpl trn, TrnType tp, boolean strong) {
        if (target.isLocked())
            return null;

        if (trn == target)
            return null;

        if (parentsMapVirtual.get(trn) != null || parentsMap.get(trn) != null) {
            return null;
        }

        TreenotationImpl bnd = boundingsMapVirtual.get(trn);
        if (bnd == null) {
            bnd = boundingsMap.get(trn);
        }

        TreenotationImpl aggregate = (TreenotationImpl) TreetonFactory.newTreenotation(null, null, tp);
        aggregate.locked = false;

        if (bnd == null) {
            if (!trn.isLocked() && trn.getContext() != null) {
                return null;
            }
            if (checkCoverage(trn)) {
                boundingsMapVirtual.put(aggregate, target);
                boundingsMapVirtual.put(trn, aggregate);
                markTokens(trn);

                if (strong) {
                    parentsMapVirtual.put(trn, aggregate);
                }
            }
        } else {
            if (bnd.isLocked())
                return null;

            for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMapVirtual.entrySet()) {
                if (entry.getValue() == bnd) {
                    if (isDescendant(entry.getKey(), trn)) {
                        entry.setValue(aggregate);
                    }
                }
            }

            for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMap.entrySet()) {
                if (entry.getValue() == bnd) {
                    if (isDescendant(entry.getKey(), trn)) {
                        boundingsMapVirtual.put(entry.getKey(), aggregate);
                    }
                }
            }

            boundingsMapVirtual.put(trn, aggregate);
            boundingsMapVirtual.put(aggregate, bnd);

            if (strong) {
                parentsMapVirtual.put(trn, aggregate);
            }
        }
        return aggregate;
    }

    public void aggregate(TreenotationImpl trn, TrnType tp, TreenotationImpl aggregate, boolean strong) {
        if (!aggregate.isEmpty() || aggregate.getContext() != null || aggregate.isLocked() || aggregate.isAdded() || aggregate.getStorage() != storage)
            throw new RuntimeException("Wrong aggregate treenotation!");
        logTransformStart();


        aggregate.locked = false;
        aggregate.mapper = null;
        aggregate.data = null;
        aggregate.startToken = aggregate.endToken = null;
        TreenotationImpl bnd = boundingsMap.get(trn);

        if (bnd == null) {
            logChange_HASHMAP(boundingsMap, aggregate, null, target);
            boundingsMap.put(aggregate, target);
            TreenotationImpl.Node nd = target.addTree(aggregate);
            logChange_ADD_TREE(target, nd);
            incNumberOfContexts(aggregate);
            if (target.nView > 0) {
                incNView(aggregate, target.nView);
            }

            logChange_START_OFFSET(aggregate, aggregate.startToken, (TokenImpl) trn.getStartToken());
            aggregate.startToken = (TokenImpl) trn.getStartToken();
            logChange_END_OFFSET(aggregate, aggregate.endToken, (TokenImpl) trn.getEndToken());
            aggregate.endToken = (TokenImpl) trn.getEndToken();
            markTokens(trn);

            logChange_HASHMAP(boundingsMap, trn, bnd, aggregate);
            boundingsMap.put(trn, aggregate);
            nd = aggregate.addTree(trn);
            logChange_ADD_TREE(aggregate, nd);
            if (strong) {
                logChange_HASHMAP(parentsMap, trn, null, aggregate);
                parentsMap.put(trn, aggregate);
                nd.parentConnection = TreenotationImpl.PARENT_CONNECTION_STRONG;
            }

            incNumberOfContexts(trn);

            if (target.nView > 0) {
                incNView(trn, target.nView);
            }

            resizeUp(aggregate);
        } else {
            TokenImpl min = (TokenImpl) trn.getStartToken();
            TokenImpl max = (TokenImpl) trn.getEndToken();
            for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMap.entrySet()) {
                if (entry.getValue() == bnd) {
                    if (isDescendant(entry.getKey(), trn)) {
                        TokenImpl pmin = (TokenImpl) entry.getKey().getStartToken();
                        TokenImpl pmax = (TokenImpl) entry.getKey().getEndToken();
                        if (pmin != null && pmin.compareTo(min) < 0) {
                            min = pmin;
                        }
                        if (pmax != null && pmax.compareTo(max) > 0) {
                            max = pmax;
                        }
                        logChange_HASHMAP(boundingsMap, entry.getKey(), bnd, aggregate);
                        entry.setValue(aggregate);
                    }
                }
            }


            TreenotationImpl.Node oldNd = bnd.removeTree(trn);
            logChange_REMOVE_TREE(bnd, oldNd);
            TreenotationImpl.Node nd = bnd.addTree(aggregate);
            nd.parentConnection = oldNd.parentConnection;
            logChange_ADD_TREE(bnd, nd);
            incNumberOfContexts(aggregate);
            if (bnd.nView > 0) {
                incNView(aggregate, bnd.nView);
            }

            logChange_START_OFFSET(aggregate, aggregate.startToken, min);
            aggregate.startToken = min;
            logChange_END_OFFSET(aggregate, aggregate.endToken, max);
            aggregate.endToken = max;
            logChange_HASHMAP(boundingsMap, aggregate, null, bnd);
            boundingsMap.put(aggregate, bnd);

            logChange_ADD_TREE(aggregate, oldNd);
            oldNd.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
            aggregate.addTree(oldNd);
            if (strong) {
                logChange_HASHMAP(parentsMap, trn, null, aggregate);
                parentsMap.put(trn, aggregate);
                oldNd.parentConnection = TreenotationImpl.PARENT_CONNECTION_STRONG;
            }
            resizeUp(aggregate);
        }

        if (!removeEmpty)
            resizeDown(target);
    }

    public TreenotationImpl aggregate(TreenotationImpl trn, TrnType tp, boolean strong) {
        TreenotationImpl aggregate = (TreenotationImpl) TreetonFactory.newSyntaxTreenotation(storage, null, null, tp);

        aggregate(trn, tp, aggregate, strong);

        return aggregate;
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

    public boolean addMemberWeakVirtual(TreenotationImpl bounding, TreenotationImpl member) {
        if (target.isLocked())
            return false;

        if (bounding.isLocked())
            return false;

        if (member == bounding)
            return false;

        if (member == target)
            return false;

        if (parentsMapVirtual.get(member) != null || parentsMap.get(member) != null) {
            return false;
        }

        if (bounding != target && boundingsMapVirtual.get(bounding) == null && boundingsMap.get(bounding) == null)
            return false;

        TreenotationImpl memberb = boundingsMapVirtual.get(member);
        if (memberb == null) {
            memberb = boundingsMap.get(member);
        }

        if (memberb == bounding) {
            return false;
        }

        if (memberb == null) {
            if (!member.isLocked() && member.getContext() != null) {
                return false;
            }
            if (checkCoverage(member)) {
                boundingsMapVirtual.put(member, bounding);
                markTokens(member);
            }
        } else {
            if (isDescendant(bounding, memberb) && !isDescendant(bounding, member)) {
                if (memberb.isLocked())
                    return false;
                for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMapVirtual.entrySet()) {
                    if (entry.getValue() == memberb) {
                        if (isDescendant(entry.getKey(), member)) {
                            entry.setValue(bounding);
                        }
                    }
                }
                for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMap.entrySet()) {
                    if (entry.getValue() == memberb) {
                        if (isDescendant(entry.getKey(), member)) {
                            boundingsMapVirtual.put(entry.getKey(), bounding);
                        }
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public void addMemberWeak(TreenotationImpl bounding, TreenotationImpl member) {
        logTransformStart();

        TreenotationImpl memberb = boundingsMap.get(member);

        if (memberb == null) {
            logChange_HASHMAP(boundingsMap, member, null, bounding);
            boundingsMap.put(member, bounding);
            incNumberOfContexts(member);
            if (bounding.nView > 0) {
                incNView(member, bounding.nView);
            }
            markTokens(member);
            TreenotationImpl.Node nd = bounding.addTree(member);
            logChange_ADD_TREE(bounding, nd);
            resizeUp(member);
        } else {
            TokenImpl min = (TokenImpl) member.getStartToken();
            TokenImpl max = (TokenImpl) member.getEndToken();
            int d = bounding.nView - memberb.nView;
            for (Map.Entry<TreenotationImpl, TreenotationImpl> entry : boundingsMap.entrySet()) {
                if (entry.getValue() == memberb) {
                    if (isDescendant(entry.getKey(), member)) {
                        TokenImpl pmin = (TokenImpl) entry.getKey().getStartToken();
                        TokenImpl pmax = (TokenImpl) entry.getKey().getEndToken();
                        if (pmin != null && pmin.compareTo(min) < 0) {
                            min = pmin;
                        }
                        if (pmax != null && pmax.compareTo(max) > 0) {
                            max = pmax;
                        }

                        logChange_HASHMAP(boundingsMap, entry.getKey(), memberb, bounding);
                        entry.setValue(bounding);
                        if (d > 0) {
                            incNView(entry.getKey(), d);
                        }
                    }
                }
            }

            TreenotationImpl.Node memberNd = memberb.removeTree(member);
            logChange_REMOVE_TREE(memberb, memberNd);
            memberNd.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
            bounding.addTree(memberNd);
            logChange_ADD_TREE(bounding, memberNd);
            ensureBoundings(bounding, min, max);
            resizeUp(bounding);
        }

        if (!removeEmpty)
            resizeDown(target);

    }

    protected boolean isDescendant(TreenotationImpl t1, TreenotationImpl t2) {
        while (t1 != null) {
            if (t1 == t2)
                return true;
            TreenotationImpl t;
            if (virtualStackpos != -1) {
                t = parentsMapVirtual.get(t1);
                if (t == null) {
                    t = parentsMap.get(t1);
                }
            } else {
                t = parentsMap.get(t1);
            }
            if (t == null) {
                if (virtualStackpos != -1) {
                    t = boundingsMapVirtual.get(t1);
                    if (t == null) {
                        t1 = boundingsMap.get(t1);
                    } else {
                        t1 = t;
                    }
                } else {
                    t1 = boundingsMap.get(t1);
                }
            } else {
                t1 = t;
            }
        }
        return false;
    }

    public int countHeight(TreenotationImpl trn) {
        int n = -1;

        while (trn != null) {
            TreenotationImpl t = parentsMap.get(trn);
            if (t == null) {
                trn = boundingsMap.get(trn);
            } else {
                trn = t;
            }
            n++;
        }
        return n;
    }

    private boolean ensureBoundings(TreenotationImpl trn, TokenImpl min, TokenImpl max) {
        if (trn.isLocked())
            return false;

        boolean ch = false;
        boolean wasAdded = false;
        if (trn.startToken == null || trn.startToken.compareTo(min) > 0) {
            if (trn.isAdded()) {
                wasAdded = true;
                storage._remove(trn);
            }
            logChange_START_OFFSET(trn, trn.startToken, min);
            trn.startToken = min;
            ch = true;
        }

        if (trn.endToken == null || trn.endToken.compareTo(max) < 0) {
            if (!ch) {
                if (trn.isAdded()) {
                    wasAdded = true;
                    storage._remove(trn);
                }
                ch = true;
            }
            logChange_END_OFFSET(trn, trn.endToken, max);
            trn.endToken = max;
        }

        if (wasAdded) {
            storage._add(trn);
        }

        return ch;
    }

    void resizeUp(TreenotationImpl trn) {
        if (trn == target) {
            ensureVisibility();
            return;
        }

        TreenotationImpl cur = trn;
        TokenImpl min = (TokenImpl) cur.getStartToken();
        TokenImpl max = (TokenImpl) cur.getEndToken();

        while (true) {
            TreenotationImpl par = parentsMap.get(cur);
            TreenotationImpl bnd = boundingsMap.get(cur);
            if (par == null || par == bnd) {
                if (!ensureBoundings(bnd, min, max)) {
                    return;
                }
                if (bnd == target) {
                    ensureVisibility();
                    break;
                }
                cur = bnd;
                min = bnd.startToken;
                max = bnd.endToken;
            } else {
                TokenImpl pmin = (TokenImpl) par.getStartToken();
                TokenImpl pmax = (TokenImpl) par.getEndToken();
                if (pmin != null && pmin.compareTo(min) < 0) {
                    min = pmin;
                }
                if (pmax != null && pmax.compareTo(max) > 0) {
                    max = pmax;
                }
                cur = par;
            }
        }
    }

    public boolean contains(TreenotationImpl trn) {
        if (!isConnected()) {
            return false;
        } else {
            if (trn == target) {
                return true;
            }
            return boundingsMap.containsKey(trn);
        }
    }

    Bounding resizeDown(TreenotationImpl trn) {
        if (trn == target && target.isEmpty()) {
            return null;
        }

        if (trn.isLocked()) {
            return new Bounding((TokenImpl) trn.getStartToken(), (TokenImpl) trn.getEndToken());
        }

        Bounding bnd = new Bounding(null, null);
        if (trn.trees != null) {
            for (TreenotationImpl.Node tree : trn.trees) {
                if (tree == null)
                    continue;
                bnd.merge(resizeDown(tree));
            }
        }
        if (bnd.start == null) {
            bnd.start = (TokenImpl) trn.getStartToken();
        }
        if (bnd.end == null) {
            bnd.end = (TokenImpl) trn.getEndToken();
        }
        boolean wasAdded = false;
        if (trn.startToken == null || trn.startToken != bnd.start) {
            if (trn.isAdded()) {
                wasAdded = true;
                storage._remove(trn);
            }
            trn.startToken = bnd.start;
        }

        if (trn.endToken == null || trn.endToken != bnd.end) {
            if (trn.isAdded()) {
                wasAdded = true;
                storage._remove(trn);
            }
            trn.endToken = bnd.end;
        }

        if (wasAdded) {
            storage._add(trn);
        }

        return bnd;
    }

    Bounding resizeDown(TreenotationImpl.Node nd) {
        Bounding bnd = new Bounding(null, null);

        bnd.merge(resizeDown(nd.trn));

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        bnd.merge(resizeDown(child));
                    }
                }
            }
        }
        return bnd;
    }

    public boolean mayBeRemovedTreenotation(TreenotationImpl trn) {
//        if (trn.isLocked())
//            return false;

        TreenotationImpl bounding = boundingsMap.get(trn);

        return bounding != null;
    }

    public void removeTreenotation(TreenotationImpl trn) {
        if (undoEnabled) {
            changes.clear();
        }

        TreenotationImpl bounding = boundingsMap.get(trn);
        boundingsMap.remove(trn);

        TreenotationImpl parent = parentsMap.get(trn);

        if (parent != null) {
            parentsMap.remove(trn);
        }

        TreenotationImpl.Node nd;
        if (parent == null || parent == bounding) {
            nd = bounding.removeTree(trn);
        } else {
            TreenotationImpl.Node parentNd = bounding.findNode(parent);
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

        if (checkCoverage && trn.isLocked()) {
            TokenImpl start = (TokenImpl) trn.getStartToken();
            TokenImpl end = (TokenImpl) trn.getEndToken();
            while (start != end) {
                if (start.isMarked(bar)) {
                    start.unmark(bar);
                }
                start = (TokenImpl) start.getNextToken();
            }
            if (start.isMarked(bar)) {
                start.unmark(bar);
            }
        }
    }

    public boolean mayBeRemovedIncomingLink(TreenotationImpl slave) {
        TreenotationImpl bounding = boundingsMap.get(slave);

        if (bounding == null) {
            return false;
        }

        if (bounding.isLocked()) {
            return false;
        }

        TreenotationImpl parent = parentsMap.get(slave);

        return parent != null;
    }

    public void removeIncomingLink(TreenotationImpl slave) {
        if (undoEnabled) {
            changes.clear();
        }

        TreenotationImpl bounding = boundingsMap.get(slave);
        TreenotationImpl parent = parentsMap.get(slave);

        parentsMap.remove(slave);

        if (parent == bounding) {
            TreenotationImpl.Node nd;
            nd = bounding.findNode(slave);
            nd.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
        } else {
            TreenotationImpl.Node parentNd = bounding.findNode(parent);
            TreenotationImpl.Node nd = parentNd.removeRelation(slave);
            nd.parentConnection = TreenotationImpl.PARENT_CONNECTION_WEAK;
            bounding.addTree(nd);
        }
        resizeDown(target);
        ensureVisibility();
    }

    public TreenotationImpl getHost(TreenotationImpl slave) {
        return parentsMap.get(slave);
    }

    public TreenotationImpl getBounding(TreenotationImpl trn) {
        return boundingsMap.get(trn);
    }

    public Iterator<TreenotationImpl> internalTrnsIterator() {
        return boundingsMap.keySet().iterator();
    }

    public void turnOnVirtualMode() {
        virtualStackpos = markStack.getPosition();
    }

    public void rollBackVirtualChanges() {
        while (markStack.getPosition() != virtualStackpos) {
            TreenotationImpl trn = (TreenotationImpl) markStack.pop();
            trn.unmark(bar);
        }
        virtualStackpos = -1;
        boundingsMapVirtual.clear();
        parentsMapVirtual.clear();
    }

    public void undoLastTransform() {
        if (undoEnabled) {
            while (!changes.isEmpty()) {
                Change c = changes.pop();
                c.undo();
                if (c instanceof TRANSFORM_STARTED) {
                    break;
                }
            }
        }
    }

    public TreenotationImpl getTarget() {
        return target;
    }

    public void setRemoveEmpty(boolean removeEmpty) {
        this.removeEmpty = removeEmpty;
    }

    interface Change {
        void undo();
    }

    class HASHMAP_Change implements Change {
        HashMap<TreenotationImpl, TreenotationImpl> map;
        TreenotationImpl key;
        TreenotationImpl oldV;
        TreenotationImpl newV;

        public HASHMAP_Change(HashMap<TreenotationImpl, TreenotationImpl> map, TreenotationImpl key, TreenotationImpl oldV, TreenotationImpl newV) {
            this.map = map;
            this.key = key;
            this.oldV = oldV;
            this.newV = newV;
        }

        public void undo() {
            if (oldV == null) {
                map.remove(key);
            } else {
                map.put(key, oldV);
            }
        }
    }

    class INC_CONTEXT_Change implements Change {
        TreenotationImpl trn;

        public INC_CONTEXT_Change(TreenotationImpl trn) {
            this.trn = trn;
        }

        public void undo() {
            decNumberOfContexts(trn);
        }
    }

    class ADD_TREE_Change implements Change {
        TreenotationImpl bounding;
        TreenotationImpl.Node nd;

        public ADD_TREE_Change(TreenotationImpl bounding, TreenotationImpl.Node nd) {
            this.bounding = bounding;
            this.nd = nd;
        }

        public void undo() {
            bounding.removeTree(nd.trn);
        }
    }

    class INC_VIEW_Change implements Change {
        TreenotationImpl trn;
        int i;

        public INC_VIEW_Change(TreenotationImpl trn, int i) {
            this.trn = trn;
            this.i = i;
        }

        public void undo() {
            decNView(trn, i);
        }
    }

    class REMOVE_TREE_Change implements Change {
        TreenotationImpl bounding;
        TreenotationImpl.Node nd;

        public REMOVE_TREE_Change(TreenotationImpl bounding, TreenotationImpl.Node nd) {
            this.bounding = bounding;
            this.nd = nd;
        }

        public void undo() {
            bounding.addTree(nd);
        }
    }

    class ADD_RELATION_Change implements Change {
        TreenotationImpl.Node host;
        TreenotationImpl.Node slave;

        public ADD_RELATION_Change(TreenotationImpl.Node host, TreenotationImpl.Node slave) {
            this.host = host;
            this.slave = slave;
        }

        public void undo() {
            host.removeRelation(slave.trn);
        }
    }

    class START_OFFSET_Change implements Change {
        TreenotationImpl trn;
        TokenImpl oldV;
        TokenImpl newV;

        public START_OFFSET_Change(TreenotationImpl trn, TokenImpl oldV, TokenImpl newV) {
            this.trn = trn;
            this.oldV = oldV;
            this.newV = newV;
        }

        public void undo() {
            if (trn.isAdded()) {
                storage._remove(trn);
                trn.startToken = oldV;
                if (oldV != null) {
                    storage._add(trn);
                }
            } else {
                trn.startToken = oldV;
            }
        }
    }

    class END_OFFSET_Change implements Change {
        TreenotationImpl trn;
        TokenImpl oldV;
        TokenImpl newV;

        public END_OFFSET_Change(TreenotationImpl trn, TokenImpl oldV, TokenImpl newV) {
            this.trn = trn;
            this.oldV = oldV;
            this.newV = newV;
        }

        public void undo() {
            if (trn.isAdded()) {
                storage._remove(trn);
                trn.endToken = oldV;
                if (oldV != null) {
                    storage._add(trn);
                }
            } else {
                trn.endToken = oldV;
            }
        }
    }

    class TRANSFORM_STARTED implements Change {
        long markStackPos;

        public TRANSFORM_STARTED(long markStackPos) {
            this.markStackPos = markStackPos;
        }

        public void undo() {
            while (!markStack.isEmpty() && markStack.getPosition() != markStackPos) {
                TreenotationImpl trn = (TreenotationImpl) markStack.pop();
                trn.unmark(bar);
            }
        }
    }

    class Bounding {
        TokenImpl start;
        TokenImpl end;

        public Bounding(TokenImpl start, TokenImpl end) {
            this.start = start;
            this.end = end;
        }

        void merge(Bounding other) {
            if (other.start != null && (start == null || start.compareTo(other.start) > 0))
                start = other.start;
            if (other.end != null && (end == null || end.compareTo(other.end) < 0))
                end = other.end;
        }
    }
}
