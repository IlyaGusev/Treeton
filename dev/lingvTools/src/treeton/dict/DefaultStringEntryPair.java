/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.dict;

public class DefaultStringEntryPair implements StringEntryPair {
    private String string;
    private InputEntry entry;

    public DefaultStringEntryPair(String string, InputEntry entry) {
        this.string = string;
        this.entry = entry;
    }

    public String getString() {
        return string;
    }

    public InputEntry getEntry() {
        return entry;
    }
}
