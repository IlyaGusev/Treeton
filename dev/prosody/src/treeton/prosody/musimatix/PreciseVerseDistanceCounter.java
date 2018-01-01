/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.musimatix;

import java.util.ArrayList;

public class PreciseVerseDistanceCounter {
    private ArrayList<VerseDescription> sourceVerseInfo; // Набор строчек текста-запроса.
    private final int sourceFirstLineIndex; // Сдвиг в тексте-запросе (часто 0).
    private int[] meterRegressionIndex; // Индексы столбцов в регрессии.
    private DimensionOperation[] dimensionOperations; // Операции для "сложения" вероятностей в разных столбцах.
    private int numberOfFeatures; // Количество фич в регрессии.
    private double[] regressionCoefficients; // Коэффициенты для разных столбцов.

    public enum DimensionOperation {
        Multiplication, Delta
    }

    PreciseVerseDistanceCounter( ArrayList<VerseDescription> sourceVerseInfo, int sourceFirstLineIndex,
                                 int numberOfFeatures, int[] meterRegressionIndex,
                                 DimensionOperation[] dimensionOperations, double[] regressionCoefficients )
    {
        this.sourceVerseInfo = sourceVerseInfo;
        this.sourceFirstLineIndex = sourceFirstLineIndex;
        this.meterRegressionIndex = meterRegressionIndex;
        this.dimensionOperations = dimensionOperations;
        this.numberOfFeatures = numberOfFeatures;
        this.regressionCoefficients = regressionCoefficients;
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
        double[] sumVectorSimilarity = new double[source.get(0).metricVector.size()];
        for( int k = 0; k < sumVectorSimilarity.length; k++ ) {
            sumVectorSimilarity[k] = 0.0;
        }
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

            double[] vectorSimilarity = getVectorSimilarity(x, y);
            for( int k = 0; k < sumVectorSimilarity.length; k++ ) {
                sumVectorSimilarity[k] += vectorSimilarity[k];
            }
            sourceShift++;
            targetShift++;
            realLinesNum++;
        }
        if( realLinesNum != sourceLineNum && realLinesNum != targetLineNum ) {
            return Double.MAX_VALUE;
        }
        for( int k = 0; k < sumVectorSimilarity.length; k++ ) {
            sumVectorSimilarity[k] /= realLinesNum;
        }

        double[] maxByColumn = new double[numberOfFeatures+1];
        for( int k = 0; k < maxByColumn.length; k++ ) {
            maxByColumn[k] = 0.0;
        }
        for( int k = 0; k < sumVectorSimilarity.length; k++ ) {
            int colIndex = meterRegressionIndex[k];
            if( colIndex != -1 ) {
                maxByColumn[colIndex] = Math.max(maxByColumn[colIndex], sumVectorSimilarity[k]);
            }
        }
        maxByColumn[numberOfFeatures] = 1.0; // Добавляем константу.
        for( int k = 0; k < maxByColumn.length; k++ ) {
            similarity += regressionCoefficients[k] * maxByColumn[k];
        }
        similarity = Math.min(similarity, 1.0);
        similarity = Math.max(similarity, 0.0);

        return similarity;
    }

    private double[] getVectorSimilarity(VerseDescription x, VerseDescription y) {
        assert x.metricVector.size() == y.metricVector.size();

        double[] vectorSimilarity = new double[x.metricVector.size()];
        for( int k = 0; k < vectorSimilarity.length; k++ ) {
            vectorSimilarity[k] = 0.0;
        }

        for( int k = 0; k < x.metricVector.size(); k++ ) {
            Double xk = x.metricVector.get(k);
            Double yk = y.metricVector.get(k);

            double xp = xk == null ? 0.0 : xk;
            double yp = yk == null ? 0.0 : yk;
            vectorSimilarity[k] = dimensionOperations[k] == DimensionOperation.Multiplication ?
                    getNumberSimilarity( xp, yp ) : 1 - Math.abs( xp - yp );
        }

        return vectorSimilarity;
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
            i++;
            lineNum++;
        }
        return lineNum;
    }

    private int getFirstValuableLine( ArrayList<VerseDescription> verseInfo, int shift ) {
        int shiftOfValuable = shift;
        for( ; shiftOfValuable < verseInfo.size(); shiftOfValuable++ ) {
            VerseDescription x = verseInfo.get( shiftOfValuable );
            if( x != null && !x.metricVectorIsZero() ) {
                break;
            }
        }
        return shiftOfValuable;
    }

    private double getNumberSimilarity( double i, double j ) {
        if (i*j < 0.00001) {
            return 0.0;
        }
        return Math.pow((1.0 - Math.abs(i - j)), 3.0);
    }
}
