/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

public class RusAttributes {

    protected static String trTableDelim = "|";
    protected static String attrDelim = ",";
    protected static String cmdDelim = ",";
    protected static String eq = "=";

    protected static final String trTable =
            "res._:" + TagSet.AN_POS + "=_|" +
                    "res.N:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_CAS + eq + TagSet.AV_CAS_NOM + "|" +
                    "res.G:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_CAS + eq + TagSet.AV_CAS_GEN + "|" +
                    "res.D:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_CAS + eq + TagSet.AV_CAS_DAT + "|" +
                    "res.A:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_CAS + eq + TagSet.AV_CAS_ACC + "|" +
                    "res.I:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_CAS + eq + TagSet.AV_CAS_INST + "|" +
                    "res.L:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_CAS + eq + TagSet.AV_CAS_PRP + "|" +
                    "res.G2:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_CAS + eq + TagSet.AV_CAS_GEN2 + cmdDelim +
                    TagSet.AN_NMB + eq + TagSet.AV_NMB_SG + "|" +
                    "res.L2:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_CAS + eq + TagSet.AV_CAS_PRP2 + cmdDelim +
                    TagSet.AN_NMB + eq + TagSet.AV_NMB_SG + "|" +
                    "res.S:" + TagSet.AN_POS + eq + TagSet.AV_POS_A + cmdDelim +
                    TagSet.AN_ATTR + eq + TagSet.AV_ATTR_SH + "|" +
                    "res.Cmp:" + TagSet.AN_POS + eq + TagSet.AV_POS_A + cmdDelim +
                    TagSet.AN_DGR + eq + TagSet.AV_DGR_COMP + "|" +
                    "res.s:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",NMB=sg|" +
                    "res.p:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",NMB=pl|" +
                    "res.m:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=m|" +
                    "res.f:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=f|" +
                    "res.n:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=n|" +
                    "res.a:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",ANIM=anim|" +
                    "res.i:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",ANIM=inan|" +
                    "messf.п:" + TagSet.AN_POS + eq + TagSet.AV_POS_A + "|" +
                    "messf.м:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=m,ANIM=inan|" +
                    "messf.ж:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=f,ANIM=inan|" +
                    "messf.М:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=m,ANIM=inan|" +
                    "messf.Ж:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=f,ANIM=inan|" +
                    "messf.мж:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=mf,ANIM=inan|" +
                    "messf.с:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",POS=N,GEND=n,ANIM=inan|" +
                    "messf.мо:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=m,ANIM=anim|" +
                    "messf.жо:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=f,ANIM=anim|" +
                    "messf.со:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=n,ANIM=anim|" +
                    "messf.мо-жо:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=mf,ANIM=anim|" +
                    "messf.ч:" + TagSet.AN_POS + eq + TagSet.AV_POS_NUM + "|" +
                    "messf.числ.:" + TagSet.AN_POS + eq + TagSet.AV_POS_NUM + "|" +
                    "messf.н:" + TagSet.AN_POS + eq + TagSet.AV_POS_ADV + "|" +
                    "mess.н;:" + TagSet.AN_POS + eq + TagSet.AV_POS_ADV + "|" +
                    "messf.предик.:" + TagSet.AN_POS + eq + TagSet.AV_POS_PRED + "|" +
                    "messf.предик:" + TagSet.AN_POS + eq + TagSet.AV_POS_PRED + "|" +
                    "messf.вводн:" + TagSet.AN_POS + eq + TagSet.AV_POS_PAR + "|" +
                    "messf.вводн.:" + TagSet.AN_POS + eq + TagSet.AV_POS_PAR + "|" +
                    "messf.сравн.:" + TagSet.AN_POS + eq + TagSet.AV_POS_ADV + "|" +
                    "messf.предл.:" + TagSet.AN_POS + eq + TagSet.AV_POS_PREP + "|" +
                    "messf.союз:" + TagSet.AN_POS + eq + TagSet.AV_POS_CONJ + "|" +
                    "messf.част.:" + TagSet.AN_POS + eq + TagSet.AV_POS_PCL + "|" +
                    "messf.межд.:" + TagSet.AN_POS + eq + TagSet.AV_POS_INTJ + "|" +
                    "messf.мс:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_PRN + eq + TagSet.AV_PRN_PRN + "|" +
                    "messo.мс:" + TagSet.AN_PRN + eq + TagSet.AV_PRN_PRN + "|" +
                    "messf.мс-п:" + TagSet.AN_POS + eq + TagSet.AV_POS_A + cmdDelim +
                    TagSet.AN_PRN + eq + TagSet.AV_PRN_PRN + "|" +
                    "messf.числ.-п:" + TagSet.AN_POS + eq + TagSet.AV_POS_A + "|" +
                    "messf.мп:" + TagSet.AN_POS + eq + TagSet.AV_POS_A + cmdDelim +
                    TagSet.AN_PRN + eq + TagSet.AV_PRN_PRN + "|" +
                    "messf.неод.:" + TagSet.AN_ANIM + eq + TagSet.AV_ANIM_INAN + "|" +
                    "messf.одуш.:" + TagSet.AN_ANIM + eq + TagSet.AV_ANIM_ANIM + "|" +
                    "messf2.п:" + TagSet.AN_ADJI + eq + TagSet.AV_ADJI_ADJI + "|" +
                    "messf2.!!м:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=m,ANIM=inan|" +
                    "messf2.!!ж:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=f,ANIM=inan|" +
                    "messf2.!!с:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=n,ANIM=inan|" +
                    "messf2.!!мо:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=m,ANIM=anim|" +
                    "messf2.!!жо:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=f,ANIM=anim|" +
                    "messf2.!!со:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=f,ANIM=anim|" +
                    "messf2.!!мо-жо:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + ",GEND=mf,ANIM=anim|" +
                    "messf2.!!ф.:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_PNT + eq + TagSet.AV_PNT_FAM + cmdDelim +
                    "PRN=|" +
                    "messf2.!!ф,:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_PNT + eq + TagSet.AV_PNT_FAM + cmdDelim +
                    "PRN=|" +
                    "messf2.!!ф:" + TagSet.AN_POS + eq + TagSet.AV_POS_N + cmdDelim +
                    TagSet.AN_PNT + eq + TagSet.AV_PNT_FAM + cmdDelim +
                    "PRN=|" +
                    "messf2.!!\\ю:OPGEND=opgend|" +
                    "messf2.!!мн.:" + TagSet.AN_GEND + eq + TagSet.AV_GEND_PLT + "|" +
                    "messf2.!!ordin:" + TagSet.AN_ORDIN + eq + TagSet.AV_ORDIN_ORDIN + "|" +
                    "messf2.!!irel:" + TagSet.AN_IREL + eq + TagSet.AV_IREL_IREL + cmdDelim +
                    TagSet.AN_PRN + eq + TagSet.AV_PRN_PRN + "|" +
                    "messf2.безл.:" + TagSet.AN_IMPERS + eq + TagSet.AV_IMPERS_IMPERS + "|" +
                    "messfrq.многокр.:FREQ=freq|" +
                    "";

    protected static Hashtable ht = null;

    public static void buildHashtable() {
        if (ht == null)
            ht = new Hashtable();
        else
            ht.clear();
        StringTokenizer st = new StringTokenizer(trTable, trTableDelim);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            int i = s.indexOf(":");
            if (i > 0) {
                ht.put(s.substring(0, i), s.substring(i + 1));
            } else
                ht.put(s, "");
        }
    }

    public static void translateAndSetProperties(String _prefix,
                                                 String _attrs, Properties _p) {
        boolean ok = false;
        int unknownNo = 1;
        if (ht == null)
            buildHashtable();

        StringTokenizer st = new StringTokenizer(_attrs, attrDelim);
        while (st.hasMoreTokens()) {
            String nextAttr = st.nextToken().trim();
            if (nextAttr != null && nextAttr.length() > 0) {
                String cmds = (String) ht.get(_prefix + nextAttr);
                if (cmds != null) {
                    cmds = cmds.trim();
                    if (cmds.length() > 0) {
                        StringTokenizer scmd = new StringTokenizer(cmds, cmdDelim);
                        while (scmd.hasMoreTokens()) {
                            String cmd = scmd.nextToken().trim();
                            int i = cmd.indexOf("=");
                            if (i > 0) {
                                String propName = cmd.substring(0, i).trim();
                                String propValue = cmd.substring(i + 1).trim();
                                if (propValue.length() > 0) {
                                    _p.setProperty(propName, propValue);
                                } else {
                                    _p.remove(propName);
                                }
                            }
                        }
                    }
                    ok = true;
                } else {
//          _p.setProperty(  "ATTR_"+unknownNo, nextAttr );
//          ok = true;
                }
            }
        }

//    if( !ok )
//      _p.setProperty(  _prefix+"FULL", _attrs );
    }

    public static void setPropertiesFromString(String s, Properties p) {
        if (s != null) {
            s = s.trim();
            if (s.length() > 0) {
                StringTokenizer scmd = new StringTokenizer(s, cmdDelim);
                while (scmd.hasMoreTokens()) {
                    String cmd = scmd.nextToken().trim();
                    int i = cmd.indexOf("=");
                    if (i > 0) {
                        String propName = cmd.substring(0, i).trim();
                        String propValue = cmd.substring(i + 1).trim();
                        if (propValue.length() > 0) {
                            p.setProperty(propName, propValue);
                        } else {
                            p.remove(propName);
                        }
                    }
                }
            }
        }
    }
}
