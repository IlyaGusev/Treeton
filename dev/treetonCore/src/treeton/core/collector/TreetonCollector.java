/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.collector;

import treeton.core.*;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnRelationType;
import treeton.core.model.TrnType;
import treeton.core.util.collector.Collectable;
import treeton.core.util.collector.Collector;
import treeton.core.util.collector.CollectorException;
import treeton.core.util.collector.Mutable;
import treeton.core.util.nu;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;

public class TreetonCollector extends Collector {

    private static final BlackBoard localboard = TreetonFactory.newBlackBoard(100, false);
    protected TreenotationsContext runtimeContext;

    public TreetonCollector(TreenotationsContext runtimeContext) {
        if (runtimeContext == null) throw new InvalidParameterException("runtimeContext is null");
        this.runtimeContext = runtimeContext;
    }

    public TreetonCollector(ByteBuffer buf, TreenotationsContext runtimeContext) {
        super(buf);
        if (runtimeContext == null) throw new InvalidParameterException("runtimeContext is null");
        this.runtimeContext = runtimeContext;
    }

    public TreetonCollector(File f, TreenotationsContext runtimeContext) throws IOException {
        super(f);
        if (runtimeContext == null) throw new InvalidParameterException("runtimeContext is null");
        this.runtimeContext = runtimeContext;
    }

    protected static void appendIntFeatureMap(Collector col, IntFeatureMap map, TrnType type) throws CollectorException {
        try {
            int s = map.size();
            col.put(s);
            for (int i = 0; i < s; i++) {
                int key = map.getKey(i);
                if (key < 0) throw new CollectorException("Wrong key");
                String name = type.getFeatureNameByIndex(key);
                col.put(name);
                Object val = map.get(key);
                col.put(val);
            }
        } catch (Exception e) {
            throw new CollectorException("Cannot append IntFeatureMap to collector!", e);
        }
    }

    protected static void readInIntFeatureMap(Collector col, IntFeatureMap map, TrnType type) throws CollectorException {
        synchronized (localboard) {
            readInBlackBoard(col, localboard, type);
            map.put(localboard);
        }
    }

    protected static void readInBlackBoard(Collector col, BlackBoard board, TrnType type) throws CollectorException {
        try {
            board.clean();
            int s = (Integer) col.get();
            for (int i = 0; i < s; i++) {
                String name = (String) col.get();
                int key = type.getFeatureIndex(name);
                if (key < 0)
                    throw new CollectorException("Wrong data");

                Object o = col.get();
                if (o != null)
                    board.put(key, o);
            }
        } catch (Exception e) {
            throw new CollectorException("Cannot readIn IntFeatureMap from collector!", e);
        }
    }

    protected void loadCollectables() {
        super.loadCollectables();
        loadCollectable(TrnType.class, new TrnTypeLightCollectable());
        loadCollectable(Treenotation.class, new TreenotationCollectable());
        loadCollectable(TreenotationImplHashable.class, new TreenotationHashableCollectable());
        loadCollectable(TrnRelationType.class, new TrnRelationTypeLightCollectable());
        loadCollectable(TString.class, new TStringCollectable());
        loadCollectable(nu.class, new NuCollectable());
    }

    protected boolean getByEqualsMode() {
        return true;
    }

    private class TreenotationCollectable extends Mutable {
        public Object newInstance(Collector col, Class c) throws IllegalAccessException, InstantiationException, CollectorException {
            TrnType tp = ((TrnType) col.get());
            return TreetonFactory.newTreenotation(null, null, tp);
        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            Treenotation trn = (Treenotation) o;
            TrnType type = trn.getType();
            col.put(type);
            TreetonCollector.appendIntFeatureMap(col, trn, type);
        }

        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            Treenotation trn = (Treenotation) o;
            TreetonCollector.readInIntFeatureMap(col, trn, trn.getType());
        }
    }

    private class TreenotationHashableCollectable extends TreenotationCollectable {
        public Object newInstance(Collector col, Class c) throws IllegalAccessException, InstantiationException, CollectorException {
            TrnType tp = (TrnType) col.get();
            return TreetonFactory.newTreenotationHashable(null, null, tp);
        }
    }

    private class TStringCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            put(new String(((TString) o).toCharArray()));
        }

        public Object readIn(Collector col) throws CollectorException {
            return TreetonFactory.newTString((String) col.get());
        }
    }

    private class NuCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            if (nu.ll.equals(o)) {
                col.put((byte) 0);
            } else if (nu.other.equals(o)) {
                col.put((byte) 1);
            } else {
                throw new CollectorException("Incorrect value of NU");
            }
        }

        public Object readIn(Collector col) throws CollectorException {
            byte n = (Byte) col.get();
            switch (n) {
                case 0:
                    return nu.ll;
                case 1:
                    return nu.other;
                default:
                    throw new CollectorException("Incorrect value for NU. Should be 0 or 1 but " + n);
            }
        }
    }

    public class TrnTypeLightCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            TrnType t = (TrnType) o;
            try {
                String name = t.getName();
                col.put(name);
            } catch (TreetonModelException e) {
                throw new CollectorException(e);
            }

        }

        public Object readIn(Collector col) throws CollectorException {
            String name = (String) col.get();
            try {
                return runtimeContext.getType(name);
            } catch (TreetonModelException e) {
                throw new CollectorException(e);
            }
        }
    }

    public class TrnRelationTypeLightCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            TrnRelationType t = (TrnRelationType) o;
            try {
                String name = t.getName();
                col.put(name);
            } catch (TreetonModelException e) {
                throw new CollectorException(e);
            }

        }

        public Object readIn(Collector col) throws CollectorException {
            String name = (String) col.get();
            try {
                return runtimeContext.getRelType(name);
            } catch (TreetonModelException e) {
                throw new CollectorException(e);
            }
        }

    }

}
