/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz;

import treeton.core.*;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeSet;
import treeton.core.model.TrnTypeSetFactory;
import treeton.core.util.RBTreeMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WeightMinimizator extends Resource {
    TrnTypeSetFactory fact = new TrnTypeSetFactory();
    List<String> types;
    String weightFeatureName;

    public String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        RBTreeMap tok2node = new RBTreeMap();
        TrnType[] arr = new TrnType[types.size()];
        for (int i = 0; i < types.size(); i++) {
            try {
                arr[i] = storage.getTypes().get(types.get(i));
            } catch (TreetonModelException e) {
                arr[i] = null;
            }
            if (arr[i] == null) {
                throw new IllegalArgumentException("Type " + types.get(i) + " is not registered");
            }
        }
        TrnTypeSet tpset = fact.newTrnTypeSet(arr, arr.length);
        TypeIteratorInterface tit = storage.typeIterator(arr);

        while (tit.hasNext()) {
            Treenotation trn = (Treenotation) tit.next();

            Token tok = trn.getStartToken();
            Node nd = (Node) tok2node.get(tok);
            if (nd == null) {
                nd = new Node(tok);
                tok2node.put(tok, nd);
            }
            nd.children.add(new Link(trn));
        }

        FollowIteratorInterface fit = storage.followIterator(tpset, null, null);
        Iterator it = tok2node.values().iterator();
        while (it.hasNext()) {
            Node nd = (Node) it.next();
            for (Link link : nd.children) {
                fit.reset(tpset, link.trn.getEndToken());
                while (fit.hasNext()) {
                    Treenotation follow = (Treenotation) fit.next();
                    Node target = (Node) tok2node.get(follow.getStartToken());
                    int i;
                    for (i = 0; i < link.targets.size(); i++) {
                        if (link.targets.get(i) == target)
                            break;
                    }
                    if (i == link.targets.size()) {
                        link.targets.add(target);
                    }
                }
            }
        }

        Node[] nodes = new Node[tok2node.size()];
        tok2node.values().toArray(nodes);
        Node best = null;
        for (int i = nodes.length - 1; i >= 0; i--) {
            processNode(nodes[i]);
            if (best == null || nodes[i].bestList.weight > best.bestList.weight) {
                best = nodes[i];
            }
        }
        if (best != null) {
            short ch = ((TreenotationStorageImpl) storage).getChannel();
            ListNode cur = best.bestList;
            while (cur != null) {
                ((TreenotationImpl) cur.trn).mark(ch);
                cur = cur.next;
            }

            tit = storage.typeIterator(arr);

            while (tit.hasNext()) {
                TreenotationImpl trn = (TreenotationImpl) tit.next();
                if (!trn.isMarked(ch)) {
                    storage.forgetPostFactum(trn);
                }
            }

            storage.applyPostFactumTrns();

            cur = best.bestList;
            while (cur != null) {
                ((TreenotationImpl) cur.trn).unmark(ch);
                cur = cur.next;
            }
            ((TreenotationStorageImpl) storage).freeChannel(ch);
        }
        return null;
    }

    public void stop() {
    }

    public void processTerminated() {
    }

    private void processNode(Node nd) {
        if (nd.bestList != null)
            return;
        ListNode best = new ListNode();
        for (Link link : nd.children) {
            if (link.targets.size() == 0) {
                if (best.trn == null || best.weight < link.weight) {
                    best.trn = link.trn;
                    best.weight = link.weight;
                    best.next = null;
                }
            } else {
                for (Node node : link.targets) {
                    processNode(node);
                    if (best.trn == null || best.weight < node.bestList.weight + link.weight) {
                        best.trn = link.trn;
                        best.weight = node.bestList.weight + link.weight;
                        best.next = node.bestList;
                    }
                }
            }
        }
        nd.bestList = best;
    }

    public void init() throws ResourceInstantiationException {
        types = (List<String>) getInitialParameters().get("types");
        weightFeatureName = (String) getInitialParameters().get("weightFeatureName");
    }

    public void deInit() {
    }

    private class ListNode {
        double weight;
        Treenotation trn;
        ListNode next;
    }

    private class Node {
        ListNode bestList;

        Token tok;
        List<Link> children = new ArrayList<Link>();

        public Node(Token tok) {
            this.tok = tok;
        }
    }

    private class Link {
        Treenotation trn;
        double weight;
        List<Node> targets = new ArrayList<Node>();

        public Link(Treenotation trn) {
            this.trn = trn;
            Object o = trn.get(weightFeatureName);
            try {
                weight = o != null ? Double.valueOf(o.toString()) : 0.01;
            } catch (NumberFormatException ex) {
                weight = 0.01;
            }
        }
    }
}
