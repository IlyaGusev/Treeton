/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.collector;

public class CollectorException extends Exception {
    public CollectorException(Throwable cause) {
        super(cause);
    }

    public CollectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public CollectorException(String message) {
        super(message);
    }
}
