/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

public class ScapeBindingSet implements Comparable {
    ScapeBinding[] bindings;
    int size;

    public int compareTo(Object o) {
        if (o instanceof ScapeBindingSet) {
            ScapeBindingSet other = (ScapeBindingSet) o;
            int len1 = size;
            int len2 = other.size;
            int n = Math.min(len1, len2);
            ScapeBinding[] v1 = bindings;
            ScapeBinding[] v2 = other.bindings;
            int i = 0;

            while (i < n) {
                int s1 = v1[i].getId();
                int s2 = v2[i].getId();
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
        if (o instanceof ScapeBindingSet) {
            ScapeBindingSet other = (ScapeBindingSet) o;
            if (size != other.size)
                return false;
            ScapeBinding[] v1 = bindings;
            ScapeBinding[] v2 = other.bindings;
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
        StringBuffer b = new StringBuffer();
        b.append("[");
        for (int i = 0; i < size; i++) {
            Object o = bindings[i];
            if (o instanceof ScapeDFSMBinding) {
                b.append("<b>");
                b.append(((ScapeDFSMBinding) o).getId());
                b.append("</b> (<i>old: </i>");

                for (int j = 0; j < ((ScapeDFSMBinding) o).oldBindings.length; j++) {
                    ScapeFSMBinding ob = ((ScapeDFSMBinding) o).oldBindings[j];
                    b.append("<b>");
                    b.append(ob.rule.name);
                    b.append(":");
                    b.append(ob.name);
                    b.append("</b>, ");
                }
                b.append("), ");
            } else if (o instanceof ScapeFSMBinding) {
                ScapeFSMBinding ob = (ScapeFSMBinding) o;
                b.append("<i>old </i>");
                b.append("<b>");
                b.append(ob.rule.name);
                b.append(":");
                b.append(ob.name);
                b.append("</b>, ");
            }
        }
        b.append("]");
        return b.toString();
    }
}
