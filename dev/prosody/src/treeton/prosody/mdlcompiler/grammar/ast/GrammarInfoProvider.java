/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import java.io.File;

public interface GrammarInfoProvider {
    File getGrammarFile();
    String getGrammarBody();
}
