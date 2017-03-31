/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import treeton.prosody.mdlcompiler.grammar.TreevialSymbol;

public class SyllConstraint extends BaseNode {
	private TreevialSymbol lParen, rParen, kleene;

	private SyllConstraintOr constraint;

	private SyllBasicPatternElement basicPatternElement;

	private SyllConstraint() {
	}

	public SyllConstraint(TreevialSymbol lParen, SyllConstraintOr constraint,
			TreevialSymbol rParen) {
		this.lParen = lParen;
		this.constraint = constraint;
		this.rParen = rParen;
	}

	public SyllConstraint(TreevialSymbol lParen, SyllConstraintOr constraint,
			TreevialSymbol rParen, TreevialSymbol kleene) {
		this.lParen = lParen;
		this.constraint = constraint;
		this.rParen = rParen;
		this.kleene = kleene;
	}

	public SyllConstraint(SyllBasicPatternElement basicPatternElement) {
		this.basicPatternElement = basicPatternElement;
	}

	public SyllConstraintOr getConstraint() {
		return this.constraint;
	}

	public TreevialSymbol get() {
		return kleene;
	}

	public SyllBasicPatternElement getBasicPatternElement() {
		return basicPatternElement;
	}

	// leftmost can be (order matters):
	// not, lParen, basicPatternElement, reference
	public int getColumn() {
		if (this.lParen != null)
			return this.lParen.getColumn();
		if (this.basicPatternElement != null)
			return this.basicPatternElement.getColumn();
		throw new RuntimeException("Internal error!");
	}

	public int getLeft() {
		if (this.lParen != null)
			return this.lParen.getLeft();
		if (this.basicPatternElement != null)
			return this.basicPatternElement.getLeft();
		throw new RuntimeException("Internal error!");
	}

	public int getLine() {
		if (this.lParen != null)
			return this.lParen.getLine();
		if (this.basicPatternElement != null)
			return this.basicPatternElement.getLine();
		throw new RuntimeException("Internal error!");
	}

	// rightmost can be (order matters):
	// 1) kleene | MatchFilter | SimpleBinding
	// 2) rParen | basicPatternElement | constraint | reference
	public int getRight() {
		if (this.kleene != null)
			return this.kleene.getRight();
		if (this.rParen != null)
			return this.rParen.getRight();
		if (this.constraint != null)
			return this.constraint.getRight();
		if (this.basicPatternElement != null)
			return this.basicPatternElement.getRight();
		throw new RuntimeException("Internal error!");
	}

	@Override
	public void visit(Visitor visitor, boolean visitChildren) {
		if (visitChildren) {
			if (this.constraint != null)
				this.constraint.visit(visitor, visitChildren);
			if (this.basicPatternElement != null)
				this.basicPatternElement.visit(visitor, visitChildren);
		}
		visitor.execute(this);
	}

    public TreevialSymbol getKleene() {
        return kleene;
    }
}
