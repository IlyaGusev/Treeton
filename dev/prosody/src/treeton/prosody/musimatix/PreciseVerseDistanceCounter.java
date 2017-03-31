/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.musimatix;

import java.util.ArrayList;
import java.util.Vector;

public class PreciseVerseDistanceCounter {
    private final int sourceFirstLineIndex;
    private ArrayList<VerseDescription> sourceVerseInfo;
    private Vector<Double> sourceAverage;
    private boolean averageFootnessMode;
    private double meterMult;
    private double footnessMult;
    private int[] dimensionPriorities;
    private boolean[] multOrDeltaForDimensions;
    private int numberOfPriorities;
    private double[] dimensionProbsMask;
    private double sumProbForDimensionProbsMask;

    PreciseVerseDistanceCounter(ArrayList<VerseDescription> sourceVerseInfo, Vector<Double> sourceAverage, int sourceFirstLineIndex, boolean averageFootnessMode, double meterMult, double footnessMult) {
        this.sourceVerseInfo = sourceVerseInfo;
        this.meterMult = meterMult * meterMult;
        this.footnessMult = footnessMult * footnessMult;
        this.averageFootnessMode = averageFootnessMode;
        this.sourceFirstLineIndex = sourceFirstLineIndex;
        this.sourceAverage = sourceAverage;

        if( averageFootnessMode ) {
            dimensionPriorities = new int[0];
            multOrDeltaForDimensions = new boolean[0];
            numberOfPriorities = 0;
            dimensionProbsMask = new double[0];
        }
    }

    public static class DistanceWithShift {
        DistanceWithShift( double distance, int shift ) {
            this.distance = distance;
            this.shift = shift;
        }

        public double distance;
        public int shift;

        @Override
        public String toString() {
            return "DistanceWithShift{" +
                    "distance=" + distance +
                    ", shift=" + shift +
                    '}';
        }
    }

    public DistanceWithShift countDistance( ArrayList<VerseDescription> verseInfo, int firstLineIndex ) {
        double[] maxProbs = new double[numberOfPriorities];
        int cntx = 0;
        int cnty = 0;
        double similarity = 0.0;
        int i= sourceFirstLineIndex,j=firstLineIndex;
        for( ; i < sourceVerseInfo.size() && j < verseInfo.size(); ) {
            VerseDescription x = sourceVerseInfo.get(i);

            if( x == null || x.metricVectorIsZero() ) {
                i++;
                continue;
            }

            VerseDescription y = verseInfo.get(j);

            if( y == null || y.metricVectorIsZero() ) {
                j++;
                continue;
            }

            assert x.metricVector.size() == y.metricVector.size();

            for (int k = 0; k < maxProbs.length; k++) {
                maxProbs[k] = 0.0;
            }

            double d = 0.0;
            for( int k = 0; k < x.metricVector.size(); k++ ) {
                Double xk = x.metricVector.get(k);
                Double yk = y.metricVector.get(k);

                if( averageFootnessMode ) {
                    double delta = (xk == null ? 0.0 : xk) - (yk == null ? 0.0 : yk);
                    delta = delta * delta;

                    int l = k % 3;
                    if( l == 0 ) {
                        delta *= meterMult;
                    } else if( l == 1 ) {
                        delta *= footnessMult;
                    }
                    d += delta;
                } else {
                    double xp = xk == null ? 0.0 : xk;
                    double yp = yk == null ? 0.0 : yk;
                    double pp = multOrDeltaForDimensions[k] ? xp * yp :
                            1 - Math.abs( xp - yp );
                    int priority = dimensionPriorities[k];
                    maxProbs[priority] = Math.max(maxProbs[priority],pp);
                }
            }

            if( averageFootnessMode ) {
                similarity += Math.sqrt(d);
            } else {
                double sum = 0.0;
                for (int k = 0; k < maxProbs.length; k++) {
                    double maxProb = maxProbs[k];
                    sum += maxProb * dimensionProbsMask[k];
                }
                similarity += sum / sumProbForDimensionProbsMask;
            }

            cntx++;
            cnty++;
            i++;
            j++;
        }

        assert cntx == cnty;
        assert i == sourceVerseInfo.size() || j == verseInfo.size();

        similarity /= cntx;

        if( !averageFootnessMode ) {
            similarity = 1 - similarity;
        }

        int shiftOfMin=sourceFirstLineIndex;
        for( ; i < sourceVerseInfo.size(); shiftOfMin++ ) {
            VerseDescription x = sourceVerseInfo.get(shiftOfMin);

            if( x != null && !x.metricVectorIsZero() ) {
                break;
            }
        }

        if( i < sourceVerseInfo.size() ) {
            //Сравниваемый текст меньше исходного
            return new DistanceWithShift(similarity,shiftOfMin);
        }

        for( ; j < verseInfo.size(); j++ ) {
            VerseDescription y = verseInfo.get(j);

            if (y == null || y.metricVectorIsZero()) {
                continue;
            }

            cnty++;
        }

        //Сравниваемый текст больше исходного (cntx < cnty), идем окном

        double minSimilarity = similarity;

        cnty--;
        for( int l = firstLineIndex + 1; l < verseInfo.size() && cntx <= cnty; ) {
            // l - смещение в сравниваемом тексте

            VerseDescription y = verseInfo.get(l);

            if( y == null || y.metricVectorIsZero() ) {
                l++;
                continue;
            }

            i=sourceFirstLineIndex;
            j=l;
            double currentSimilarity = 0;
            for( ; i < sourceVerseInfo.size() && j < verseInfo.size(); ) {
                VerseDescription x = sourceVerseInfo.get(i);

                if( x == null || x.metricVectorIsZero() ) {
                    i++;
                    continue;
                }

                y = verseInfo.get(j);

                if( y == null || y.metricVectorIsZero() ) {
                    j++;
                    continue;
                }

                assert x.metricVector.size() == y.metricVector.size();

                for (int k = 0; k < maxProbs.length; k++) {
                    maxProbs[k] = 0.0;
                }

                double d = 0.0;
                for( int k = 0; k < x.metricVector.size(); k++ ) {
                    Double xk = x.metricVector.get(k);
                    Double yk = y.metricVector.get(k);

                    if( averageFootnessMode ) {
                        double delta = (xk == null ? 0.0 : xk) - (yk == null ? 0.0 : yk);
                        delta = delta * delta;

                        int m = k % 3;
                        if( m == 0 ) {
                            delta *= meterMult;
                        } else if( m == 1 ) {
                            delta *= footnessMult;
                        }
                        d += delta;
                    } else {
                        double xp = xk == null ? 0.0 : xk;
                        double yp = yk == null ? 0.0 : yk;
                        double pp = multOrDeltaForDimensions[k] ? xp * yp :
                                1 - Math.abs( xp - yp );
                        int priority = dimensionPriorities[k];
                        maxProbs[priority] = Math.max(maxProbs[priority],pp);
                    }
                }

                if( averageFootnessMode ) {
                    currentSimilarity += Math.sqrt(d);
                } else {
                    double sum = 0.0;
                    for (int k = 0; k < maxProbs.length; k++) {
                        double maxProb = maxProbs[k];
                        sum += maxProb * dimensionProbsMask[k];
                    }
                    currentSimilarity += sum / sumProbForDimensionProbsMask;
                }

                i++;
                j++;
            }

            currentSimilarity /= cntx;
            if( !averageFootnessMode ) {
                currentSimilarity = 1 - currentSimilarity;
            }

            if( currentSimilarity < minSimilarity ) {
                minSimilarity = currentSimilarity;
                shiftOfMin =    l;
            }

            cnty--;
            l++;
        }

        return new DistanceWithShift(minSimilarity,shiftOfMin);
    }

    public void setDimensionInfo(int numberOfPriorities, int[] dimensionPriorities,boolean[] multOrDeltaForDimensions) {
        this.dimensionPriorities = dimensionPriorities;
        this.multOrDeltaForDimensions = multOrDeltaForDimensions;
        this.numberOfPriorities = numberOfPriorities;

        dimensionProbsMask = new double[numberOfPriorities];

        for(int i = 0; i < dimensionProbsMask.length; i++) {
            dimensionProbsMask[i] = 0.0;
        }

        for(int i = 0; i < sourceAverage.size(); i++) {
            int priority = dimensionPriorities[i];
            boolean multOrDelta = multOrDeltaForDimensions[i];
            Double p =  multOrDelta ? sourceAverage.get(i) : 1.0;
            double prevP = dimensionProbsMask[priority];
            dimensionProbsMask[priority] = Math.max((p==null ? 0.0 : p),prevP);
        }

        // TODO, то, что не учитываем здесь последнее измерение это костыль,
        // избавиться от него через multOrDeltaForProrities

        //Костыль!!!!

        dimensionProbsMask[2] *= (1 - dimensionProbsMask[0]);
        dimensionProbsMask[1] *= (1 - dimensionProbsMask[0]);
        dimensionProbsMask[2] *= (1 - dimensionProbsMask[1]);
        dimensionProbsMask[3] *= 0.2;

        sumProbForDimensionProbsMask = 0.0;
        for (double p : dimensionProbsMask) {
            sumProbForDimensionProbsMask += p;
        }

        if( sumProbForDimensionProbsMask < 0.0001 ) { //защита
            sumProbForDimensionProbsMask = 0.01;
        }
    }
}
