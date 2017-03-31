/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

public interface Visitor {
	public void execute(BaseNode node);
}
