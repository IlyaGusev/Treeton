/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar;

public interface PositionAware {
	int getLeft();

	int getRight();

	int getLine();

	int getColumn();
}
