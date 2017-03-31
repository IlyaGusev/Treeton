/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler;

import java.net.URI;

public class CompileMessage {
	private URI grammarUri;

	private int line, column, left, right;

	private String message;

	private String signature;

	public CompileMessage(URI grammarUri, int line, int column, int start,
			int end, String message) {
		this.grammarUri = grammarUri;
		this.line = line;
		this.column = column;
		this.left = start;
		this.right = end;
		this.message = message;
	}
	public CompileMessage(URI grammarUri, int line, int column, int start,
			int end, String message,String signature) {
		this.grammarUri = grammarUri;
		this.line = line;
		this.column = column;
		this.left = start;
		this.right = end;
		this.message = message;
		this.signature = signature;
	}

	public int getColumn() {
		return column;
	}

	public int getRight() {
		return right;
	}

	public URI getGrammarUri() {
		return grammarUri;
	}

	public int getLine() {
		return line;
	}

	public String getMessage() {
		return message;
	}

	public int getLeft() {
		return left;
	}

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
	public String toString() {
		return grammarUri.toString() + " (" + line + "," + column + ") "
				+ message;
	}
}
