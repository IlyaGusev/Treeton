/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.fsm;

import treeton.core.Treenotation;
import treeton.core.model.TrnType;
import treeton.core.scape.ScapeVariable;
import treeton.core.util.BlockStack;
import treeton.core.util.nu;

import java.util.Iterator;

public class MatchedValueGroupsIterator implements Iterator {
    ScapeVariable var;
    Treenotation trn;
    ScapeTreenotationIndex.FinalNode cur;
    private ScapeTreenotationIndex scapeTreenotationIndex;

    private BlockStack stack = new BlockStack(100);


    public MatchedValueGroupsIterator(ScapeTreenotationIndex scapeTreenotationIndex, Treenotation trn) {
        this.scapeTreenotationIndex = scapeTreenotationIndex;
        this.var = null;
        this.trn = trn;
        cur = null;
        if (trn != null) {
            stack.push(scapeTreenotationIndex.root);
            findNextFinal();
        }
    }

    public MatchedValueGroupsIterator(ScapeTreenotationIndex scapeTreenotationIndex, ScapeVariable var) {
        this.scapeTreenotationIndex = scapeTreenotationIndex;
        this.var = var;
        this.trn = null;
        cur = null;
        if (var != null) {
            stack.push(scapeTreenotationIndex.root);
            findNextFinal();
        }
    }

    public void reset(ScapeVariable var) {
        this.var = var;
        cur = null;
        stack.clean();
        if (var != null) {
            stack.push(scapeTreenotationIndex.root);
            findNextFinal();
        }
    }

    public void reset(Treenotation trn) {
        this.trn = trn;
        this.var = null;
        cur = null;
        stack.clean();
        if (trn != null) {
            stack.push(scapeTreenotationIndex.root);
            findNextFinal();
        }
    }

    public void reset(ScapeTreenotationIndex scapeTreenotationIndex, ScapeVariable var) {
        this.scapeTreenotationIndex = scapeTreenotationIndex;
        this.var = var;
        this.trn = null;
        cur = null;
        stack.clean();
        if (var != null) {
            stack.push(scapeTreenotationIndex.root);
            findNextFinal();
        }
    }

    public void reset(ScapeTreenotationIndex scapeTreenotationIndex, Treenotation trn) {
        this.scapeTreenotationIndex = scapeTreenotationIndex;
        this.var = null;
        this.trn = trn;
        cur = null;
        stack.clean();
        if (trn != null) {
            stack.push(scapeTreenotationIndex.root);
            findNextFinal();
        }
    }

    public void remove() {
    }

    public boolean hasNext() {
        return cur != null;
    }

    public Object next() {
        Object result = cur;
        findNextFinal();
        return result;
    }

    void findNextFinal() {
        while (true) {
            if (stack.isEmpty()) {
                cur = null;
                break;
            } else {
                Object o = stack.pop();
                if (o instanceof ScapeTreenotationIndex.Distributer) {
                    ScapeTreenotationIndex.Distributer distr = (ScapeTreenotationIndex.Distributer) o;
                    if (distr.key == -3) {
                        TrnType tp = getType();
                        Object next;
                        if ((next = distr.map.get(tp)) != null) {
                            stack.push(next);
                        }
                    } else {
                        if (distr.key != -1) {
                            Object v = getValue(distr.key);
                            if (v == null) {
                                v = nu.ll;
                            }
                            Object next;
                            if ((next = distr.map.get(v)) != null) {
                                stack.push(next);
                            }
                        }
                        if (distr.otherTransition != null) {
                            stack.push(distr.otherTransition);
                        }
                    }
                } else if (o instanceof ScapeTreenotationIndex.SequenceReader) {
                    ScapeTreenotationIndex.SequenceReader reader = (ScapeTreenotationIndex.SequenceReader) o;
                    int i = 0;
                    for (; i < reader.map.size(); i++) {
                        int key = reader.map.getIndexByNumber(i);
                        Object v = getValue(key);
                        if (v == null) {
                            v = nu.ll;
                        }
                        if (!v.equals(reader.map.getByNumber(i))) {
                            break;
                        }
                    }
                    if (i == reader.map.size())
                        stack.push(reader.nd);
                }
                if (o instanceof ScapeTreenotationIndex.FinalNode) {
                    cur = (ScapeTreenotationIndex.FinalNode) o;
                    break;
                }
            }
        }
    }

    private Object getValue(int key) {
        if (var != null)
            return var.getValue(key);
        return trn.get(key);
    }

    private TrnType getType() {
        if (var != null)
            return var.getType();
        return trn.getType();
    }
}
