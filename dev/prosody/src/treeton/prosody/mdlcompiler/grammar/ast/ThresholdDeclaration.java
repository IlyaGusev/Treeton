/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import treeton.prosody.mdlcompiler.grammar.TreevialSymbol;
import treeton.prosody.mdlcompiler.grammar.ast.BaseNode;

public class ThresholdDeclaration extends BaseNode {
    private TreevialSymbol thresholdKeyword;
    private TreevialSymbol value;
    private TreevialSymbol semicolon;

    public ThresholdDeclaration(TreevialSymbol thresholdKeyword, TreevialSymbol value, TreevialSymbol semicolon) {
        this.thresholdKeyword = thresholdKeyword;
        this.value = value;
        this.semicolon = semicolon;
    }

    public TreevialSymbol getThresholdKeyword() {
        return thresholdKeyword;
    }

    public TreevialSymbol getValue() {
        return value;
    }

    public Integer getIntegerValue() {
        return ((Long) value.getSymbolValue()).intValue();
    }

    @Override
    public int getColumn() {
        return thresholdKeyword.getColumn();
    }

    @Override
    public int getLeft() {
        return thresholdKeyword.getLeft();
    }

    @Override
    public int getLine() {
        return thresholdKeyword.getLine();
    }

    @Override
    public int getRight() {
        return semicolon.getRight();
    }
}