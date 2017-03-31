/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import treeton.prosody.mdlcompiler.grammar.Constants;

public class SyllConstraintOr extends BaseListNode<SyllConstraintList> implements
		Constants {
	public SyllConstraintOr() {
		super("|");
	}

	public SyllConstraintOr(SyllConstraintList constraintList) {
		super(constraintList, "|");
	}
}
