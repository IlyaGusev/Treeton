/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.labelgen;

import treeton.core.TString;
import treeton.core.Treenotation;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.util.TagSet;
import treeton.util.Ts;

import java.util.regex.Pattern;

public class GrammLabelGenerator
        implements TrnLabelGenerator {

    final StringBuffer curHypoStr = new StringBuffer();
    boolean baseInСaption;
    boolean html;
    boolean printSID;
    boolean showZINDEX;
    // существительное
    String[] attrNL = {
            Ts.AN_POS, Ts.AN_GEND, Ts.AN_ANIM, Ts.AN_PRN,
            Ts.AN_PNT, Ts.AN_HPC, Ts.AN_INVAR, Ts.AN_ADJI,
            Ts.AN_NUMTYPE, Ts.AN_NUMORD, Ts.AN_IREL,
    };
    String[] attrNM = {
            Ts.AN_NMB, Ts.AN_CAS, Ts.AN_ADPREP};
    // числительное
    String[] attrUL = {
            Ts.AN_POS, Ts.AN_PRN, Ts.AN_INVAR, Ts.AN_NUMTYPE,
            Ts.AN_NUMORD, Ts.AN_COLL, Ts.AN_IREL,
    };
    String[] attrUM = {
            Ts.AN_NMB, Ts.AN_CAS, Ts.AN_GEND, Ts.AN_ANIM};
    // прилагательное
    String[] attrAL = {
            Ts.AN_POS, Ts.AN_PRN, Ts.AN_INVAR, Ts.AN_NUMTYPE,
            Ts.AN_NUMORD, Ts.AN_ORDIN, Ts.AN_IREL};
    String[] attrAM = {
            Ts.AN_NMB, Ts.AN_CAS, Ts.AN_GEND, Ts.AN_ANIM,
            Ts.AN_ATTR, Ts.AN_DGR};
    // глагол
    String[] attrVL = {
            Ts.AN_POS, Ts.AN_ASP, Ts.AN_TRANS, Ts.AN_FREQ,
            Ts.AN_IMPERS};
    String[] attrVM = {
            Ts.AN_REPR, Ts.AN_VOX, Ts.AN_MD, Ts.AN_TNS,
            Ts.AN_NMB, Ts.AN_PRS, Ts.AN_GEND, Ts.AN_REFL};
    // причастие
    String[] attrPL = {
            Ts.AN_POS, Ts.AN_ASP, Ts.AN_TRANS};
    String[] attrPM = {
            Ts.AN_REPR, Ts.AN_VOX, Ts.AN_MD, Ts.AN_TNS,
            Ts.AN_NMB, Ts.AN_PRS, Ts.AN_GEND, Ts.AN_REFL,
            Ts.AN_CAS, Ts.AN_ANIM, Ts.AN_ATTR, Ts.AN_DGR};
    // вводное слово
    String[] attrPredL = {
            Ts.AN_POS};
    String[] attrPredM = {};
    // вводное слово
    String[] attrParL = {
            Ts.AN_POS};
    String[] attrParM = {};
    // остальные части речи
    String[] attrXL = {
            Ts.AN_POS, Ts.AN_PRN, Ts.AN_IREL, "GCAS"};
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

    public GrammLabelGenerator() {
        this(true, false, false, true);
    }

    public GrammLabelGenerator(boolean baseInCaption, boolean showZINDEX, boolean html, boolean printSID) {
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
        TString TYPE = (TString) trn.get("TYPE");
        TString SYNTTYPE = (TString) trn.get("SYNTTYPE");

        if (TYPE == null || TYPE.equals("Morph") || TYPE.equals("Gramm")) {
            if (trn.get(Ts.AN_BASE) == null && trn.get(Ts.AN_POS) == null || !baseInСaption) {
                return "???";
            }

            Object o = trn.get(Ts.AN_BASE);
            String base = (o == null) ? "" : o.toString();
            if (printSID) {
                base = trn.getId() + ": " + base;
            }

            if (showZINDEX) {
                o = trn.get("ZINDEX");
                if (o != null) {
                    base = base + "<" + o + ">";
                }
            }

            return base;
        } else if (TYPE.equals("Syntax")) {
            if (SYNTTYPE == null) {
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
        synchronized (curHypoStr) {
            if (trn.get(Ts.AN_POS) == null) {
                return "";
            }

            String pos = trn.get(Ts.AN_POS).toString();
            Object o = trn.get("TYPE");
            String TYPE = o == null ? null : o.toString();

            curHypoStr.setLength(0);

            if (TagSet.AV_POS_N.equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrNL)).append(")");
                String s = getPropsByNames(trn, attrNM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if (TagSet.AV_POS_PRED.equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrPredL)).append(")");
                String s = getPropsByNames(trn, attrPredM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if (TagSet.AV_POS_PAR.equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrParL)).append(")");
                String s = getPropsByNames(trn, attrParM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if (TagSet.AV_POS_NUM.equals(pos)) {
                curHypoStr.append("(").append(getPropsByNames(trn, attrUL)).append(")");
                String s = getPropsByNames(trn, attrUM);
                if (s.length() > 0) {
                    curHypoStr.append(" ").append(s);
                }
            } else if (TagSet.AV_POS_V.equals(pos)) {
                Object repr = trn.get(Ts.AN_REPR);
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
            } else if (TagSet.AV_POS_A.equals(pos)) {
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
