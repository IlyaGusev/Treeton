/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import treeton.prosody.mdlcompiler.grammar.TreevialSymbol;

public class MeterDescription extends BaseNode {
	private TreevialSymbol meterKeyword;
	private TreevialSymbol meterId;
	private TreevialSymbol lbrace;
    private MeterDescriptionMemberDeclarationList members;
    private SyllConstraintOr pattern;
	private TreevialSymbol rbrace;


    public MeterDescription(TreevialSymbol meterKeyword, TreevialSymbol meterId, TreevialSymbol lbrace, MeterDescriptionMemberDeclarationList members, SyllConstraintOr pattern, TreevialSymbol rbrace) {
        this.meterKeyword = meterKeyword;
        this.meterId = meterId;
        this.lbrace = lbrace;
        this.members = members;
        this.pattern = pattern;
        this.rbrace = rbrace;
    }

    public TreevialSymbol getMeterId() {
        return meterId;
    }

    public MeterDescriptionMemberDeclarationList getMembers() {
        return members;
    }

    public SyllConstraintOr getPattern() {
        return pattern;
    }

    public int getColumn() {
		return this.meterKeyword.getColumn();
	}

	public int getLeft() {
		return this.meterKeyword.getLeft();
	}

	public int getLine() {
		return this.meterKeyword.getLine();
	}

	public int getRight() {
		return this.rbrace.getRight();
	}

	@Override
	public void visit(Visitor visitor, boolean visitChildren) {
		if (visitChildren) {
			this.members.visit(visitor, visitChildren);
			this.pattern.visit(visitor, visitChildren);
        }
		visitor.execute(this);
	}


}
