/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.musimatix;

import java.util.ArrayList;
import java.util.Vector;

public class PreciseVerseDistanceCounter {
    private ArrayList<VerseDescription> sourceVerseInfo; // Набор строчек текста-запроса.
    private final int sourceFirstLineIndex; // Сдвиг в тексте-запросе (часто 0).
    private int[] dimensionPriorities; // Приоритеты столбцов матрицы "вероятностей".
    private DimensionOperation[] dimensionOperations; // Операции для "сложения" вероятностей в разных столбцах.
    private int numberOfPriorities; // Количество приоритетов.
    private double[] dimensionProbsMask; // Коэффициенты для разных столбцов.
    // Нормировочный коэффициент на основе средних вероятностей по строкам и наших коэффициентов.
    private double sumProbForDimensionProbsMask;

    public enum DimensionOperation {
        Multiplication, Delta
    }

    PreciseVerseDistanceCounter( ArrayList<VerseDescription> sourceVerseInfo, Vector<Double> sourceAverage,
                                 int sourceFirstLineIndex, int numberOfPriorities, int[] dimensionPriorities,
                                 DimensionOperation[] dimensionOperations )
    {
        this.sourceVerseInfo = sourceVerseInfo;
        this.sourceFirstLineIndex = sourceFirstLineIndex;
        this.dimensionPriorities = dimensionPriorities;
        this.dimensionOperations = dimensionOperations;
        this.numberOfPriorities = numberOfPriorities;

        dimensionProbsMask = new double[numberOfPriorities];
        for( int i = 0; i < dimensionProbsMask.length; i++ ) {
            dimensionProbsMask[i] = 0.0;
        }
        for(int i = 0; i < sourceAverage.size(); i++) {
            int priority = dimensionPriorities[i];
            // TODO: Костыль ниже.
            Double p =  dimensionOperations[i] == DimensionOperation.Multiplication ? sourceAverage.get(i) : 1.0;
            p = p == null ? 0.0 : p;
            dimensionProbsMask[priority] = Math.max( dimensionProbsMask[priority], p );
        }

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

    public static class DistanceWithShift {
        DistanceWithShift( double distance, int sourceShift, int targetShift ) {
            this.distance = distance;
            this.sourceShift = sourceShift;
            this.targetShift = targetShift;
        }

        public double distance;
        public int sourceShift;
        public int targetShift;

        @Override
        public String toString() {
            return "DistanceWithShift{" +
                    "distance=" + distance +
                    ", source shift=" + sourceShift +
                    ", target shift=" + targetShift +
                    '}';
        }
    }

    public DistanceWithShift countDistance( ArrayList<VerseDescription> targetVerseInfo, int targetFirstLineIndex ) {
        double minSimilarity = Double.MAX_VALUE;
        DistanceWithShift minDWS = new DistanceWithShift(Double.MAX_VALUE, sourceFirstLineIndex, targetFirstLineIndex );
        for( int i = sourceFirstLineIndex; i < sourceVerseInfo.size(); i++ ) {
            for( int j = targetFirstLineIndex; j < targetVerseInfo.size(); j++ ) {
                double similarity = getMatrixSimilarity(sourceVerseInfo, targetVerseInfo, i, j );
                if( similarity < minSimilarity ) {
                    minSimilarity = similarity;
                    int leftIndex = getFirstValuableLine(sourceVerseInfo, i);
                    int rightIndex = getFirstValuableLine(targetVerseInfo, j);
                    minDWS = new DistanceWithShift( minSimilarity, leftIndex, rightIndex );
                }
            }
        }
        return minDWS;
    }

    private double getMatrixSimilarity(ArrayList<VerseDescription> source, ArrayList<VerseDescription> target,
                                       int sourceShift, int targetShift)
    {
        int sourceLineNum = countLines( source );
        int targetLineNum = countLines( target );
        double similarity = 0;
        int realLinesNum = 0;
        while( sourceShift < source.size() && targetShift < target.size() ) {
            VerseDescription x = source.get(sourceShift);
            if( x == null || x.metricVectorIsZero() ) {
                sourceShift++;
                continue;
            }

            VerseDescription y = target.get(targetShift);
            if( y == null || y.metricVectorIsZero() ) {
                targetShift++;
                continue;
            }

            similarity += getVectorSimilarity(x, y);

            sourceShift++;
            targetShift++;
            realLinesNum++;
        }
        if( realLinesNum != sourceLineNum && realLinesNum != targetLineNum ) {
            return Double.MAX_VALUE;
        }
        similarity /= realLinesNum;
        similarity = 1 - similarity;
        return similarity;
    }

    private double getVectorSimilarity(VerseDescription x, VerseDescription y) {
        assert x.metricVector.size() == y.metricVector.size();

        // maxProbabilities - максимальные вероятности для каждого из приоритетов.
        double[] maxProbabilities = new double[numberOfPriorities];
        for( int k = 0; k < maxProbabilities.length; k++ ) {
            maxProbabilities[k] = 0.0;
        }

        for( int k = 0; k < x.metricVector.size(); k++ ) {
            Double xk = x.metricVector.get(k);
            Double yk = y.metricVector.get(k);

            double xp = xk == null ? 0.0 : xk;
            double yp = yk == null ? 0.0 : yk;
            double pp = dimensionOperations[k] == DimensionOperation.Multiplication ? xp * yp : 1 - Math.abs( xp - yp );
            int priority = dimensionPriorities[k];
            maxProbabilities[priority] = Math.max(maxProbabilities[priority], pp);
        }

        double sum = 0.0;
        for( int k = 0; k < maxProbabilities.length; k++ ) {
            double maxProbability = maxProbabilities[k];
            sum += maxProbability * dimensionProbsMask[k];
        }
        return sum / sumProbForDimensionProbsMask;
    }

    private int countLines(ArrayList<VerseDescription> verseInfo ) {
        int i = 0;
        int lineNum = 0;
        while( i < verseInfo.size() ) {
            VerseDescription x = verseInfo.get(i);
            if (x == null || x.metricVectorIsZero()) {
                i++;
                continue;
            }
            lineNum += 1;
        }
        return lineNum;
    }

    private int getFirstValuableLine( ArrayList<VerseDescription> verseInfo, int shift ) {
        int shiftOfValuable = shift;
        for( ; shiftOfValuable < verseInfo.size(); shiftOfValuable++ ) {
            VerseDescription x = sourceVerseInfo.get( shiftOfValuable );
            if( x != null && !x.metricVectorIsZero() ) {
                break;
            }
        }
        return shiftOfValuable;
    }
}
