/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.lomonosov;

public class MarkupHandlerException extends Exception {
    public MarkupHandlerException() {
    }

    public MarkupHandlerException(String message) {
        super(message);
    }

    public MarkupHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public MarkupHandlerException(Throwable cause) {
        super(cause);
    }
}
