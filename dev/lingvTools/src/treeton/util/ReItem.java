/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ReItem {

    public static final String sFlagCaseSensitive = "CASE_SENSITIVE";
    public final static String fieldDelimiter = "@";
    public final static int fieldDelimiterLength = fieldDelimiter.length();
    public String strMatch;
    public String strReplace;
    public Pattern ptrn;
    public boolean needRecompilePattern;
    /**
     * Имя группы
     */
    protected String group;
    /**
     * Шаблон
     */
    protected String rule;
    /**
     * Правило вычисления лексемы
     */
    protected String base;
    /**
     * Возврящаемые атрибуты
     */
    protected String attribs;
    /**
     * Дополнительные требования и служебные атрибуты
     */
    protected String extra;
    /**
     * Пояснения
     */
    protected String comment;
    /**
     * Ссылка на родительский список правил (контейнер правил).
     * Эта ссылка необходима для того, чтобы при работе с
     * несколькими контейнерами для данного правила
     * всегда можно было определить родительский контейнер.
     */
    protected ReWalker container;
    /**
     * Номер строки в родительском списке правил (контейнере).
     */
    protected long row;
    /**
     * Флаги в бинарном сочетании.<br>
     * См. java.util.regex.Pattern.compile(String regex, <b>int flags</b>).<br>
     * Возможные значения: CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ.
     * Пока используется только (CASE_INSENSITIVE | UNICODE_CASE) или (UNICODE_CASE).
     */
    protected int flags;

    public ReItem() {
        needRecompilePattern = true;
        flags = (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        row = -1L;
    }

    public String getGroup() {
        return group != null ? group : "";
    }

    public void setGroup(String group) {
        this.group = group != null ? group : "";
    }

    public String getRule() {
        return rule != null ? rule : "";
    }

    public void setRule(String rule) {
        this.rule = rule != null ? rule : "";
        ptrn = null;
        needRecompilePattern = true;
    }

    public String getBase() {
        return base != null ? base : "";
    }

    public void setBase(String base) {
        this.base = base != null ? base : "";
        ptrn = null;
        needRecompilePattern = true;
    }

    public String getAttribs() {
        return attribs != null ? attribs : "";
    }

    public void setAttribs(String attribs) {
        this.attribs = attribs != null ? attribs : "";
        ptrn = null;
        needRecompilePattern = true;
    }

    public String getExtra() {
        return extra != null ? extra : "";
    }

    public void setExtra(String extra) {
        this.extra = extra != null ? extra : "";
        ptrn = null;
        needRecompilePattern = true;
    }

    public String getComment() {
        return comment != null ? comment : "";
    }

    public void setComment(String comment) {
        this.comment = comment != null ? comment : "";
    }

    public ReWalker getContainer() {
        return container;
    }

    public ReItem setContainer(ReWalker container) {
        this.container = container;
        return this;
    }

    public long getRow() {
        return row;
    }

    public ReItem setRow(long row) {
        this.row = row;
        return this;
    }

    public int getFlags() {
        return flags;
    }

    public ReItem setFlags(int flags) {
        this.flags = flags;
        needRecompilePattern = true;
        return this;
    }

    public ReItem parse(String _s) {
        int curPos = 0;
        int sLen = _s.length();
        Object[] curVals;

        curVals = parsePartOfString(curPos, _s, sLen);
        curPos = ((Integer) curVals[0]).intValue();
        group = (String) curVals[1];

        curVals = parsePartOfString(curPos, _s, sLen);
        curPos = ((Integer) curVals[0]).intValue();
        rule = (String) curVals[1];

        curVals = parsePartOfString(curPos, _s, sLen);
        curPos = ((Integer) curVals[0]).intValue();
        base = (String) curVals[1];

        curVals = parsePartOfString(curPos, _s, sLen);
        curPos = ((Integer) curVals[0]).intValue();
        attribs = (String) curVals[1];

        curVals = parsePartOfString(curPos, _s, sLen);
        curPos = ((Integer) curVals[0]).intValue();
        extra = (String) curVals[1];

        comment = (curPos < sLen) ? _s.substring(curPos) : "";

        buildPattern();

        return this;
    }

    public void buildPattern() {
        if (getRule().length() > 0 &&
                !getGroup().startsWith(ReWalker.prefixComment)) {
            //
            // Строим шаблон (strMatch), который проверяется на совпадение со словом.
            // Потом, возможно, он будет строиться сложнее.
            //
            strMatch = new StringBuffer("^").append(rule).append("$").toString();

            //
            // Строим шаблон результата (strReplace).
            //
            StringBuffer sbr = new StringBuffer(base).append(fieldDelimiter).append(attribs);
            if (extra.length() > 0) {
                if (attribs.length() > 0) {
                    sbr.append(", ");
                }
                sbr.append(extra);
            }
            strReplace = sbr.toString();
            ptrn = null;
            try {
                ptrn = Pattern.compile(strMatch, flags);
            } catch (PatternSyntaxException ex) {
                System.out.println(ex.getMessage());
            }
        }
        needRecompilePattern = false;
    }

    // Используется функцией parse.
    // Возвращает массив, где
    //     элемент 0 - новое значение curPos,
    //     элемент 1 - содержимое очередной подстроки
    // Содержимое подстроки никогда не бывает null.
    protected Object[] parsePartOfString(int _curPos, String _s, int _sLen) {
        String curSubstr = "";
        if (_curPos < _sLen) {
            int delmPos = _s.indexOf(fieldDelimiter, _curPos);
            int endPos = (delmPos >= 0) ? delmPos : _sLen;
            curSubstr = _s.substring(_curPos, endPos);
            _curPos = endPos;
            if (delmPos >= 0) {
                _curPos += fieldDelimiterLength;
            }
        }
        return new Object[]{new Integer(_curPos), curSubstr};
    }

    public String compose() {
        StringBuffer rslt = new StringBuffer();
        if (getGroup().startsWith(ReWalker.prefixComment)) {
            rslt.append(getGroup());
        } else {
            rslt.append(getGroup()).append(fieldDelimiter).
                    append(getRule()).append(fieldDelimiter).
                    append(getBase()).append(fieldDelimiter).
                    append(getAttribs()).append(fieldDelimiter).
                    append(getExtra()).append(fieldDelimiter).
                    append(getComment());
        }
        return rslt.toString();
    }

    public Matcher matcher(String what) {
        Matcher rslt = null;
        if (needRecompilePattern) {
            buildPattern();
        }
        if (ptrn != null) {
            Matcher m = ptrn.matcher(what);
            if (m.find()) {
                rslt = m;
            }
        }
        return rslt;
    }

    public String getTreeString() {
        return (getGroup().length() == 0) ? getRule() :
                new StringBuffer(getGroup()).append(fieldDelimiter).
                        append(getRule()).toString();

    }
}
