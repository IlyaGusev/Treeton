/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

public class ReWalkerArray {
    private ArrayList rews;

    private String encoding = null;

    public ReWalkerArray() {
        rews = new ArrayList();
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public ArrayList getWalkers() {
        return rews;
    }

    public ArrayList getHyposAsString(String word) {
        return getHyposAsString(new ArrayList(), word);
    }

    public ArrayList getHyposAsString(ArrayList a, String word) {
        a.clear();
        return addHyposAsString(a, word);
    }

    public ArrayList addHyposAsString(ArrayList a, String word) {
        Iterator itr = rews.iterator();
        while (itr.hasNext()) {
            ReWalker rew = (ReWalker) itr.next();
            ArrayList hps = rew.getHyposAsString(word);
            Iterator itr2 = hps.iterator();
            while (itr2.hasNext()) {
                a.add(itr2.next());
            }
        }
        return a;
    }

    public ArrayList getHyposAsProps(String word) {
        return getHyposAsProps(new ArrayList(), word);
    }

    public ArrayList getHyposAsProps(ArrayList a, String word) {
        a.clear();
        return addHyposAsProps(a, word);
    }

    public ArrayList addHyposAsProps(ArrayList a, String word) {
        Iterator itr = rews.iterator();
        while (itr.hasNext()) {
            ReWalker rew = (ReWalker) itr.next();
            ArrayList hps = rew.getHyposAsProps(word);
            Iterator itr2 = hps.iterator();
            while (itr2.hasNext()) {
                a.add(itr2.next());
            }
        }
        return a;
    }

    public boolean loadFromList(String fileList) {
        return loadFromList("", fileList);
    }

    public boolean loadFromList(String prefix, String fileList) {
        boolean success = false;
        rews.clear();
        StringTokenizer st = new StringTokenizer(fileList, ",");
        long id = 0L;
        while (st.hasMoreTokens()) {
            String curFileName = prefix + st.nextToken().trim();
            ReWalker r = encoding == null ? new ReWalker() : new ReWalker(encoding);
            r.addFromFile(curFileName);
            r.id = id++;
            rews.add(r);
        }
        return success;
    }


}
