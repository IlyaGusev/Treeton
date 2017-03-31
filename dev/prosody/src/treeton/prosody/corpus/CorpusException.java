/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

public class CorpusException extends Exception {
    public CorpusException(String message) {
        super(message);
    }

    public CorpusException(String message, Throwable cause) {
        super(message, cause);
    }
}
