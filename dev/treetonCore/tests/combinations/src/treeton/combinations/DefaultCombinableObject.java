/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.combinations;

public class DefaultCombinableObject extends DefaultPriorityProvider {
    double c;

    public DefaultCombinableObject(double priority, double c) {
        super(priority);
        this.c = c;
    }

    public int compareTo(DefaultCombinableObject o) {
        double d = c - o.c;
        return d < 0 ? -1 : d > 0 ? 1 : 0;
    }

    public String toString() {
        return super.toString() + " : " + Double.toString(c);
    }
}
