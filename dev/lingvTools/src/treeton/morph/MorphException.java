/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.morph;

public class MorphException extends Exception {
    public MorphException() {
    }

    public MorphException(String message) {
        super(message);
    }

    public MorphException(String message, Throwable cause) {
        super(message, cause);
    }

    public MorphException(Throwable cause) {
        super(cause);
    }
}
