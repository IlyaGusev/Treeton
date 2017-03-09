/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.util.ObjectPair;
import treeton.core.util.Utils;
import treeton.dict.Dictionary;
import treeton.morph.MorphInterface;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class MorphInterfaceBenchMarkingTool {
    private List<MorphInterface> morphInterfaces = new ArrayList<MorphInterface>();
    private List<Dictionary> dictionaries = new ArrayList<Dictionary>();
    private MorphInterface goldStandard;
    private Dictionary goldDictionary;

    private Set<String> ignoredProperties = new HashSet<String>();

    public void addMorphInterface(MorphInterface morphInterface, Dictionary dictionary) {
        morphInterfaces.add(morphInterface);
        dictionaries.add(dictionary);
    }

    public MorphInterface getGoldStandard() {
        return goldStandard;
    }

    public void setGoldStandard(MorphInterface goldStandard) {
        this.goldStandard = goldStandard;
    }

    public Dictionary getGoldDictionary() {
        return goldDictionary;
    }

    public void setGoldDictionary(Dictionary goldDictionary) {
        this.goldDictionary = goldDictionary;
    }

    public void benchMark(Iterator<String> words, Writer buf) throws IOException {
        int columnWidth = 100 / (morphInterfaces.size() + 1);

        while (words.hasNext()) {
            String word = words.next();

            List<Properties> gold;
            try {
                gold = new ArrayList<Properties>();
                Collection<Properties> c = goldStandard.processOneWord(word, goldDictionary);
                if (c != null) {
                    gold.addAll(c);
                }
            } catch (Throwable e) {
                Properties props = new Properties();
                String s = e.toString();
                props.setProperty("Exception!!!", s.length() > 20 ? s.substring(0, 20) : s);
                gold = Arrays.asList(props);
            }

            boolean needToWrite = false;

            List<List<Properties>> list = new ArrayList<List<Properties>>();
            int maxRows = 0;

            for (int i = 0; i < morphInterfaces.size(); i++) {
                MorphInterface morphInterface = morphInterfaces.get(i);
                Dictionary dict = dictionaries.get(i);

                Collection<Properties> propsCollection;

                try {
                    propsCollection = morphInterface.processOneWord(word, dict);
                } catch (Throwable e) {
                    Properties props = new Properties();
                    String s = e.toString();
                    props.setProperty("Exception!!!!", s.length() > 20 ? s.substring(0, 20) : s);
                    propsCollection = Arrays.asList(props);
                }

                ObjectPair<List<Properties>, List<Integer>> pair = sortRelativeToGoldStandard(gold, propsCollection);

                if (!needToWrite) {
                    for (Integer integer : pair.getSecond()) {
                        if (integer == null || integer > 0) {
                            needToWrite = true;
                            break;
                        }
                    }
                }

                list.add(pair.getFirst());

                if (maxRows < pair.getFirst().size()) {
                    maxRows = pair.getFirst().size();
                }
            }

            if (needToWrite) {
                buf.append("<tr><td><b>").append(word).append("</b></td></td>");
                buf.append("<tr><td><table border=\"1\" width=\"100%\">");
                for (int i = 0; i < maxRows; i++) {
                    buf.append("<tr>");
                    Properties gp = gold.size() > i ? gold.get(i) : null;
                    buf.append("<td width=\"");
                    buf.append(String.valueOf(columnWidth));
                    buf.append("%\">");
                    if (gp != null)
                        appendProperties(buf, gp, gp);
                    buf.append("</td>");

                    for (List<Properties> props : list) {
                        Properties p = props.size() > i ? props.get(i) : null;
                        if (p != null) {
                            buf.append("<td width=\"");
                            buf.append(String.valueOf(columnWidth));
                            buf.append("%\">");
                            appendProperties(buf, p, gp);
                            buf.append("</td>");
                        } else {
                            buf.append("<td bgcolor=\"red\" width=\"");
                            buf.append(String.valueOf(columnWidth));
                            buf.append("%\">&nbsp;</td>");
                        }
                    }

                    buf.append("</tr>");
                }
                buf.append("</table></td><tr>");
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private void appendProperties(Writer buf, Properties props, Properties gold) throws IOException {
        Set allKeys = new HashSet(props.keySet());
        if (gold != null)
            allKeys.addAll(gold.keySet());

        Object[] arr = allKeys.toArray(new Object[allKeys.size()]);

        Arrays.sort(arr);

        for (int i = 0; i < arr.length; i++) {
            Object k = arr[i];
            Object v = props.get(k);

            if (i > 0) {
                buf.append("; ");
            }

            if (gold != null && gold.containsKey(k)) {
                if (Utils.smartEquals(v, gold.get(k)) || ignoredProperties.contains(k.toString()) && props.containsKey(k)) {
                    buf.append(k.toString()).append(" = ").append(v == null ? "null" : v.toString());
                } else {
                    if (props.containsKey(k)) {
                        buf.append(k.toString()).append(" = ").append("<font color=\"red\">").append(v == null ? "null" : v.toString()).append("</font>");
                    } else {
                        v = gold.get(k);
                        if (!ignoredProperties.contains(k.toString())) {
                            buf.append("<strike><font color=\"red\">").append(k.toString()).append(" = ").append(v == null ? "null" : v.toString()).append("</font></strike>");
                        } else {
                            buf.append("<strike>").append(k.toString()).append(" = ").append(v == null ? "null" : v.toString()).append("</strike>");
                        }
                    }
                }
            } else {
                if (!ignoredProperties.contains(k.toString())) {
                    buf.append("<font color=\"red\">").append(k.toString()).append(" = ").append(v == null ? "null" : v.toString()).append("</font>");
                } else {
                    buf.append(k.toString()).append(" = ").append(v == null ? "null" : v.toString());
                }
            }
        }
    }

    private int countDistance(Properties p1, Properties p2) {
        int d = 0;

        for (Map.Entry<Object, Object> entry : p1.entrySet()) {
            Object k = entry.getKey();

            if (ignoredProperties.contains(k.toString()))
                continue;

            Object v1 = entry.getValue();

            Object v2 = p2.get(k);

            if (!Utils.smartEquals(v1, v2)) {
                d++;
            }
        }

        for (Map.Entry<Object, Object> entry : p2.entrySet()) {
            Object k = entry.getKey();

            if (ignoredProperties.contains(k.toString()))
                continue;

            if (!p1.containsKey(k)) {
                d++;
            }
        }

        return d;
    }

    private ObjectPair<List<Properties>, List<Integer>> sortRelativeToGoldStandard(List<Properties> goldStandard, Collection<Properties> props) {
        List<Properties> resProps = new ArrayList<Properties>();
        List<Properties> sourceProps = new ArrayList<Properties>(props);
        List<Integer> resDistances = new ArrayList<Integer>();

        int sz = goldStandard.size();

        for (int i = 0; i < sz; i++) {
            resProps.add(null);
            resDistances.add(null);
        }

        int[][] matrix = new int[goldStandard.size()][props.size()];

        for (int i = 0; i < goldStandard.size(); i++) {
            Properties p1 = goldStandard.get(i);

            for (int j = 0; j < sourceProps.size(); j++) {
                Properties p2 = sourceProps.get(j);

                matrix[i][j] = countDistance(p1, p2);
            }
        }

        while (sz > 0) {
            int mini = Integer.MAX_VALUE, minj = Integer.MAX_VALUE, mind = Integer.MAX_VALUE;

            for (int i = 0; i < goldStandard.size(); i++) {
                for (int j = 0; j < sourceProps.size(); j++) {
                    if (sourceProps.get(j) == null)
                        continue;

                    int d = matrix[i][j];

                    if (d < mind) {
                        mind = d;
                        mini = i;
                        minj = j;
                    }
                }
            }

            if (mind == Integer.MAX_VALUE)
                return new ObjectPair<List<Properties>, List<Integer>>(resProps, resDistances);

            resProps.set(mini, sourceProps.get(minj));
            resDistances.set(mini, mind);
            sourceProps.set(minj, null);

            sz--;
        }

        for (Properties p : sourceProps) {
            if (p != null) {
                resProps.add(p);
                resDistances.add(Integer.MAX_VALUE);
            }
        }

        return new ObjectPair<List<Properties>, List<Integer>>(resProps, resDistances);
    }

    public void addIgnoredProperty(String property) {
        ignoredProperties.add(property);
    }
}
