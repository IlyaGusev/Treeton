/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

public class MeterDescriptionMemberDeclarationList extends
		BaseListNode<MeterDescriptionMemberDeclaration> {

	public MeterDescriptionMemberDeclarationList(
			MeterDescriptionMemberDeclaration memberDeclaration) {
		super(memberDeclaration, null);
	}
}
