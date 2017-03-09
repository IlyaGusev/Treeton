/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

import java.util.ArrayList;


public class ReMatchNode {
    public ReMatchNode parent;
    public int level;
    public ReItem re;
    public String base;
    public String attribs;
    public ArrayList reqs;
    public boolean success;

    public ReMatchNode() {
        success = false;
    }

    public void add(ReMatchNode node) {
        if (reqs == null) {
            reqs = new ArrayList();
        }
        reqs.add(node);
    }

    public ReMatchNode get(int i) {
        ReMatchNode rslt = null;
        if (reqs != null && i >= 0 && reqs.size() > i) {
            rslt = (ReMatchNode) reqs.get(i);
        }
        return rslt;
    }

    public void clear() {
        if (reqs != null) {
            reqs.clear();
        }
    }

    public int size() {
        return (reqs != null) ? reqs.size() : 0;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public ReMatchNode getRoot() {
        return isRoot() ? this : parent.getRoot();
    }

    public ReMatchNode getFirst() {
        return isRoot() ? this :
                parent.isRoot() ? this : parent.getFirst();
    }

    public String getReqsString() {
        String rslt = null;
        if (attribs != null) {
            int reqsPos = attribs.indexOf(ReWalker.reqsAttrib);
            if (reqsPos >= 0) {
                int begPos = reqsPos + ReWalker.reqsAttrib.length();
                int endPos = attribs.indexOf(",", begPos);
                endPos = (endPos < 0) ? attribs.length() : endPos;
                rslt = attribs.substring(begPos, endPos).trim();
                if (rslt.length() == 0) {
                    rslt = null;
                }
            }
        }
        return rslt;
    }
}
