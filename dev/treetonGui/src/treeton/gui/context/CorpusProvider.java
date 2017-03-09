/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import java.util.Collection;

public interface CorpusProvider {
    public Collection<CorpusElement> getCorpusContent();

    public int contentSize();
}
