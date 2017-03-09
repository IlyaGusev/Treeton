/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReWalker {

    public static String defaultEncoding = "windows-1251"; // "IBM866";
    public static String lineTerminator = "\r\n";
    public static String reqsAttrib = "_REQS=";
    public static String prefixComment = "#";
    public String fileName;
    public ArrayList rules;
    public String encoding;
    public long id;
    public boolean isCaseSensitive = false;

    public boolean traceMatch;

    public ReWalker() {
        this(defaultEncoding);
    }

    public ReWalker(String _encoding) {
        encoding = _encoding;
        rules = new ArrayList();
        traceMatch = false;
    }

    public static Properties translateAttribString(String attribString) {
        Properties rslt = null;

        int iSpace = attribString.indexOf(' ');
        if (iSpace > 0) {
            String base = attribString.substring(0, iSpace);
            String attribsOnly = attribString.substring(iSpace + 1);
            StringTokenizer st = new StringTokenizer(attribsOnly, ",");
            while (st.hasMoreTokens()) {
                String tok = st.nextToken().trim();
                int iEq = tok.indexOf('=');
                if (iEq > 0) {
                    String name = tok.substring(0, iEq);
                    String val = tok.substring(iEq + 1);
                    if (name != null) {
                        name = name.trim();
                    }
                    if (val != null) {
                        val = val.trim();
                    }
                    if (name.length() > 0 &&
                            name.charAt(0) != '_' &&
                            val.length() > 0) {
                        if (rslt == null) {
                            rslt = new Properties();
                            rslt.setProperty("base", base);
                        }
                        rslt.setProperty(name, val);
                    }
                }
            }
        }
        return rslt;
    }

    public static void buildResult(ArrayList terms, ReMatchNode node) {
        if (node.size() == 0) {
            terms.add(node);
        } else {
            Iterator itr = node.reqs.iterator();
            while (itr.hasNext()) {
                buildResult(terms, (ReMatchNode) itr.next());
            }
        }
    }

    public static ReMatchNode[] buildPath(ReMatchNode node) {
        ReMatchNode[] rslt;
        ArrayList backPath = new ArrayList();
        ReMatchNode cur = node;
        while (cur.level > 0) {
            backPath.add(cur);
            cur = cur.parent;
        }
        int n = backPath.size();
        if (n > 0) {
            rslt = new ReMatchNode[n];
            n--;
            Iterator itr = backPath.iterator();
            while (itr.hasNext() && n >= 0) {
                rslt[n--] = (ReMatchNode) itr.next();
            }
        } else {
            rslt = null;
        }
        return rslt;
    }

    public static StringBuffer buildStringHypo(ReWalker rw, ReMatchNode[] path) {
        StringBuffer rslt = null;
        StringBuffer attribs = new StringBuffer();

        String attribsDelim = "";
        String attribsDelimNext = ", ";

        if (path != null && path.length > 0) {
            String base = null;
            if (path[0].success && path[path.length - 1].success) {
                for (int i = 0; i < path.length; i++) {
                    ReMatchNode cur = path[i];
                    if (base == null && cur.base != null && cur.base.length() > 0) {
                        base = cur.base;
                        if (!rw.isCaseSensitive) {
                            base = base.toLowerCase();
                        }
                    }
                    if (cur.attribs.length() > 0) {
                        attribs.append(attribsDelim).append(cur.attribs);
                        attribsDelim = attribsDelimNext;
                    }
                }
                if (base != null && base.length() > 0) {
                    rslt = new StringBuffer(base).append(" ").append(attribs);
                }
            }
        }
        return rslt;
    }

    public String getFileName() {
        return fileName;
    }

    public ReWalker setFileName(String _fileName) {
        fileName = _fileName;
        return this;
    }

    public void clear() {
        rules.clear();
    }

    public boolean loadFromFile(String fileName) {
        clear();
        return addFromFile(fileName);
    }

    public boolean addFromFile(String fileName) {
        boolean success = false;
        int curFileFlags =
                (Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        System.out.println("rulesFile: " + fileName);
        try {
            InputStream ist = new FileInputStream(fileName);
            InputStreamReader isr = new InputStreamReader(ist, encoding);
            BufferedReader brd = new BufferedReader(isr);
            String line;
            while ((line = brd.readLine()) != null) {
                if (line.startsWith(prefixComment)) {
                    if (line.substring(1).indexOf(ReItem.sFlagCaseSensitive) >= 0) {
                        curFileFlags = Pattern.UNICODE_CASE;
                        isCaseSensitive = true;
                    }
                }
                rules.add(new ReItem().parse(line).
                        setFlags(curFileFlags).setContainer(this).
                        setRow((long) rules.size()));
            }
            brd.close();
            isr.close();
            ist.close();
            success = true;
        } catch (NullPointerException ex1) {
        } catch (FileNotFoundException ex1) {
        } catch (UnsupportedEncodingException ex) {
        } catch (IOException ex2) {
        }
        return success;
    }

    public boolean saveToFile(String fileName) {
        boolean success = false;
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            OutputStreamWriter osr = new OutputStreamWriter(fos, encoding);
            Iterator itr = rules.iterator();
            StringBuffer sb = new StringBuffer();
            while (itr.hasNext()) {
                sb.setLength(0);
                sb.append(((ReItem) itr.next()).compose());
                sb.append(lineTerminator);
                osr.write(sb.toString());
            }
            osr.close();
            fos.close();
            success = true;
        } catch (NullPointerException ex1) {
        } catch (FileNotFoundException ex1) {
        } catch (UnsupportedEncodingException ex) {
        } catch (IOException ex2) {
            System.out.println("save file IOException");
        }
        return success;
    }

    public ArrayList findMatch(String _word) {
        ReMatchNode root = new ReMatchNode();
        root.level = 0;
        root.reqs = new ArrayList();
        root.success = false;
        findReqsForNode(_word, root, "");
        return root.reqs;
    }

    protected void findReqsForNode(String _word,
                                   ReMatchNode _node, String _group) {
        int childLevel = _node.level + 1;
        if (_node.level > 10) {
            return;
        }
        Iterator itr = rules.iterator();

        if (traceMatch) {
            System.out.println("RE -- Group: " + _group + " Word: " + _word);
        }

//    _node.success = false;
        while (itr.hasNext()) {
            ReItem ri = (ReItem) itr.next();
            if (ri.getGroup().equalsIgnoreCase(_group)) {
                Matcher m = ri.matcher(_word);
                if (traceMatch) {
                    if (ri.ptrn != null) {
                        System.out.println("RE -- Pattern 0 -- >>>" + ri.ptrn.pattern() + "<<<");
                    } else {
                        System.out.println("RE -- Pattern 0 -- null");
                    }
                }
                if (m != null) {

                    if (traceMatch) {
                        System.out.println("RE -- Pattern 1 -- matched");
                    }

                    String s = m.replaceAll(ri.strReplace);

                    if (traceMatch) {
                        System.out.println("RE -- Replace -- >>>" +
                                ri.strReplace + "<<< -- >>>" + s + "<<<");
                    }

                    int pos = s.indexOf(ReItem.fieldDelimiter);
                    if (pos >= 0) {
                        ReMatchNode child = new ReMatchNode();
                        child.re = ri;
                        child.base = s.substring(0, pos);
                        child.attribs = s.substring(pos + 1);
                        child.level = childLevel;
                        child.parent = _node;
                        child.success = false;
                        String allReqs = child.getReqsString();
                        if (allReqs != null) {
                            StringTokenizer st = new StringTokenizer(allReqs, "|");
                            while (st.hasMoreTokens()) {
                                String nextGroup = st.nextToken().trim();
                                findReqsForNode(_word, child, nextGroup);
                            }
                        } else {
                            child.success = true;
                        }
                        _node.add(child);
                        _node.success = _node.success || child.success;
                    }
                }
            }
        }
    }

    public ArrayList getHyposAsString(String _word) {
        ArrayList rslt = new ArrayList();
        ArrayList hypos = findMatch(_word);
        ArrayList terms = new ArrayList();

        Iterator itr = hypos.iterator();
        while (itr.hasNext()) {
            ReMatchNode node = (ReMatchNode) itr.next();
            buildResult(terms, node);
        }

        itr = terms.iterator();
        while (itr.hasNext()) {
            ReMatchNode node = (ReMatchNode) itr.next();
            ReMatchNode[] path = buildPath(node);
            StringBuffer sbHypo = buildStringHypo(this, path);
            if (sbHypo != null && sbHypo.length() > 0) {
                rslt.add(sbHypo.toString());
            }
        }
        return rslt;
    }

    public ArrayList getHyposAsProps(String _word) {
        ArrayList hypos = getHyposAsString(_word);
        ArrayList rslt = new ArrayList();

        Iterator itr = hypos.iterator();

        while (itr.hasNext()) {
            String s = (String) itr.next();
            rslt.add(translateAttribString(s));
        }
        return rslt;
    }

    public void printSpaces(int n) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
        System.out.print(sb.toString());
    }

}
