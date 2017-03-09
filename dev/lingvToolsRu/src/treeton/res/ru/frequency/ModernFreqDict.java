/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru.frequency;

import java.io.*;
import java.util.*;

/**
 * Новый частотный словарь русской лексики. О. Н. Ляшевская, С. А. Шаров
 * <p>
 * Парсер таблицы по адресу:
 * http://dict.ruslang.ru/freq.php
 * I. Общая лексика
 * Частотный список лемм
 * <p>
 * Таблица была копипастнута из браузера в текстовый файл.
 */
public class ModernFreqDict implements FreqDict {
    private static Map<String, String> posmapping = new HashMap<String, String>();

    static {
        Map<String, String> m = posmapping;
        m.put("s", "N"); // существительное
        m.put("v", "V"); // глагол
        m.put("a", "A"); // прилагательное
        m.put("adv", "ADV"); // наречие
        m.put("part", "PCL"); // частица
        m.put("pr", "PREP"); // предлог
        m.put("advpro", "ADV"); // местоименное наречие
        m.put("conj", "CONJ"); // союз
        m.put("num", "NUM"); // числительное
        m.put("apro", "A"); // местоименное прилагательное
        m.put("intj", "INTJ");
        m.put("spro", "N"); // местоименное существительное
        m.put("anum", "A");
    }

    public Map<String, Collection<Stat>> base2stat = new LinkedHashMap<String, Collection<Stat>>();

    public ModernFreqDict(File file, Integer nMostFrequent) throws IOException {
        this(new FileInputStream(file), nMostFrequent);
    }

    public ModernFreqDict(File file) throws IOException {
        this(file, null);
    }

    public ModernFreqDict(InputStream inputStream) throws IOException {
        this(inputStream, null);
    }

    public ModernFreqDict(InputStream inputStream, Integer nMostFrequent) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "CP1251"));
        try {
            String line;
            int i = 0;
            while (null != (line = in.readLine())) {
                if (nMostFrequent != null && i++ > nMostFrequent)
                    break;

                if (line.startsWith("//"))
                    continue;
                String[] split = line.split("\t");
                String word = split[1].trim();
                Collection<Stat> stats = base2stat.get(word);
                if (stats == null)
                    base2stat.put(word, stats = new TreeSet<Stat>());
                String pos = split[2].trim();
                assert posmapping.containsKey(pos) : "unmaped pos: " + pos;
                if (!stats.add(new Stat(Double.valueOf(split[3].trim()), pos)))
                    assert false : "Different entries of " + word + " are mapped to the same pos.";
            }
        } finally {
            in.close();
        }
    }

    public static void main(String[] args) throws IOException {
        File file = new File("freq_words/freq_dict.txt");
        ModernFreqDict freqDict = new ModernFreqDict(file);
        for (String word : Arrays.asList("нищий", "хохолок", "по", "рассматривать", "а", "для", "какой", "тот", "этот", "пирожок", "есть")) {
            Collection<Stat> stats = freqDict.base2stat.get(word);
            if (stats == null)
                System.out.println(word + " ?");
            else {
                System.out.println(word);
                for (Stat stat : stats) {
                    System.out.println("\t" + stat.pos + " " + stat.ipm);
                }
            }
        }

    }

    public double getIPM(String base, String pos) {
        Collection<Stat> stats = base2stat.get(base);
        if (stats == null || stats.isEmpty())
            return 0.0;
        for (Stat stat : stats) {
            if (posmapping.get(stat.pos).equals(pos))
                return stat.ipm;
        }
        return 0.0;
    }

    public double getMaxIPM(String base) {
        Collection<Stat> stats = base2stat.get(base);
        if (stats == null || stats.isEmpty())
            return 0.0;
        return stats.iterator().next().ipm;
    }

    public String getMaxIPMPos(String base) {
        Collection<Stat> stats = base2stat.get(base);
        if (stats == null || stats.isEmpty())
            return null;
        return stats.iterator().next().pos;
    }

    public Iterator<String> wordIterator() {
        return base2stat.keySet().iterator();
    }

    public List<String> getWordList(Set<String> pos, Double minIPM, Double maxIPM, Set<String> stopWords) {
        List<String> words = new ArrayList<String>();
        for (Map.Entry<String, Collection<ModernFreqDict.Stat>> entry : base2stat.entrySet()) {
            String word = entry.getKey();
            if (stopWords != null && stopWords.contains(word))
                continue;
            if (!checkPosAndIPM(entry.getValue(), pos, minIPM, maxIPM))
                continue;

            words.add(word);
        }
        return words;
    }

    private boolean checkPosAndIPM(Collection<Stat> value, Set<String> pos, Double minIPM, Double maxIPM) {
        for (ModernFreqDict.Stat stat : value) {
            if (pos != null && !pos.contains(stat.getPos(false)))
                continue;
            if (minIPM != null && stat.getIpm() <= minIPM)
                continue;
            if (maxIPM != null && stat.getIpm() > maxIPM)
                continue;
            return true;
        }
        return false;
    }

    public class Stat implements Comparable<Stat> {
        double ipm;
        String pos;

        Stat(double ipm, String pos) {
            this.ipm = ipm;
            this.pos = pos;
        }

        public double getIpm() {
            return ipm;
        }

        public String getPos(boolean treetonNotation) {
            return treetonNotation ? posmapping.get(pos) : pos;
        }

        public int compareTo(Stat o) {
            return this.ipm < o.ipm ? 1 : (this.ipm > o.ipm ? -1 : this.pos.compareTo(o.pos));
        }
    }
}
