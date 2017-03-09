/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.dict;

import treeton.core.util.sut;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class UriTable {
    Map<Integer, String[]> map = new HashMap<Integer, String[]>();
    Map<String, String> uniquenamespaces = new HashMap<String, String>();
    Map<String, Map<String, Integer>> namespaces = new HashMap<String, Map<String, Integer>>();

    private int counter = 0;

    public URI get(int n) {
        String[] arr = map.get(n);
        if (arr == null)
            return null;
        try {
            return new URI(arr[0] + arr[1]);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Something wrong in this world", e);
        }
    }

    public int addURI(String uri) {
        int i = uri.lastIndexOf('#');

        String fragment = i == -1 || i == uri.length() - 1 ? "" : uri.substring(i + 1);

        String namespace = uri.substring(0, uri.length() - fragment.length());

        Map<String, Integer> fragments = namespaces.get(namespace);

        if (fragments == null) {
            fragments = new HashMap<String, Integer>();
            i = counter++;
            fragments.put(fragment, i);
            namespaces.put(namespace, fragments);
            uniquenamespaces.put(namespace, namespace);
            map.put(i, new String[]{namespace, fragment});
            return i;
        } else {
            Integer integer = fragments.get(fragment);
            if (integer == null) {
                integer = counter++;
                fragments.put(fragment, integer);
                namespace = uniquenamespaces.get(namespace);
                map.put(integer, new String[]{namespace, fragment});
            }

            return integer;
        }
    }

    public byte[] getByteRepresentation() {
        int size = 0;
        size += 4; //Number of namespaces

        for (Map.Entry<String, Map<String, Integer>> e : namespaces.entrySet()) {
            size += 4; //Namespace name length
            size += e.getKey().length() * 2;
            size += 4; //Number of fragments;
            for (Map.Entry<String, Integer> e1 : e.getValue().entrySet()) {
                size += 4; //Fragment name length;
                size += e1.getKey().length() * 2;
                size += 4; //integer value
            }
        }

        ByteBuffer buf = ByteBuffer.allocate(size);

        buf.putInt(namespaces.size());

        for (Map.Entry<String, Map<String, Integer>> e : namespaces.entrySet()) {
            sut.putString(buf, e.getKey());
            buf.putInt(e.getValue().size());
            for (Map.Entry<String, Integer> e1 : e.getValue().entrySet()) {
                sut.putString(buf, e1.getKey());
                buf.putInt(e1.getValue());
            }
        }

        return buf.array();
    }

    public int readInFromBytes(byte[] arr, int from) {
        int n = sut.getIntegerFromBytes(arr, from);
        from += 4;
        for (int i = 0; i < n; i++) {
            String namespace = sut.readInStringFromBytes(arr, from);
            from += 4 + namespace.length() * 2;

            uniquenamespaces.put(namespace, namespace);

            HashMap<String, Integer> mp = new HashMap<String, Integer>();

            int nf = sut.getIntegerFromBytes(arr, from);
            from += 4;
            for (int j = 0; j < nf; j++) {
                String fragment = sut.readInStringFromBytes(arr, from);
                from += 4 + fragment.length() * 2;
                int val = sut.getIntegerFromBytes(arr, from);
                from += 4;

                if (counter <= val) {
                    counter = val + 1;
                }

                mp.put(fragment, val);
                map.put(val, new String[]{namespace, fragment});
            }

            namespaces.put(namespace, mp);
        }
        return from;
    }
}
