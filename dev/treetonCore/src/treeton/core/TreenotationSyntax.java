/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnType;

public class TreenotationSyntax extends TreenotationImpl {
    TreenotationStorageImpl storage;

    TreenotationSyntax(TreenotationStorage storage, Token _startToken, Token _endToken, TrnType _type) {
        super(_startToken, _endToken, _type);
        if (storage == null) {
            throw new RuntimeException("Storage must be specified");
        }
        if (getType() != null) {
      /*if (storage.getTypes() != getType().getStorage()) {
        throw new RuntimeException("Type and TreenotationStorage have different TrnTypeStorages");
      } */
        } else {
            throw new RuntimeException("Unable to add the Treenotation with the null-type to the storage");
        }
        locked = false;
        this.storage = (TreenotationStorageImpl) storage;
        if (startToken != null && startToken.getStorage() != storage) {
            throw new RuntimeException("Tokens don't belong to the same storage with this TreenotationSyntax");
        }
        this.storage.obtainTrn(this);
    }

    TreenotationSyntax(TreenotationStorage storage, Token _startToken, Token _endToken, TrnType _type, BlackBoard board) {
        super(_startToken, _endToken, _type, board);
        if (storage == null) {
            throw new RuntimeException("Storage must be specified");
        }
        if (getType() != null) {
      /*if (storage.getTypes() != getType().getStorage()) {
        throw new RuntimeException("Type and TreenotationStorage have different TrnTypeStorages");
      } */
        } else {
            throw new RuntimeException("Unable to add the Treenotation with the null-type to the storage");
        }
        locked = false;
        this.storage = (TreenotationStorageImpl) storage;
        if (startToken != null && startToken.getStorage() != storage) {
            throw new RuntimeException("Tokens don't belong to the same storage with this TreenotationSyntax");
        }
        this.storage.obtainTrn(this);
    }

    TreenotationSyntax(TreenotationStorage storage, Token _startToken, Token _endToken, TrnType _type, BlackBoard board, String uri) {
        super(_startToken, _endToken, _type, board);
        if (storage == null) {
            throw new RuntimeException("Storage must be specified");
        }
        if (getType() != null) {
      /*if (storage.getTypes() != getType().getStorage()) {
        throw new RuntimeException("Type and TreenotationStorage have different TrnTypeStorages");
      } */
        } else {
            throw new RuntimeException("Unable to add the Treenotation with the null-type to the storage");
        }
        locked = false;
        if (startToken != null && startToken.getStorage() != storage) {
            throw new RuntimeException("Tokens don't belong to the same storage with this TreenotationSyntax");
        }
        this.storage = (TreenotationStorageImpl) storage;
        setUri(uri);
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
            }
        }
    }

    public TreenotationStorage getStorage() {
        return storage;
    }

    public Object clone() {
        TreenotationImpl t = new TreenotationSyntax(storage, getStartToken(), getEndToken(), this.getType());
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
}
