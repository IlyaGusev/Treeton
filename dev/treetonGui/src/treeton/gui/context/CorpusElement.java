/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.context;

import treeton.core.config.context.resources.TextMarkingStorage;

public class CorpusElement {
    String docName;
    String text;
    String result;
    TextMarkingStorage storage;

    public CorpusElement(String docName, String text, TextMarkingStorage storage) {
        this.docName = docName;
        this.text = text;
        this.storage = storage;
    }

    public String getDocName() {
        return docName;
    }

    public String getText() {
        return text;
    }

    public TextMarkingStorage getStorage() {
        return storage;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
