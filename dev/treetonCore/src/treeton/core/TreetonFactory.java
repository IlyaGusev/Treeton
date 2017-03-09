/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.fsm.CharFSM;
import treeton.core.fsm.CharFSMImpl;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnRelationTypeStorage;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;

import java.util.HashMap;
import java.util.Iterator;

public class TreetonFactory {
    private static TreetonFactory instance = null;
    private static HashMap<String, Recoder> recoders = null;
    private static Recoder defaultRecoder = null;

    private TreetonFactory() {
        instance = this;
    }

    public static TreetonFactory getInstance() {
        if (instance == null)
            return new TreetonFactory();
        return instance;
    }


    public static IntFeatureMapStatic newIntFeatureMapStatic(BlackBoard board) {
        int max = board.getDepth();
        int min = board.getFirstNumber();
        int n = board.getNumberOfObjects();

        if (max - min < 32 || (double) n / (double) (max - min + 1) > 0.3) {
            return new IntFeatureMapStaticStraight(board);
        } else {
            return new IntFeatureMapStaticLog(board);
        }
    }

    public static BlackBoard newBlackBoard(int size, boolean lock) {
        return new BlackBoardImpl(size, lock);
    }

    public static CharFeatureMap newCharFeatureMap(int capacity) {
        return new CharFeatureMapImpl(capacity, getRecoder());
    }

    public static CharFeatureMap newCharFeatureMap(CharFSM cfsm) {
        return new CharFeatureMapImpl((CharFSMImpl) cfsm);
    }

    public static CharFeatureMap newCharFeatureMap(int capacity, String recoderName) {
        return new CharFeatureMapImpl(capacity, getRecoder(recoderName));
    }

    public static CharFSM newCharFSM() {
        return new CharFSMImpl(getRecoder());
    }

    public static CharFSM newCharFSMSimple() {
        return new CharFSMImpl(getRecoder());
    }

    public static CharFSM newCharFSMSimple(String t) {
        return new CharFSMImpl(getRecoder(t));
    }

    public static CharFSM newCharFSM(String recoderName) {
        return new CharFSMImpl(getRecoder(recoderName));
    }

    public static Recoder getRecoder() {
        if (defaultRecoder != null)
            return defaultRecoder;
        return getRecoder("Default");
    }

    public static Recoder newRecoder() {
        return new RecoderImpl();
    }

    public static void changeDefaultRecoder(String name) {
        defaultRecoder = getRecoder(name);
    }


    public static Recoder getRecoder(String s) {
        Recoder t;
        if (recoders == null) {
            t = new RecoderImpl();
            recoders = new HashMap<String, Recoder>();
            recoders.put(s, t);
        } else {
            if ((t = recoders.get(s)) == null) {
                t = new RecoderImpl();
                recoders.put(s, t);
            }
        }
        return t;
    }

    public static Treenotation newTreenotation(Token _startToken, Token _endToken, String _type) {
        try {
            return new TreenotationImpl(_startToken, _endToken, _startToken.getType().getStorage().get(_type));
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error when trying to get storage from token");
        }
    }

    public static Treenotation newTreenotation(Token _startToken, Token _endToken, String _type, BlackBoard board) {
        try {
            return new TreenotationImpl(_startToken, _endToken, _startToken.getType().getStorage().get(_type), board);
        } catch (TreetonModelException e) {
            throw new RuntimeException("Error when trying to get storage from token");
        }
    }

    public static Treenotation newTreenotation(Token _startToken, Token _endToken, TrnType _type) {
        return new TreenotationImpl(_startToken, _endToken, _type);
    }

    public static Treenotation newTreenotation(Token _startToken, Token _endToken, TrnType _type, BlackBoard board) {
        return new TreenotationImpl(_startToken, _endToken, _type, board);
    }

    public static Treenotation newSyntaxTreenotation(TreenotationStorage storage, Token _startToken, Token _endToken, TrnType _type, BlackBoard board) {
        TreenotationImpl res = new TreenotationSyntax(storage, _startToken, _endToken, _type, board);
        res.locked = false;
        return res;
    }

    public static Treenotation newSyntaxTreenotation(TreenotationStorage storage, Token _startToken, Token _endToken, TrnType _type) {
        TreenotationImpl res = new TreenotationSyntax(storage, _startToken, _endToken, _type);
        res.locked = false;
        return res;
    }

    public static Treenotation newTreenotationHashable(Token _startToken, Token _endToken, TrnType _type, BlackBoard board) {
        return new TreenotationImplHashable(_startToken, _endToken, _type, board);
    }

    public static Treenotation newTreenotationHashable(Token _startToken, Token _endToken, TrnType _type) {
        return new TreenotationImplHashable(_startToken, _endToken, _type);
    }

    public static Treenotation newTreenotationHashable(Treenotation t) {
        return new TreenotationImplHashable((TreenotationImpl) t);
    }

    public static TreenotationStorage newTreenotationStorage(TreenotationsContext context) {
        return new TreenotationStorageImpl(context);
    }

    public static TreenotationStorage newTreenotationStorage() {
        return new TreenotationStorageImpl();
    }

    public static TreenotationStorage newTreenotationStorage(TrnTypeStorage types, TrnRelationTypeStorage rels) {
        return new TreenotationStorageImpl(types, rels);
    }

    public static TString newTString() {
        return new TStringImpl();
    }

    public static TString newTString(String original) {
        return new TStringImpl(original);
    }

    public static TString newTString(TString original) {
        return new TStringImpl(original);
    }

    public static TString newTString(String original, int start, int end) {
        return new TStringImpl(original, start, end);
    }

    public static TString newTString(char value[]) {
        return new TStringImpl(value);
    }

    public static TString newTString(char value[], int offset, int count) {
        return new TStringImpl(value, offset, count);
    }

    public static int getMostRelevantKey(Iterator featureMaps, int[] filter, int filterLength, Class[] _cls) {
        return IntFeatureMapImpl.getMostRelevantKey(featureMaps, filter, filterLength, _cls);
    }

    public static int getMostRelevantKey(Iterator featureMaps, int[] filter, int filterLength) {
        return IntFeatureMapImpl.getMostRelevantKey(featureMaps, filter, filterLength);
    }

    static Object[] getCharFeatureMapValues(CharFeatureMap fm) {
        return ((CharFeatureMapImpl) fm).getValueArr();
    }

    static Object[] getIntFeatureMapData(IntFeatureMap fm) {
        return ((IntFeatureMapImpl) fm).getData();
    }

    static Object[] getIntFeatureMapStaticData(IntFeatureMapStatic fm) {
        if (fm instanceof IntFeatureMapStaticLog) {
            return ((IntFeatureMapStaticLog) fm).getData();
        } else {
            return ((IntFeatureMapStaticStraight) fm).getData();
        }
    }
}
