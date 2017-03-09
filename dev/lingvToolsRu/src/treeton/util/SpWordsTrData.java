/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

public class SpWordsTrData {
    public static final String next = "|";
    public static final String to = ":";
    public static final String eq = "=";
    public static final String trTable =
            Ts.AV_POS_N + to + Ts.AN_POS + eq + Ts.AV_POS_N + next +
                    Ts.AV_POS_A + to + Ts.AN_POS + eq + Ts.AV_POS_A + next +
                    Ts.AV_POS_NUM + to + Ts.AN_POS + eq + Ts.AV_POS_NUM + next +
                    Ts.AV_POS_V + to + Ts.AN_POS + eq + Ts.AV_POS_V + next +
                    Ts.AV_POS_ADV + to + Ts.AN_POS + eq + Ts.AV_POS_ADV + next +
                    Ts.AV_POS_PRED + to + Ts.AN_POS + eq + Ts.AV_POS_PRED + next +
                    Ts.AV_POS_ART + to + Ts.AN_POS + eq + Ts.AV_POS_ART + next +
                    Ts.AV_POS_PAR + to + Ts.AN_POS + eq + Ts.AV_POS_PAR + next +
                    Ts.AV_POS_PREP + to + Ts.AN_POS + eq + Ts.AV_POS_PREP + next +
                    Ts.AV_POS_CONJ + to + Ts.AN_POS + eq + Ts.AV_POS_CONJ + next +
                    Ts.AV_POS_INTJ + to + Ts.AN_POS + eq + Ts.AV_POS_INTJ + next +
                    Ts.AV_POS_PCL + to + Ts.AN_POS + eq + Ts.AV_POS_PCL + next +

                    Ts.AV_NEG_NEG + to + Ts.AN_NEG + eq + Ts.AV_NEG_NEG + next +

                    Ts.AV_SBS_SBS + to + Ts.AN_SBS + eq + Ts.AV_SBS_SBS + next +

                    Ts.AV_NMB_SG + to + Ts.AN_NMB + eq + Ts.AV_NMB_SG + next +
                    Ts.AV_NMB_PL + to + Ts.AN_NMB + eq + Ts.AV_NMB_PL + next +

                    Ts.AV_GEND_M + to + Ts.AN_GEND + eq + Ts.AV_GEND_M + next +
                    Ts.AV_GEND_F + to + Ts.AN_GEND + eq + Ts.AV_GEND_F + next +
                    Ts.AV_GEND_N + to + Ts.AN_GEND + eq + Ts.AV_GEND_N + next +
                    Ts.AV_GEND_MF + to + Ts.AN_GEND + eq + Ts.AV_GEND_MF + next +
                    Ts.AV_GEND_MN + to + Ts.AN_GEND + eq + Ts.AV_GEND_MN + next +
                    Ts.AV_GEND_MFN + to + Ts.AN_GEND + eq + Ts.AV_GEND_MFN + next +
                    Ts.AV_GEND_PLT + to + Ts.AN_GEND + eq + Ts.AV_GEND_PLT + next +

                    Ts.AV_ANIM_ANIM + to + Ts.AN_ANIM + eq + Ts.AV_ANIM_ANIM + next +
                    Ts.AV_ANIM_INAN + to + Ts.AN_ANIM + eq + Ts.AV_ANIM_INAN + next +

                    Ts.AV_CAS_NOM + to + Ts.AN_CAS + eq + Ts.AV_CAS_NOM + next +
                    Ts.AV_CAS_GEN + to + Ts.AN_CAS + eq + Ts.AV_CAS_GEN + next +
                    Ts.AV_CAS_GEN2 + to + Ts.AN_CAS + eq + Ts.AV_CAS_GEN2 + next +
                    Ts.AV_CAS_DAT + to + Ts.AN_CAS + eq + Ts.AV_CAS_DAT + next +
                    Ts.AV_CAS_ACC + to + Ts.AN_CAS + eq + Ts.AV_CAS_ACC + next +
                    Ts.AV_CAS_ACC2 + to + Ts.AN_CAS + eq + Ts.AV_CAS_ACC2 + next +
                    Ts.AV_CAS_INST + to + Ts.AN_CAS + eq + Ts.AV_CAS_INST + next +
                    Ts.AV_CAS_PRP + to + Ts.AN_CAS + eq + Ts.AV_CAS_PRP + next +
                    Ts.AV_CAS_PRP2 + to + Ts.AN_CAS + eq + Ts.AV_CAS_PRP2 + next +
                    Ts.AV_CAS_VOC + to + Ts.AN_CAS + eq + Ts.AV_CAS_VOC + next +

                    Ts.AV_POSS_POSS + to + Ts.AN_POSS + eq + Ts.AV_POSS_POSS + next +

                    Ts.AV_SIZE_DIM + to + Ts.AN_SIZE + eq + Ts.AV_SIZE_DIM + next +
                    Ts.AV_SIZE_AUG + to + Ts.AN_SIZE + eq + Ts.AV_SIZE_AUG + next +

                    Ts.AV_ATTR_SH + to + Ts.AN_ATTR + eq + Ts.AV_ATTR_SH + next +

                    Ts.AV_DGR_COMP + to + Ts.AN_DGR + eq + Ts.AV_DGR_COMP + next +
                    Ts.AV_DGR_SUP + to + Ts.AN_DGR + eq + Ts.AV_DGR_SUP + next +

                    Ts.AV_ATT_ATT + to + Ts.AN_ATT + eq + Ts.AV_ATT_ATT + next +

                    Ts.AV_REPR_FIN + to + Ts.AN_REPR + eq + Ts.AV_REPR_FIN + next +
                    Ts.AV_REPR_INF + to + Ts.AN_REPR + eq + Ts.AV_REPR_INF + next +
                    Ts.AV_REPR_PART + to + Ts.AN_REPR + eq + Ts.AV_REPR_PART + next +
                    Ts.AV_REPR_GER + to + Ts.AN_REPR + eq + Ts.AV_REPR_GER + next +
                    Ts.AV_REPR_ZUINF + to + Ts.AN_REPR + eq + Ts.AV_REPR_ZUINF + next +

                    Ts.AV_ASP_PF + to + Ts.AN_ASP + eq + Ts.AV_ASP_PF + next +
                    Ts.AV_ASP_IPF + to + Ts.AN_ASP + eq + Ts.AV_ASP_IPF + next +
                    Ts.AV_ASP_PFIPF + to + Ts.AN_ASP + eq + Ts.AV_ASP_PFIPF + next +

                    Ts.AV_VOX_ACT + to + Ts.AN_VOX + eq + Ts.AV_VOX_ACT + next +
                    Ts.AV_VOX_PASS + to + Ts.AN_VOX + eq + Ts.AV_VOX_PASS + next +

                    Ts.AV_REFL_REFL + to + Ts.AN_REFL + eq + Ts.AV_REFL_REFL + next +

                    Ts.AV_TRANS_VT + to + Ts.AN_TRANS + eq + Ts.AV_TRANS_VT + next +
                    Ts.AV_TRANS_VI + to + Ts.AN_TRANS + eq + Ts.AV_TRANS_VI + next +

                    Ts.AV_IMPERS_IMPERS + to + Ts.AN_IMPERS + eq + Ts.AV_IMPERS_IMPERS + next +

                    Ts.AV_FREQ_FREQ + to + Ts.AN_FREQ + eq + Ts.AV_FREQ_FREQ + next +

                    Ts.AV_TREN_TR + to + Ts.AN_TREN + eq + Ts.AV_TREN_TR + next +
                    Ts.AV_TREN_UNTR + to + Ts.AN_TREN + eq + Ts.AV_TREN_UNTR + next +

                    Ts.AV_MD_IND + to + Ts.AN_MD + eq + Ts.AV_MD_IND + next +
                    Ts.AV_MD_IMP + to + Ts.AN_MD + eq + Ts.AV_MD_IMP + next +
                    Ts.AV_MD_COND + to + Ts.AN_MD + eq + Ts.AV_MD_COND + next +
                    Ts.AV_MD_CNJ + to + Ts.AN_MD + eq + Ts.AV_MD_CNJ + next +
                    Ts.AV_MD_CNJ1 + to + Ts.AN_MD + eq + Ts.AV_MD_CNJ1 + next +
                    Ts.AV_MD_CNJ2 + to + Ts.AN_MD + eq + Ts.AV_MD_CNJ2 + next +

                    Ts.AV_TNS_PRES + to + Ts.AN_TNS + eq + Ts.AV_TNS_PRES + next +
                    Ts.AV_TNS_PAST + to + Ts.AN_TNS + eq + Ts.AV_TNS_PAST + next +
                    Ts.AV_TNS_IMPF + to + Ts.AN_TNS + eq + Ts.AV_TNS_IMPF + next +
                    Ts.AV_TNS_FUT + to + Ts.AN_TNS + eq + Ts.AV_TNS_FUT + next +

                    Ts.AV_PRS_1 + to + Ts.AN_PRS + eq + Ts.AV_PRS_1 + next +
                    Ts.AV_PRS_2 + to + Ts.AN_PRS + eq + Ts.AV_PRS_2 + next +
                    Ts.AV_PRS_3 + to + Ts.AN_PRS + eq + Ts.AV_PRS_3 + next +

                    Ts.AV_DEF_DEF + to + Ts.AN_DEF + eq + Ts.AV_DEF_DEF + next +
                    Ts.AV_DEF_INDEF + to + Ts.AN_DEF + eq + Ts.AV_DEF_INDEF + next +

                    Ts.AV_TOP_TOP + to + Ts.AN_TOP + eq + Ts.AV_TOP_TOP + next +

                    Ts.AV_PRN_PRN + to + Ts.AN_PRN + eq + Ts.AV_PRN_PRN + next +

                    "hasgend" + to + "HASGEND" + eq + "hasgend" + next +
                    Ts.AV_NUMTYPE_NUM1 + to + Ts.AN_NUMTYPE + eq + Ts.AV_NUMTYPE_NUM1 + next +
                    Ts.AV_NUMTYPE_SML + to + Ts.AN_NUMTYPE + eq + Ts.AV_NUMTYPE_SML + next +
                    Ts.AV_NUMTYPE_LRG + to + Ts.AN_NUMTYPE + eq + Ts.AV_NUMTYPE_LRG + next +
                    Ts.AV_NUMTYPE_FRCD + to + Ts.AN_NUMTYPE + eq + Ts.AV_NUMTYPE_FRCD + next +

                    Ts.AV_NUMORD_FIGS + to + Ts.AN_NUMORD + eq + Ts.AV_NUMORD_FIGS + next +
                    Ts.AV_NUMORD_FIGC + to + Ts.AN_NUMORD + eq + Ts.AV_NUMORD_FIGC + next +
                    Ts.AV_NUMORD_TENHUN + to + Ts.AN_NUMORD + eq + Ts.AV_NUMORD_TENHUN + next +

                    Ts.AV_COLL_COLL + to + Ts.AN_COLL + eq + Ts.AV_COLL_COLL + next +

                    Ts.AV_INVAR_INVAR + to + Ts.AN_INVAR + eq + Ts.AV_INVAR_INVAR + next +

                    Ts.AV_ADJI_ADJI + to + Ts.AN_ADJI + eq + Ts.AV_ADJI_ADJI + next +

                    Ts.AV_IREL_IREL + to + Ts.AN_IREL + eq + Ts.AV_IREL_IREL + next +

                    Ts.AV_SGT_SGT + to + Ts.AN_SGT + eq + Ts.AV_SGT_SGT + next +

                    Ts.AV_ORDIN_ORDIN + to + Ts.AN_ORDIN + eq + Ts.AV_ORDIN_ORDIN + next +

                    Ts.AV_PNT_FAM + to + Ts.AN_PNT + eq + Ts.AV_PNT_FAM + next +
                    Ts.AV_PNT_FNAM + to + Ts.AN_PNT + eq + Ts.AV_PNT_FNAM + next +
                    Ts.AV_PNT_PTRN + to + Ts.AN_PNT + eq + Ts.AV_PNT_PTRN + next +

                    Ts.AV_ADPREP_ADPREP + to + Ts.AN_ADPREP + eq + Ts.AV_ADPREP_ADPREP + next +

                    Ts.AV_HPC_HPC + to + Ts.AN_HPC + eq + Ts.AV_HPC_HPC + next +

                    Ts.AV_PAR_PAR + to + Ts.AN_PAR + eq + Ts.AV_PAR_PAR + next +

                    Ts.AV_AMBIG_AMBIG + to + Ts.AN_AMBIG + eq + Ts.AV_AMBIG_AMBIG;
}
