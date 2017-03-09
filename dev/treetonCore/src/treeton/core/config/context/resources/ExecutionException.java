/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

public class ExecutionException extends Exception {
    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
