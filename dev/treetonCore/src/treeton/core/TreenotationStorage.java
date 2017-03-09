/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import org.w3c.dom.Document;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TrnRelationTypeStorage;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeSet;
import treeton.core.model.TrnTypeStorage;

import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;

//todo: Перейти на даблы (вместо Fraction)

public interface TreenotationStorage extends TextMarkingStorage {
    public String getUri();

    public void setURI(String uri) throws IllegalAccessException;

    public TrnTypeStorage getTypes();

    public void setTypes(TrnTypeStorage types) throws IllegalAccessException;

    public TrnRelationTypeStorage getRelations();

    public void setRelations(TrnRelationTypeStorage relations) throws IllegalAccessException;


    Token getTokenByNumber(int n);

    int getTokenNumber(Token token);

    Token getTokenByStartOffset(Fraction f);

    Token getTokenByStartOffset(int n, int d);

    Token getTokenByOffset(Fraction f, boolean includeLeft);

    Token getTokenByOffset(int n, int d, boolean includeLeft);

    void add(Treenotation trn);

    void addPostFactum(Treenotation trn);

    void removePostFactum(Treenotation trn);

    void forgetPostFactum(Treenotation trn);

    void applyPostFactumTrns();

    void forget(Treenotation _trn);

    void remove(Treenotation trn);

    void removeAll();

    void forgetAll();

    Token addToken(int n, int d, TrnType type, BlackBoard board, String text);

    void buildIndex();

    int nTokens();

    Token firstToken();

    Token lastToken();

    boolean isEmpty();

    TypeIteratorInterface typeIterator(TrnType type);

    TypeIteratorInterface typeIterator(TrnType type, Fraction f);

    TypeIteratorInterface typeIterator(TrnType type, Fraction from, Fraction to);

    TypeIteratorInterface typeIterator(TrnType type, Token from, Token to);

    TypeIteratorInterface typeIterator(TrnType[] type);

    TypeIteratorInterface typeIterator(TrnType[] type, Token from, Token to);

    TypeIteratorInterface typeIterator(TrnType[] type, Fraction f);

    TypeIteratorInterface typeIterator(TrnType[] type, Fraction from, Fraction to);

    TypeIteratorInterface typeIterator(TrnType[] commonTypes, TrnType[] tokenTypes);

    TypeIteratorInterface typeIterator(TrnType[] commonTypes, TrnType[] tokenTypes, Fraction f);

    TypeIteratorInterface typeIterator(TrnType[] commonTypes, TrnType[] tokenTypes, Fraction from, Fraction to);

    TypeIteratorInterface typeIterator(TrnType[] commonTypes, TrnType[] tokenTypes, Token from, Token to);

    FollowIteratorInterface followIterator(TrnTypeSet input, TrnTypeSet followTypes, Token after);

    TypeIteratorInterface sortedTypeIterator(TrnType[] type, Token from, Token to);

    TypeIteratorInterface sortedTypeIterator(TrnType[] commonTypes, TrnType[] tokenTypes, Token from, Token to);

    /**
     * Итератор по токенам, упорядоченным слева направо (множество токенов, принадлежащих неоторому TreenotaitonStorage,
     * является линейно упорядоченным множеством).
     *
     * @return итератор по токенам
     */
    Iterator<Token> tokenIterator();

    /**
     * То же, что {@link treeton.core.TreenotationStorage#tokenIterator()}, но выдача начинается с указанного токена.
     *
     * @param first с какого токена начинать
     * @return итератор по токенам, начиная с указанного
     */
    Iterator<Token> tokenIterator(Token first);

    /**
     * То же, что {@link treeton.core.TreenotationStorage#tokenIterator()}, но токены переносятся в указанный массив,
     * если в нем достаточно места (в противном случае создается и возвращается новый массив). Внимание: указанный
     * массив не обнуляется; если его размер больше, чем количество токенов, лишние элементы не изменятся!
     *
     * @param a куда переносить токены
     * @return a, если в нем достаточно места, иначе - новый массив походящего размера
     */
    Token[] tokens2Array(Token a[]);

    /**
     * Создается, заполняется и возвращается массив токенов, принадлежащих данному TreenotationStorage.
     *
     * @return массив токенов, принадлежащих данному TreenotationStorage
     */
    Token[] tokens2Array();

    void minimize(TrnType tp);

    UncoveredAreasIterator uncoveredAreasIterator(TrnType tp, Token from, Token to);

    public TrnIterator internalTrnsIterator(Token from, Token to, Treenotation trn);

    public RelationsIterator internalRelationsIterator(Treenotation trn, Token from, Token to);

    public void setFeature(String name, Object value);

    public Object getFeature(String name);

    public void importXML(InputStream is) throws Exception;

    public void importXML(Document doc) throws Exception;

    public Document exportXML() throws ParserConfigurationException;

    public Document exportXML(boolean exportTokens, HashSet<String> alreadyExportedURI, Iterator<Treenotation> it) throws ParserConfigurationException;

    public Iterator<? extends Treenotation> allTrnsIterator();

    Token splitToken(Token _tok, int offsNumerator, int offsDenominator, String textLeft, String textRight);

    Treenotation getByUri(String uri);

    void changeType(Treenotation trn, TrnType type);
}
