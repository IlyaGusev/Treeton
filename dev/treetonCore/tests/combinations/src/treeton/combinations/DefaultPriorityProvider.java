/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.combinations;

import treeton.core.util.combinator.PriorityProvider;

public class DefaultPriorityProvider implements PriorityProvider {
    private double priority;

    public DefaultPriorityProvider(double priority) {
        this.priority = priority;
    }

    public double getPriority() {
        return priority;
    }

    public String toString() {
        return Double.toString(priority);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultPriorityProvider that = (DefaultPriorityProvider) o;

        if (Double.compare(that.priority, priority) != 0) return false;

        return true;
    }

    public int hashCode() {
        long temp = priority != +0.0d ? Double.doubleToLongBits(priority) : 0L;
        return (int) (temp ^ (temp >>> 32));
    }
}