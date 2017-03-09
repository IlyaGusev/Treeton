/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.combinator;

import treeton.core.util.AbsoluteTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class Combinator2D<T> implements Combinator<T>, SortedEntriesListener<T> {
    private Comparator<Combination<T>> combinationComparator;

    private List<CombinatorListener<T>> listeners;
    private String name;
    private List<SortedEntries<T>> sortedEntriesList;

    private boolean started = false;
    private ListNode worst;
    private ListNode best;

    public Combinator2D(SortedEntries<T> first, SortedEntries<T> second, Comparator<Combination<T>> combinationComparator) {
        listeners = new ArrayList<CombinatorListener<T>>();
        this.sortedEntriesList = new ArrayList<SortedEntries<T>>();
        this.sortedEntriesList.add(first);
        this.sortedEntriesList.add(second);

        first.addListener(this);
        second.addListener(this);

        this.combinationComparator = combinationComparator;
    }

    public void addCombinatorListener(CombinatorListener<T> listener) {
        listeners.add(listener);
    }

    @SuppressWarnings({"unchecked"})
    public void start() {
        Entry<T> e = sortedEntriesList.get(0).getFirst();
        SortedEntries<T> entries = sortedEntriesList.get(1);

        if (entries.size() > 0) {
            while (e != null) {
                Entry<T> p = entries.getPreceiding(e.getObject());
                Entry<T> s;
                if (p == null) {
                    s = entries.getFirst();
                } else {
                    s = p.getSuccessor();
                }


                if (p != null) {
                    addCombination(null, e, p, 0, false);
                }
                if (s != null) {
                    addCombination(null, e, s, 0, true);
                }
                e = e.getSuccessor();
            }
        }

        started = true;
    }

    public Combination<T> getCurrentCombination() {
        return best == null ? null : best.combination;
    }

    public String getStatistics() {
        return "";
    }

    public List<SortedEntries<T>> getSortedEntriesList() {
        return sortedEntriesList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean next() {
        if (best == null) {
            return false;
        }
        combinationUsed(best.combination);
        return getCurrentCombination() != null;
    }

    public Collection<Combination> getCombinationsFront() {
        List<Combination> result = new ArrayList<Combination>();
        ListNode nd = best;

        while (nd != null) {
            result.add(nd.combination);
            nd = nd.prev;
        }

        return result;
    }

    private ListNode addCombination(ListNode after, Entry<T> e1, Entry<T> e2, int fixedDimension, boolean positive) {
        ListNode ne = new ListNode(e1, e2, fixedDimension, positive);
        if (after == null) {
            if (worst == null) {
                worst = best = ne;
                return worst;
            } else if (isWorse(ne.combination, worst)) {
                ListNode e = worst;
                worst = ne;
                worst.next = e;
                e.prev = worst;
                return worst;
            }
            after = worst;
        }

        ListNode e = after.next;

        while (e != null && !isWorse(ne.combination, e)) {
            after = e;
            e = e.next;
        }

        after.next = ne;
        ne.prev = after;
        if (e == null) {
            best = ne;
        } else {
            ne.next = e;
            e.prev = ne;
        }

        return ne;
    }

    private void notifyListeners() {
        for (CombinatorListener<T> listener : listeners) {
            listener.combinationChanged(this);
        }
    }

    private boolean isWorse(Combination<T> combination, ListNode worst) {
        if (combinationComparator != null)
            return combinationComparator.compare(combination, worst.combination) > 0;

        return combination.compareTo(worst.combination) > 0;
    }

    public void combinationUsed(Combination<T> combination) {
        if (!started)
            return;

        if (best == null)
            return;

        ListNode nd = ((Combination2D<T>) combination).getNode();

        if (nd.combination == combination) {
            nd.next();

            if (nd.combination == null) {
                if (nd.prev != null) {
                    nd.prev.next = nd.next;
                    if (nd.next != null) {
                        nd.next.prev = nd.prev;
                    } else {
                        best = nd.prev;
                    }
                } else if (nd.next != null) {
                    nd.next.prev = null;
                    worst = nd.next;
                } else {
                    best = worst = null;
                }
                return;
            }
        }

        //shift left

        ListNode prev = nd.prev;

        while (prev != null && isWorse(nd.combination, prev)) {
            if (nd.next != null) {
                nd.next.prev = prev;
                prev.next = nd.next;
            } else {
                prev.next = null;
                best = prev;
            }

            ListNode ne = prev.prev;
            prev.prev = nd;
            nd.next = prev;

            nd.prev = ne;
            if (ne != null)
                ne.next = nd;

            prev = nd.prev;
        }

        if (prev == null) {
            worst = nd;
        }
    }

    public void entryAdded(Entry<T> e, SortedEntries<T> sortedEntries) {
        if (!started)
            return;

        ListNode nd = best;

        int fixedDimension = sortedEntriesList.get(0) == sortedEntries ? 0 : 1;
        SortedEntries<T> entries = sortedEntriesList.get(1 - fixedDimension);
        if (entries.size() > 0) {
            Entry<T> p = entries.getPreceiding(e.getObject());
            Entry<T> s;
            if (p == null) {
                s = entries.getFirst();
            } else {
                s = p.getSuccessor();
            }

            if (p != null) {
                addCombination(null, fixedDimension == 0 ? e : p, fixedDimension == 0 ? p : e, fixedDimension, false);
            }
            if (s != null) {
                addCombination(null, fixedDimension == 0 ? e : s, fixedDimension == 0 ? s : e, fixedDimension, true);
            }
        }

        if (best != nd) {
            notifyListeners();
        }
    }

    class ListNode {
        long creationTime;
        Entry<T> e1;
        Entry<T> e2;
        boolean positive;

        int fixedDimension;

        ListNode next;
        ListNode prev;

        Combination2D<T> combination;

        public ListNode(Entry<T> e1, Entry<T> e2, int fixedDimension, boolean positive) {
            creationTime = AbsoluteTime.tick();
            this.e1 = e1;
            this.e2 = e2;
            this.fixedDimension = fixedDimension;
            this.combination = new Combination2D<T>(e1, e2);
            this.combination.setNode(this);
            this.positive = positive;
        }

        void next() {
            Entry<T> e = fixedDimension == 0 ? e2 : e1;
            Entry<T> n = positive ? e.getSuccessor() : e.getPredecessor();
            while (n != null && n.getCreationTime() > creationTime) {
                n = positive ? n.getSuccessor() : n.getPredecessor();
            }
            if (n != null) {
                if (fixedDimension == 0) {
                    e2 = n;
                } else {
                    e1 = n;
                }
                combination = new Combination2D<T>(e1, e2);
                combination.setNode(this);
            } else {
                combination = null;
            }
        }
    }
}

