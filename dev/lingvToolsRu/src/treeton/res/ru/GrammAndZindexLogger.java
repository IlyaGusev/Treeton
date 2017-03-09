/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.config.context.ContextException;

public interface GrammAndZindexLogger {
    void logZindexesGrammsAndErrors() throws ContextException;

    boolean isLogging();
}
