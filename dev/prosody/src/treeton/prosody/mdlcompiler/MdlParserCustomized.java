/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler;

import java_cup.runtime.Scanner;
import java_cup.runtime.Symbol;
import treeton.prosody.mdlcompiler.grammar.TreevialSymbol;

import java.io.File;
import java.io.IOException;

public class MdlParserCustomized extends MdlParser {
    public MdlParserCustomized() {
    }
    public MdlParserCustomized(Scanner s) {
        super(s);
    }
    public MdlParserCustomized(File grammarFile, MdlCompiler mdlCompiler, String encoding) throws IOException {
        super(grammarFile, mdlCompiler, encoding);
    }

    public void report_fatal_error(String message, Object info) {
      report_error(message, info);
      CompileMessage compileMessage = createCompileMessage(message, info);
      getMdlCompiler().addUnparsedSourceFilesError(compileMessage);
      throw new RuntimeException(compileMessage.toString());
    }

    private CompileMessage createCompileMessage(String message, Object info) {
        if(info instanceof Symbol)return createCompileMessage(message,(Symbol)info);
        if(info instanceof TreevialSymbol)return createCompileMessage(message,(TreevialSymbol)info);
        return new CompileMessage(getGrammarFile().toURI(),0,0,0,0,message);
    }
    private CompileMessage createCompileMessage(String message, Symbol s) {
        return createCompileMessage(message,s.value);
    }
    private CompileMessage createCompileMessage(String message, TreevialSymbol s) {
        return new CompileMessage(getGrammarFile().toURI(),s.getLine(),s.getColumn(),s.getLeft(),s.getRight(),message);
    }

}
