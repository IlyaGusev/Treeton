/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar;

import java.util.List;

/**
 * We need apropriate matching of signature for NULL, not just String.equals()
 */
public class Signature implements Constants {
	private String namePart;

	private List<String> paramsPart;

	public Signature(String namePart, List<String> paramsPart) {
		super();
		this.namePart = namePart;
		this.paramsPart = paramsPart;
	}

	@Override
	public int hashCode() {
		return this.namePart.hashCode();
	}

	@Override
	public boolean equals(Object otherSignature) {
		if (this == otherSignature)
			return true;
		if (otherSignature == null)
			return false;
		try {
			Signature s = (Signature) otherSignature;
			if (!this.namePart.equals(s.namePart))
				return false;
			if (this.paramsPart.size() != s.paramsPart.size())
				return false;
			for (int i = 0; i < this.paramsPart.size(); i++) {
				if (this.paramsPart.get(i).equals(TYPE_OBJECT)
						|| s.paramsPart.get(i).equals(TYPE_OBJECT))
					continue;
				if (!this.paramsPart.get(i).equals(s.paramsPart.get(i)))
					return false;
			}
			return true;
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(this.namePart);
		sb.append('(');
		if (this.paramsPart.size() > 0)
			sb.append(this.paramsPart.get(0));
		for (int i = 1; i < this.paramsPart.size(); i++) {
			sb.append(',');
			sb.append(this.paramsPart.get(i));
		}
		sb.append(')');
		return sb.toString();
	}

    public String getType(int i) {
        return paramsPart.get(i);
    }

    public String getNamePart() {
        return namePart;
    }

    public List<String> getParamsPart() {
        return paramsPart;
    }
}
