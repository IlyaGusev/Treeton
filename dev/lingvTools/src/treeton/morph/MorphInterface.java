/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.morph;

import treeton.dict.Dictionary;

import java.util.Collection;
import java.util.Properties;

public interface MorphInterface {
    Collection<Properties> processOneWord(String word, Dictionary dictArray) throws MorphException;

    void reset();

    void deInit();
}