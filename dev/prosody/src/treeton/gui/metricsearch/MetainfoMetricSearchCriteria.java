/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.metricsearch;

import treeton.core.*;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.prosody.corpus.CorpusEntry;

import java.util.Objects;

public class MetainfoMetricSearchCriteria extends MetricSearchCriteria {
    private TrnType trnType;
    private int featureIndex;
    private MetricSearchCriteriaSignature signature;

    private Object currentValue;
    private OperatorType operatorForIntegerParameter;
    private boolean isValid = false;

    public MetainfoMetricSearchCriteria( TrnType trnType, int featureIndex ) throws TreetonModelException {
        signature = new MetricSearchCriteriaSignature();
        this.featureIndex = featureIndex;
        this.trnType = trnType;
        Class featureType = trnType.getFeatureTypeByIndex(featureIndex);

        MetricSearchParameter.ParameterType pType = featureType == TString.class ?
                MetricSearchParameter.ParameterType.STRING : MetricSearchParameter.ParameterType.REAL;

        MetricSearchParameter parameter = new MetricSearchParameter(trnType.getFeatureNameByIndex(featureIndex),pType,null,"");
        signature.metricSearchParameters.add(parameter);

        operatorForIntegerParameter = OperatorType.EQUAL;
        setParameterInfo(parameter.name,pType == MetricSearchParameter.ParameterType.STRING ? "" : "0",operatorForIntegerParameter);
    }

    @Override
    public String getName() {
        try {
            return "Ограничение на параметр " + trnType.getFeatureNameByIndex(featureIndex);
        } catch (TreetonModelException e) {
            return "TreetonModelException!!!";
        }
    }

    @Override
    public MetricSearchCriteriaSignature getSignature() {
        return signature;
    }

    @Override
    public boolean setParameterInfo(String parameter, String value, OperatorType operatorType) {
        try {
            if( parameter == null || !parameter.equals( trnType.getFeatureNameByIndex(featureIndex) ) ) {
                isValid = false;
                return false;
            }

            if( trnType.getFeatureTypeByIndex(featureIndex) == TString.class ) {
                currentValue = TreetonFactory.newTString(value);
            } else if( trnType.getFeatureTypeByIndex(featureIndex) == Integer.class ) {
                try {
                    currentValue = Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    isValid = false;
                    return false;
                }

                operatorForIntegerParameter = operatorType;
            } else {
                assert false;
            }
        } catch (TreetonModelException e) {
            isValid = false;
            return false;
        }

        isValid = true;
        return true;
    }

    @Override
    public String getParameterValue(String parameter) {
        return currentValue.toString();
    }

    @Override
    public OperatorType getOperatorType(String parameter) {
        return signature.metricSearchParameters.get(0).type == MetricSearchParameter.ParameterType.REAL ? operatorForIntegerParameter : null;
    }

    @Override
    public boolean match(CorpusEntry entry) {
        TreenotationStorageImpl metadata = entry.getMetadata();
        TypeIteratorInterface iterator = metadata.typeIterator(trnType);
        assert iterator.hasNext();

        Treenotation corpusElementTrn = (Treenotation) iterator.next();

        if( iterator.hasNext() ) {
            iterator.close();
            assert false;
        }

        Object o = corpusElementTrn.get(featureIndex);

        if( o == null ) {
            return false;
        }

        try {
            if( trnType.getFeatureTypeByIndex(featureIndex) == TString.class ) {
                return o.equals(currentValue);
            } else if( trnType.getFeatureTypeByIndex(featureIndex) == Integer.class ) {
                Integer corpusElementVal = (Integer) o;
                Integer curVal = (Integer) currentValue;

                assert( curVal != null );

                if( operatorForIntegerParameter == OperatorType.EQUAL ) {
                    return Objects.equals(curVal, corpusElementVal);
                } else if( operatorForIntegerParameter == OperatorType.LESS ) {
                    return corpusElementVal < curVal;
                } else if( operatorForIntegerParameter == OperatorType.GREAT ) {
                    return corpusElementVal > curVal;
                } else {
                    assert false;
                }
            } else {
                assert false;
            }
        } catch (TreetonModelException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public String getCriteriaName() {
        try {
            return "Ограничение на "+trnType.getFeatureNameByIndex(featureIndex);
        } catch (TreetonModelException e) {
            return "Treeton model error!!!";
        }
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if( isNegated() ) {
            sb.append("^");
        }
        try {
            sb.append(trnType.getFeatureNameByIndex(featureIndex)).append(operatorForIntegerParameter == null ? "=" : operatorForIntegerParameter);
            sb.append(currentValue);
        } catch (TreetonModelException e) {
            sb.append("Treeton model error!!!");
        }

        return sb.toString();
    }
}
