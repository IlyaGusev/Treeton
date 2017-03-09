/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.scape.ParseException;
import treeton.core.scape.RegexpVariable;
import treeton.core.scape.ScapeVariable;
import treeton.core.util.sut;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

public class ScapeRHSJavaAction extends ScapeRHSAction {
    private static char[] keyword = "java".toCharArray();
    ScapeRHSJavaWrapper wrapper;
    String javaClassName;
    int id;

    protected ScapeRHSJavaAction(ScapeRule rule, int id) {
        super(rule);
        this.id = id;
    }

    public ScapeRHSActionResult buildResult() {
        ScapeRHSComplexActionResult result;
        wrapper.reset();
        try {
            wrapper.execute();
        } catch (Exception e) {
            e.printStackTrace();
            //todo
        }
        result = wrapper.getResult();
        wrapper.deinit();
        return result;
    }

    public int readIn(char[] s, int pl, int endpl) throws ParseException, IOException {
        pl = sut.skipSpacesEndls(s, pl, endpl);
        pl = sut.readInString(s, pl, endpl, keyword);
        pl = sut.skipSpacesEndls(s, pl, endpl);

        sut.checkEndOfStream(s, pl, endpl);

        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }

        int beg = pl;
        pl++;
        int cnt = 1;

        while (cnt > 0 && pl <= endpl) {
            if (s[pl] == '}') {
                cnt--;
            } else if (s[pl] == '{') {
                cnt++;
            }
            pl++;
        }

        if (pl > endpl && cnt != 0) {
            throw new ParseException("Unclosed '{'", null, s, pl, endpl);
        }

        String usersJava = new String(s, beg + 1, pl - beg - 2);
        javaClassName = rule.phase.name + "_" + rule.name + "_" + id;

        StringBuffer buf = new StringBuffer();

        buf.append("package treeton.scape;\n").
                append("\n").
                append("import java.util.*;\n").
                append("import java.lang.*;\n").
                append("import treeton.core.*;\n").
                append("import treeton.core.util.*;\n").
                append("import treeton.core.Treenotation;\n").
                append("import treeton.core.TreetonFactory;\n").
                append("import treeton.scape.ScapeBinding;\n").
                append("import treeton.core.scape.RegexpVariable;\n").
                append("\n").
                append("public class ").
                append(javaClassName).
                append(" extends ScapeRHSJavaWrapper {\n");

        buf.append("  public TreetonEnvironment env = TreetonEnvironment.getInstance();\n");

        for (String nm : rule.bindings.keySet()) {
            buf.append("  public ScapeBinding ").
                    append(nm).
                    append(";\n");
        }

        for (String nm : rule.regexBindings.keySet()) {
            buf.append("  public RegexpVariable ").
                    append(rule.bindings.containsKey(nm) ? "$" : "").
                    append(nm).
                    append(";\n");
        }

        buf.append("\n  protected void execute() throws Exception {").
                append(usersJava).
                append("}\n}");

        File outFile = new File(rule.phase.getJavaContentLocation().getPath() + "/" + javaClassName + ".java");
        FileOutputStream fos = new FileOutputStream(outFile);
        fos.write(buf.toString().getBytes("UTF-8"));

        return pl;
    }

    public void obtainClass(Class c) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        wrapper = (ScapeRHSJavaWrapper) c.newInstance();

        for (Map.Entry<String, ScapeVariable> e : rule.bindings.entrySet()) {
            Field f = c.getField(e.getKey());
            f.set(wrapper, e.getValue());
        }

        for (Map.Entry<String, RegexpVariable> e : rule.regexBindings.entrySet()) {
            String nm = e.getKey();
            if (rule.bindings.containsKey(nm)) {
                nm = "$" + nm;
            }
            Field f = c.getField(nm);
            f.set(wrapper, e.getValue());
        }
    }
}
