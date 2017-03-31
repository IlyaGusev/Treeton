/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.lomonosov;

import treeton.gui.labelgen.TrnLabelGenerator;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.Treenotation;
import treeton.core.TString;
import treeton.util.TagSet;
import treeton.util.Ts;

import java.util.regex.Pattern;

public class GlossLabelGenerator implements TrnLabelGenerator {
    boolean baseInСaption;
    boolean html;
    boolean printSID;
    boolean showZINDEX;

    // существительное
    String[] attrNL = {
            "pos", "gend", "anim", Ts.AN_PRN,
            Ts.AN_PNT, Ts.AN_HPC, Ts.AN_INVAR, Ts.AN_ADJI,
            Ts.AN_NUMTYPE, Ts.AN_NUMORD, Ts.AN_IREL,
    };
    String[] attrNM = {
            "numb", "case", Ts.AN_ADPREP };

    // числительное
    String[] attrUL = {
            "pos", Ts.AN_PRN, Ts.AN_INVAR, Ts.AN_NUMTYPE,
            Ts.AN_NUMORD, Ts.AN_COLL, Ts.AN_IREL,
    };
    String[] attrUM = {
            "numb", "case", "gend", "anim" };

    // прилагательное
    String[] attrAL = {
            "pos", Ts.AN_PRN, Ts.AN_INVAR, Ts.AN_NUMTYPE,
            Ts.AN_NUMORD, Ts.AN_ORDIN, Ts.AN_IREL};
    String[] attrAM = {
            "numb", "case", "gend", "anim",
            Ts.AN_ATTR, Ts.AN_DGR };

    // глагол
    String[] attrVL = {
            "pos", "perf", "tran", Ts.AN_FREQ,
            Ts.AN_IMPERS};
    String[] attrVM = {
            "repr", "voice", "mood", "tense",
            "numb", "pers", "gend", Ts.AN_REFL };

    // причастие
    String[] attrPL = {
            "pos", "perf", "tran"};
    String[] attrPM = {
            "repr", "voice", "mood", "tense",
            "numb", "pers", "gend", Ts.AN_REFL,
            "case", "anim", Ts.AN_ATTR, Ts.AN_DGR };

    // вводное слово
    String[] attrPredL = {
            "pos" };
    String[] attrPredM = {};

    // вводное слово
    String[] attrParL = {
            "pos" };
    String[] attrParM = {};

    // остальные части речи
    String[] attrXL = {
            "pos", Ts.AN_PRN, Ts.AN_IREL, "GCAS" };
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
        this(true, false, false);
    }

    public GlossLabelGenerator(boolean baseInCaption, boolean html, boolean printSID) {
        this.baseInСaption = baseInCaption;
        this.html = html;
        this.printSID = printSID;
    }

    final StringBuffer curHypoStr = new StringBuffer();

    public String generateCaption(Treenotation trn) {
        TString TYPE = (TString) trn.get("TYPE");
        TString SYNTTYPE = (TString) trn.get("SYNTTYPE");

        if (TYPE==null || TYPE.equals("Morph") || TYPE.equals("Gramm")) {
            if(trn.get("lem") == null && trn.get("pos") == null || !baseInСaption) {
                return "???";
            }

            Object o = trn.get("lem");
            String base = (o==null) ? "" : o.toString();
            if (printSID) {
                base = trn.getId() + ": " + base;
            }

            if (showZINDEX) {
                o = trn.get("ZINDEX");
                if (o!=null) {
                    base = base + "<"+o+">";
                }
            }

            return base;
        } else if (TYPE.equals("Syntax")) {
            if (SYNTTYPE==null) {
                return "???";
            }

            String base = SYNTTYPE.toString();
            if (printSID) {
                base = trn.getId() + ": " + base;
            }

            return base;
        }
        return "";
    }

    public void init(TreenotationsContext context) {
    }

    public String generateLabel(Treenotation trn) {
        return generateMorphLabel(trn);
    }

    public String generateMorphLabel(Treenotation trn) {
        synchronized(curHypoStr) {
            if(trn.get("pos") == null) {
                return "";
            }

            String pos = trn.get("pos").toString();
            Object o = trn.get("TYPE");
            String TYPE = o == null ? null : o.toString();

            curHypoStr.setLength(0);

            if (TagSet.AV_POS_N.equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrNL)).append(")");
                String s = getPropsByNames(trn,attrNM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else  if (TagSet.AV_POS_PRED.equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrPredL)).append(")");
                String s = getPropsByNames(trn,attrPredM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else  if (TagSet.AV_POS_PAR.equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrParL)).append(")");
                String s = getPropsByNames(trn,attrParM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else  if (TagSet.AV_POS_NUM.equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn,attrUL)).append(")");
                String s = getPropsByNames(trn,attrUM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if (TagSet.AV_POS_V.equals(pos)) {
                Object repr = trn.get("repr");
                if (repr != null && "part".equals(repr.toString())) {
                    curHypoStr.append("(").append(getPropsByNames(trn,attrPL)).append(")");
                    String s = getPropsByNames(trn,attrPM);
                    if (s.length() > 0) {
                        curHypoStr.append(" ").append(s);
                    }
                } else {
                    curHypoStr.append("(").append(getPropsByNames(trn,attrVL)).append(")");
                    String s = getPropsByNames(trn,attrVM);
                    if (s.length() > 0) {
                        curHypoStr.append(" ").append(s);
                    }
                }
            } else if(TagSet.AV_POS_A.equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn,attrAL)).append(")");
                String s = getPropsByNames(trn,attrAM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if ("unknown".equals(TYPE)){
                curHypoStr.append("<unknown>");
            } else {
                curHypoStr.append("(").append(getPropsByNames(trn,attrXL)).append(")");
                String s = getPropsByNames(trn,attrXM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            }
            return curHypoStr.toString();
        }
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

    public void setPrintSID(boolean printSID) {
        this.printSID = printSID;
    }
}
