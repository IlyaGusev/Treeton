/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.metricsearch;

import java.util.Collection;

public class MetricSearchParameter {
    public MetricSearchParameter(String name, ParameterType type, Collection<String> predefinedValues, String defaultValue) {
        this.name = name;
        this.type = type;
        this.predefinedValues = predefinedValues;
        this.defaultValue = defaultValue;
    }

    public String name;
    public ParameterType type;
    public Collection<String> predefinedValues;
    public String defaultValue;

    public enum ParameterType {
        STRING,
        REAL,
        ENUM
    }
}
