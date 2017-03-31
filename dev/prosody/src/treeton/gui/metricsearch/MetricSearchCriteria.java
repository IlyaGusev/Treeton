/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.metricsearch;

import treeton.prosody.corpus.CorpusEntry;

import java.util.Map;

public abstract class MetricSearchCriteria {
    enum OperatorType {
        EQUAL,
        GREAT,
        LESS,;

        @Override
        public String toString() {
            switch (this) {
                case EQUAL:
                    return "=";
                case GREAT:
                    return ">";
                case LESS:
                    return "<";

            }

            assert false;
            return "!";
        }
    }

    public abstract String getName();
    public abstract MetricSearchCriteriaSignature getSignature();

    public abstract boolean setParameterInfo( String parameter, String value, OperatorType operatorType );
    public abstract String getParameterValue( String parameter );
    public abstract OperatorType getOperatorType( String parameter );

    public abstract boolean match(CorpusEntry entry);

    public abstract String getCriteriaName();

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    private boolean negated = false;

    public abstract boolean isValid();
}
