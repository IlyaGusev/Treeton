/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import org.w3c.dom.*;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.*;
import treeton.core.util.BlockStack;
import treeton.core.util.MutableInteger;
import treeton.core.util.RBTreeMap;
import treeton.core.util.SmartIDGenerator;
import treeton.core.util.xml.XMLParser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

// todo 5. Написать комментарии
// todo 6. Переделать итератор по всему множеству

public class TreenotationStorageImpl implements TreenotationStorage {
    public static Token textStart = new TokenImpl(0, 0, 0, 0, null, null);
    public static Token textEnd = new TokenImpl(0, 0, 0, 0, null, null);
    TrnTypeStorage types;
    TrnRelationTypeStorage relations;
    ArrayList<Treenotation> postFactumTrnsAdd = new ArrayList<Treenotation>();
    ArrayList<Treenotation> postFactumTrnsRemove = new ArrayList<Treenotation>();
    ArrayList<Treenotation> postFactumTrnsForget = new ArrayList<Treenotation>();
    boolean indexReady = false;
    private long ID = 0;
    private String uri;
    private HashMap<String, TreenotationImpl> uri2trn = new HashMap<String, TreenotationImpl>();
    private TokenImpl first;
    private TokenImpl last;
    private int size;
    private RBTreeMap tokens; //{Token} -> {Token}
    private int channels;
    private BlockStack freeStacks;
    private BlockStack markStack;
    private TokenImpl fakeToken;
    private SmartIDGenerator idGen;
    private Map<String, Object> features = new HashMap<String, Object>();

    public TreenotationStorageImpl(TreenotationsContext context) {
        init();
        try {
            this.types = context.getTypes();
            this.relations = context.getRelations();
        } catch (TreetonModelException e) {
            throw new RuntimeException("Problems with TreetonModel", e);
        }
    }

    public TreenotationStorageImpl(TrnTypeStorage types, TrnRelationTypeStorage relations) {
        init();
        this.types = types;
        this.relations = relations;
    }

    public TreenotationStorageImpl() {
        init();
    }

    BlockStack getStack() {
        synchronized (this) {
            if (freeStacks.isEmpty()) {
                return new BlockStack(100);
            } else {
                return (BlockStack) freeStacks.pop();
            }
        }
    }

    void freeStack(BlockStack stack) {
        synchronized (this) {
            stack.clean();
            freeStacks.push(stack);
        }
    }

    public TrnTypeStorage getTypes() {
        return types;
    }

    public void setTypes(TrnTypeStorage types) throws IllegalAccessException {
        if (uri2trn.size() > 0) {
            throw new IllegalAccessException("Unable to change TrnTypeStorage. TreenotationStorage is not empty.");
        }
        this.types = types;
    }

    public TrnRelationTypeStorage getRelations() {
        return relations;
    }

    public void setRelations(TrnRelationTypeStorage relations) throws IllegalAccessException {
        if (uri2trn.size() > 0) {
            throw new IllegalAccessException("Unable to change TrnTypeStorage. TreenotationStorage is not empty.");
        }
        this.relations = relations;
    }

    private void init() {
        first = null;
        last = null;
        tokens = new RBTreeMap();
        channels = 0;
        size = 0;
        freeStacks = new BlockStack(20);
        markStack = new BlockStack(100);
        fakeToken = new TokenImpl(0, 1, 0, 1, null, null);
        idGen = new SmartIDGenerator(0);
    }

    public Token getTokenByNumber(int n) {
        int i = 0;
        Token cur = first;
        while (cur != last) {
            if (i == n)
                return cur;
            i++;
            cur = cur.getNextToken();
        }
        if (i == n)
            return cur;
        return null;
    }

    public int getTokenNumber(Token token) {
        int i = 0;
        Token cur = first;
        while (cur != last) {
            if (cur == token)
                return i;
            i++;
            cur = cur.getNextToken();
        }
        if (cur == token)
            return i;
        return -1;
    }

    public Token getTokenByStartOffset(Fraction f) {
        if (f == null)
            return null;
        return getTokenByStartOffset(f.numerator, f.denominator);
    }

    public Token getTokenByStartOffset(int n, int d) {
        if (first == null) {
            return null;
        }
        fakeToken.setStartNumerator(n);
        fakeToken.setStartDenominator(d);
        fakeToken.setEndNumerator(n);
        fakeToken.setEndDenominator(d);
        if (!indexReady) {
            Iterator i = tokenIterator();
            while (i.hasNext()) {
                Token cur = (Token) i.next();
                int c = fakeToken.compareTo(cur);
                if (c == 0) {
                    return cur;
                } else if (c < 0) {
                    return null;
                }
            }
            return null;
        }
        return (Token) tokens.get(fakeToken);
    }

    public Token getTokenByOffset(Fraction f, boolean includeLeft) {
        if (f == null)
            return null;
        return getTokenByOffset(f.numerator, f.denominator, includeLeft);
    }

    public Token getTokenByOffset(int n, int d, boolean includeLeft) {
        if (first == null) {
            return null;
        }
        fakeToken.setStartNumerator(n);
        fakeToken.setStartDenominator(d);
        fakeToken.setEndNumerator(n);
        fakeToken.setEndDenominator(d);
        if (!indexReady) {
            Iterator i = tokenIterator();
            Token prev = null;
            while (i.hasNext()) {
                Token cur = (Token) i.next();
                int c = fakeToken.compareTo(cur);
                if (c == 0) {
                    return cur;
                } else if (c < 0) {
                    return prev;
                }
                prev = cur;
            }
            if (prev.getEndNumerator() * d <= prev.getEndDenominator() * n)
                return null;
            return prev;
        }
        Token val = includeLeft ? (Token) tokens.getPreviousOrEqualOf(fakeToken) : (Token) tokens.getPreviousOf(fakeToken);
        if (val == last) {
            int c = val.getEndNumerator() * d - val.getEndDenominator() * n;
            if (includeLeft ? c <= 0 : c < 0) {
                return null;
            }
        }
        return val;
    }

    public UncoveredAreasIterator uncoveredAreasIterator(TrnType tp, Token from, Token to) {
        return new UncoveredAreasIteratorImpl(tp, from, to);
    }

    private void climbUp(short bar, TreenotationImpl cur, TreenotationImpl[] nexts, int i, TreenotationImpl newTrn) {
        TreenotationImpl next = nexts[i];

        if (next == null)
            return;

        if (newTrn.insideOf(next)) {
            if (!next.isMarked(bar)) {
                newTrn.addParent(next);
                next.mark(bar);
                markStack.push(next);
            }
            if (!cur.isMarked(bar)) {
                nexts[i] = newTrn;
                cur.mark(bar);
                markStack.push(cur);
            } else {
                nexts[i] = null;
            }
        } else if (next.insideOf(newTrn)) {
            if (!next.isMarked(bar)) {
                TreenotationImpl[] parent = next.getParent();
                if (parent == null) {
                    next.addParent(newTrn);
                    next.mark(bar);
                    markStack.push(next);
                } else {
                    for (int j = 0; j < parent.length; j++) {
                        climbUp(bar, next, parent, j, newTrn);
                    }
                    if (!next.isMarked(bar)) {
                        next.addParent(newTrn);
                        next.mark(bar);
                        markStack.push(next);
                    }
                }
            }
            if (!cur.isMarked(bar)) {
                cur.mark(bar);
                markStack.push(cur);
            }
        }
    }

    public void add(Treenotation _trn) {
        TreenotationImpl trn = (TreenotationImpl) _trn;
        if (trn.getStorage() != this) {
            throw new IllegalArgumentException("Only treenotations from this storage can be added");
        }
        if (trn.isMarked((short) 1))
            return;
        _add(trn);
    }

    void incNView(TreenotationImpl trn, int n) {
        trn.nView += n;
        if (trn.id == -1 && idGen != null) {
            trn.id = idGen.getNextID();
        }

        if (trn.isEmpty()) {
            return;
        }

        for (TreenotationImpl.Node tree : trn.trees) {
            if (tree == null)
                continue;
            incNView(tree, n);
        }
    }

    private void incNView(TreenotationImpl.Node nd, int n) {
        incNView(nd.trn, n);

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        incNView(child, n);
                    }
                }
            }
        }
    }

    void _add(TreenotationImpl trn) {
        trn.mark((short) 1);
        incNView(trn, 1);
        TrnType tp = trn.getType();
        short bar = getChannel();

        TokenImpl cur = (TokenImpl) trn.getStartToken();
        if (cur == null)
            throw new IllegalArgumentException();
        TokenImpl end = (TokenImpl) trn.getEndToken();
        if (end == null)
            throw new IllegalArgumentException();
        while (true) {
            TreenotationImpl[] upper = cur.getParent(tp);

            if (upper == null) {
                cur.addParent(trn);
            } else {
                for (int i = 0; i < upper.length; i++) {
                    climbUp(bar, cur, upper, i, trn);
                }
                if (!cur.isMarked(bar)) {
                    cur.addParent(trn);
                    cur.mark(bar);
                    markStack.push(cur);
                }
            }

            if (cur == end) {
                break;
            } else {
                cur = (TokenImpl) cur.getNextToken();
            }
        }

        while (!markStack.isEmpty()) {
            TreenotationImpl t = (TreenotationImpl) markStack.pop();
            t.unmark(bar);
        }

        freeChannel(bar);
    }

    public void addPostFactum(Treenotation trn) {
        postFactumTrnsAdd.add(trn);
    }

    public void removePostFactum(Treenotation trn) {
        postFactumTrnsRemove.add(trn);
    }

    public void forgetPostFactum(Treenotation trn) {
        postFactumTrnsForget.add(trn);
    }

    public void applyPostFactumTrns() {
        for (Treenotation postFactumTrn : postFactumTrnsAdd) {
            add(postFactumTrn);
        }
        postFactumTrnsAdd.clear();
        for (Treenotation postFactumTrn : postFactumTrnsRemove) {
            remove(postFactumTrn);
        }
        postFactumTrnsRemove.clear();
        for (Treenotation postFactumTrn : postFactumTrnsForget) {
            forget(postFactumTrn);
        }
        postFactumTrnsForget.clear();
    }

    private void findOtherWays(short bar1, short bar2, TreenotationImpl[] nexts, int i, Token max, Token min) {
        TreenotationImpl next = nexts[i];

        if (next == null)
            return;

        if (next.isMarked(bar2))
            return;

        if (next.isMarked(bar1)) {
            next.mark(bar2);
            markStack.push(next);
        } else {
            if (next.getStartToken().compareTo(min) < 0) {
                return;
            }
            if (next.getEndToken().compareTo(max) > 0) {
                return;
            }
            TreenotationImpl[] parent = next.getParent();
            for (int j = 0; j < parent.length; j++) {
                findOtherWays(bar1, bar2, parent, j, max, min);
            }
            next.mark(bar2);
            markStack.push(next);
        }
    }

    private boolean findUpChild(short bar1, short bar2, TreenotationImpl cur, TreenotationImpl[] nexts, int i, TreenotationImpl trn, Token max, Token min) {
        TreenotationImpl next = nexts[i];

        if (next == null)
            return false;

        if (next == trn) {
            nexts[i] = null;
            TreenotationImpl[] parent = next.getParent();
            if (parent != null) {
                long pos = markStack.getPosition();
                for (int j = 0; j < nexts.length; j++) {
                    if (j != i)
                        findOtherWays(bar1, bar2, nexts, j, max, min);
                }
                for (TreenotationImpl p : parent) {
                    if (p != null) {
                        if (!p.isMarked(bar2))
                            cur.addParent(p);
                    }
                }
                while (markStack.getPosition() != pos) {
                    TreenotationImpl t = (TreenotationImpl) markStack.pop();
                    t.unmark(bar2);
                }
            }

            return true;
        } else {
            if (next.insideOf(trn)) {
                if (!next.isMarked(bar2)) {
                    TreenotationImpl[] parent = next.getParent();
                    for (int j = 0; j < parent.length; j++) {
                        findUpChild(bar1, bar2, next, parent, j, trn, max, min);
                    }
                    if (!next.isMarked(bar2)) {
                        next.mark(bar2);
                        markStack.push(next);
                    }
                }
            }
        }
        return false;
    }

    public void forget(Treenotation _trn) {
        if (_trn instanceof Token) {
            throw new IllegalArgumentException("Unable to forget Token. Use removeAll");
        }
        if (_trn.getContext() != null) {
            throw new IllegalArgumentException("Unable to forget Treenotation. It belongs to some context. Use TopologyManager.");
        }
        if (!_trn.isEmpty()) {
            throw new IllegalArgumentException("Unable to forget non-empty Treenotation.");
        }
        if (uri2trn.get(_trn.getUri()) == _trn) {
            if (_trn.isAdded())
                remove(_trn);
            uri2trn.remove(_trn.getUri());
            ((TreenotationImpl) _trn).setUri(null);
            ((TreenotationImpl) _trn).startToken = null;
            ((TreenotationImpl) _trn).endToken = null;
            if (_trn instanceof TreenotationSyntax) {
                ((TreenotationSyntax) _trn).storage = null;
            }
        }
    }

    public void remove(Treenotation _trn) {
        TreenotationImpl trn = (TreenotationImpl) _trn;

        if (trn == null)
            throw new IllegalArgumentException("");

        if (!trn.isMarked((short) 1))
            return;
        _remove(trn);
    }

    void decNView(TreenotationImpl trn, int n) {
        trn.nView -= n;
        if (trn.nView <= 0 && trn.id != -1 && idGen != null) {
            idGen.freeID(trn.id);
            trn.id = -1;
        }

        if (trn.isEmpty()) {
            return;
        }

        for (TreenotationImpl.Node tree : trn.trees) {
            if (tree == null)
                continue;
            decNView(tree, n);
        }
    }

    private void decNView(TreenotationImpl.Node nd, int n) {
        decNView(nd.trn, n);

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        decNView(child, n);
                    }
                }
            }
        }
    }

    void _remove(TreenotationImpl trn) {
        TrnType tp = trn.getType();
        short bar1 = getChannel();
        short bar2 = getChannel();


        try {
            if (tp.isTokenType()) {
                throw new IllegalArgumentException("Unable to remove TreenotationImpl of token type (use removeAll() method)");
            }
        } catch (TreetonModelException e) {
            throw new IllegalArgumentException("Corrupted type");
        }

        TreenotationImpl[] pars = trn.getParent();

        Token max = trn.getEndToken(), min = trn.getStartToken();

        if (pars != null) {
            for (TreenotationImpl p : pars) {
                if (p != null) {
                    p.mark(bar1);
                    Token s = p.getStartToken();
                    if (s.compareTo(min) < 0)
                        min = s;
                    Token e = p.getEndToken();
                    if (e.compareTo(max) > 0)
                        max = e;
                }
            }
        }

        TokenImpl cur = (TokenImpl) trn.getStartToken();
        while (true) {
            TreenotationImpl[] upper = cur.getParent(tp);

            for (int i = 0; i < upper.length; i++) {
                if (findUpChild(bar1, bar2, cur, upper, i, trn, max, min))
                    break;
            }

            if (cur == trn.getEndToken()) {
                break;
            } else {
                cur = (TokenImpl) cur.getNextToken();
            }
        }

        while (!markStack.isEmpty()) {
            TreenotationImpl t = (TreenotationImpl) markStack.pop();
            t.unmark(bar2);
        }

        if (pars != null) {
            for (TreenotationImpl p : pars) {
                if (p != null) {
                    p.unmark(bar1);
                }
            }
        }
        freeChannel(bar1);
        freeChannel(bar2);

        trn.unmark((short) 1);
        TreenotationImpl[] arr = trn.getParent();
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                arr[i] = null;
            }
        }
        decNView(trn, 1);
    }

    private void removeUp(short bar, TreenotationImpl[] nexts, int i) {
        TreenotationImpl next = nexts[i];

        if (next == null)
            return;

        if (!next.isMarked(bar)) {
            TreenotationImpl[] parent = next.getParent();
            if (parent != null) {
                for (int j = 0; j < parent.length; j++) {
                    removeUp(bar, parent, j);
                }
            }
            next.unmark((short) 1);
            decNView(next, 1);
            next.mark(bar);
            markStack.push(next);
        }
        nexts[i] = null;
    }

    public void removeAll() {
        TokenImpl t;
        if (last == null)
            return;
        short bar = getChannel();
        TokenImpl cur = last;
        while (true) {
            Iterator it = cur.parentsIterator();
            while (it.hasNext()) {
                TreenotationImpl[] upper = (TreenotationImpl[]) it.next();

                for (int i = 0; i < upper.length; i++) {
                    removeUp(bar, upper, i);
                }
            }

            if (cur == first) {
                cur.unmark((short) 1);
                cur.storage = null;
                decNView(cur, 1);
                break;
            } else {
                t = (TokenImpl) cur.getPreviousToken();
                cur.setPreviousToken(null);
                cur.storage = null;
                cur.unmark((short) 1);
                decNView(cur, 1);
                t.setNextToken(null);
                cur = t;
            }
        }
        last = first = null;
        size = 0;
        tokens.clear();
        indexReady = false;

        while (!markStack.isEmpty()) {
            TreenotationImpl trn = (TreenotationImpl) markStack.pop();
            trn.unmark(bar);
        }
        freeChannel(bar);
        idGen.reset();
    }

    public void forgetAll() {
        removeAll();
        for (TreenotationImpl trn : uri2trn.values()) {
            trn.setUri(null);
            ((TreenotationImpl) trn).startToken = null;
            ((TreenotationImpl) trn).endToken = null;
            if (trn instanceof TreenotationSyntax) {
                ((TreenotationSyntax) trn).storage = null;
            }
        }
        uri2trn.clear();
        postFactumTrnsAdd.clear();
        postFactumTrnsRemove.clear();
        postFactumTrnsForget.clear();
        features.clear();
        channels = 0;
    }

    public Token addToken(int n, int d, TrnType type, BlackBoard board, String text) {
        /*if (type.getStorage() != types) {
         throw new IllegalArgumentException("The specified TrnType belongs to another domain");
       } */
        return _addToken(n, d, type, board, text, null);
    }

    public Token _addToken(int n, int d, TrnType type, BlackBoard board, String text, String uri) {
        TokenImpl t;
        try {
            if (!type.isTokenType()) {
                throw new IllegalArgumentException("Unable to create the token of non-token type");
            }
        } catch (TreetonModelException e) {
            throw new IllegalArgumentException("Corrupted type");
        }

        if (first == null) {
            t = new TokenImpl(0, 1, n, d, type, board);
            t.storage = this;
            if (uri == null) {
                obtainTrn(t);
            } else {
                t.setUri(uri);
            }
            t.mark((short) 1);
            incNView(t, 1);
            first = t;
            last = t;
            size++;
        } else {
            t = new TokenImpl(last, n, d, type, board);
            t.storage = this;
            if (uri == null) {
                obtainTrn(t);
            } else {
                t.setUri(uri);
            }
            t.mark((short) 1);
            incNView(t, 1);
            last.setNextToken(t);
            t.setPreviousToken(last);
            last = t;
            size++;
        }
        if (indexReady) {
            tokens.put(t, t);
        }
        t.setText(text);
        return t;
    }

    synchronized void obtainTrn(TreenotationImpl t) {
        t.setUri(generateURI());
        uri2trn.put(t.getUri(), t);
    }

    private String generateURI() {
        return Long.toString(ID++);
    }

    public short getChannel() {
        int c;
        c = 2;
        while (c != (1 << 16)) {
            if ((c & channels) == 0) {
                channels |= c;
                return (short) c;
            }
            c <<= 1;
        }
        throw new RuntimeException("no free channels in TreenotationImpl Storage");
    }

    public void freeChannel(short ch) {
        channels = channels & (~ch);
    }

    public void buildIndex() {
        tokens.putAll(this);
        indexReady = true;
    }

    public int nTokens() {
        return size;
    }

    public Token firstToken() {
        return first;
    }

    public Token lastToken() {
        return last;
    }

    public boolean isEmpty() {
        return uri2trn.size() == 0;
    }

    public TypeIteratorInterface typeIterator(TrnType type) {
        return typeIterator(type, (Token) null, (Token) null);
    }

    public TypeIteratorInterface typeIterator(TrnType type, Fraction f) {
        return typeIterator(new TrnType[]{type}, f, f);
    }

    public TypeIteratorInterface typeIterator(TrnType type, Fraction from, Fraction to) {
        return typeIterator(new TrnType[]{type}, from, to);
    }

    public TypeIteratorInterface typeIterator(TrnType type, Token from, Token to) {
        return typeIterator(new TrnType[]{type}, from, to);
    }

    public TypeIteratorInterface typeIterator(TrnType[] type) {
        return typeIterator(type, (Token) null, (Token) null);
    }

    public TypeIteratorInterface typeIterator(TrnType[] type, Token from, Token to) {
        TrnType[] commonTypes = new TrnType[0];
        TrnType[] tokenTypes = new TrnType[0];
        try {
            int cn = 0, tn = 0;
            for (TrnType aType : type) {
                if (aType.isTokenType()) {
                    tn++;
                } else {
                    cn++;
                }
            }
            commonTypes = new TrnType[cn];
            tokenTypes = new TrnType[tn];
            cn = 0;
            tn = 0;
            for (TrnType cur : type) {
                if (cur.isTokenType()) {
                    tokenTypes[tn++] = cur;
                } else {
                    commonTypes[cn++] = cur;
                }
            }
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error with model", e);
        }
        return new TypeIterator(commonTypes, tokenTypes, (TokenImpl) from, (TokenImpl) to);
    }

    public TypeIteratorInterface typeIterator(TrnType[] type, Fraction f) {
        return typeIterator(type, f, f);
    }

    public TypeIteratorInterface typeIterator(TrnType[] type, Fraction from, Fraction to) {
        TrnType[] commonTypes = new TrnType[0];
        TrnType[] tokenTypes = new TrnType[0];
        try {
            int cn = 0, tn = 0;
            for (TrnType aType : type) {
                if (aType.isTokenType()) {
                    tn++;
                } else {
                    cn++;
                }
            }
            commonTypes = new TrnType[cn];
            tokenTypes = new TrnType[tn];
            cn = 0;
            tn = 0;
            for (TrnType cur : type) {
                if (cur.isTokenType()) {
                    tokenTypes[tn++] = cur;
                } else {
                    commonTypes[cn++] = cur;
                }
            }
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error with model", e);
        }
        return typeIterator(commonTypes, tokenTypes, from, to);
    }

    public TypeIteratorInterface typeIterator(TrnType[] commonTypes, TrnType[] tokenTypes) {
        return new TypeIterator(commonTypes, tokenTypes, (TokenImpl) null, (TokenImpl) null);
    }

    public TypeIteratorInterface typeIterator(TrnType[] commonTypes, TrnType[] tokenTypes, Fraction f) {
        return typeIterator(commonTypes, tokenTypes, f, f);
    }

    public TypeIteratorInterface typeIterator(TrnType[] commonTypes, TrnType[] tokenTypes, Fraction from, Fraction to) {
        TokenImpl f, t;
        if (from == null) {
            if (to == null) {
                return new TypeIterator(commonTypes, tokenTypes, null, null);
            } else {
                t = (TokenImpl) getTokenByOffset(to, false);
                if (t == null) {
                    if (to.numerator * first.getStartDenominator() <= first.getStartNumerator() * to.denominator) {
                        return new TypeIterator(null, null, null, null);
                    } else {
                        return new TypeIterator(commonTypes, tokenTypes, null, null);
                    }
                } else {
                    return new TypeIterator(commonTypes, tokenTypes, null, t);
                }
            }
        } else if (to == null) {
            f = (TokenImpl) getTokenByOffset(from, true);
            if (f == null) {
                if (from.numerator * last.getEndDenominator() >= last.getEndNumerator() * from.denominator) {
                    return new TypeIterator(null, null, null, null);
                } else {
                    return new TypeIterator(commonTypes, tokenTypes, null, null);
                }
            } else {
                return new TypeIterator(commonTypes, tokenTypes, f, null);
            }
        }

        int c;
        if ((c = from.compareTo(to)) > 0)
            return new TypeIterator(null, null, null, null);

        f = (TokenImpl) getTokenByOffset(from, true);
        t = (TokenImpl) getTokenByOffset(to, false);

        if (f == null && from.numerator * last.getEndDenominator() >= last.getEndNumerator() * from.denominator) {
            return new TypeIterator(null, null, null, null);
        }
        if (t == null && to.numerator * first.getStartDenominator() <= first.getStartNumerator() * to.denominator) {
            return new TypeIterator(null, null, null, null);
        }

        if (c == 0)
            return new TypeIterator(commonTypes, tokenTypes, f, f);

        return new TypeIterator(commonTypes, tokenTypes, f, t);
    }

    public TypeIteratorInterface typeIterator(TrnType[] commonTypes, TrnType[] tokenTypes, Token from, Token to) {
        return new TypeIterator(commonTypes, tokenTypes, (TokenImpl) from, (TokenImpl) to);
    }

    public FollowIteratorInterface followIterator(TrnTypeSet input, TrnTypeSet followTypes, Token after) {
        return new FollowIterator(input, followTypes, (TokenImpl) after);
    }

    public TypeIteratorInterface sortedTypeIterator(TrnType[] type, Token from, Token to) {
        TrnType[] commonTypes;
        TrnType[] tokenTypes;
        try {
            int cn = 0, tn = 0;
            for (TrnType aType : type) {
                if (aType.isTokenType()) {
                    tn++;
                } else {
                    cn++;
                }
            }
            commonTypes = new TrnType[cn];
            tokenTypes = new TrnType[tn];
            cn = 0;
            tn = 0;
            for (TrnType cur : type) {
                if (cur.isTokenType()) {
                    tokenTypes[tn++] = cur;
                } else {
                    commonTypes[cn++] = cur;
                }
            }
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error with model", e);
        }
        return new SortedTypeIterator(commonTypes, tokenTypes, (TokenImpl) from, (TokenImpl) to);
    }

    public TypeIteratorInterface sortedTypeIterator(TrnType[] commonTypes, TrnType[] tokenTypes, Token from, Token to) {
        return new SortedTypeIterator(commonTypes, tokenTypes, (TokenImpl) from, (TokenImpl) to);
    }

    public Iterator<Token> tokenIterator() {
        return new TokenIterator(first);
    }

    public Iterator<Token> tokenIterator(Token first) {
        return new TokenIterator(first);
    }

    public Token[] tokens2Array(Token a[]) {
        if (a.length > size)
            a = new Token[size];
        Iterator<Token> itr = tokenIterator();
        int i = 0;
        while (itr.hasNext()) {
            a[i] = itr.next();
            i++;
        }
        return a;
    }

    public Token[] tokens2Array() {
        Token[] a;
        a = new Token[size];
        Iterator<Token> itr = tokenIterator();
        int i = 0;
        while (itr.hasNext()) {
            a[i] = itr.next();
            i++;
        }
        return a;
    }

    public void minimize(TrnType tp) {
        try {
            if (tp.isTokenType())
                return;
        } catch (TreetonModelException e) {
            return;
        }

        TokenImpl curToken = first;
        BlockStack stack = getStack();
        BlockStack minimzStack = getStack();
        BlockStack smallStack = getStack();
        short bar = getChannel();

        while (curToken != null) {
            TreenotationImpl[] pars = curToken.getParent(tp);
            if (pars != null) {
                for (TreenotationImpl par : pars) {
                    if (par != null)
                        stack.push(par);
                }
            }
            while (!stack.isEmpty()) {
                TreenotationImpl trn = (TreenotationImpl) stack.pop();
                if (trn.isMarked(bar))
                    continue;
                trn.mark(bar);
                int n;
                TreenotationImpl parent;
                TreenotationImpl curTrn = trn;

                while (true) {
                    n = 0;
                    pars = curTrn.getParent();
                    parent = null;
                    if (pars != null) {
                        for (int i = 0; i < pars.length && n <= 1; i++) {
                            if (pars[i] != null) {
                                parent = pars[i];
                                n++;
                            }
                        }
                    }
                    if (parent == null) {
                        smallStack.clean();
                        minimzStack.push(trn);
                        break;
                    } else if (n == 1) {
                        if (curTrn.getStartToken().compareTo(parent.getStartToken()) != 0 ||
                                parent.getEndToken().compareTo(curTrn.getEndToken()) != 0) {
                            while (!smallStack.isEmpty()) {
                                TreenotationImpl del = (TreenotationImpl) smallStack.pop();
                                del.unmark((short) 1);
                                decNView(del, 1);
                            }
                            curTrn.unmark((short) 1);
                            decNView(curTrn, 1);
                            stack.push(parent);
                            break;
                        } else {
                            smallStack.push(curTrn);
                            curTrn = parent;
                        }
                    } else {
                        while (!smallStack.isEmpty()) {
                            TreenotationImpl del = (TreenotationImpl) smallStack.pop();
                            del.unmark((short) 1);
                            decNView(del, 1);
                        }
                        curTrn.unmark((short) 1);
                        decNView(curTrn, 1);
                        for (TreenotationImpl par : pars) {
                            if (par != null) {
                                stack.push(par);
                            }
                        }
                        break;
                    }
                }
            }
            curToken = (TokenImpl) curToken.getNextToken();
        }

        while (!minimzStack.isEmpty()) {
            TreenotationImpl trn = (TreenotationImpl) minimzStack.pop();
            trn.unmark(bar);

            TokenImpl cur = (TokenImpl) trn.getStartToken();
            TokenImpl end = (TokenImpl) trn.getEndToken();

            while (true) {
                TreenotationImpl[] pars = cur.getParent(tp);

                if (!cur.isMarked(bar)) {
                    if (pars != null) {
                        for (int i = 0; i < pars.length; i++) {
                            pars[i] = null;
                        }
                    }
                    cur.mark(bar);
                    stack.push(cur);
                }

                cur.addParent(trn);

                if (cur == end) {
                    break;
                } else {
                    cur = (TokenImpl) cur.getNextToken();
                }
            }
        }

        while (!stack.isEmpty()) {
            TokenImpl tok = (TokenImpl) stack.pop();
            tok.unmark(bar);
        }

        freeChannel(bar);
        freeStack(smallStack);
        freeStack(minimzStack);
        freeStack(stack);
    }

    public TrnIterator internalTrnsIterator(Token from, Token to, Treenotation trn) {
        return new InternalTrnsIterator((TreenotationImpl) trn, (TokenImpl) from, (TokenImpl) to);
    }

    public RelationsIterator internalRelationsIterator(Treenotation trn, Token from, Token to) {
        return new InternalRelationsIterator((TreenotationImpl) trn, (TokenImpl) from, (TokenImpl) to);
    }

    public String getUri() {
        return uri;
    }

    public void setURI(String uri) throws IllegalAccessException {
        if (uri2trn.size() > 0) {
            throw new IllegalAccessException("Unable to change URI. TreenotationStorage is not empty.");
        }
        this.uri = uri;
    }

    public void importXML(InputStream is) throws Exception {
        Document xml;
        xml = XMLParser.parse(is, new File(BasicConfiguration.getResource("/schema/TrnStorageSchema.xsd").toString()));
        importXML(xml);
    }

    public void importXML(Document doc) {
        HashSet<TreenotationImpl> hasParent = new HashSet<TreenotationImpl>();
        Element e = doc.getDocumentElement();

        String focusIntervalStart = e.getAttribute("focusIntervalStart");

        if (focusIntervalStart != null && focusIntervalStart.length() > 0) {
            setFeature("focusIntervalStart", Integer.valueOf(focusIntervalStart));
        }


        String tdURI = e.getAttribute("storageURI");
        if (uri != null && !tdURI.equals(uri)) {
            throw new RuntimeException("Wrong storage URI " + tdURI + ". Was expecting " + uri);
        }

        uri = tdURI;

        Node xmlnd = e.getFirstChild();
        ElementInfo info = new ElementInfo();
        while (xmlnd != null) {
            if (xmlnd instanceof Element) {
                Element cur = (Element) xmlnd;
                if ("tok".equals(cur.getTagName())) {
                    info.extract(cur);
                    if (uri2trn.containsKey(info.uri)) {
                        throw new RuntimeException("Duplicate uri " + info.uri);
                    }
                    TreenotationImpl trn = (TreenotationImpl) _addToken(info.lengthN, info.lengthD, info.type, info.board, info.text, info.uri);
                    verifyID(info.uri);
                    uri2trn.put(info.uri, trn);
                } else if (cur.getTagName().equals("trn")) {
                    info.extract(cur);
                    if (uri2trn.containsKey(info.uri)) {
                        throw new RuntimeException("Duplicate uri " + info.uri);
                    }
                    TokenImpl st = null;
                    if (info.start != null) {
                        Object o = uri2trn.get(info.start);
                        if (o == null) {
                            throw new RuntimeException("No such uri " + info.start);
                        }
                        if (!(o instanceof TokenImpl)) {
                            throw new RuntimeException("Corresponding Treenotation is not a Token: uri " + info.start);
                        }
                        st = (TokenImpl) o;
                    }
                    TokenImpl et = null;
                    if (info.end != null) {
                        Object o = uri2trn.get(info.end);
                        if (o == null) {
                            throw new RuntimeException("No such uri " + info.end);
                        }
                        if (!(o instanceof TokenImpl)) {
                            throw new RuntimeException("Corresponding Treenotation is not a Token: uri " + info.end);
                        }
                        et = (TokenImpl) o;
                    }
                    TreenotationImpl trn = new TreenotationSyntax(this, st, et, info.type, info.board, info.uri);
                    hasParent.clear();
                    hasParent.add(trn);
                    Node curNd = cur.getFirstChild();
                    while (curNd != null) {
                        if (curNd instanceof Element) {
                            Element elem = (Element) curNd;
                            if ("nd".equals(elem.getTagName())) {
                                parseNd(elem, trn, null, hasParent, (TokenImpl) trn.getStartToken(), (TokenImpl) trn.getEndToken());
                            }
                        }
                        curNd = curNd.getNextSibling();
                    }
                    if (!info.hidden && trn.startToken != null)
                        add(trn);
                    if (!info.opened)
                        trn.lock();
                    verifyID(info.uri);
                    uri2trn.put(info.uri, trn);
                }
            }
            xmlnd = xmlnd.getNextSibling();
        }
        hasParent.clear();
    }

    public Document exportXML() throws ParserConfigurationException {
        short bar = getChannel();
        markStack.clean();

        Document doc = XMLParser.createDocument("http://starling.rinet.ru/treeton", "Document");
        doc.getDocumentElement().setAttribute("storageURI", getUri());
        doc.getDocumentElement().setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        doc.getDocumentElement().setAttribute("xsi:schemaLocation", "http://starling.rinet.ru/treeton http://starling.rinet.ru/treeton/TrnStorageSchema.xsd");

        ExportEnv env = new ExportEnv();

        _exportXML(doc, bar, tokenIterator(), env);
        _exportXML(doc, bar, uri2trn.values().iterator(), env);

        while (!markStack.isEmpty()) {
            TreenotationImpl t = (TreenotationImpl) markStack.pop();
            t.unmark(bar);
        }

        freeChannel(bar);
        return doc;
    }

    public Document exportXML(boolean exportTokens, HashSet<String> alreadyExportedURI, Iterator<Treenotation> it) throws ParserConfigurationException {
        short bar = getChannel();
        markStack.clean();

        Document doc = XMLParser.createDocument("http://starling.rinet.ru/treeton", "Document");
        doc.getDocumentElement().setAttribute("storageURI", getUri());
        doc.getDocumentElement().setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        doc.getDocumentElement().setAttribute("xsi:schemaLocation", "http://starling.rinet.ru/treeton http://starling.rinet.ru/treeton/TrnStorageSchema.xsd");

        if (alreadyExportedURI != null) {
            for (String uri : alreadyExportedURI) {
                TreenotationImpl trn = uri2trn.get(uri);
                if (trn != null) {
                    trn.mark(bar);
                    markStack.push(trn);
                }
            }
        }

        ExportEnv env = new ExportEnv();
        if (exportTokens) {
            _exportXML(doc, bar, tokenIterator(), env);
        }
        _exportXML(doc, bar, it, env);

        while (!markStack.isEmpty()) {
            TreenotationImpl t = (TreenotationImpl) markStack.pop();
            t.unmark(bar);
        }

        freeChannel(bar);
        return doc;
    }

    public Iterator<? extends Treenotation> allTrnsIterator() {
        return uri2trn.values().iterator();
    }

    public Token splitToken(Token _tok, int offsNumerator, int offsDenominator, String textLeft, String textRight) {
        if (
                (_tok.getStartNumerator() * offsDenominator > _tok.getStartDenominator() * offsNumerator) ||
                        (_tok.getEndDenominator() * offsNumerator > _tok.getEndNumerator() * offsDenominator)
                ) {
            throw new IllegalArgumentException("Wrong split offset");
        }
        TokenImpl tok = (TokenImpl) _tok;

        int n = tok.endNumerator;
        int d = tok.endDenominator;

        tok.endNumerator = offsNumerator;
        tok.endDenominator = offsDenominator;
        tok.simplifyFractions();
        tok.setText(textLeft);

        TokenImpl t = new TokenImpl(tok.endNumerator, tok.endDenominator, n, d, tok.getType(), null);
        t.storage = this;
        obtainTrn(t);
        t.mark((short) 1);
        incNView(t, 1);

        t.next = tok.next;
        t.previous = tok;
        t.parents = new IntFeatureMapImpl();
        for (int i = 0; i < tok.parents.size(); i++) {
            TreenotationImpl[] arr = (TreenotationImpl[]) tok.parents.getByIndex(i);
            if (arr != null) {
                TreenotationImpl[] narr = new TreenotationImpl[arr.length];
                System.arraycopy(arr, 0, narr, 0, arr.length);
                t.parents.put(tok.parents.getKey(i), narr);
            }
        }

        tok.next = t;

        size++;

        if (t.parents.size() > 0) {
            for (int i = 0; i <= t.parents.mapper.getMaxValue(); i++) {
                fixEndTokensAfterSplit((TreenotationImpl[]) t.parents.data[i], tok, t);
            }
        }

        if (last == tok) {
            last = t;
        }

        if (indexReady) {
            tokens.put(t, t);
        }
        t.setText(textRight);
        return t;
    }

    public Treenotation getByUri(String uri) {
        return uri2trn.get(uri);
    }

    public void changeType(Treenotation trn, TrnType type) {
        boolean isAdded = trn.isAdded();

        if (isAdded)
            remove(trn);

        ((TreenotationImpl) trn).setType(type);

        if (isAdded)
            add(trn);
    }

    private void fixEndTokensAfterSplit(TreenotationImpl[] arr, TokenImpl oldToken, TokenImpl newToken) {
        if (arr != null) {
            for (int j = 0; j < arr.length; j++) {
                TreenotationImpl treenotation = arr[j];
                if (treenotation != null && treenotation.getEndToken() == oldToken) {
                    treenotation.endToken = newToken;
                    fixEndTokensAfterSplit(treenotation.getParent(), oldToken, newToken);
                }
            }
        }
    }

    private void _exportXML(Document doc, short bar, Iterator<? extends Treenotation> it, ExportEnv env) {
        StringBuffer buf = new StringBuffer();
        while (it.hasNext()) {
            TreenotationImpl trn = (TreenotationImpl) it.next();
            if (trn.isMarked(bar)) {
                continue;
            }
            exportTrn(trn, env, doc, bar, buf);
        }
    }

    private void exportTrn(TreenotationImpl trn, ExportEnv env, Document doc, short bar, StringBuffer buf) {
        Element elem;
        if (trn instanceof TokenImpl) {
            TokenImpl tok = (TokenImpl) trn;
            if (env.lastExportedToken != null && env.lastExportedToken.getNextToken() != trn) {
                throw new RuntimeException("Tokens must be exported consequently");
            }
            elem = createTokElement(tok, doc, bar, buf, env);
            env.lastExportedToken = tok;
        } else {
            elem = createTrnElement(trn, doc, bar, buf, env);
        }
        doc.getDocumentElement().appendChild(elem);
        trn.mark(bar);
        markStack.push(trn);
    }

    private Element createTrnElement(TreenotationImpl trn, Document doc, short bar, StringBuffer buf, ExportEnv env) {
        Element elem = doc.createElement("trn");

        try {
            elem.setAttribute("type", trn.getType().getName());
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error!!!");
        }
        elem.setAttribute("uri", trn.getUri());
        if (!trn.isAdded()) {
            elem.setAttribute("hidden", "true");
        }
        if (!trn.isLocked()) {
            elem.setAttribute("opened", "true");
        }
        TokenImpl st = (TokenImpl) trn.getStartToken();
        if (st != null) {
            if (!st.isMarked(bar)) {
                throw new RuntimeException("Token " + st.getUri() + " must be exported before Treenotation " + trn.getUri());
            }
            elem.setAttribute("start", st.getUri());
        }
        TokenImpl et = (TokenImpl) trn.getEndToken();
        if (et != null) {
            if (!et.isMarked(bar)) {
                throw new RuntimeException("Token " + et.getUri() + " must be exported before Treenotation " + trn.getUri());
            }
            elem.setAttribute("end", et.getUri());
        }

        int sz = trn.size();
        if (sz > 0) {
            Element attrs = doc.createElement("attrs");
            for (int i = 0; i < sz; i++) {
                int f = trn.getKey(i);
                if (f != -1) {
                    Object o = trn.getByIndex(i);
                    if (o != null) {
                        buf.setLength(0);
                        if (o instanceof Object[]) {
                            Object[] arr = (Object[]) o;
                            buf.append("[");
                            for (int j = 0; j < arr.length; j++) {
                                try {
                                    if (trn.getType().getFeatureTypeByIndex(f).equals(Treenotation.class)) {
                                        TreenotationImpl reftrn = (TreenotationImpl) arr[j];
                                        if (!reftrn.isMarked(bar)) {
                                            exportTrn(reftrn, env, doc, bar, new StringBuffer());
                                        }

                                        buf.append(reftrn.getUri());
                                    } else {
                                        buf.append(arr[j].toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(";", "\\\\;"));
                                    }
                                } catch (TreetonModelException e) {
                                    throw new RuntimeException("Error!!!");
                                }
                                if (j < arr.length - 1) {
                                    buf.append(";");
                                }
                            }
                            buf.append("]");
                        } else {
                            try {
                                if (trn.getType().getFeatureTypeByIndex(f).equals(Treenotation.class)) {
                                    TreenotationImpl reftrn = (TreenotationImpl) o;
                                    if (!reftrn.isMarked(bar)) {
                                        exportTrn(reftrn, env, doc, bar, new StringBuffer());
                                    }

                                    buf.append(reftrn.getUri());
                                } else {
                                    buf.append(o);
                                }
                            } catch (TreetonModelException e) {
                                throw new RuntimeException("Error!!!");
                            }
                        }
                        try {
                            attrs.setAttribute(trn.getType().getFeatureNameByIndex(f), buf.toString());
                        } catch (TreetonModelException e) {
                            throw new RuntimeException("Error!!!");
                        }
                        buf.setLength(0);
                    }
                }
            }
            if (attrs.getAttributes().getLength() > 0) {
                elem.appendChild(attrs);
            }
        }

        if (trn.trees != null && trn.trees.length > 0) {
            for (TreenotationImpl.Node tree : trn.trees) {
                if (tree == null)
                    continue;
                Element nd = createNd(tree, doc, bar, buf, env);
                if (tree.parentConnection == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                    nd.setAttribute("pcon", "s");
                } else if (tree.parentConnection == TreenotationImpl.PARENT_CONNECTION_WEAK) {
                    //nd.setAttribute("pcon","w"); //default
                } else if (tree.parentConnection == TreenotationImpl.PARENT_CONNECTION_PATH) {
                    nd.setAttribute("pcon", "p");
                }
                elem.appendChild(nd);
            }
        }

        return elem;
    }

    private Element createTokElement(TokenImpl tok, Document doc, short bar, StringBuffer buf, ExportEnv env) {
        Element elem = doc.createElement("tok");

        try {
            elem.setAttribute("type", tok.getType().getName());
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error!!!", e);
        }
        elem.setAttribute("uri", tok.getUri());
        elem.setAttribute("length", env.df.format((double) tok.getEndNumerator() / (double) tok.getEndDenominator() - (double) tok.getStartNumerator() / (double) tok.getStartDenominator()));
        elem.setAttribute("text", tok.getText());

        int sz = tok.size();
        if (sz > 0) {
            Element attrs = doc.createElement("attrs");
            for (int i = 0; i < sz; i++) {
                int f = tok.getKey(i);
                if (f != -1) {
                    Object o = tok.getByIndex(i);
                    if (o != null) {
                        buf.setLength(0);
                        if (o instanceof Object[]) {
                            Object[] arr = (Object[]) o;
                            buf.append("[");
                            for (int j = 0; j < arr.length; j++) {
                                try {
                                    if (tok.getType().getFeatureTypeByIndex(f).equals(Treenotation.class)) {
                                        buf.append(((Treenotation) arr[j]).getUri());
                                    } else {
                                        buf.append(arr[j].toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(";", "\\\\;"));
                                    }
                                } catch (TreetonModelException e) {
                                    throw new RuntimeException("Error!!!", e);
                                }
                                if (j < arr.length - 1) {
                                    buf.append(";");
                                }
                            }
                            buf.append("]");
                        } else {
                            try {
                                if (tok.getType().getFeatureTypeByIndex(f).equals(Treenotation.class)) {
                                    buf.append(((Treenotation) o).getUri());
                                } else {
                                    buf.append(o);
                                }
                            } catch (TreetonModelException e) {
                                throw new RuntimeException("Error!!!", e);
                            }
                        }
                        try {
                            attrs.setAttribute(tok.getType().getFeatureNameByIndex(f), buf.toString());
                        } catch (TreetonModelException e) {
                            throw new RuntimeException("Error!!!", e);
                        }
                        buf.setLength(0);
                    }
                }
            }
            if (attrs.getAttributes().getLength() > 0) {
                elem.appendChild(attrs);
            }
        }
        return elem;
    }

    private Element createNd(TreenotationImpl.Node nd, Document doc, short bar, StringBuffer buf, ExportEnv env) {
        Element ndElem = doc.createElement("nd");
        if (!nd.trn.isMarked(bar)) {
            exportTrn(nd.trn, env, doc, bar, buf);
        }
        ndElem.setAttribute("ref", nd.trn.getUri());

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        Element childElem = createNd(child, doc, bar, buf, env);
                        try {
                            childElem.setAttribute("relType", relations.get(nd.relations.getKey(i)).getName());
                        } catch (TreetonModelException e) {
                            throw new RuntimeException("Error!!!", e);
                        }
                        ndElem.appendChild(childElem);
                    }
                }
            }
        }

        return ndElem;
    }

    private void verifyID(String uri) {
        try {
            long v = Long.valueOf(uri);
            if (v >= ID) {
                ID = v + 1;
            }
        } catch (Exception e) {
            //do nothing
        }
    }

    private boolean checkTopology(HashSet<TreenotationImpl> hasParent, TreenotationImpl trn) {
        if (hasParent.contains(trn))
            return false;
        if (trn.isEmpty()) {
            return true;
        }

        for (TreenotationImpl.Node tree : trn.trees) {
            if (tree == null)
                continue;
            if (!checkTopology(hasParent, tree))
                return false;
        }
        return true;
    }

    private boolean checkTopology(HashSet<TreenotationImpl> hasParent, TreenotationImpl.Node nd) {
        if (!checkTopology(hasParent, nd.trn)) {
            return false;
        }

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        if (!checkTopology(hasParent, child)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private void updateHasParent(HashSet<TreenotationImpl> hasParent, TreenotationImpl trn) {
        hasParent.add(trn);
        if (trn.isEmpty()) {
            return;
        }

        for (TreenotationImpl.Node tree : trn.trees) {
            if (tree == null)
                continue;
            updateHasParent(hasParent, tree);
        }
    }

    private void updateHasParent(HashSet<TreenotationImpl> hasParent, TreenotationImpl.Node nd) {
        updateHasParent(hasParent, nd.trn);

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        updateHasParent(hasParent, child);
                    }
                }
            }
        }
    }

    private void parseNd(Element curNd, TreenotationImpl context,
                         TreenotationImpl.Node parent,
                         HashSet<TreenotationImpl> hasParent,
                         TokenImpl leftBounding, TokenImpl rightBounding) {
        TreenotationImpl trn = uri2trn.get(curNd.getAttribute("ref"));
        if (trn == null) {
            throw new RuntimeException("Wrong uri " + curNd.getAttribute("ref") + ". Unable to find corresponding treenotation.");
        }
        if (trn.getStartToken() != null && (
                trn.getStartToken().compareTo(leftBounding) < 0 ||
                        trn.getEndToken().compareTo(rightBounding) > 0
        )) {
            throw new RuntimeException("Wrong boundings");
        }

        if (!checkTopology(hasParent, trn)) {
            throw new RuntimeException("Wrong treenotation structure");
        }


        int pc = -1;
        if (curNd.hasAttribute("pcon")) {
            String s = curNd.getAttribute("pcon");
            if (s.equals("s")) {
                pc = TreenotationImpl.PARENT_CONNECTION_STRONG;
            } else if (s.equals("w")) {
                pc = TreenotationImpl.PARENT_CONNECTION_WEAK;
            } else if (s.equals("p")) {
                pc = TreenotationImpl.PARENT_CONNECTION_PATH;
            }
        }

        TrnRelationType relType = null;
        if (curNd.hasAttribute("relType")) {
            try {
                relType = relations.get(curNd.getAttribute("relType"));
            } catch (TreetonModelException e) {
                relType = null;
            }
            if (relType == null) {
                throw new RuntimeException("Wrong type name: " + curNd.getAttribute("relType"));
            }
        }

        TreenotationImpl.Node nd = new TreenotationImpl.Node(pc == -1 ? TreenotationImpl.PARENT_CONNECTION_WEAK : pc, trn);

        if (parent == null) {
            context.addTree(nd);
        } else {
            parent.addRelation(trn, relType, nd);
        }

        updateHasParent(hasParent, trn);

        addContext(trn, context);


        Node xmlnd = curNd.getFirstChild();
        while (xmlnd != null) {
            if (xmlnd instanceof Element) {
                Element child = (Element) xmlnd;
                if ("nd".equals(child.getTagName())) {
                    parseNd(child, context, nd, hasParent, leftBounding, rightBounding);
                }
            }
            xmlnd = xmlnd.getNextSibling();
        }
    }

    private void addContext(TreenotationImpl trn, TreenotationImpl context) {
        Object o = trn.getContext();
        if (o == null) {
            if (!trn.isEmpty()) {
                for (TreenotationImpl.Node tree : trn.trees) {
                    if (tree == null)
                        continue;
                    _removeContext(tree, trn);
                }
            }
        }
        _addContext(trn, context);
    }

    private void _removeContext(TreenotationImpl trn, TreenotationImpl context) {
        Object o = trn.getContext();
        if (o == context) {
            trn.context = null;
        } else if (o instanceof MutableInteger) {
            MutableInteger i = (MutableInteger) o;
            i.value--;
            if (i.value == 0) {
                trn.context = null;
            }
        }

        if (trn.isEmpty()) {
            return;
        }

        for (TreenotationImpl.Node tree : trn.trees) {
            if (tree == null)
                continue;
            _removeContext(tree, context);
        }
    }

    private void _removeContext(TreenotationImpl.Node nd, TreenotationImpl context) {
        _removeContext(nd.trn, context);

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        _removeContext(child, context);
                    }
                }
            }
        }
    }

    private void _addContext(TreenotationImpl trn, TreenotationImpl context) {
        Object o = trn.getContext();

        if (!trn.isLocked() && o != null && o != this) {
            trn.lock();
        }

        if (o == null) {
            trn.context = context;
        } else if (o instanceof Treenotation) {
            trn.context = new MutableInteger(2);
        } else {
            ((MutableInteger) o).value++;
        }

        if (trn.isEmpty()) {
            return;
        }

        for (TreenotationImpl.Node tree : trn.trees) {
            if (tree == null)
                continue;
            _addContext(tree, context);
        }
    }

    private void _addContext(TreenotationImpl.Node nd, TreenotationImpl context) {
        _addContext(nd.trn, context);

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (TreenotationImpl.Node child : children) {
                        if (child == null)
                            continue;
                        _addContext(child, context);
                    }
                }
            }
        }
    }

    public void setFeature(String name, Object value) {
        features.put(name, value);
    }

    public Object getFeature(String name) {
        return features.get(name);
    }

    public int getNumberOfTakenChannels() {
        int c;
        c = 2;
        int n = 0;
        while (c != (1 << 16)) {
            if ((c & channels) == 0) {
                return n;
            }
            n++;
            c <<= 1;
        }
        return n;
    }

    class UncoveredAreasIteratorImpl implements UncoveredAreasIterator {
        TrnType tp;
        Token from;
        Token to;

        TokenImpl curStart;
        TokenImpl curEnd;

        public UncoveredAreasIteratorImpl(TrnType tp, Token from, Token to) {
            this.tp = tp;
            this.from = from == null ? firstToken() : from;
            this.to = to == null ? lastToken() : to;
            curStart = null;
            curEnd = null;
        }

        public boolean next() {
            findNextStart();
            if (curStart == null)
                return false;
            findNextEnd();
            return true;
        }

        private void findNextEnd() {
            curEnd = curStart;
            while (true) {
                if (curEnd == to) {
                    break;
                }
                TokenImpl next = (TokenImpl) curEnd.getNextToken();
                if (next == null) {
                    break;
                }
                if (next.getParent(tp) != null) {
                    break;
                }
                curEnd = next;
            }
        }

        private void findNextStart() {
            if (curEnd == null) {
                curStart = (TokenImpl) from;
            } else {
                if (curEnd != to) {
                    curStart = (TokenImpl) curEnd.getNextToken();
                } else {
                    curStart = null;
                    return;
                }
            }

            while (true) {
                if (curStart == null) {
                    break;
                }

                if (curStart.getParent(tp) == null) {
                    break;
                }

                if (curStart == to) {
                    curStart = null;
                    break;
                }

                curStart = (TokenImpl) curStart.getNextToken();
            }
        }

        public Token getStartToken() {
            return curStart;
        }

        public Token getEndToken() {
            return curEnd;
        }
    }

    public class SortedTypeIterator implements TypeIteratorInterface {
        private TypeIterator tit;
        private TrnType[] curType;
        private int curTypeIndex;
        private boolean tokensNow;
        private TrnType[] tokenTypes;
        private TrnType[] commonTypes;
        private TokenImpl from;
        private TokenImpl to;
        private TrnType[] fake;

        SortedTypeIterator(TrnType[] _commonTypes, TrnType[] _tokenTypes, TokenImpl _from, TokenImpl _to) {
            if (_commonTypes == null && _tokenTypes == null) {
                curType = null;
                tit = null;
                curTypeIndex = -1;
            } else {
                tokenTypes = _tokenTypes;
                commonTypes = _commonTypes;
                curType = new TrnType[1];
                from = _from;
                to = _to;
                fake = new TrnType[0];
                curTypeIndex = 0;
                if (_tokenTypes == null || _tokenTypes.length == 0) {
                    tokensNow = false;
                    curType[0] = commonTypes[0];
                    tit = new TypeIterator(curType, fake, from, to);
                } else {
                    curType[0] = tokenTypes[0];
                    tit = new TypeIterator(fake, curType, from, to);
                    tokensNow = true;
                }
                if (!tit.hasNext()) {
                    while (true) {
                        curTypeIndex++;
                        if (tokensNow) {
                            if (curTypeIndex < tokenTypes.length) {
                                curType[0] = tokenTypes[curTypeIndex];
                                tit.reset(fake, curType, from, to);
                                if (tit.hasNext())
                                    break;
                            } else {
                                tokensNow = false;
                                if (commonTypes != null) {
                                    curTypeIndex = 0;
                                    curType[0] = commonTypes[0];
                                    tit.reset(curType, fake, from, to);
                                    if (tit.hasNext())
                                        break;
                                } else {
                                    tit = null;
                                    break;
                                }
                            }
                        } else {
                            if (curTypeIndex < commonTypes.length) {
                                curType[0] = commonTypes[curTypeIndex];
                                tit.reset(curType, fake, from, to);
                                if (tit.hasNext())
                                    break;
                            } else {
                                tit = null;
                                break;
                            }
                        }
                    }
                }

            }
        }

        public void close() {
            finalize();
            tit = null;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return tit != null;
        }

        public Object next() {
            Object t = tit.next();
            if (!tit.hasNext()) {
                while (true) {
                    curTypeIndex++;
                    if (tokensNow) {
                        if (curTypeIndex < tokenTypes.length) {
                            curType[0] = tokenTypes[curTypeIndex];
                            tit.reset(fake, curType, from, to);
                            if (tit.hasNext())
                                break;
                        } else {
                            tokensNow = false;
                            if (commonTypes != null) {
                                curTypeIndex = 0;
                                curType[0] = commonTypes[0];
                                tit.reset(curType, fake, from, to);
                                if (tit.hasNext())
                                    break;
                            } else {
                                tit = null;
                                break;
                            }
                        }
                    } else {
                        if (curTypeIndex < commonTypes.length) {
                            curType[0] = commonTypes[curTypeIndex];
                            tit.reset(curType, fake, from, to);
                            if (tit.hasNext())
                                break;
                        } else {
                            tit = null;
                            break;
                        }
                    }
                }
            }
            return t;
        }

        public void finalize() {
            if (tit != null && tit.hasNext())
                tit.finalize();
        }

        public int getTokenNumber() {
            return tit.getTokenNumber();
        }

        public void reset(TrnType[] commonTypes, TrnType[] tokenTypes, Token from, Token to) {
        }

        public void reset(Token from, Token to) {
        }

        public void skipTillToken(Token until) {
        }
    }

    public class TypeIterator implements TypeIteratorInterface {
        private TreenotationImpl curTrn;
        private int curTrnTypeIndex;
        private TrnType[] tokenTypes;
        private TrnType[] commonTypes;
        private TokenImpl curToken;
        private int curTokenNumber;
        private TokenImpl from;
        private TokenImpl to;
        private BlockStack markIteratorStack;
        private BlockStack wayStack;
        private short bar;

        public TypeIterator(TrnType[] _commonTypes, TrnType[] _tokenTypes, TokenImpl _from, TokenImpl _to) {
            if (_commonTypes == null && _tokenTypes == null) {
                curToken = null;
                curTrn = null;
                markIteratorStack = null;
                curTokenNumber = -1;
            } else {
                markIteratorStack = getStack();
                wayStack = getStack();
                commonTypes = _commonTypes;
                tokenTypes = _tokenTypes;
                from = _from == null ? first : _from;
                to = _to == null ? last : _to;
                curToken = from;
                curTrn = null;
                curTokenNumber = 0;
                curTrnTypeIndex = 0;

                while (curToken != null) {
                    TrnType tp = curToken.getType();
                    for (TrnType tokenType : tokenTypes) {
                        if (tokenType.equals(tp)) {
                            curTrn = curToken;
                            bar = getChannel();
                            break;
                        }
                    }
                    if (curTrn != null)
                        break;
                    TreenotationImpl[] tokpars = null;
                    for (int i = 0; i < commonTypes.length; i++)
                        if ((tokpars = curToken.getParent(commonTypes[i])) != null) {
                            for (TreenotationImpl tokpar : tokpars) {
                                if (tokpar != null) {
                                    if (curTrn == null)
                                        curTrn = tokpar;
                                    else
                                        wayStack.push(tokpar);
                                }
                            }
                            if (curTrn != null) {
                                curTrnTypeIndex = i + 1;
                                bar = getChannel();
                                break;
                            }
                        }
                    if (curTrn != null)
                        break;
                    if (curToken == to) {
                        curToken = null;
                    } else {
                        curToken = (TokenImpl) curToken.getNextToken();
                        curTokenNumber++;
                    }
                }
                if (curToken == null) {
                    finalize();
                }

            }
        }

        public void close() {
            reset(null, null, null, null);
        }

        public void reset() {
            reset(commonTypes, tokenTypes, from, to);
        }

        public void reset(Token from, Token to) {
            reset(commonTypes, tokenTypes, from, to);
        }

        public void skipTillToken(Token until) {
            if (curToken == null)
                return;

            from = (TokenImpl) until.getNextToken();

            if (from != null && to != null && from.compareTo(to) > 0) {
                from = null;
            }

            if (from == null || curToken.compareTo(from) < 0) {
                while (curToken != from) {
                    curTokenNumber++;
                    curToken = (TokenImpl) curToken.getNextToken();
                }
            }

            curTrn = null;
            curTrnTypeIndex = 0;
            if (wayStack != null)
                wayStack.clean();

            while (curToken != null) {
                TrnType tp = curToken.getType();
                for (TrnType tokenType : tokenTypes) {
                    if (tokenType.equals(tp)) {
                        curTrn = curToken;
                        break;
                    }
                }
                if (curTrn != null)
                    break;
                TreenotationImpl[] tokpars = null;
                for (int i = 0; i < commonTypes.length; i++) {
                    if ((tokpars = curToken.getParent(commonTypes[i])) != null) {
                        for (TreenotationImpl tokpar : tokpars) {
                            if (tokpar != null) {
                                if (curTrn == null)
                                    curTrn = tokpar;
                                else
                                    wayStack.push(tokpar);
                            }
                        }
                        if (curTrn != null) {
                            curTrnTypeIndex = i + 1;
                            break;
                        }
                    }
                }
                if (curTrn != null)
                    break;
                if (curToken == to) {
                    curToken = null;
                } else {
                    curToken = (TokenImpl) curToken.getNextToken();
                    curTokenNumber++;
                }
            }
            if (curToken == null) {
                finalize();
            }

        }

        public void reset(TrnType[] _commonTypes, TrnType[] _tokenTypes, Token _from, Token _to) {
            finalize();

            if (_commonTypes == null && _tokenTypes == null) {
                curToken = null;
                curTrn = null;
                markIteratorStack = null;
                curTokenNumber = -1;
            } else {
                markIteratorStack = getStack();
                wayStack = getStack();
                commonTypes = _commonTypes;
                tokenTypes = _tokenTypes;
                from = _from == null ? first : (TokenImpl) _from;
                to = _to == null ? last : (TokenImpl) _to;
                curToken = from;
                curTrn = null;
                curTokenNumber = 0;
                curTrnTypeIndex = 0;

                while (curToken != null) {
                    TrnType tp = curToken.getType();
                    for (TrnType tokenType : tokenTypes) {
                        if (tokenType.equals(tp)) {
                            curTrn = curToken;
                            bar = getChannel();
                            break;
                        }
                    }
                    if (curTrn != null)
                        break;
                    TreenotationImpl[] tokpars = null;
                    for (int i = 0; i < commonTypes.length; i++) {
                        if ((tokpars = curToken.getParent(commonTypes[i])) != null) {
                            for (TreenotationImpl tokpar : tokpars) {
                                if (tokpar != null) {
                                    if (curTrn == null)
                                        curTrn = tokpar;
                                    else
                                        wayStack.push(tokpar);
                                }
                            }
                            if (curTrn != null) {
                                curTrnTypeIndex = i + 1;
                                bar = getChannel();
                                break;
                            }
                        }
                    }
                    if (curTrn != null)
                        break;
                    if (curToken == to) {
                        curToken = null;
                    } else {
                        curToken = (TokenImpl) curToken.getNextToken();
                        curTokenNumber++;
                    }
                }
                if (curToken == null) {
                    finalize();
                }

            }
        }


        public void remove() {
        }

        public void finalize() {
            if (markIteratorStack != null) {
                while (!markIteratorStack.isEmpty()) {
                    TreenotationImpl t = (TreenotationImpl) markIteratorStack.pop();
                    t.unmark(bar);
                }

                freeChannel(bar);
                freeStack(markIteratorStack);
                freeStack(wayStack);
                markIteratorStack = null;
                wayStack = null;
            }
        }

        public boolean hasNext() {
            return curToken != null;
        }

        public Object next() {
            TreenotationImpl t = curTrn;
            TreenotationImpl next = null;
            curTrn.mark(bar);
            markIteratorStack.push(curTrn);

            TreenotationImpl[] parent = curTrn.getParent();
            if (parent != null) {
                for (TreenotationImpl aParent : parent) {
                    if (aParent != null && !aParent.isMarked(bar)) {
                        if (next == null)
                            next = aParent;
                        else
                            wayStack.push(aParent);
                    }
                }
            }

            if (next == null) {
                while (!wayStack.isEmpty()) {
                    next = (TreenotationImpl) wayStack.pop();
                    if (!next.isMarked(bar)) {
                        break;
                    } else {
                        next = null;
                    }
                }
            }

            if (next == null) {
                TreenotationImpl[] tokpars = null;
                for (int i = curTrnTypeIndex; i < commonTypes.length; i++) {
                    if ((tokpars = curToken.getParent(commonTypes[i])) != null) {
                        for (TreenotationImpl tokpar : tokpars) {
                            if (tokpar != null && !tokpar.isMarked(bar)) {
                                if (next == null)
                                    next = tokpar;
                                else
                                    wayStack.push(tokpar);
                            }
                        }
                        if (next != null) {
                            curTrnTypeIndex = i + 1;
                            break;
                        }
                    }
                }
            }


            if (next == null) {
                if (curToken == to) {
                    curToken = null;
                } else {
                    curToken = (TokenImpl) curToken.getNextToken();
                    curTokenNumber++;
                    while (curToken != null) {
                        TrnType tp = curToken.getType();
                        for (TrnType tokenType : tokenTypes) {
                            if (tokenType.equals(tp)) {
                                curTrnTypeIndex = commonTypes.length;
                                next = curToken;
                                curTrnTypeIndex = 0;
                                break;
                            }
                        }
                        if (next != null)
                            break;
                        TreenotationImpl[] tokpars = null;
                        for (int i = 0; i < commonTypes.length; i++) {
                            if ((tokpars = curToken.getParent(commonTypes[i])) != null) {
                                for (TreenotationImpl tokpar : tokpars) {
                                    if (tokpar != null && !tokpar.isMarked(bar)) {
                                        if (next == null)
                                            next = tokpar;
                                        else
                                            wayStack.push(tokpar);
                                    }
                                }
                                if (next != null) {
                                    curTrnTypeIndex = i + 1;
                                    break;
                                }
                            }
                        }
                        if (next != null)
                            break;
                        if (curToken == to) {
                            curToken = null;
                        } else {
                            curToken = (TokenImpl) curToken.getNextToken();
                            curTokenNumber++;
                        }
                    }
                }
            }

            curTrn = next;

            if (curToken == null) {
                finalize();
            }

            return t;
        }

        public int getTokenNumber() {
            return curTokenNumber;
        }
    }

    private class FollowIterator implements FollowIteratorInterface {
        private TokenImpl current;
        private TokenImpl after;
        private TrnTypeSet followTypes;
        private TrnTypeSet input;
        private TreenotationImpl curTrn;
        private TokenImpl rightEdge;
        private BlockStack markIteratorStack;
        private BlockStack wayStack;
        private short bar;

        FollowIterator(TrnTypeSet _input, TrnTypeSet _followTypes, TokenImpl _after) {
            input = _input;

            if (_input == null || _followTypes == null) {
                bar = -1;
                current = null;
                return;
            }

            followTypes = _followTypes;

            after = _after;

            current = after == null ? first : (TokenImpl) after.getNextToken();

            if (current != null) {
                rightEdge = last;
                curTrn = null;
                wayStack = getStack();
                markIteratorStack = getStack();
                bar = getChannel();

                findNext();
            } else {
                bar = -1;
                wayStack = null;
            }
        }

        public void reset(TrnTypeSet _input, TrnTypeSet _followTypes, Token _after) {
            input = _input;

            finalize();

            if (_input == null || _followTypes == null) {
                bar = -1;
                current = null;
                return;
            }

            followTypes = _followTypes;

            after = (TokenImpl) _after;

            current = after == null ? first : (TokenImpl) after.getNextToken();

            if (current != null) {
                rightEdge = last;
                curTrn = null;
                if (wayStack == null) {
                    wayStack = getStack();
                    markIteratorStack = getStack();
                }
                bar = getChannel();

                findNext();
            } else {
                bar = -1;
                wayStack = null;
            }
        }

        public void reset(TrnTypeSet _followTypes, Token _after) {
            finalize();

            if (_followTypes == null) {
                current = null;
                bar = -1;
                return;
            }

            followTypes = _followTypes;

            after = (TokenImpl) _after;

            current = after == null ? first : (TokenImpl) after.getNextToken();

            if (current != null) {
                rightEdge = last;
                curTrn = null;
                bar = getChannel();

                if (wayStack == null) {
                    wayStack = getStack();
                    markIteratorStack = getStack();
                }

                findNext();
            } else {
                bar = -1;
                wayStack = null;
            }
        }


        private void findNext() {
            while (current != null) {
                if (wayStack.isEmpty()) {
                    if (current.isMarked(bar)) {
                        current = (TokenImpl) current.getNextToken();
                        continue;
                    } else {
                        current.mark(bar);
                        markIteratorStack.push(current);
                    }

                    TrnType tp = current.getType();
                    if (current.compareTo(rightEdge) > 0) {
                        current = null;
                        break;
                    }

                    if (input.contains(tp)) {
                        if (current.compareTo(rightEdge) < 0) {
                            rightEdge = current;
                        }
                    }
                    for (TrnType type : input.getTypes()) {
                        TreenotationImpl[] trns;
                        if ((trns = (TreenotationImpl[]) current.getParent(type)) != null) {
                            for (TreenotationImpl trn : trns) {
                                if (trn == null || trn.isMarked(bar))
                                    continue;
                                Token tok = trn.getStartToken();
                                if (tok.compareTo(rightEdge) <= 0
                                        && (after == null || tok.compareTo(after) > 0)) {
                                    wayStack.push(trn);
                                }
                            }
                        }
                    }
                    if (followTypes.contains(tp) && input.contains(tp)) {
                        curTrn = current;
                        break;
                    }
                } else {
                    TreenotationImpl trn = (TreenotationImpl) wayStack.pop();

                    if (trn.getEndToken().compareTo(rightEdge) < 0) {
                        rightEdge = (TokenImpl) trn.getEndToken();
                    }

                    trn.mark(bar);
                    markIteratorStack.push(trn);

                    TreenotationImpl[] pars;
                    if ((pars = (TreenotationImpl[]) trn.getParent()) != null) {
                        for (TreenotationImpl par : pars) {
                            if (par == null || par.isMarked(bar))
                                continue;
                            Token tok = par.getStartToken();
                            if (tok.compareTo(rightEdge) <= 0
                                    && (after == null || tok.compareTo(after) > 0)) {
                                wayStack.push(par);
                            }
                        }
                    }
                    if (followTypes.contains(trn.getType())) {
                        curTrn = trn;
                        break;
                    }
                }
            }
            if (current == null) {
                while (!markIteratorStack.isEmpty()) {
                    TreenotationImpl t = (TreenotationImpl) markIteratorStack.pop();
                    t.unmark(bar);
                }

                freeChannel(bar);
                bar = -1;
            }
        }

        public void finalize() {
            if (bar != -1) {
                while (!markIteratorStack.isEmpty()) {
                    TreenotationImpl t = (TreenotationImpl) markIteratorStack.pop();
                    t.unmark(bar);
                }

                freeChannel(bar);
                bar = -1;
                freeStack(markIteratorStack);
                freeStack(wayStack);
                markIteratorStack = null;
                wayStack = null;
            }
        }

        public int getTokenNumber() {
            return 0;
        }

        public void remove() {

        }

        public boolean hasNext() {
            return current != null;
        }

        public Object next() {
            Treenotation trn = curTrn;
            findNext();
            return trn;
        }

        public void close() {
            reset(null, null);
        }
    }

    private class TokenIterator implements Iterator<Token> {
        private Token curToken;

        public TokenIterator(Token first) {
            curToken = first;
        }

        public void remove() {
        }

        public boolean hasNext() {
            return curToken != null;
        }

        public Token next() {
            Token t = curToken;
            curToken = curToken.getNextToken();
            return t;
        }
    }

    private class InternalTrnsIterator implements TrnIterator {
        private TreenotationImpl current;
        private BlockStack stack;

        private TokenImpl from;
        private TokenImpl to;

        InternalTrnsIterator(TreenotationImpl trn, TokenImpl from, TokenImpl to) {
            this.from = from;
            this.to = to;
            stack = getStack();
            if (from == null || from.compareTo(trn.startToken) < 0) {
                this.from = trn.startToken;
            }
            if (to == null || to.compareTo(trn.endToken) > 0) {
                this.to = trn.endToken;
            }
            current = null;
            if (trn.trees != null) {
                for (TreenotationImpl.Node tree : trn.trees) {
                    if (tree == null)
                        continue;
                    stack.push(tree);
                }
            }
            find_next();
        }

        private void find_next() {
            while (!stack.isEmpty()) {
                TreenotationImpl.Node curNd = (TreenotationImpl.Node) stack.pop();
                current = curNd.trn;

                if (curNd.relations != null) {
                    int sz = curNd.relations.size();
                    for (int i = 0; i < sz; i++) {
                        TreenotationImpl.Node[] nodes = (TreenotationImpl.Node[]) curNd.relations.getByIndex(i);
                        if (nodes != null) {
                            for (int j = 0; j < nodes.length; j++) {
                                TreenotationImpl.Node nd = nodes[j];
                                if (nd == null)
                                    continue;
                                stack.push(nd);
                            }
                        }
                    }
                }

                if (current.getStartToken().compareTo(to) <= 0 && current.getEndToken().compareTo(from) >= 0) {
                    return;
                }
            }
            current = null;
        }

        public boolean hasNext() {
            return current != null;
        }

        public Object next() {
            Object res = current;
            find_next();
            return res;
        }

        public void remove() {
        }

        public int getTokenNumber() {
            TokenImpl cur = from;
            TreenotationImpl curTrn = current;
            int i = 0;
            while (cur.compareTo(curTrn.getStartToken()) < 0) {
                i++;
                cur = (TokenImpl) cur.getNextToken();
            }
            return i;
        }
    }

    private class InternalRelationsIterator implements RelationsIterator {
        private TreenotationImpl host, slave;
        private TrnRelationType curType;
        private BlockStack stack;

        private TokenImpl from;
        private TokenImpl to;

        InternalRelationsIterator(TreenotationImpl trn, TokenImpl from, TokenImpl to) {
            this.from = from;
            this.to = to;
            stack = getStack();
            if (from == null || from.compareTo(trn.startToken) < 0) {
                this.from = trn.startToken;
            }
            if (to == null || to.compareTo(trn.endToken) > 0) {
                this.to = trn.endToken;
            }
            host = slave = null;
            curType = null;
            if (trn.trees != null) {
                for (TreenotationImpl.Node tree : trn.trees) {
                    if (tree == null)
                        continue;
                    stack.push(tree);
                    try {
                        if (tree.parentConnection == TreenotationImpl.PARENT_CONNECTION_STRONG) {
                            stack.push(relations.get(TrnRelationTypeStorage.root_RELATION));
                        } else if (tree.parentConnection == TreenotationImpl.PARENT_CONNECTION_PATH) {
                            stack.push(relations.get(TrnRelationTypeStorage.root_path_RELATION));
                        } else {
                            stack.push(null);
                        }
                    } catch (TreetonModelException e) {
                        stack.push(null);
                    }
                    stack.push(trn);
                }
            }
        }

        private void find_next() {
            while (!stack.isEmpty()) {
                host = (TreenotationImpl) stack.pop();
                curType = (TrnRelationType) stack.pop();
                TreenotationImpl.Node toNd = (TreenotationImpl.Node) stack.pop();
                slave = toNd.trn;

                if (toNd.relations != null) {
                    int sz = toNd.relations.size();
                    for (int i = 0; i < sz; i++) {
                        TreenotationImpl.Node[] nodes = (TreenotationImpl.Node[]) toNd.relations.getByIndex(i);
                        if (nodes != null) {
                            for (TreenotationImpl.Node node : nodes) {
                                if (node == null)
                                    continue;
                                stack.push(node);
                                try {
                                    stack.push(relations.get(toNd.relations.getKey(i)));
                                } catch (TreetonModelException e) {
                                    stack.push(null);
                                }
                                stack.push(toNd.trn);
                            }
                        }
                    }
                }

                Token min;
                Token max;
                try {
                    if (curType == null || curType.isRoot()) {
                        min = slave.getStartToken();
                        max = slave.getEndToken();
                    } else {
                        min = host.getStartToken().compareTo(slave.getStartToken()) < 0 ? host.getStartToken() : slave.getStartToken();
                        max = host.getEndToken().compareTo(slave.getEndToken()) < 0 ? slave.getEndToken() : host.getEndToken();
                    }
                    if (min.compareTo(to) <= 0 && max.compareTo(from) >= 0) {
                        return;
                    }
                } catch (TreetonModelException e) {
                    throw new RuntimeException("Error with model", e);
                }
            }

            host = slave = null;
            curType = null;
        }

        public Treenotation getHost() {
            return host;
        }

        public Treenotation getSlave() {
            return slave;
        }

        public TrnRelationType getType() {
            return curType;
        }

        public boolean next() {
            find_next();
            return host != null;
        }
    }

    class ElementInfo {
        String uri;
        TrnType type;
        boolean hidden;
        boolean opened;
        String text;
        String start;
        String end;
        int lengthN;
        int lengthD;

        ArrayList tarr = new ArrayList();
        StringBuffer buf = new StringBuffer();

        BlackBoard board = TreetonFactory.newBlackBoard(100, false);

        private void extract(Element cur) {
            try {
                type = types.get(cur.getAttribute("type"));
            } catch (TreetonModelException e) {
                type = null;
            }
            if (type == null) {
                throw new RuntimeException("Wrong type name: " + cur.getAttribute("type"));
            }
            uri = cur.getAttribute("uri");
            text = cur.getAttribute("text");
            hidden = "true".equals(cur.getAttribute("hidden"));
            opened = "true".equals(cur.getAttribute("opened"));
            String s = cur.getAttribute("start");
            Double d;
            if (s.length() > 0) {
                start = s;
            } else {
                start = null;
            }
            s = cur.getAttribute("end");
            if (s.length() > 0) {
                end = s;
            } else {
                end = null;
            }
            s = cur.getAttribute("length");
            if (s.length() > 0) {
                d = Double.valueOf(s);
                if (d.intValue() == d) {
                    lengthN = d.intValue();
                    lengthD = 1;
                } else {
                    lengthN = (int) (d / 0.01);
                    lengthD = 100;
                }
            }
            NodeList l = cur.getElementsByTagName("attrs");
            if (l.getLength() > 0) {
                Element attrs = (Element) l.item(0);
                NamedNodeMap nm = attrs.getAttributes();
                for (int i = 0; i < nm.getLength(); i++) {
                    Attr attr = (Attr) nm.item(i);
                    int fi;
                    try {
                        fi = type.getFeatureIndex(attr.getName());
                    } catch (TreetonModelException e) {
                        fi = -1;
                    }
                    if (fi == -1) {
                        throw new RuntimeException("Wrong feature name \'" + attr.getName());
                    }
                    Class fType = null;
                    try {
                        fType = type.getFeatureTypeByIndex(fi);
                    } catch (TreetonModelException e) {
                        //do nothing
                    }
                    String val = attr.getValue();
                    if (val.length() > 0 && val.charAt(0) == '[') {
                        int j = 1;
                        buf.setLength(0);
                        tarr.clear();
                        for (; j < val.length(); j++) {
                            char c = val.charAt(j);
                            if (c == ';') {
                                if (fType == Treenotation.class) {
                                    Treenotation trn = uri2trn.get(buf.toString());
                                    if (trn == null) {
                                        throw new RuntimeException("Wrong uri " + buf + ". Unable to find corresponding treenotation.");
                                    }
                                    tarr.add(trn);
                                } else {
                                    tarr.add(buf.toString());
                                }
                                buf.setLength(0);
                            } else if (c == '\\') {
                                if (j < val.length() - 1) {
                                    j++;
                                    buf.append(val.charAt(j));
                                } else {
                                    buf.append('\\');
                                }
                            } else if (c == ']') {
                                if (fType == Treenotation.class) {
                                    Treenotation trn = uri2trn.get(buf.toString());
                                    if (trn == null) {
                                        throw new RuntimeException("Wrong uri " + buf + ". Unable to find corresponding treenotation.");
                                    }
                                    tarr.add(trn);
                                } else {
                                    tarr.add(buf.toString());
                                }
                                buf.setLength(0);
                            } else {
                                buf.append(c);
                            }
                        }
                        board.put(fi, type, tarr.toArray());
                    } else {
                        board.put(fi, type, val);
                    }
                }
            }
        }
    }

    class ExportEnv {
        TokenImpl lastExportedToken;
        DecimalFormat df;

        public ExportEnv() {
            lastExportedToken = null;
            df = new DecimalFormat();
            DecimalFormatSymbols dfs = new DecimalFormatSymbols();
            dfs.setDecimalSeparator('.');
            df.setDecimalFormatSymbols(dfs);
            df.setMaximumFractionDigits(6);
        }
    }
}
