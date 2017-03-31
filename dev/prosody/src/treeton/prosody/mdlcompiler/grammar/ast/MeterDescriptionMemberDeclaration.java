/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

public class MeterDescriptionMemberDeclaration extends BaseNode {
	private BaseNode descriptionMember;

    public MeterDescriptionMemberDeclaration(PriorityDeclaration descriptionMember) {
		this.descriptionMember = descriptionMember;
	}

	public MeterDescriptionMemberDeclaration(ThresholdDeclaration descriptionMember) {
		this.descriptionMember = descriptionMember;
	}

    public MeterDescriptionMemberDeclaration(MeterNameDeclaration descriptionMember) {
		this.descriptionMember = descriptionMember;
	}

	public int getColumn() {
		return this.descriptionMember.getColumn();
	}

	public int getLeft() {
		return this.descriptionMember.getLeft();
	}

	public int getLine() {
		return this.descriptionMember.getLine();
	}

	public int getRight() {
		return this.descriptionMember.getRight();
	}

	@Override
	public void visit(Visitor visitor, boolean visitChildren) {
		if (visitChildren)
			this.descriptionMember.visit(visitor, visitChildren);
		visitor.execute(this);
	}

    public BaseNode getDescriptionMember() {
        return descriptionMember;
    }
}
