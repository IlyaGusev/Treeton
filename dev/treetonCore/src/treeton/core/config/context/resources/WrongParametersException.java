/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

import treeton.core.config.context.ContextException;

public class WrongParametersException extends ContextException {
    public WrongParametersException(String message) {
        super(message);
    }
}
