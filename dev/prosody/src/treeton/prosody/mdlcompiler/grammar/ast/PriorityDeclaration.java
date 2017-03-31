/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import treeton.prosody.mdlcompiler.grammar.TreevialSymbol;
import treeton.prosody.mdlcompiler.grammar.ast.BaseNode;

public class PriorityDeclaration extends BaseNode {
    private TreevialSymbol priorityKeyword;
    private TreevialSymbol value;
    private TreevialSymbol semicolon;

    public PriorityDeclaration(TreevialSymbol priorityKeyword, TreevialSymbol value, TreevialSymbol semicolon) {
        this.priorityKeyword = priorityKeyword;
        this.value = value;
        this.semicolon = semicolon;
    }

    public TreevialSymbol getPriorityKeyword() {
        return priorityKeyword;
    }

    public TreevialSymbol getValue() {
        return value;
    }

    public Integer getIntegerValue() {
        return ((Long) value.getSymbolValue()).intValue();
    }

    @Override
    public int getColumn() {
        return priorityKeyword.getColumn();
    }

    @Override
    public int getLeft() {
        return priorityKeyword.getLeft();
    }

    @Override
    public int getLine() {
        return priorityKeyword.getLine();
    }

    @Override
    public int getRight() {
        return semicolon.getRight();
    }
}