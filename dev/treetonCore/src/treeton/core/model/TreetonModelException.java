/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.model;

public class TreetonModelException extends Exception {
    public TreetonModelException() {
        super();
    }

    public TreetonModelException(String message) {
        super(message);
    }

    public TreetonModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public TreetonModelException(Throwable cause) {
        super(cause);
    }
}
