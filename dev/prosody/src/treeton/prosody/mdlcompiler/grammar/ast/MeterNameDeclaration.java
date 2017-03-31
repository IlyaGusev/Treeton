/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import treeton.prosody.mdlcompiler.grammar.TreevialSymbol;
import treeton.prosody.mdlcompiler.grammar.ast.BaseNode;

public class MeterNameDeclaration extends BaseNode {
    private TreevialSymbol nameKeyword;
    private TreevialSymbol value;
    private TreevialSymbol semicolon;

    public MeterNameDeclaration(TreevialSymbol nameKeyword, TreevialSymbol value, TreevialSymbol semicolon) {
        this.nameKeyword = nameKeyword;
        this.value = value;
        this.semicolon = semicolon;
    }

    public TreevialSymbol getNameKeyword() {
        return nameKeyword;
    }

    public TreevialSymbol getValue() {
        return value;
    }

    public String getStringValue() {
        return (String) value.getSymbolValue();
    }

    @Override
    public int getColumn() {
        return nameKeyword.getColumn();
    }

    @Override
    public int getLeft() {
        return nameKeyword.getLeft();
    }

    @Override
    public int getLine() {
        return nameKeyword.getLine();
    }

    @Override
    public int getRight() {
        return semicolon.getRight();
    }
}