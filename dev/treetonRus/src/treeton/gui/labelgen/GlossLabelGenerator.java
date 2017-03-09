/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.TString;
import treeton.core.Treenotation;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.model.TreetonModelException;

import java.util.regex.Pattern;

public class GlossLabelGenerator
        implements TrnLabelGenerator {

    final StringBuffer curHypoStr = new StringBuffer();
    boolean baseInСaption;
    boolean html;
    boolean printSID;
    boolean showZINDEX;
    // существительное
    String[] attrNL = {
            "pos", "gend", "anim",
            "sem",
    };
    String[] attrNM = {
            "numb", "case"};
    // числительное
    String[] attrUL = {
            "pos",
    };
    String[] attrUM = {
            "numb", "case", "gend", "anim"};
    // прилагательное
    String[] attrAL = {
            "pos"};
    String[] attrAM = {
            "numb", "case", "gend", "anim",
            "pred", "grad"};
    // глагол
    String[] attrVL = {
            "pos", "perf", "tran", //Ts.AN_FREQ, Ts.AN_IMPERS todo
    };
    String[] attrVM = {
            "repr", "voice", "mood", "tens",
            "numb", "pers", "gend"};
    // причастие
    String[] attrPL = {
            "pos", "perf", "tran"};
    String[] attrPM = {
            "repr", "voice", "mood", "tens",
            "numb", "pers", "gend",
            "case", "anim", "pred", "grad"};
    // вводное слово
    String[] attrPredL = {
            "pos"};
    String[] attrPredM = {};
    // вводное слово
    String[] attrParL = {
            "pos"};
    String[] attrParM = {};
    // остальные части речи
    String[] attrXL = {
            "pos", "gcas"};
    String[] attrXM = {};
    String lt = html ? "&lt;" : "<";
    String gt = html ? "&gt;" : ">";
    String br = html ? "<br>\n" : "\n";
    String sub0 = html ? "<font size=\"-1\">[" : "[";
    String sub1 = html ? "]</font>" : "]";
    String b0 = html ? "<b>" : "";
    String b1 = html ? "</b>" : "";
    Pattern ptrnLt = Pattern.compile("<");
    Pattern ptrnGt = Pattern.compile(">");

    public GlossLabelGenerator() {
        this(true, false, false, true);
    }

    public GlossLabelGenerator(boolean baseInCaption, boolean showZINDEX, boolean html, boolean printSID) {
        this.baseInСaption = baseInCaption;
        this.html = html;
        this.printSID = printSID;
        this.showZINDEX = showZINDEX;
    }

    public static String getPropsByNames(Treenotation trn, String[] sa) {
        StringBuffer rslt = new StringBuffer();
        String curDelim = "";
        for (String aSa : sa) {
            Object val = trn.get(aSa);
            if (val != null) {
                rslt.append(curDelim).append(val.toString());
                curDelim = ", ";
            }
        }
        return rslt.toString();
    }

    public String generateCaption(Treenotation trn) {
        String tp = null;
        try {
            tp = trn.getType().getName();
        } catch (TreetonModelException e) {
            //
        }

        if ("gloss".equals(tp)) {
            if (trn.get("lem") == null && trn.get("pos") == null || !baseInСaption) {
                return "???";
            }

            Object o = trn.get("lem");
            String base = (o == null) ? "" : o.toString();
            if (printSID) {
                base = trn.getId() + ": " + base;
            }

            if (showZINDEX) {
                o = trn.get("flex");
                if (o != null) {
                    base = base + "<" + o + ">";
                }
            }

            return base;
        } else if ("syntax".equals(tp)) {
            TString str = (TString) trn.get("PHRASE");
            if (str == null) {
                str = (TString) trn.get("PGROUP");
            }
            String res = str == null ? "???" : str.toString();
            if (printSID) {
                res = trn.getId() + ": " + res;
            }

            return res;
        }
        return "";
    }

    public void init(TreenotationsContext context) {
    }

    public String generateLabel(Treenotation trn) {
        return generateMorphLabel(trn);
    }

    public String generateMorphLabel(Treenotation trn) {
        synchronized (curHypoStr) {
            if (trn.get("pos") == null) {
                return "";
            }

            String pos = trn.get("pos").toString();
            Object o = trn.get("TYPE");
            String TYPE = o == null ? null : o.toString();

            curHypoStr.setLength(0);

            if ("N".equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrNL)).append(")");
                String s = getPropsByNames(trn, attrNM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if ("PRED".equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrPredL)).append(")");
                String s = getPropsByNames(trn, attrPredM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if ("PARENTH".equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrParL)).append(")");
                String s = getPropsByNames(trn, attrParM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if ("NUM".equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrUL)).append(")");
                String s = getPropsByNames(trn, attrUM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if ("V".equals(pos)) {
                Object repr = trn.get("repr");
                if (repr != null && "part".equals(repr.toString())) {
                    curHypoStr.append("(").append(getPropsByNames(trn, attrPL)).append(")");
                    String s = getPropsByNames(trn, attrPM);
                    if (s.length() > 0) {
                        curHypoStr.append(" ").append(s);
                    }
                } else {
                    curHypoStr.append("(").append(getPropsByNames(trn, attrVL)).append(")");
                    String s = getPropsByNames(trn, attrVM);
                    if (s.length() > 0) {
                        curHypoStr.append(" ").append(s);
                    }
                }
            } else if ("A".equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrAL)).append(")");
                String s = getPropsByNames(trn, attrAM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if ("unknown".equals(TYPE)) {
                curHypoStr.append("<unknown>");
            } else {
                curHypoStr.append("(").append(getPropsByNames(trn, attrXL)).append(")");
                String s = getPropsByNames(trn, attrXM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            }
            return curHypoStr.toString();
        }
    }

    public void setPrintSID(boolean printSID) {
        this.printSID = printSID;
    }
}