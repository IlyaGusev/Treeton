/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;

import java.util.HashMap;

public interface Treenotation extends IntFeatureMap {
    TrnType getType();

    boolean insideOf(Treenotation trn);

    boolean intersects(Treenotation trn);

    String toString();

    int getStartNumerator();

    int getStartDenominator();

    int getEndNumerator();

    int getEndDenominator();

    Token getStartToken();

    Token getEndToken();

    boolean isChildOf(Treenotation trn);

    boolean corrupted();

    double toDouble();

    double endToDouble();

    int toInt();

    int endToInt();

    Object get(String featureName);

    void put(String featureName, Object value);

    void put(int feature, Object value);

    public void put(BlackBoard board);

    String getString();

    String getHtmlString();

    public String getText();

    public Object clone();

    public void appendTrnStringView(StringBuffer buf);

    public int readInFromStringView(TrnTypeStorage types, char[] view, int pl);

    int getTokenLength();

    public boolean isLocked();

    public boolean isEmpty();

    public Object getContext();

    public boolean contains(Treenotation trn);

    public Treenotation getCopy(HashMap<Treenotation, Treenotation> old2new, boolean cloneLocked);

    boolean isAdded();

    long getId();

    TreenotationStorage getStorage();

    String getUri();
}
