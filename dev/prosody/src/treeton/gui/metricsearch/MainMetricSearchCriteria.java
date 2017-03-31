/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.metricsearch;

import treeton.prosody.corpus.CorpusEntry;
import treeton.prosody.metricindex.MetricIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainMetricSearchCriteria extends MetricSearchCriteria {
    private final String metricTemplateParameterName = "Метрический шаблон";
    private final String footnessParameterName = "Количество стоп";
    private final String modeParameterName = "Режим";
    private final String thresholdParameterName = "Пороговое значение";
    private final String modeParameterAverageValue = "по среднему значению";
    private final String modeParameterMaxValue = "по максимальному значению";

    private enum Mode {
        MAX,
        AVERAGE;

        @Override
        public String toString() {
            return this == MAX ? "max" : "avg";
        }
    }

    private Mode mode;
    private String meter;
    private int footness;
    private OperatorType operatorForFootness;
    private double threshold;
    private OperatorType operatorForThreshold;
    private MetricIndex metricIndex;
    private Set<String> validParameters = new HashSet<>();

    public MainMetricSearchCriteria( MetricIndex index ) {
        signature = new MetricSearchCriteriaSignature();
        metricIndex = index;

        assert !index.getMeters().isEmpty();

        MetricSearchParameter meterMetricSearchParameter = new MetricSearchParameter(metricTemplateParameterName,
                MetricSearchParameter.ParameterType.ENUM, index.getMeters(), index.getMeters().iterator().next() );

        signature.metricSearchParameters.add(meterMetricSearchParameter);
        meter = meterMetricSearchParameter.defaultValue;
        validParameters.add(metricTemplateParameterName);

        MetricSearchParameter footnessMetricSearchParameter = new MetricSearchParameter(footnessParameterName,
                MetricSearchParameter.ParameterType.REAL, null, "4" );

        signature.metricSearchParameters.add(footnessMetricSearchParameter);
        footness = 4;
        operatorForFootness = OperatorType.EQUAL;
        validParameters.add(footnessParameterName);

        ArrayList<String> modes = new ArrayList<>();
        modes.add(modeParameterAverageValue);
        modes.add(modeParameterMaxValue);

        MetricSearchParameter modeMetricSearchParameter = new MetricSearchParameter(modeParameterName,
                MetricSearchParameter.ParameterType.ENUM, modes, modes.get(0) );
        mode = Mode.AVERAGE;
        validParameters.add(modeParameterName);

        signature.metricSearchParameters.add(modeMetricSearchParameter);

        MetricSearchParameter thresholdMetricSearchParameter = new MetricSearchParameter(thresholdParameterName,
                MetricSearchParameter.ParameterType.REAL, null, "0" );

        threshold = 0;
        operatorForThreshold = OperatorType.EQUAL;

        signature.metricSearchParameters.add(thresholdMetricSearchParameter);
        validParameters.add(thresholdParameterName);
    }

    private MetricSearchCriteriaSignature signature;

    @Override
    public String getName() {
        return "Ритмико-метрическое ограничение";
    }

    @Override
    public MetricSearchCriteriaSignature getSignature() {
        return signature;
    }

    @Override
    public boolean setParameterInfo(String parameter, String value, OperatorType operatorType) {
        switch( parameter ) {
            case metricTemplateParameterName:
                if( !metricIndex.getMeters().contains(value) ) {
                    validParameters.remove(parameter);
                    return false;
                }
                meter = value;
                break;
            case footnessParameterName:
                try {
                    double d = Double.valueOf(value);

                    if( Math.ceil(d) != d ) {
                        validParameters.remove(parameter);
                        return false;
                    }

                    if( d <= 0 ) {
                        validParameters.remove(parameter);
                        return false;
                    }

                    footness = (int) d;
                } catch (NumberFormatException e) {
                    validParameters.remove(parameter);
                    return false;
                }

                operatorForFootness = operatorType;

                break;
            case modeParameterName:
                if(modeParameterAverageValue.equals(value)) {
                    mode = Mode.AVERAGE;
                } else if(modeParameterMaxValue.equals(value)) {
                    mode = Mode.MAX;
                } else {
                    validParameters.remove(parameter);
                    return false;
                }

                break;
            case thresholdParameterName:
                try {
                    double d = Double.valueOf(value);

                    if( d < 0 ) {
                        validParameters.remove(parameter);
                        return false;
                    }

                    threshold = d;
                } catch (NumberFormatException e) {
                    validParameters.remove(parameter);
                    return false;
                }

                operatorForThreshold = operatorType;
                break;
            default:
                validParameters.remove(parameter);
                return false;

        }

        validParameters.add(parameter);
        return true;
    }

    @Override
    public String getParameterValue(String parameter) {
        switch( parameter ) {
            case metricTemplateParameterName:
                return meter;
            case footnessParameterName:
                return Integer.toString(footness);
            case modeParameterName:
                return mode.toString();
            case thresholdParameterName:
                return Double.toString(threshold);
            default:
                assert false;
        }
        return null;
    }

    @Override
    public OperatorType getOperatorType(String parameter) {
        switch( parameter ) {
            case metricTemplateParameterName:
                return null;
            case footnessParameterName:
                return operatorForFootness;
            case modeParameterName:
                return null;
            case thresholdParameterName:
                return operatorForThreshold;
            default:
                assert false;
        }

        return null;
    }

    @Override
    public boolean match(CorpusEntry entry) {
        ArrayList<Double> allProbabilities = new ArrayList<>();

        if( operatorForFootness == OperatorType.EQUAL ) {
            double[] probabilities = metricIndex.getMeterProbabilities(entry, meter, footness);

            if( probabilities == null ) {
                return false;
            }

            for (double probability : probabilities) {
                allProbabilities.add(probability);
            }
        } else if( operatorForFootness == OperatorType.LESS ) {
            for( int i = 1; i < footness; i++ ) {
                double[] probabilities = metricIndex.getMeterProbabilities(entry, meter, i);

                if( probabilities != null ) {
                    for (double probability : probabilities) {
                        allProbabilities.add(probability);
                    }
                }
            }
        } else if( operatorForFootness == OperatorType.GREAT ) {
            int max = metricIndex.getMaxFootCount(entry, meter);
            for( int i = footness + 1; i <= max; i++ ) {
                double[] probabilities = metricIndex.getMeterProbabilities(entry, meter, i);

                if( probabilities != null ) {
                    for (double probability : probabilities) {
                        allProbabilities.add(probability);
                    }
                }
            }
        } else {
            assert false;
        }

        if( allProbabilities.size() == 0 ) {
            return false;
        }

        double d = 0;

        if( mode == Mode.AVERAGE ) {
            for (double probability : allProbabilities) {
                d += probability;
            }

            d /= allProbabilities.size();
        } else if( mode == Mode.MAX ) {
            for (double probability : allProbabilities) {
                d = Math.max( d, probability );
            }
        }

        if( operatorForThreshold == OperatorType.EQUAL ) {
            return threshold == d;
        } else if( operatorForThreshold == OperatorType.LESS ) {
            return d < threshold;
        } else if( operatorForThreshold == OperatorType.GREAT ) {
            return d > threshold;
        } else {
            assert false;
        }

        return false;
    }

    @Override
    public String getCriteriaName() {
        return "Ритмико-метрическое ограничение";
    }

    @Override
    public boolean isValid() {
        return signature.metricSearchParameters.size() == validParameters.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if( isNegated() ) {
            sb.append("^");
        }
        sb.append(meter).append(":n").append(operatorForFootness).append(footness).append(":");
        sb.append(mode).append(":d").append(operatorForThreshold).append(String.format("%.2f",threshold));

        return sb.toString();
    }
}
