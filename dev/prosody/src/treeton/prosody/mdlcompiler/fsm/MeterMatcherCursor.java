/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

public interface MeterMatcherCursor {
    int isStressed(); // 0 - unstressed, 1 - stressed, -1 - ambigious
    MeterMatcherCursor shift( boolean stressed );
    boolean isPhoneticWordEnded();
    boolean isEndOfInput();
}
