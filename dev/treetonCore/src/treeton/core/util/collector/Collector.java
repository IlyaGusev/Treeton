/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.collector;

import treeton.core.util.ObjectPair;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.*;

@SuppressWarnings({"unchecked"})
public class Collector {
    protected static final int SIZEOF_INT = Integer.SIZE >> 3;
    static final int FINISH = -2;
    static final int NULL = -1;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private HashMap<Class, Collectable> collectables = new HashMap<Class, Collectable>();
    private HashMap<Class, Integer> typeIndexes = new HashMap<Class, Integer>();
    private ArrayList<Class> classes = new ArrayList<Class>();
    private HashMap<Integer, ArrayList<Integer>> map = new HashMap<Integer, ArrayList<Integer>>();
    private ArrayList<Object> values = new ArrayList<Object>();
    private ByteBuffer buf;
    private boolean finished = false;

    public Collector() {
        this(ByteBuffer.allocateDirect(2048));
    }

    public Collector(ByteBuffer buf) {
        loadCollectables();
        this.buf = buf;
    }

    public Collector(File f) throws IOException {
        loadCollectables();
        InputStream is = new FileInputStream(f);
        byte[] arr = new byte[(int) f.length()];
        int len = is.read(arr, 0, arr.length);
        is.close();
        buf = ByteBuffer.allocateDirect(arr.length + 10);
        buf.put(arr);
        buf.position(0);
    }

    private void ensureAvailableSpace(int len) throws CollectorException {
        if (buf.position() + len >= buf.limit()) {
            ByteBuffer newBuffer;
            int pos = buf.position();
            buf.position(0);
            try {
                newBuffer = ByteBuffer.allocateDirect(buf.limit() * 2 + len + 1024);
            } catch (Throwable e) {
                throw new CollectorException(e);
            }
            newBuffer.put(buf);
            newBuffer.position(pos);
            buf.position(pos);
            buf = newBuffer;
        }
    }

    protected void loadCollectables() {
        loadCollectable(Integer.class, new IntegerCollectable());
        loadCollectable(Long.class, new LongCollectable());
        loadCollectable(Byte.class, new ByteCollectable());
        loadCollectable(String.class, new StringCollectable());
        loadCollectable(Enum.class, new EnumCollectable());
        loadCollectable(Boolean.class, new BooleanCollectable());
        loadCollectable(byte[].class, new ArrayByteCollectable());
        loadCollectable(int[].class, new ArrayIntCollectable());
        loadCollectable(Character.class, new CharacterCollectable());

        loadCollectable(Object[].class, new ArrayCollectable(Object.class));
        loadCollectable(Map.class, new MapCollectable());
        loadCollectable(Collection.class, new CollectionCollectable());
        loadCollectable(List.class, new ListCollectable());
        loadCollectable(Set.class, new SetCollectable());

        loadCollectable(URI.class, new URICollectable());
        loadCollectable(Class.class, new ClassCollectable());
        loadCollectable(ObjectPair.class, new ObjectPairCollectable());

    }

    public int loadCollectable(Class c, Collectable collectable) {
        int i = loadObjectCollectable(c, collectable);
        loadObjectCollectable(Array.newInstance(c, 0).getClass(), new ArrayCollectable(c));
        return i;
    }

    private int loadObjectCollectable(Class c, Collectable collectable) {
        if (collectables.containsKey(c))
            throw new InvalidParameterException("Class " + c.getSimpleName() + " is already loaded to collector");
        collectables.put(c, collectable);
        int type = classes.size();
        classes.add(c);
        typeIndexes.put(c, type);
        return type;
    }

    public ByteBuffer getBuf() {
        return buf;
    }

    public int put(Object o) throws CollectorException {
        while (buf.position() + 1024 > buf.limit()) {
            ByteBuffer newBuffer;
            int pos = buf.position();
            buf.position(0);
            try {
                newBuffer = ByteBuffer.allocateDirect(buf.limit() * 2 + 1024);
            } catch (Throwable e) {
                throw new CollectorException(e);
            }
            newBuffer.put(buf);
            newBuffer.position(pos);
            buf.position(pos);
            buf = newBuffer;
        }
        if (o == null) {
            int i = NULL;
            buf.putInt(i);
            return i;
        }

        int hashCode = getHashCode(o);
        ArrayList<Integer> ids = map.get(hashCode);   //i=getObjectId(o);
        Integer i = null;
        if (ids != null) {
            for (Integer id : ids) {
                if (equals(o, values.get(id))) {
                    i = id;
                    break;
                }
            }
        }
        if (i != null) {
            buf.putInt(i);
        } else {
            if (ids == null) {
                ids = new ArrayList<Integer>(1);
                map.put(hashCode, ids);
            } else {
//                collision
//                System.out.println("Collision");
            }
            i = values.size();
            values.add(o);
            //setObjectId(o, i);
            ids.add(i);
            buf.putInt(i);
            appendObject(o);
        }
        return i;
    }

    private boolean equals(Object o, Object o2) {
        if (o instanceof String && o2 instanceof String) return o.equals(o2);
        return o == o2;
    }

    protected int getHashCode(Object o) {
        if (o instanceof String) {
            return o.hashCode();
        }
        return System.identityHashCode(o);
    }

    public Object get() throws CollectorException {
        if (finished)
            throw new CollectorException("Collector finished reading");
        int i = buf.getInt();
        if (!buf.hasRemaining()) {
            finished = true;
        }
        if (i == NULL) {
            return null;
        } else if (i == FINISH) {
            finished = true;
            throw new CollectorException("Collector finished reading");
        } else if (i < values.size()) {
            return values.get(i);
        } else if (i == values.size()) {
            Object o;
            int id = values.size();
            values.add(null);
            o = readObject(id);
            return o;
        } else
            throw new CollectorException("Error id=" + Integer.toString(i) + " is not loaded!");
    }

    public int readContent(Object obj) throws CollectorException {
        if (finished)
            throw new CollectorException("Collector finished reading");
        int i = buf.getInt();
        if (i == NULL) {
            throw new CollectorException("No object!!!");
        } else if (i == FINISH || !buf.hasRemaining()) {
            throw new CollectorException("No object!!!");
        } else if (i < values.size()) {
            throw new CollectorException("Object was read before!!! Method is not appliable");
        } else if (i == values.size()) {
            int id = values.size();
            values.add(obj);
            return readObject(obj, id);
        } else {
            throw new CollectorException("Error id=" + Integer.toString(i) + " is not loaded!");
        }
    }

    private Integer getType(Class c) {
        Integer type = typeIndexes.get(c);
        Class sub = c;
        if (type == null) {
            for (Class iface : c.getInterfaces()) {
                type = getType(iface);
                if (type != null) break;
            }
        }
        while (type == null) {
            sub = sub.getSuperclass();
            if (sub == null) break;
            type = getType(sub);
        }
        return type;
    }

    private void appendObject(Object o) throws CollectorException {
        if (o == null) {
            putInt(NULL);
        } else {
            Class c = o.getClass();
            Integer type = getType(c);
            if (type == null) {
                throw new CollectorException("Don't know how to append " + c.getSimpleName());
            }
            putInt(type);
            Collectable collectable = collectables.get(classes.get(type));
            assert collectable != null;
            collectable.append(this, o);
        }
    }

    private void putInt(int i) throws CollectorException {
        ensureAvailableSpace(SIZEOF_INT);
        buf.putInt(i);
    }

    private Object readObject(int id) throws CollectorException {
        int type = buf.getInt();
        Object val = null;
        if (type != NULL) {
            try {
                if (type >= classes.size())
                    throw new CollectorException("Don't know how to read object with index  " + type);
                Class c = classes.get(type);
                Collectable collectable = collectables.get(c);
                assert collectable != null;
                if (collectable instanceof MutableCollectable) {
                    val = ((MutableCollectable) collectable).newInstance(this, c);
                    values.set(id, val);
                    ((MutableCollectable) collectable).readIn(this, val);
                } else {
                    val = collectable.readIn(this);
                    values.set(id, val);

                }
            } catch (IllegalAccessException e) {
                throw new CollectorException(e);
            } catch (InstantiationException e) {
                throw new CollectorException(e);
            }
        }

        return val;
    }

    private int readObject(Object obj, int id)
            throws CollectorException {
        int type = buf.getInt();
        if (type == NULL) {
            throw new CollectorException("No type!!!");
        } else {
            if (type >= classes.size())
                throw new CollectorException("Don't know how to read object with index  " + type);
            Class c = classes.get(type);
            if (!c.isAssignableFrom(obj.getClass())) {
                throw new CollectorException("Type of the target object doesn't match the serialized type");
            }

            Collectable collectable = collectables.get(c);
            assert collectable != null;
            if (collectable instanceof MutableCollectable) {
                values.set(id, obj);
                ((MutableCollectable) collectable).readIn(this, obj);
            } else {
                throw new CollectorException("Collectable pair for target object class is not Mutable");
            }
        }
        return buf.position();
    }

    public void save(File f) throws IOException {
        byte[] bytes = getBytes();
        OutputStream os = new FileOutputStream(f);
        os.write(bytes);
        os.close();
    }

    public byte[] getBytes() {
        int pos = buf.position();
        byte[] bytes = new byte[pos];
        buf.position(0);
        buf.get(bytes, 0, pos);
        buf.position(pos);
        return bytes;
    }

    public boolean isFinished() {
        return !buf.hasRemaining();
    }

    private class EnumCollectable implements Collectable<Enum> {
        public void append(Collector col, Enum o) throws CollectorException, ClassCastException {
            col.put(o.getDeclaringClass());
            col.put(o.toString());
        }

        @SuppressWarnings({"unchecked"})
        public Enum readIn(Collector col) throws CollectorException {
            Class<Enum> cls = (Class<Enum>) col.get();
            String name = (String) col.get();
            return Enum.valueOf(cls, name);
        }
    }

    private class StringCollectable implements Collectable {

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            byte[] b = ((String) o).getBytes(UTF8);
            col.ensureAvailableSpace(b.length + SIZEOF_INT);
            buf.putInt(b.length);
            buf.put(b);
        }

        public Object readIn(Collector col) throws CollectorException {
            int len = buf.getInt();
            byte[] cb = new byte[len];
            buf.get(cb);
            return new String(cb, UTF8);
        }
    }

    private class ArrayByteCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            byte[] b = (byte[]) o;
            col.ensureAvailableSpace(b.length + SIZEOF_INT);
            buf.putInt(b.length);
            buf.put(b);
        }

        public Object readIn(Collector col) throws CollectorException {
            int len = buf.getInt();
            byte[] cb = new byte[len];
            buf.get(cb);
            return cb;
        }
    }

    private class ArrayIntCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            int[] b = ((int[]) o);
            col.ensureAvailableSpace(SIZEOF_INT * (b.length + 1));

            buf.putInt(b.length);
            for (int i = 0; i < b.length; i++) buf.putInt(b[i]);
        }

        public Object readIn(Collector col) throws CollectorException {
            int len = buf.getInt();
            int[] b = new int[len];
            for (int i = 0; i < len; i++) b[i] = buf.getInt();
            return b;
        }
    }

    private class IntegerCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            putInt((Integer) o);
        }

        public Object readIn(Collector col) throws CollectorException {
            return buf.getInt();
        }
    }

    private class LongCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            col.ensureAvailableSpace(Long.SIZE >> 3);
            buf.putLong((Long) o);
        }

        public Object readIn(Collector col) throws CollectorException {
            return buf.getLong();
        }
    }

    private class ByteCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            col.ensureAvailableSpace(Byte.SIZE >> 3);
            buf.put((Byte) o);
        }

        public Object readIn(Collector col) throws CollectorException {
            return buf.get();
        }
    }

    private class CharacterCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            col.ensureAvailableSpace(Character.SIZE >> 3);
            buf.putChar((Character) o);
        }

        public Object readIn(Collector col) throws CollectorException {
            return buf.getChar();
        }
    }

    private class BooleanCollectable implements Collectable {
        static final byte TRUE = 1;
        static final byte FALSE = 0;

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            col.ensureAvailableSpace(1);
            buf.put((Boolean) o ? TRUE : FALSE);
        }

        public Object readIn(Collector col) throws CollectorException {
            byte v = buf.get();
            switch (v) {
                case TRUE:
                    return Boolean.TRUE;
                case FALSE:
                    return Boolean.FALSE;
                default:
                    throw new CollectorException("Bad boolean: must be " + TRUE + "/" + FALSE + " but found " + v);
            }
        }
    }

    private class MapCollectable extends Mutable<Map> {
        public void append(Collector col, Map c) throws CollectorException, ClassCastException {
            putInt(c.size());
            for (Object o : c.keySet()) {
                put(o);
                put(c.get(o));
            }
        }

        public void readIn(Collector col, Map o) throws CollectorException, ClassCastException {
            int size = buf.getInt();
            if (size < 0) throw new CollectorException("Length " + size + " is negative");
            for (int i = 0; i < size; i++)
                o.put(get(), get());
        }

        @Override
        public Map newInstance(Collector col, Class<? extends Map> c) throws IllegalAccessException, InstantiationException, CollectorException {
            return new HashMap();
        }
    }

    private class SetCollectable extends Mutable<Set> {
        public void append(Collector col, Set c) throws CollectorException, ClassCastException {
            putInt(c.size());
            for (Object o : c)
                put(o);
        }

        public void readIn(Collector col, Set o) throws CollectorException, ClassCastException {
            int size = buf.getInt();
            if (size < 0) throw new CollectorException("Length " + size + " is negative");
            for (int i = 0; i < size; i++)
                o.add(get());
        }

        @Override
        public Set newInstance(Collector col, Class<? extends Set> c) throws IllegalAccessException, InstantiationException, CollectorException {
            return new HashSet();
        }
    }

    protected class ArrayCollectable extends Mutable {
        Class clazz;

        public ArrayCollectable(Class clazz) {
            this.clazz = clazz;
        }

        protected ArrayCollectable() {
        }

        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            int size = Array.getLength(o);
            putInt(size);
            for (int i = 0; i < size; i++) {
                put(Array.get(o, i));
            }
        }

        public void readIn(Collector col, Object o) throws CollectorException, ClassCastException {
            int size = Array.getLength(o);
            for (int i = 0; i < size; i++)
                Array.set(o, i, get());
        }

        public Object newInstance(Collector col, Class c) throws IllegalAccessException, InstantiationException, CollectorException {
            int size = buf.getInt();
            if (size < 0) throw new CollectorException("Length " + size + " is negative");
            return newInstance(col, c, size);
        }

        protected Object newInstance(Collector col, Class c, int size) {
            return Array.newInstance(clazz, size);
        }
    }

    private class ListCollectable extends Mutable<List> {
        public void append(Collector col, List al) throws CollectorException, ClassCastException {
            putInt(al.size());
            for (Object o : al)
                put(o);
        }

        public void readIn(Collector col, List o) throws CollectorException, ClassCastException {
            int size = buf.getInt();
            if (size < 0) throw new CollectorException("Length " + size + " is negative");
            for (int i = 0; i < size; i++)
                o.add(get());
        }

        public List newInstance(Collector col, Class<? extends List> c) throws IllegalAccessException, InstantiationException, CollectorException {
            return new ArrayList();
        }
    }

    private class CollectionCollectable implements Collectable<Collection> {
        public void append(Collector col, Collection o) throws CollectorException, ClassCastException {
            putInt(o.size());
            for (Object e : o)
                put(e);
        }

        public Collection readIn(Collector col) throws CollectorException {
            ArrayList res = new ArrayList();
            int size = buf.getInt();
            if (size < 0) throw new CollectorException("Length " + size + " is negative");
            for (int i = 0; i < size; i++)
                res.add(get());
            return res;
        }
    }

    private class URICollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            col.put(o.toString());
        }

        public Object readIn(Collector col) throws CollectorException {
            try {
                return new URI((String) col.get());
            } catch (URISyntaxException e) {
                throw new CollectorException("URISyntaxException", e);
            }
        }
    }

    private class ClassCollectable implements Collectable {
        public void append(Collector col, Object o) throws CollectorException, ClassCastException {
            String s = ((Class) o).getName();
            col.put(s);
        }

        public Object readIn(Collector col) throws CollectorException {
            try {
                String name = (String) col.get();
                return ClassLoader.getSystemClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new CollectorException(e);
            }
        }
    }

    private class ObjectPairCollectable extends Mutable<ObjectPair> {
        public void readIn(Collector col, ObjectPair o) throws CollectorException, ClassCastException {
            Object o1 = col.get();
            Object o2 = col.get();
            o.setObjects(o1, o2);
        }

        public void append(Collector col, ObjectPair o) throws CollectorException, ClassCastException {
            col.put(o.getFirst());
            col.put(o.getSecond());
        }

        @Override
        public ObjectPair newInstance(Collector col, Class<? extends ObjectPair> c) throws IllegalAccessException, InstantiationException, CollectorException {
            return new ObjectPair(null, null);
        }
    }
}
