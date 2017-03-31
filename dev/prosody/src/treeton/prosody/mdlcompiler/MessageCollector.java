/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler;

import treeton.prosody.mdlcompiler.grammar.ast.BaseNode;
import treeton.prosody.mdlcompiler.grammar.ast.Visitor;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class MessageCollector implements Visitor {
	public static final String ERROR_PREFIX = "Error";

	public static final String WARNING_PREFIX = "Warning";

	private List<CompileMessage> errors;

	private List<CompileMessage> warnings;

	public MessageCollector() {
		this.errors = new ArrayList<CompileMessage>();
		this.warnings = new ArrayList<CompileMessage>();
	}

	public void execute(BaseNode node) {
		List<String> errs = node.getErrors();
		if (errs != null) {
			for (String s : errs) {
				this.errors.add(new CompileMessage(node.getGrammarInfoProvider()
						.getGrammarFile().toURI(), node.getLine(), node
						.getColumn(), node.getLeft(), node.getRight(), s));
			}
		}
		List<String> warns = node.getWarnings();
		if (warns != null) {
			for (String s : warns) {
				this.warnings.add(new CompileMessage(node.getGrammarInfoProvider()
						.getGrammarFile().toURI(), node.getLine(), node
						.getColumn(), node.getLeft(), node.getRight(), s));
			}
		}
	}

	public void addError(CompileMessage m) {
		errors.add(m);
	}

	public void addWarning(CompileMessage m) {
		warnings.add(m);
	}

	public List<CompileMessage> getErrors() {
		return this.errors;
	}

	public List<CompileMessage> getWarnings() {
		return this.warnings;
	}

	public void printErrors(PrintStream ps) {
		for (CompileMessage m : errors) {
			ps.println(ERROR_PREFIX + ": " + m.toString());
		}
	}

	public void printWarnings(PrintStream ps) {
		for (CompileMessage m : warnings) {
			ps.println(WARNING_PREFIX + ": " + m.toString());
		}
	}
}
