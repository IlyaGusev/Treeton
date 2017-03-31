/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.prosody.mdlcompiler.api.fsm.TermStatePair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.PriorityQueue;

public class MeterMatcher {
    class MatchingState implements Comparable<MatchingState> {
        int nStressRestrictionViolations;
        int nReaccentuationRestrictionViolations;
        int nInSuccessionSkippedStresses;

        DefaultState state;
        SyllTerm term;
        boolean isStressed;
        MatchingState previous;
        MeterMatcherCursor cursor;
        int nStressed;

        MatchingState(DefaultState state, SyllTerm term, MatchingState previous, boolean isStressed) {
            this.state = state;
            this.term = term;
            this.previous = previous;
            this.isStressed = isStressed;

            if (previous != null) {
                cursor = previous.cursor.shift(isStressed);
                nStressed = previous.nStressed +
                        ((term.getType() == SyllablePatternType.STRESSED ||
                                term.getType() == SyllablePatternType.OBLIGATORY_STRESSED) ? 1 : 0);
            } else {
                cursor = null;
                nStressed = 0;

                nStressRestrictionViolations = 0;
                nReaccentuationRestrictionViolations = 0;
                nInSuccessionSkippedStresses = 0;
            }
        }

        public int compareTo(MatchingState o) {
            double p1 = countPenalty();
            double p2 = o.countPenalty();

            return p1 < p2 ? -1 : p1 == p2 ? 0 : 1;
        }

        private double countPenalty() {
            return nStressRestrictionViolations * stressRestrictionWeight + nReaccentuationRestrictionViolations * reaccentuationRestrictionWeight;
        }

    }

    private PriorityQueue<MatchingState> queue;
    private PriorityQueue<MatchingState> nextLevelQueue;
    private double stressRestrictionWeight;
    private double reaccentuationRestrictionWeight;
    private int maxStressRestrictionViolations;
    private int maxReaccentuationRestrictionViolations;
    private int maxInSuccessionSkippedStresses;

    private List<MatchingState> results;

    private MatchingState currentBestVariant;

    private int currentStressLevel;

    public MeterMatcher(Meter meter, MeterMatcherCursor input, double obligatoryStressRestrictionWeight, double reaccentuationRestrictionWeight,
                        int maxStressRestrictionViolations, int maxReaccentuationRestrictionViolations, int maxInSuccessionSkippedStresses ) {
        this.maxStressRestrictionViolations = maxStressRestrictionViolations;
        this.maxReaccentuationRestrictionViolations = maxReaccentuationRestrictionViolations;
        this.maxInSuccessionSkippedStresses = maxInSuccessionSkippedStresses;
        queue = new PriorityQueue<>();
        nextLevelQueue = new PriorityQueue<>();
        MatchingState start = new MatchingState(meter.getFsm(), null, null, false);
        queue.add(start);
        this.stressRestrictionWeight = obligatoryStressRestrictionWeight;
        this.reaccentuationRestrictionWeight = reaccentuationRestrictionWeight;

        start.cursor = input;

        currentStressLevel = 0;
        results = new ArrayList<>();
    }

    @SuppressWarnings({"unchecked"})
    public void match(boolean onlyBestVariants) {
        while (!queue.isEmpty() || !nextLevelQueue.isEmpty()) {
            if (queue.isEmpty()) {
                if (onlyBestVariants && currentBestVariant != null) {
                    results.add(currentBestVariant);
                    currentBestVariant = null;
                }

                PriorityQueue<MatchingState> q = queue;
                queue = nextLevelQueue;
                nextLevelQueue = q;

                currentStressLevel++;
            }

            MatchingState s = queue.poll();

            if (onlyBestVariants && currentBestVariant != null && currentBestVariant.compareTo(s) < 0) {
                queue.clear();
                continue;
            }

            if (!s.cursor.isEndOfInput()) {
                List<TermStatePair> list = s.state.getTransitions();
                for (TermStatePair pair : list) {
                    int stressed = s.cursor.isStressed();
                    if (stressed == 0 || stressed == -1) {
                        MatchingState ns = new MatchingState((DefaultState) pair.getState(), (SyllTerm) pair.getTerm(), s, false);
                        countPenalty(ns);
                        if (ns.nReaccentuationRestrictionViolations > maxReaccentuationRestrictionViolations) {
                            continue;
                        }

                        if (ns.nStressRestrictionViolations > maxStressRestrictionViolations) {
                            continue;
                        }

                        if (ns.nInSuccessionSkippedStresses > maxInSuccessionSkippedStresses) {
                            continue;
                        }

                        if (ns.nStressed > currentStressLevel) {
                            nextLevelQueue.add(ns);
                        } else {
                            queue.add(ns);
                        }
                    }

                    if (stressed == 1 || stressed == -1) {
                        MatchingState ns = new MatchingState((DefaultState) pair.getState(), (SyllTerm) pair.getTerm(), s, true);
                        countPenalty(ns);
                        if (ns.nReaccentuationRestrictionViolations > maxReaccentuationRestrictionViolations) {
                            continue;
                        }

                        if (ns.nStressRestrictionViolations > maxStressRestrictionViolations) {
                            continue;
                        }

                        if (ns.nStressed > currentStressLevel) {
                            nextLevelQueue.add(ns);
                        } else {
                            queue.add(ns);
                        }
                    }
                }
            } else if (s.state.isFinal()) {
                if( onlyBestVariants && ( currentBestVariant == null || s.compareTo(currentBestVariant) < 0 ) ) {
                    currentBestVariant = s;
                } else if( !onlyBestVariants ) {
                    results.add( s );
                }
            }
        }

        if ( onlyBestVariants && currentBestVariant != null) {
            results.add(currentBestVariant);
        }
    }

    private void countPenalty(MatchingState s) {
        s.nStressRestrictionViolations = s.previous.nStressRestrictionViolations;
        s.nReaccentuationRestrictionViolations = s.previous.nReaccentuationRestrictionViolations;
        s.nInSuccessionSkippedStresses = s.previous.nInSuccessionSkippedStresses;

        boolean isStressed = s.isStressed;
        SyllablePatternType pt = s.term.getType();

        if (isStressed && pt == SyllablePatternType.OBLIGATORY_UNSTRESSED || !isStressed && pt == SyllablePatternType.OBLIGATORY_STRESSED) {
            s.nStressRestrictionViolations++;
        }

        if (pt == SyllablePatternType.OBLIGATORY_STRESSED || pt == SyllablePatternType.STRESSED) {
            if (isStressed) {
                s.nInSuccessionSkippedStresses = 0;
            } else {
                s.nInSuccessionSkippedStresses++;
            }
        }

        if (s.cursor.isPhoneticWordEnded()) {
            MatchingState cur = s;

            int nPositiveViolations = 0;
            int nNegativeViolations = 0;

            while (cur.term != null && (cur == s || !cur.cursor.isPhoneticWordEnded()) &&
                    (nNegativeViolations == 0 || nPositiveViolations == 0)) {
                if (!cur.term.match(cur.isStressed)) {
                    if (cur.isStressed) {
                        nNegativeViolations++;
                    } else {
                        nPositiveViolations++;
                    }
                }

                cur = cur.previous;
            }

            if (nNegativeViolations != 0 && nPositiveViolations != 0) {
                s.nReaccentuationRestrictionViolations++;
            }
        }
    }

    @SuppressWarnings("unused")
    public SyllablePatternType[] createMatchingArray(int resultNumber, boolean reverse) {
        MatchingState s = results.get(resultNumber);
        int n = 0;
        while (s.previous != null) {
            n++;
            s = s.previous;
        }

        s = results.get(resultNumber);

        SyllablePatternType[] arr = new SyllablePatternType[n];

        if (reverse) {
            int i = 0;
            while (s.term != null) {
                arr[i++] = s.term.getType();
                s = s.previous;
            }
        } else {
            int i = arr.length - 1;
            while (s.term != null) {
                arr[i--] = s.term.getType();
                s = s.previous;
            }
        }

        return arr;
    }

    public BitSet getStressSequence(int resultNumber, boolean reverse) {
        MatchingState s = results.get(resultNumber);

        int n = 0;
        while (s.previous != null) {
            n++;
            s = s.previous;
        }

        BitSet result = new BitSet(n);
        s = results.get(resultNumber);

        if (reverse) {
            int i = 0;
            while (s.previous != null) {
                result.set(i++,s.isStressed);
                s = s.previous;
            }
        } else {
            int i = n - 1;
            while (s.previous != null) {
                result.set(i--,s.isStressed);
                s = s.previous;
            }
        }

        return result;
    }

    public int getNumberOfResults() {
        return results.size();
    }

    public int getNumberOfStressedSyllables(int numberOfResult) {
        return results.get(numberOfResult).nStressed;
    }

    public double getPenalty(int numberOfResult) {
        return results.get(numberOfResult).countPenalty();
    }

    /*public double getOverStressed(int numberOfResult) {
        MatchingState state = results.get(numberOfResult);

        int n = 0;
        while( state.previous != null ) {
            n++;
            state = state.previous;
        }

        assert( n > 0 );

        return ((double) state.nOverStressed)/n;
    }

    public double getUnstressed(int numberOfResult) {
        MatchingState state = results.get(numberOfResult);

        int n = 0;
        while( state.previous != null ) {
            n++;
            state = state.previous;
        }

        assert( n > 0 );

        return ((double) state.nUnstressed)/n;
    }*/
}
