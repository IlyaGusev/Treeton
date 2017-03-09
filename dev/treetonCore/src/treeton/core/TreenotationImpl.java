/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnRelationType;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.util.MutableInteger;

import java.util.HashMap;

public class TreenotationImpl extends IntFeatureMapImpl implements Treenotation {
    public static final int PARENT_CONNECTION_SIMPLE = 0;
    public static final int PARENT_CONNECTION_WEAK = 1;
    public static final int PARENT_CONNECTION_STRONG = 2;
    public static final int PARENT_CONNECTION_PATH = 3;
    private static final BlackBoard localboard = TreetonFactory.newBlackBoard(10, false);
    TokenImpl startToken;
    TokenImpl endToken;
    //Tree part
    boolean locked = true;
    Object context;
    int nView = 0;
    Node[] trees;
    int id = -1;
    private TrnType type;
    private String uri;
    private TreenotationImpl[] parent;
    private short label;

    TreenotationImpl(Token _startToken, Token _endToken, TrnType _type) {
        super(null);
        startToken = (TokenImpl) _startToken;
        endToken = (TokenImpl) _endToken;
        type = _type;
        checkTokens();
        parent = null;
        label = 0;
    }

    TreenotationImpl(Token _startToken, Token _endToken, TrnType _type, BlackBoard board) {
        super();
        startToken = (TokenImpl) _startToken;
        endToken = (TokenImpl) _endToken;
        type = _type;
        checkTokens();
        parent = null;
        if (board != null)
            put(board);
    }

    public String getUri() {
        return uri;
    }

    void setUri(String uri) {
        this.uri = uri;
    }

    public TreenotationStorage getStorage() {
        return startToken != null ? startToken.getStorage() : null;
    }

    protected void checkTokens() {
        if (startToken == null && endToken != null
                || endToken == null && startToken != null
                ) {
            throw new IllegalArgumentException("Wrong tokens");
        }
        if (startToken != null) {
            if (startToken.compareTo(endToken) > 0) {
                throw new RuntimeException("startToken must precede the endToken");
            }
            if (startToken.getStorage() != endToken.getStorage()) {
                throw new RuntimeException("Tokens must belong to the same storage");
            } else if (type != null) {
        /*if (startToken.getStorage().getTypes() != type.getStorage()) {
          throw new RuntimeException("Type and TreenotationStorage have different TrnTypeStorages");
        } */
                ((TreenotationStorageImpl) startToken.getStorage()).obtainTrn(this);
            } else {
                throw new RuntimeException("Unable to add the Treenotation with the null-type to the storage");
            }
        }
    }

    void addParent(TreenotationImpl p) {
        if (!p.type.equals(type))
            throw new IllegalArgumentException();

        if (parent == null) {
            parent = new TreenotationImpl[1];
            parent[0] = p;
        } else {
            for (int i = 0; i < parent.length; i++) {
                if (parent[i] == null) {
                    parent[i] = p;
                    return;
                }
                if (parent[i] == p) {
                    return;
                }
            }
            TreenotationImpl[] t = new TreenotationImpl[parent.length + 1];
            System.arraycopy(parent, 0, t, 0, parent.length);
            t[parent.length] = p;
            parent = t;
        }
    }

    public TrnType getType() {
        return type;
    }

    protected void setType(TrnType type) {
        this.type = type;
    }

    public Object getValue(int feature) {
        return get(feature);
    }

    public Object getValue(String featureName) {
        return get(featureName);
    }

    public void mark(short bar) {
        label |= bar;
    }

    public void unmark(short bar) {
        label &= ~bar;
    }

    public boolean isMarked(short bar) {
        return (label & bar) != 0;
    }

    public boolean insideOf(Treenotation trn) {
        return ((TreenotationImpl) trn).getStartToken().compareTo(getStartToken()) <= 0 && getEndToken().compareTo(((TreenotationImpl) trn).getEndToken()) <= 0;
    }

    public boolean intersects(Treenotation trn) {
        return ((TreenotationImpl) trn).getStartToken().compareTo(getEndToken()) <= 0 && getStartToken().compareTo(((TreenotationImpl) trn).getEndToken()) <= 0;
    }

    public String toString() {
        return "( " +
                (
                        startToken != null ?
                                startToken.getStartNumerator() + "/" + startToken.getStartDenominator()
                                : ""
                ) + " : " + getString() + " : " +
                (
                        endToken != null ?
                                endToken.getEndNumerator() + "/" + endToken.getEndDenominator()
                                : ""
                ) +
                " )";
    }

    public int getStartNumerator() {
        return startToken.getStartNumerator();
    }

    public int getStartDenominator() {
        return startToken.getStartDenominator();
    }

    public int getEndNumerator() {
        return endToken.getEndNumerator();
    }

    public int getEndDenominator() {
        return endToken.getEndDenominator();
    }

    public Token getStartToken() {
        return startToken;
    }

    public Token getEndToken() {
        return endToken;
    }

    public TreenotationImpl[] getParent() {
        return parent;
    }

    public boolean isChildOf(Treenotation trn) {
        if (trn == null)
            return false;
        if (parent == null)
            return false;
        for (TreenotationImpl aParent : parent) {
            if (aParent == trn)
                return true;
        }
        return false;
    }

    public boolean corrupted() {
        if (parent == null) {
            return false;
        }

        for (int i = 0; i < parent.length; i++) {
            if (parent[i] != null) {
                for (int j = i + 1; j < parent.length; j++) {
                    if (parent[i] != null) {
                        if (parent[i] == parent[j])
                            return true;
                    }
                }
            }
        }

        return false;
    }

    public double toDouble() {
        return startToken.toDouble();
    }

    public double endToDouble() {
        return endToken.endToDouble();
    }

    public int toInt() {
        return startToken.toInt();
    }

    public int endToInt() {
        return endToken.endToInt();
    }

    public Object get(String featureName) {
        int fn;
        try {
            fn = type.getFeatureIndex(featureName);
        } catch (TreetonModelException e) {
            fn = -1;
        }
        if (fn == -1)
            return null;
        return this.get(fn);
    }

    public void put(String featureName, Object value) {
        int fn = 0;
        try {
            fn = type.getFeatureIndex(featureName);
        } catch (TreetonModelException e) {
            fn = -1;
        }
        if (fn == -1)
            return;
        put(fn, value);
    }

    public void put(int n, Object value) {
        super.put(n, value, type);
    }

    public void put(BlackBoard board) {
        super.put(board, type);
    }

    public String getString() {
        return getString(type);
    }

    public String getHtmlString() {
        return getHtmlString(type);
    }

    public Object clone() {
        TreenotationImpl t = new TreenotationImpl(getStartToken(), getEndToken(), this.type);
        t.blockSize = blockSize;
        t.mapper = mapper;
        if (mapper != null) {
            t.data = new Object[data.length];
            System.arraycopy(data, 0, t.data, 0, data.length);
        } else {
            t.data = null;
        }
        if (!isLocked()) {
            t.locked = false;
        }
        return t;
    }

    public void appendTrnStringView(StringBuffer buf) {
        try {
            buf.append(type.getIndex());
        } catch (TreetonModelException e) {
            //do nothing
        }
        buf.append((char) 0);
        int l;
        try {
            l = type.getFeaturesSize();
        } catch (TreetonModelException e) {
            l = -1;
        }
        for (int i = 0; i < l; i++) {
            Object o = get(i);
            if (o instanceof TStringImpl) {
                ((TStringImpl) o).appendToStringBuffer(buf);
                buf.append((char) 0);
            } else if (o instanceof Integer) {
                buf.append(o);
                buf.append((char) 0);
            } else if (o == null) {
                buf.append((char) 1);
            }
        }
    }

    public int readInFromStringView(TrnTypeStorage types, char[] view, int pl) {
        synchronized (localboard) {
            int oldpl = pl;
            while (view[pl++] != 0) ;
            try {
                type = types.get(Integer.parseInt(new String(view, oldpl, pl - oldpl - 1)));
                int l = type.getFeaturesSize();
                for (int i = 0; i < l; i++) {
                    if (view[pl] == 1) {
                        pl++;
                        continue;
                    }
                    oldpl = pl;
                    while (view[pl++] != 0) ;
                    localboard.put(i, TreetonFactory.newTString(view, oldpl, (pl - oldpl - 1)));
                }
                put(localboard);
            } catch (TreetonModelException e) {
                //do nothing;
            }
        }
        return pl;
    }

    public String getText() {
        if (startToken == null) {
            return null;
        }

        if (startToken == endToken) {
            return startToken.getText();
        }

        StringBuffer sb = new StringBuffer();


        TokenImpl cur = startToken;
        while (cur != endToken) {
            sb.append(cur.getText());
            cur = (TokenImpl) cur.getNextToken();
        }
        sb.append(cur.getText());

        return sb.toString();
    }

    public int getTokenLength() {
        TokenImpl cur = startToken;
        int i = 0;
        while (cur != endToken) {
            i++;
            cur = (TokenImpl) cur.getNextToken();
        }
        i++;
        return i;
    }

    public void lock() {
        if (locked)
            return;

        locked = true;

        if (isEmpty()) {
            return;
        }

        for (Node tree : trees) {
            if (tree == null)
                continue;
            lockNode(tree);
        }
    }

    private void lockNode(Node nd) {
        TreenotationImpl trn = nd.trn;

        if (nd.relations != null) {
            int sz = nd.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) nd.relations.getByIndex(i);
                if (children != null) {
                    for (Node child : children) {
                        if (child == null)
                            continue;
                        lockNode(child);
                    }
                }
            }
        }
        trn.lock();
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isEmpty() {
        return getNumberOfTrees() == 0;
    }

    public Object getContext() {
        return context;
    }

    public void addTree(Node nd) {
        //nd.parentConnection=PARENT_CONNECTION_WEAK;
        if (trees != null) {
            for (int i = 0; i < trees.length; i++) {
                Node tree = trees[i];
                if (tree == null) {
                    trees[i] = nd;
                    return;
                }
            }
            Node[] tarr = new Node[trees.length + 1];
            System.arraycopy(trees, 0, tarr, 0, trees.length);
            tarr[tarr.length - 1] = nd;
            trees = tarr;
        } else {
            trees = new Node[1];
            trees[0] = nd;
        }
    }

    public Node addTree(TreenotationImpl root) {
        Node nd = new Node(PARENT_CONNECTION_WEAK, root);
        addTree(nd);
        return nd;
    }

    Node removeTree(TreenotationImpl root) {
        if (trees == null)
            return null;
        for (int i = 0; i < trees.length; i++) {
            Node tree = trees[i];
            if (tree == null)
                continue;
            if (tree.trn == root) {
                trees[i] = null;
                return tree;
            }
        }
        return null;
    }

    public int getNumberOfTrees() {
        if (trees == null)
            return 0;
        int n = 0;
        for (Node tree : trees) {
            if (tree == null)
                continue;
            n++;
        }
        return n;
    }

    public Node[] getTrees() {
        return trees;
    }

    public int getNView() {
        return nView;
    }

    Node findNode(Node nd, Treenotation trn) {
        if (nd == null)
            return null;
        if (nd.trn == trn) {
            return nd;
        }
        if (nd.relations == null) {
            return null;
        }
        int sz = nd.relations.size();
        for (int i = 0; i < sz; i++) {
            Node[] children = (Node[]) nd.relations.getByIndex(i);
            if (children != null) {
                for (Node child : children) {
                    if (child == null)
                        continue;
                    Node f = findNode(child, trn);
                    if (f != null) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public Node findNode(Treenotation trn) {
        if (trees == null)
            return null;
        for (Node nd : trees) {
            if (nd == null)
                continue;
            if ((nd = findNode(nd, trn)) != null) {
                return nd;
            }
        }
        return null;
    }

    Node findRootNode(Treenotation trn) {
        if (trees == null)
            return null;
        for (Node nd : trees) {
            if (nd == null)
                continue;
            if (findNode(nd, trn) != null) {
                return nd;
            }
        }
        return null;
    }

    public boolean contains(Treenotation trn) {
        return findNode(trn) != null;
    }

    public boolean isAdded() {
        return isMarked((short) 1);
    }

    public long getId() {
        return id;
    }

    public TreenotationImpl getCopy(HashMap<Treenotation, Treenotation> old2new, boolean cloneLocked) {
        TreenotationImpl res = _getCopy(this, old2new, cloneLocked);
        res.context = null;
        return res;
    }

    private TreenotationImpl _getCopy(TreenotationImpl context, HashMap<Treenotation, Treenotation> old2new, boolean cloneLocked) {
        TreenotationImpl copy;
        if (!isLocked() || cloneLocked) {
            copy = (TreenotationImpl) clone();
            copy.context = context;

            if (!isEmpty()) {
                for (Node tree : trees) {
                    if (tree != null) {
                        Node nd;
                        copy.addTree(nd = getCopy(context, tree, old2new, cloneLocked));
                        nd.parentConnection = tree.parentConnection;
                    }
                }
            }

            if (isAdded()) {
                ((TreenotationStorageImpl) getStorage())._add(copy);
            }
        } else {
            copy = this;
            Object o = getContext();
            if (o instanceof Treenotation) {
                this.context = new MutableInteger(2);
            } else {
                ((MutableInteger) o).value++;
            }
        }


        old2new.put(this, copy);

        return copy;
    }

    private Node getCopy(TreenotationImpl context, Node src, HashMap<Treenotation, Treenotation> old2new, boolean cloneLocked) {
        Node copy = new Node(src.parentConnection, src.trn._getCopy(context, old2new, cloneLocked));

        if (src.relations != null) {
            copy.relations = (IntFeatureMap) src.relations.clone();
            int sz = src.relations.size();
            for (int i = 0; i < sz; i++) {
                TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) src.relations.getByIndex(i);
                if (children != null) {
                    TreenotationImpl.Node[] childrenCopy = new TreenotationImpl.Node[children.length];
                    for (int j = 0; j < children.length; j++) {
                        TreenotationImpl.Node child = children[j];
                        if (child == null) {
                            childrenCopy[j] = null;
                            continue;
                        }
                        childrenCopy[j] = getCopy(context, child, old2new, cloneLocked);
                    }
                    copy.relations.putByIndex(i, childrenCopy);
                }
            }
        }

        return copy;
    }

    public static class Node {
        int parentConnection; //0 - simple, 1-weak outer, 2-strong outer

        TreenotationImpl trn;
        IntFeatureMap relations;

        public Node(int parentConnection, TreenotationImpl trn) {
            this.parentConnection = parentConnection;
            this.trn = trn;
        }

        public IntFeatureMap getRelations() {
            return relations;
        }

        public Node addRelation(TreenotationImpl slave, TrnRelationType tp, Node nd) {
            Node[] arr;
            if (nd == null) {
                nd = new Node(PARENT_CONNECTION_SIMPLE, slave);
            } else {
                nd.parentConnection = PARENT_CONNECTION_SIMPLE;
                nd.trn = slave;
            }
            if (relations == null) {
                relations = new IntFeatureMapImpl();
                arr = new Node[]{nd};
            } else {
                try {
                    arr = (Node[]) relations.get(tp.getIndex());
                } catch (TreetonModelException e) {
                    arr = null;
                }
                if (arr == null) {
                    arr = new Node[]{nd};
                } else {
                    for (int i = 0; i < arr.length; i++) {
                        Node node = arr[i];
                        if (node == null) {
                            arr[i] = nd;
                            return nd;
                        }
                    }
                    Node[] tarr = new Node[arr.length + 1];
                    System.arraycopy(arr, 0, tarr, 0, arr.length);
                    tarr[tarr.length - 1] = nd;
                    arr = tarr;
                }
            }
            try {
                relations.put(tp.getIndex(), arr);
            } catch (TreetonModelException e) {
                //do nothing
            }
            return nd;
        }

        public Node removeRelation(TreenotationImpl slave) {
            if (relations != null) {
                int sz = relations.size();
                for (int i = 0; i < sz; i++) {
                    TreenotationImpl.Node[] children = (TreenotationImpl.Node[]) relations.getByIndex(i);
                    if (children != null) {
                        for (int j = 0; j < children.length; j++) {
                            Node child = children[j];
                            if (child == null)
                                continue;
                            if (child.trn == slave) {
                                children[j] = null;
                                return child;
                            }
                        }
                    }
                }
            }
            return null;
        }

        public TreenotationImpl.Node[] getChildren(TrnRelationType tp) {
            try {
                return (Node[]) relations.get(tp.getIndex());
            } catch (TreetonModelException e) {
                return null;
            }
        }

        public Treenotation getTrn() {
            return trn;
        }

        public int getParentConection() {
            return parentConnection;
        }
    }
}
