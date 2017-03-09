/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.Treenotation;
import treeton.core.TreenotationStorage;
import treeton.core.TypeIteratorInterface;
import treeton.core.util.TreetonEnvironment;

import java.util.*;

public class FuglyTemplate extends ScapeRHSJavaWrapper {
    public TreetonEnvironment env = TreetonEnvironment.getInstance();
    public ScapeBinding b;
    public ScapeBinding all;

    protected void execute() throws Exception {
        Map<String, String> map = (Map<String, String>) env.getProperty("SingleSyllablesTable");
        if (map == null) {
            map = new HashMap<String, String>();
            env.setProperty("SingleSyllablesTable", map);
        }
        Treenotation t = b.getTrn(0);

        TreenotationStorage treenotationStorage = t.getStorage();

        TypeIteratorInterface it = treenotationStorage.typeIterator(treenotationStorage.getTypes().get("Gramm"), t.getStartToken(), t.getEndToken());
        System.out.println(t.getText());

        Set<String> poses = new HashSet<String>();

        while (it.hasNext()) {
            Treenotation trn = (Treenotation) it.next();
            Object o = trn.get("POS");
            if (o != null) {
                poses.add(o.toString());
            }
        }
        String[] strings = poses.toArray(new String[poses.size()]);
        Arrays.sort(strings);

        StringBuffer buf = new StringBuffer();

        for (String s : strings) {
            buf.append(s);
        }

        map.put(t.getText(), buf.toString());
    }
}