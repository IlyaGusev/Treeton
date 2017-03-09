/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context;

public class ContextException extends Exception {
    public ContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextException(String message) {
        super(message);
    }
}
