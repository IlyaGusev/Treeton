/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru.wordforms;

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZindexParser {
    // 1а, 1**а, 6***в/с'', 3а'~
    static final String indexPart = "\\d+\\**";
    //    private static final String f2 = "[\\d.,]+ (\\([^\\)]+\\) )?("+letter+"+\\.?( "+letter+"+)?( \\d+\\**)?)[^\"]*(\"[^\",]*\"[^\"]*)*";
    static final Set<String> noIndex = new HashSet<String>(Arrays.asList("вводн.", "межд.", "н", "предик.", "предл.", "союз", "сравн.", "част.", "числ."));
    private static final Logger logger = Logger.getLogger(ZindexParser.class);
    private static final String letter = "[/_а-яё-]";
    //    static final String prosodyPart = "[-'!~/a-zа-я]*";
//    static final String commentPart = "";
    // м, св, мо-жо, мн., част., св-нсв
    static final String mainPart1 = letter + "+\\.?";
    // ж ип 1, ж п 3, м мс 1, мн. ж 3*, мн. неод, мн. одуш, мн. со 1, мо жо 1, мо-жо жо 6*, мс-п п 3, нсв нп 1, п мс 6*, св-нсв нп 2
    // мн. _от_ м 4, мн. _от_ ж 1
    static final String mainPart2 = letter + "+\\.?";
    // мн. неод. п 1, мн. _от_ битк м 3*,
    static final String mainPart3 = letter + "+\\.?";
    static final String pat = "(" + mainPart1 + "),?( +" + mainPart2 + ")?( +" + mainPart3 + ")?( +" + indexPart + ")?" + ".*";
    static final Pattern p = Pattern.compile(pat);

    public static String extractZindex1(String zindex1) {
        String zindex = null;
        try {
            zindex = extractZindex(zindex1);
        } catch (ParseException e) {
            logger.warn(e);

        }
        if (zindex == null) {
            zindex1 = zindex1.replace('l', '1');
            zindex1 = zindex1.replace('З', '3');
            zindex1 = zindex1.replace("б", "6");
            zindex1 = zindex1.replace("_om_ ", "");
            zindex1 = zindex1.replace('c', 'с');
            try {
                zindex = extractZindex(zindex1);
            } catch (ParseException e) {
                logger.warn(e);
            }
        }
        return zindex;
    }

    public static String extractZindex(String s) throws ParseException {
        Matcher m = p.matcher(s);
        if (!m.matches())
            throw new ParseException("Couldn't extract zindex (doesn't match template): " + s, 0);

        String p1 = m.group(1);
        String p2 = m.group(2);
        String p3 = m.group(3);
        String index = m.group(4);

        if (index == null && !noIndex.contains(p1))
            throw new ParseException("Couldn't extract zindex (digital index missed): " + s, 0);

        return (convert(p1) + convert(p2) + convert(p3) + convert(index)).trim();
    }

    private static String convert(String s) {
        return s == null ? "" : s.trim() + " ";
    }
}
