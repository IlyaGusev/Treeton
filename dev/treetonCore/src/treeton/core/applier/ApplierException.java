/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.applier;

public class ApplierException extends Exception {
    public ApplierException() {
        super();
    }

    public ApplierException(String message) {
        super(message);
    }

    public ApplierException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplierException(Throwable cause) {
        super(cause);
    }
}
