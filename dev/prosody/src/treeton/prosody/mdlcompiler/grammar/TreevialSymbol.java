/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar;

public class TreevialSymbol implements PositionAware {
	/** Offset in grammar file where this symbol starts (inclusive) */
	private int left;

	/** Offset in grammar file where this symbol ends (inclusive) */
	private int right;

	/** A line in grammar file where this symbol starts */
	private int line;

	/** A column in grammar file where this symbol starts */
	private int column;

	/** A value that corresponds to this treevial symbol */
	private Object value;

	/** Symbol type. Defined by Lexer */
	private int type;

	public TreevialSymbol(int type, int line, int column, int left, int right,
			Object value) {
		this.type = type;
		this.left = left;
		this.right = right;
		this.line = line;
		this.column = column;
		this.value = value;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	public int getLeft() {
		return left;
	}

	public int getRight() {
		return right;
	}

	public int getSymbolType() {
		return type;
	}

	public Object getSymbolValue() {
		return value;
	}

	public String toString() {
		return line + ":" + column + "symbolcode=" + type;
	}
}
