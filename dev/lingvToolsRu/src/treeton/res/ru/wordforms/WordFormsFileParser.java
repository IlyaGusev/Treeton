/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru.wordforms;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordFormsFileParser implements Iterator<WordFormsFileParser.Entry> {
    private static final Logger logger = Logger.getLogger(WordFormsFileParser.class);

    private static final String letter = "[А-ЯЁа-яё-]";
    private static final String aux = "[A-Za-z\\.\\d]";
    private static final String f1 = letter + "+";
    //    private static final String f2 = "[\\d.,]+ (\\([^\\)]+\\) )?("+letter+"+\\.?( "+letter+"+)?( \\d+\\**)?)[^\"]*(\"[^\",]*\"[^\"]*)*";
    private static final String f2 = "[\\d.,]+ (\\([^\\)]+\\) )?(.+)";
    private static final String f3 = "[^\"]*";
    private static final String f4 = "[^\"]*";

    private static final Pattern common = Pattern.compile("\"(" + f1 + ")\",\"" + f2 + "\",\"(" + f3 + ")\",\"(" + f4 + ")\"");
    private static final Pattern wordfrom = Pattern.compile("\\*?" + f1 + "(\\(ши\\))?");
    private static final Pattern aux1 = Pattern.compile("(" + aux + " *)+");
    private final boolean formsAsSet;
    private BufferedReader reader;
    private Entry current;

    public WordFormsFileParser(File f, boolean formsAsSet) throws FileNotFoundException {
        this.formsAsSet = formsAsSet;
        reader = new BufferedReader(new FileReader(f));
        getNext();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        reader.close();
    }

    private void getNext() {
        current = null;

        while (true) {
            try {
                String s = reader.readLine();
                if (s == null) //EOF
                    return;
                current = new Entry(s);
                return;
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    public boolean hasNext() {
        return current != null;
    }

    public Entry next() {
        Entry s = current;
        getNext();
        return s;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public class Entry {
        private String base, zindex;
        private Collection<String> forms = formsAsSet ? new LinkedHashSet<String>() : new ArrayList<String>();

        public Entry(String s) throws Exception {
            Matcher m = common.matcher(s);
            if (!m.matches())
                throw new Exception("String doesn't match template: " + s);
            base = m.group(1);
            zindex = ZindexParser.extractZindex(m.group(3));
            if (zindex == null)
                throw new ParseException("Couldn't extract zindex: " + s, 0);

            String wordforms = m.group(5);

            if (wordforms.isEmpty()) {
                forms.add(base);
            } else {
                for (String s1 : wordforms.split(",")) {
                    s1 = s1.trim();
                    if (wordfrom.matcher(s1).matches() && !"-".equals(s1)) {
                        if (s1.startsWith("*"))
                            s1 = s1.substring(1);
                        if (s1.endsWith("(ши)")) {
                            s1 = s1.substring(0, s1.length() - 4);
                            forms.add(s1 + "ши");

                        }
                        forms.add(s1);
                    } else if (aux1.matcher(s1).matches()) {
                        //nothing
                    } else
                        throw new Exception("String doesn't match template: " + s1 + " (" + s + ")");
                }
            }

        }

        public String getBase() {
            return base;
        }

        public String getZindex() {
            return zindex;
        }

        public Collection<String> getForms() {
            return forms;
        }
    }

}
