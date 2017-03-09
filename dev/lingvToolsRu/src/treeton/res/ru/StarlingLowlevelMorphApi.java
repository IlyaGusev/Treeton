/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.Token;
import treeton.core.Treenotation;
import treeton.core.scape.ParseException;
import treeton.morph.MorphInterface;

import java.io.IOException;
import java.util.ArrayList;

public interface StarlingLowlevelMorphApi extends MorphInterface {
    void processArray(ArrayList<Treenotation> buffer, String[] strings, String word, Token start, Token end);

    ArrayList<String[]> processOneWord(String word) throws IOException, ParseException;
}
