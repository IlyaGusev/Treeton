/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

import java.util.List;

public interface InputObjectSet<T> {
    FollowObjectIterator<T> getFollowIterator(List<Term> input, InputObjectInfoProvider<T> inputObjectInfoProvider);
    ResetableObjectIterator<T> getForwardSearchIterator(List<Term> input, TermIndexer<T> termIndexer, InputObjectInfoProvider<T> inputObjectInfoProvider);
    int length();
    Long mostLeftPosition();
}
