/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

public class ScapeRuleSet implements Comparable {
    ScapeRule[] rules;
    int size;

    public ScapeRule get(int i) {
        if (i < 0 || i >= size) {
            return null;
        }
        return rules[i];
    }

    public int size() {
        return size;
    }

    public int compareTo(Object o) {
        if (o instanceof ScapeRuleSet) {
            ScapeRuleSet other = (ScapeRuleSet) o;
            int len1 = size;
            int len2 = other.size;
            int n = Math.min(len1, len2);
            ScapeRule[] v1 = rules;
            ScapeRule[] v2 = other.rules;
            int i = 0;

            while (i < n) {
                int s1 = v1[i].index;
                int s2 = v2[i].index;
                if (s1 != s2) {
                    return s1 - s2;
                }
                i++;
            }
            return len1 - len2;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean equals(Object o) {
        if (o instanceof ScapeRuleSet) {
            ScapeRuleSet other = (ScapeRuleSet) o;
            if (size != other.size)
                return false;
            ScapeRule[] v1 = rules;
            ScapeRule[] v2 = other.rules;
            int i = 0;
            while (i < size) {
                if (v1[i] != v2[i]) {
                    return false;
                }
                i++;
            }
            return true;
        }
        return false;
    }

    String getString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Rules: ");
        for (ScapeRule rule : rules) {
            buf.append(rule.name);
            buf.append(", ");
        }
        return buf.toString();
    }
}
