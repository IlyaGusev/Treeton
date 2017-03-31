/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import treeton.prosody.mdlcompiler.grammar.TreevialSymbol;

public class SyllBasicPatternElement extends BaseNode {
	private TreevialSymbol syllablePattern;

	private SyllBasicPatternElement() {
	}

	public SyllBasicPatternElement(TreevialSymbol syllablePattern) {
		this.syllablePattern = syllablePattern;
	}

    public TreevialSymbol getSyllablePattern() {
        return syllablePattern;
    }

    public int getColumn() {
		return this.syllablePattern.getColumn();
	}

	public int getLeft() {
		return this.syllablePattern.getLeft();
	}

	public int getLine() {
		return this.syllablePattern.getLine();
	}

	public int getRight() {
		return this.syllablePattern.getRight();
	}
}
