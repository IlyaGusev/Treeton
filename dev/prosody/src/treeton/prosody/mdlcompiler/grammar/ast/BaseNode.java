/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import treeton.prosody.mdlcompiler.grammar.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseNode implements Constants, PositionAware {
	/** This node parent */
	public BaseNode parent;

	private List<String> errors;

	private List<String> warnings;

	protected BaseNode() {
	}

    public long getTimestamp() {
        return getGrammarInfoProvider().getGrammarFile().lastModified();
    }

	// TODO: do we need it? gonna be removed.
	/**
	 * @deprecated
	 */
	public StringBuffer getNodeClassAndOffsets() {
		StringBuffer sb = new StringBuffer(this.getClass().getName());
		sb.append("(" + getLine() + ":" + getColumn() + "   " + getLeft() + "-"
				+ getRight() + ")");
		return sb;
	}

	public StringBuffer dumpParseTree(String indent) {
		StringBuffer sb = new StringBuffer(getNodeClassAndOffsets());
		sb.append(this.getClass().getName());
		sb.append(".toSrting() is not overriden.");
		return sb;
	}

	public BaseNode getParent() {
		return parent;
	}

	public BaseNode getParent(Class parentType) {
		if (parent == null || parentType == null)
			return null;
		return parentType.isAssignableFrom(parent.getClass()) ? parent : parent
				.getParent(parentType);
	}

	public BaseNode getParent(Class[] parentTypes) {
		if (parent == null)
			return null;
		for (Class parentType : parentTypes) {
			if (parent.getClass() == parentType)
				return parent;
		}
		return parent.getParent(parentTypes);
	}

    public GrammarInfoProvider getGrammarInfoProvider() {
        return (GrammarInfoProvider) getParent(GrammarInfoProvider.class);
    }

	public CompilationUnit getCompilationUnit() {
		return (CompilationUnit) getParent(CompilationUnit.class);
	}

	// /**
	// * @deprecated operations with child nodes gonna go through the visitors
	// * mechanism
	// */
	// public Iterator<BaseNode> getChildIterator() {
	// return new Iterator<BaseNode>() {
	// public boolean hasNext() {
	// return false;
	// }
	//
	// public BaseNode next() {
	// return null;
	// }
	//
	// public void remove() {
	// }
	// };
	// }

	public List<String> getErrors() {
		return this.errors;
	}

	public List<String> getWarnings() {
		return this.warnings;
	}

	public boolean addError(String errorMessage) {
		if (this.errors == null)
			this.errors = new ArrayList<String>();
		return this.errors.add(errorMessage);
	}

	public boolean addWarning(String warningMessage) {
		if (this.warnings == null)
			this.warnings = new ArrayList<String>();
		return this.warnings.add(warningMessage);
	}

	public int getColumn() {
		return -1;
	}

	public int getLeft() {
		return -1;
	}

	public int getLine() {
		return -1;
	}

	public int getRight() {
		return -1;
	}

	public void visit(Visitor visitor, boolean visitChildren) {
		visitor.execute(this);
	}
}
