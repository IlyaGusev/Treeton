/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

import treeton.core.model.TrnType;

public class TreenotationImplHashable extends TreenotationImpl {
    TreenotationImplHashable(Token _startToken, Token _endToken, TrnType _type) {
        super(_startToken, _endToken, _type);
    }

    TreenotationImplHashable(Token _startToken, Token _endToken, TrnType _type, BlackBoard board) {
        super(_startToken, _endToken, _type, board);
    }

    public TreenotationImplHashable(TreenotationImpl trn) {
        super(trn.getStartToken(), trn.getEndToken(), trn.getType());
        blockSize = trn.blockSize;
        mapper = trn.mapper;
        if (trn.mapper != null) {
            data = new Object[trn.data.length];
            System.arraycopy(trn.data, 0, data, 0, trn.data.length);
        } else {
            data = null;
        }
    }

    public boolean equals(Object o) {
        if (o instanceof TreenotationImplHashable) {
            TreenotationImplHashable other = (TreenotationImplHashable) o;
            if (other.mapper != mapper) {
                return false;
            }
            if (other.getType() != getType())
                return false;
            if (other.startToken != startToken)
                return false;
            //noinspection SimplifiableIfStatement
            if (other.endToken != endToken)
                return false;
            return featureEquals(other);
        }
        return false;
    }

    private boolean featureEquals(Object o) {
        IntFeatureMapImpl other = (IntFeatureMapImpl) o;
        if (other.mapper != mapper) {
            return false;
        }

        int l = mapper == null ? -1 : mapper.getMaxValue();
        for (int i = 0; i <= l; i++) {
            if (data[i] == null) {
                if (other.data[i] != null)
                    return false;
            } else if (other.data[i] == null) {
                if (data[i] != null)
                    return false;
            } else if (!other.data[i].equals(this.data[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = getType().hashCode();
        int l = mapper == null ? -1 : mapper.getMaxValue();
        for (int i = 0; i <= l; i++) {
            if (data[i] != null) {
                h = 31 * h + data[i].hashCode();
            }
        }
        return h;
    }
}
