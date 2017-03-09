/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import gate.AnnotationSet;
import gate.Document;
import treeton.core.Token;
import treeton.core.TreenotationStorage;
import treeton.core.scape.ParseException;
import treeton.core.util.FileMapper;
import treeton.core.util.sut;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ScapeProgram {
    static final int programKeywordNumber = 0;
    static final int executeKeywordNumber = 1;
    static char[][] keywords = new char[][]{
            {'p', 'r', 'o', 'g', 'r', 'a', 'm'},
            {'e', 'x', 'e', 'c'},
    };
    List<ScapePhase> phases = new ArrayList<ScapePhase>();
    ScapeApplication application;
    String name;
    ScapePackage scapePackage;

    ScapeProgram(URL source, ScapeApplication application) throws IOException, ParseException {
        this.application = application;
        char[] content = FileMapper.map2memory(source.getPath());
        int pl = 0;
        int endpl = content.length - 1;

        pl = readIn(content, pl, endpl);

        sut.skipSpacesEndls(content, pl, endpl);

        if (pl <= endpl) {
            throw new ParseException("Some unexpected characters at the end of the input stream", null, content, pl, endpl);
        }

    }

    static int readInName(char[] s, int pl, int endpl, StringBuffer buf) throws ParseException {
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);
        if (n == programKeywordNumber) {
            pl += keywords[programKeywordNumber].length;
        } else {
            throw new ParseException("missing program keyword", null, s, pl, endpl);
        }
        pl = sut.skipSpacesEndls(s, pl, endpl);
        int beg = pl;
        pl = sut.skipVarName(s, pl, endpl);
        if (pl == beg) {
            throw new ParseException("missing program name", null, s, pl, endpl);
        }
        buf.append(new String(s, beg, pl - beg));
        return pl;
    }

    public int readIn(char s[], int pl, int endpl) throws ParseException, IOException {
        StringBuffer buf = new StringBuffer();
        pl = ScapePhase.readInPackage(pl, s, endpl, buf);
        scapePackage = application.getPackage(buf.toString(), false);
        buf.setLength(0);
        pl = readInName(s, pl, endpl, buf);
        name = buf.toString();
        pl = sut.skipSpacesEndls(s, pl, endpl);
        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '{') {
            throw new ParseException("missing '{'", null, s, pl, endpl);
        }
        pl++;

        pl = sut.skipSpacesEndls(s, pl, endpl);
        int n = sut.checkDelims(s, pl, endpl, keywords);

        while (n != -1) {
            if (n == executeKeywordNumber) {
                pl += keywords[executeKeywordNumber].length;
                pl = sut.skipSpacesEndls(s, pl, endpl);
                buf.setLength(0);
                while (true) {
                    int beg = pl;
                    pl = sut.skipVarName(s, pl, endpl);
                    if (pl == beg) {
                        throw new ParseException("missing package or phase name", null, s, pl, endpl);
                    }
                    buf.append(new String(s, beg, pl - beg));
                    pl = sut.skipSpacesEndls(s, pl, endpl);
                    sut.checkEndOfStream(s, pl, endpl);
                    if (s[pl] == '.') {
                        pl++;
                        buf.append('.');
                    } else if (s[pl] == ';') {
                        break;
                    } else {
                        throw new ParseException("unexpected character '" + s[pl] + "'", null, s, pl, endpl);
                    }
                }

                URL location;
                try {
                    location = application.getPhaseLocation(buf.toString(), scapePackage);
                } catch (ScapeException e) {
                    throw new ParseException("Unknown phase " + buf, null, s, pl, endpl);
                }
                if (location == null) {
                    throw new ParseException("Unknown phase " + buf, null, s, pl, endpl);
                }

                char[] content = FileMapper.map2memory(location.getPath());
                ScapePhase phase = new ScapePhase(application.getTypes());
                int slashPlace = Math.max(location.getPath().lastIndexOf("\\"), location.getPath().lastIndexOf("/"));
                String javaDirString = location.getPath().substring(0, slashPlace + 1) + "_generated_" + location.getPath().substring(slashPlace + 1);
                javaDirString = javaDirString.replaceAll(".scape", "");
                File javaDir = new File(javaDirString);
                if (javaDir.exists() && !javaDir.isDirectory()) {
                    throw new RuntimeException(javaDir.getPath() + " must be a directory " + buf);
                }
                if (!javaDir.exists()) {
                    if (!javaDir.mkdir()) {
                        throw new RuntimeException(javaDir.getPath() + " could not be created " + buf);
                    }
                }

                if (!emptyDir(javaDir)) {
                    throw new RuntimeException("Unable to empty " + javaDir.getPath());
                }

                phase.setJavaContentLocation(javaDir);
                phase.readIn(content, 0, content.length - 1);
                phase.initialize();

                if (javaDir.listFiles().length == 0) {
                    if (!javaDir.delete()) {
                        throw new RuntimeException("Unable to delete " + javaDir.getPath());
                    }
                }
                phases.add(phase);

                if (s[pl] != ';') {
                    throw new ParseException("missing ';'", null, s, pl, endpl);
                }
                pl++;
            }
            pl = sut.skipSpacesEndls(s, pl, endpl);
            n = sut.checkDelims(s, pl, endpl, keywords);
        }

        sut.checkEndOfStream(s, pl, endpl);
        if (s[pl] != '}') {
            throw new ParseException("missing '}'", null, s, pl, endpl);
        }
        return pl + 1;
    }

    private boolean emptyDir(File javaDir) {
        File[] files = javaDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (!emptyDir(file)) {
                    return false;
                }
            }
            if (!file.delete()) {
                return false;
            }
        }
        return true;
    }

    public void applyTo(TreenotationStorage storage) {
        applyTo(storage, null, null);
    }

    public void applyTo(TreenotationStorage storage, Token from, Token to) {
        for (ScapePhase phase : phases) {
            phase.reset(storage, from, to);
            phase.execute();
            phase.reset(null);
        }
    }

    public void reset() {
        for (ScapePhase phase : phases) {
            phase.reset(null);
        }
    }

    public void applyTo(AnnotationSet anns, Document doc) {
        for (ScapePhase phase : phases) {
            phase.reset(anns, doc);
            phase.executeForGate();
            phase.reset(null, null);
        }
    }
}
