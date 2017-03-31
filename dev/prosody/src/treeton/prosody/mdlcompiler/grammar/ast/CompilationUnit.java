/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import java.io.File;

import treeton.prosody.mdlcompiler.MdlCompiler;

public class CompilationUnit extends BaseNode implements GrammarInfoProvider {
	private MeterDescriptionList meterDescriptionList;

	private File grammarFile;

	private String grammarBody;

	private MdlCompiler mdlCompiler;

	public CompilationUnit(
			MeterDescriptionList meterDescriptionList, File grammarFile,
			String grammarBody, MdlCompiler mdlCompiler) {
		this.meterDescriptionList = meterDescriptionList;
		this.meterDescriptionList.parent = this;
		this.grammarFile = grammarFile;
		this.grammarBody = grammarBody;
		this.mdlCompiler = mdlCompiler;
	}

    public MeterDescriptionList getMeterDescriptionList() {
        return meterDescriptionList;
    }

    public File getGrammarFile() {
		return this.grammarFile;
	}

	public MdlCompiler getMdlCompiler() {
		return this.mdlCompiler;
	}

	public int getColumn() {
		return 0;
	}

	public int getLeft() {
		return 0;
	}

	public int getLine() {
		return 0;
	}

	public int getRight() {
		return this.meterDescriptionList.getRight();
	}

	@Override
	public void visit(Visitor visitor, boolean visitChildren) {
		this.meterDescriptionList.visit(visitor, visitChildren);
		visitor.execute(this);
	}

	@Override
	public String toString() {
		return (getNodeClassAndOffsets() + "{") +
				MdlCompiler.nl + "    " + this.meterDescriptionList +
				MdlCompiler.nl + "}";
	}

	public String getGrammarBody() {
		return grammarBody;
	}
}
