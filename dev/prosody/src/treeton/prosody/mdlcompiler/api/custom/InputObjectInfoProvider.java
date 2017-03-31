/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

public interface InputObjectInfoProvider<T> {
    int getObjectClassId(T o, TermIndexer<T> termIndexer);

    String getString(T start, T end);
    String getString(T source);

    String getType(T source);
    int getId(T source);

    int getStart(T source);
    int getEnd(T source);
}
