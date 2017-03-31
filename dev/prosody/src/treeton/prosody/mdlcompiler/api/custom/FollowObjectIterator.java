/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.api.custom;

public interface FollowObjectIterator<T> extends ResetableObjectIterator<T> {
    void setEndOffsetToFollow(Integer offset);
    void setTermIndexer(TermIndexer<T> termIndexer);
    Integer getEndOffsetToFollow();
}
