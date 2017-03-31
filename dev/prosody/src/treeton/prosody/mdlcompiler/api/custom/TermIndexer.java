/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

import java.util.Collection;
import java.util.Set;

public interface TermIndexer<T> {
    void prepare(Set<Term> terms);
    Set<Integer> getClasses(Term t);

    int getClassId(T o, InputObjectInfoProvider<T> inputObjectInfoProvider);

    Term construct(int classId);
    String toString(Set<Integer> classes);

    int getNClasses();

    Set<Term> getIndexedTerms(); 
}
