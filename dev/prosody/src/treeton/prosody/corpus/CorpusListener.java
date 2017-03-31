/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import java.util.Collection;

public interface CorpusListener {
    void entryCreated( CorpusEntry entry );

    void entryDeleted(CorpusEntry entry, Collection<CorpusFolder> parentFolders );

    void entryNameChanged(CorpusEntry entry);

    void entryTextChanged(CorpusEntry entry);

    void entryMetadataManuallyEdited(CorpusEntry entry);

    void entryMetadataReloaded(CorpusEntry entry);

    void folderCreated(CorpusFolder folder);

    void folderNameChanged(CorpusFolder folder);

    void folderParentChanged(CorpusFolder folder, CorpusFolder oldParent);

    void entryWasPlacedIntoFolder(CorpusEntry entry, CorpusFolder folder);

    void entryWasRemovedFromFolder(CorpusEntry entry, CorpusFolder folder);

    void folderDeleted(CorpusFolder folder);

    void corpusLabelChanged();

    void globalCorpusPropertyChanged( String propertyName );
}
