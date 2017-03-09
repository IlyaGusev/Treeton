/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.util;

import java.util.Properties;

public class TagSet {

    /**
     * <b>Тип аннотации:</b> Morph<br>
     */
    public static final String AT_MORPH = "Morph";

    /**
     * <b>Тип аннотации:</b> SuperMorph<br>
     */
    public static final String AT_SUPERMORPH = "SuperMorph";

    /**
     * <b>Тип аннотации:</b> PredMorph<br>
     */
    public static final String AT_PREDMORPH = "PredMorph";

    /**
     * <b>Тип аннотации:</b> Token<br>
     */
    public static final String AT_TOKEN = "Token";

    // Названия атрибутов и их значения
    // AN_xxx - имя атрибута (Attribute Name)
    // AV_xxx - значение атрибута (Attribute Value)
    //
    // В комментарии к имени атрибута указывается его русское название,
    // класс атрибута, и примеры.
    // Классы могут быть следующими:
    //   lex  - для лексических атрибутов;
    //   pder - для атрибуты продуктивного и регулярного
    //          словообразования;
    //   infl - для словоизменительных атрибутов.
    //   serv - служебные атрибуты (например, приоритет гипотезы)

    /**
     * <b>Название атрибута:</b> часть речи<br>
     * <b>Класс:</b> lex<br>
     * <b>Область действия:</b> все слова
     */
    public static final String AN_POS = "POS";
    /**
     * <b>Атрибут:</b> часть речи<br>
     * <b>Значение:</b> существительное
     */
    public static final String AV_POS_N = "N";
    /**
     * <b>Атрибут:</b> часть речи<br>
     * <b>Значение:</b> прилагательное
     */
    public static final String AV_POS_A = "A";
    /**
     * Атрибут: часть речи<br>
     * Значение: числительное
     */
    public static final String AV_POS_NUM = "NUM";
    /**
     * Атрибут: часть речи<br>
     * Значение: глагол
     */
    public static final String AV_POS_V = "V";
    /**
     * Атрибут: часть речи<br>
     * Значение: наречие
     */
    public static final String AV_POS_ADV = "ADV";
    /**
     * Атрибут: часть речи<br>
     * Значение: предикатив
     * Примеры: можно, нельзя, пора, ...
     */
    public static final String AV_POS_PRED = "PRED";
    /**
     * Атрибут: артикль<br>
     * Значение: артикль
     */
    public static final String AV_POS_ART = "ART";
    /**
     * Атрибут: часть речи<br>
     * Значение: вводное слово
     * Примеры: конечно, разумеется, ...
     */
    public static final String AV_POS_PAR = "PAR";
    /**
     * Атрибут: часть речи<br>
     * Значение: предлог
     */
    public static final String AV_POS_PREP = "PREP";
    /**
     * Атрибут: союз<br>
     * Значение: союз
     */
    public static final String AV_POS_CONJ = "CONJ";
    /**
     * Атрибут: часть речи<br>
     * Значение: междометие
     */
    public static final String AV_POS_INTJ = "INTJ";
    /**
     * Атрибут: часть речи<br>
     * Значение: частица
     */
    public static final String AV_POS_PCL = "PCL";

    /**
     * Название атрибута: префиксальное отрицание
     * Класс: pder
     * Примеры: некрасивый, неспециалист, нечитаемый, несмело
     */
    public static final String AN_NEG = "NEG";
    /**
     * Атрибут: префиксальное отрицание<br>
     * Значение: негатив
     */
    public static final String AV_NEG_NEG = "neg";

    // Название атрибута: субстантивированность
    // Класс: pder
    // Примеры: столовая, больной, командировочные,
    //   учащийся, das Schreiben
    public static final String AN_SBS = "SBS";
    // Значение: субстантив
    public static final String AV_SBS_SBS = "sbs";

    // Название атрибута: число
    // Класс: infl
    public static final String AN_NMB = "NMB";
    // Значение: единственное
    public static final String AV_NMB_SG = "sg";
    // Значение: множественное
    public static final String AV_NMB_PL = "pl";

    /**
     * Название атрибута: род
     * Класс:
     * AV_POS_N - lex
     * AV_POS_A, AV_POS_V - infl
     */
    public static final String AN_GEND = "GEND";
    /**
     * Атрибут: род
     * Значение: мужской
     */
    public static final String AV_GEND_M = "m";
    /**
     * Атрибут: род
     * Значение: женский
     */
    public static final String AV_GEND_F = "f";
    /**
     * Атрибут: род
     * Значение: средний
     */
    public static final String AV_GEND_N = "n";
    /**
     * Атрибут: род
     * Значение: общий (мужской или женский)
     */
    public static final String AV_GEND_MF = "mf";
    /**
     * Атрибут: род
     * Значение: мужской или средний (для местоимений "его", "ему" и т. п.
     */
    public static final String AV_GEND_MN = "mn";
    /**
     * Атрибут: род
     * Значение: мужской, женский или средний (для местоимений "я", "ты" и т. п.
     */
    public static final String AV_GEND_MFN = "mfn";
    /**
     * Атрибут: род
     * Значение: только множественное число (plurale tantum)
     */
    public static final String AV_GEND_PLT = "plt";

    // Название: одушевленность
    // Класс:
    //   AV_POS_N - lex
    //   AV_POS_A, AV_POS_V  infl
    public static final String AN_ANIM = "ANIM";
    // Значение: одушевленное
    public static final String AV_ANIM_ANIM = "anim";
    // Значение: неодушевленное
    public static final String AV_ANIM_INAN = "inan";

    // Название: падеж
    // Класс: infl
    public static final String AN_CAS = "CAS";
    // Значение: номинатив (именительный)
    public static final String AV_CAS_NOM = "nom";
    // Значение: генитив (родительный)
    public static final String AV_CAS_GEN = "gen";
    // Значение: второй генитив (второй родительный)
    // Пример: дай мне чаю
    public static final String AV_CAS_GEN2 = "gen2";
    // Значение: датив (дательный)
    public static final String AV_CAS_DAT = "dat";
    // Значение: аккузатив (винительный)
    public static final String AV_CAS_ACC = "acc";
    // Значение: второй аккузатив (второй винительный)
    // Пример: пойти в солдаты
    public static final String AV_CAS_ACC2 = "acc2";
    // Значение: инструменталис (творительный)
    public static final String AV_CAS_INST = "inst";
    // Значение: препозиционалис (предложный)
    public static final String AV_CAS_PRP = "prp";
    // Значение: второй препозиционалис (второй предложный)
    // Пример: работать в саду
    public static final String AV_CAS_PRP2 = "prp2";
    // Значение: вокатив (звательный)
    // Пример: мам, пап, тёть
    public static final String AV_CAS_VOC = "voc";

    // Название: посессивность
    // Класс: infl
    public static final String AN_POSS = "POSS";
    // Значение: посессивная форма
    // Пример: John’s, week’s
    public static final String AV_POSS_POSS = "poss";

    // Название: мензуратив
    // Класс: pder
    public static final String AN_SIZE = "SIZE";
    // Значение: диминутив
    // Пример: слоник
    public static final String AV_SIZE_DIM = "dim";
    // Значение: аугментатив
    // Пример: слонище
    public static final String AV_SIZE_AUG = "aug";

    // Название: атрибутивность прилагательного
    //   (полная/краткая форма)
    // Класс: infl
    public static final String AN_ATTR = "ATTR";
    // Значение: краткая форма
    public static final String AV_ATTR_SH = "sh";

    // Название: степень сравнения прилагательного
    // Класс: infl
    public static final String AN_DGR = "DGR";
    // Значение: сравнительная
    public static final String AV_DGR_COMP = "comp";
    // Значение: превосходная
    public static final String AV_DGR_SUP = "sup";

    // Название: аттенуативность прилагательного
    // Класс: pder
    // Пример: посмелее
    public static final String AN_ATT = "ATT";
    // Значение: аттенуатив
    public static final String AV_ATT_ATT = "att";

    // Название: репрезентация глагола
    // Класс: infl
    public static final String AN_REPR = "REPR";
    // Значение: финитная
    public static final String AV_REPR_FIN = "fin";
    // Значение: инфинитив
    public static final String AV_REPR_INF = "inf";
    // Значение: причастие
    public static final String AV_REPR_PART = "part";
    // Значение: герундий / деепричастие
    public static final String AV_REPR_GER = "gern";
    // Значение: инфинитив c zu
    // только для немецкого
    public static final String AV_REPR_ZUINF = "zuinf";

    // Название: аспект (вид) глагола
    // Класс: lex
    public static final String AN_ASP = "ASP";
    // Значение: перфектив (совершенный вид)
    public static final String AV_ASP_PF = "pf";
    // Значение: имперфектив (несовершенный вид)
    public static final String AV_ASP_IPF = "ipf";
    // Значение: двувидовость (перфектив-имперфектив)
    public static final String AV_ASP_PFIPF = "pf_ipf";

    // Название: залог глагола
    // Класс: infl
    public static final String AN_VOX = "VOX";
    // Значение: актив
    public static final String AV_VOX_ACT = "act";
    // Значение: пассив
    public static final String AV_VOX_PASS = "pass";

    // Название: возвратность, рефлексивность глагола
    // Класс: infl
    public static final String AN_REFL = "REFL";
    // Значение: рефлексив (глаголы на -ся, -сь)
    public static final String AV_REFL_REFL = "refl";

    // Название: переходность глагола
    // Класс: lex
    public static final String AN_TRANS = "TRANS";
    // Значение: переходный
    public static final String AV_TRANS_VT = "vt";
    // Значение: непереходный
    public static final String AV_TRANS_VI = "vi";

    /**
     * Название атрибута: безличность<br>
     * Класс: lex<br>
     * Область действия: AV_POS_V
     */
    public static final String AN_IMPERS = "IMPERS";
    /**
     * Значение: безличный глагол
     */
    public static final String AV_IMPERS_IMPERS = "impers";

    /**
     * Название атрибута: многократность<br>
     * Класс: lex<br>
     * Область действия: AV_POS_V
     */
    public static final String AN_FREQ = "FREQ";
    /**
     * Значение: многократнный глагол
     */
    public static final String AV_FREQ_FREQ = "freq";

    // Название: отделяемость префикса
    // Класс: lex
    public static final String AN_TREN = "TREN";
    // Значение: префикс отделяемый
    public static final String AV_TREN_TR = "tr";
    // Значение: префикс неотделяемый
    public static final String AV_TREN_UNTR = "untr";

    // Название: наклонение
    // Класс: infl
    public static final String AN_MD = "MD";
    // Значение: индикатив (изъявительное)
    // Пример: je parle
    public static final String AV_MD_IND = "ind";
    // Значение: императив (повелительное)
    // Пример: parle
    public static final String AV_MD_IMP = "imp";
    // Значение: кондиционалис (условное)
// Пример: je parlerais
    public static final String AV_MD_COND = "cond";
    // Значение: конъюнктив (сослагательное)
    // Пример: je parle
    public static final String AV_MD_CNJ = "cnj";
    // Значение:
    // Пример:
    public static final String AV_MD_CNJ1 = "cnj1";
    // Значение:
    // Пример:
    public static final String AV_MD_CNJ2 = "cnj2";

    // Название: время
    // Класс: infl
    public static final String AN_TNS = "TNS";
    // Значение: настоящее (презенс)
    // Пример: je parle
    public static final String AV_TNS_PRES = "pres";
    // Значение: прошедшее
    // Пример: je parlai
    public static final String AV_TNS_PAST = "past";
    // Значение: прошедшее незавершенное (имперфект)
    // Пример: je parlais
    public static final String AV_TNS_IMPF = "impf";
    // Значение: будущее простое
    // Пример: je parlerai
    public static final String AV_TNS_FUT = "fut";

    // Название: лицо
    // Класс: infl
    public static final String AN_PRS = "PERS";
    // Значение: первое
    public static final String AV_PRS_1 = "1";
    // Значение: второе
    public static final String AV_PRS_2 = "2";
    // Значение: третье
    public static final String AV_PRS_3 = "3";

    // Название: определенность артикля
    // Класс: lex
    // Область действия: AN_POS_ART
    public static final String AN_DEF = "DEF";
    // Значение: определенный
    public static final String AV_DEF_DEF = "def";
    // Значение: неопределенный
    public static final String AV_DEF_INDEF = "indef";

    // Название: топикальность личного местоимения
    // Класс: lex
    public static final String AN_TOP = "TOP";
    // Значение: топик
    // Пример: moi, je parle (фр.)
    public static final String AV_TOP_TOP = "top";

    // Название: местоименность
    // Класс: lex
    // Область действия: существительные, прилагательные. наречия
    public static final String AN_PRN = "PRN";
    // Значение: местоименное
    // Пример: который, каждый, где, там
    public static final String AV_PRN_PRN = "prn";

    // Название: тип числительного
    // Класс: lex
    // Область действия: AN_POS_NUM
    public static final String AN_NUMTYPE = "NUMTYPE";
    // Значение: один (произнесение заканчивается на "один")
    // Пример: 1 21 31 101 2851
    public static final String AV_NUMTYPE_NUM1 = "1ended";
    // Значение: "маленькое"
    // Пример: 2 3 4 43 104 7862
    public static final String AV_NUMTYPE_SML = "small";
    // Значение: "большое"
    // Пример: 5 6 26 15 807 17 12 114 5114 827 8488
    public static final String AV_NUMTYPE_LRG = "large";
    // Значение: десятичная дробь
    // Пример: 0.75 0,75 3,084 507.954
    public static final String AV_NUMTYPE_FRCD = "fractionDecimal";

    // Название: "разрядность" числительного
    // Класс: lex
    // Область действия: AN_POS_NUM
    public static final String AN_NUMORD = "NUMORD";
    // Значение: цифры от 0 до 9
    public static final String AV_NUMORD_FIGS = "figs";
    // Значение: число из нескольких цифр
    public static final String AV_NUMORD_FIGC = "figс";
    // Значение: 10 20 30 100 800 900 5000
    public static final String AV_NUMORD_TENHUN = "tenhun";

    // Название: признак собирательного числительного
    // Класс: lex
    // Область действия: AN_POS_NUM
    public static final String AN_COLL = "COLL";
    // Значение: собирательное
    // Пример: двое, трое, четверо, ... десятеро
    public static final String AV_COLL_COLL = "coll";

    // Название: неизменяемое слово
    // Класс: lex
    // Область действия: AN_POS_*
    public static final String AN_INVAR = "INVAR";
    // Значение: неизменяемое
    // Пример: пальто, рандеву, фондю
    public static final String AV_INVAR_INVAR = "invar";

    // Название: существительное, изменяющееся по адъективному
    //   склонению
    // Класс: lex
    // Область действия: AN_POS_N
    public static final String AN_ADJI = "ADJI";
    // Значение: адъективное склонение (портной, жаркое, суточные)
    public static final String AV_ADJI_ADJI = "adji";

    // Название: вопросительно-относительное местоимение
    // Класс: lex
    // Область действия: существительные, прилагательные. наречия
    public static final String AN_IREL = "IREL";
    // Значение: вопросительно-относительное местоимение (кто, какой, где,...)
    public static final String AV_IREL_IREL = "irel";

    // Название: Singularia tantum
    // Класс: lex
    // Область действия: V_POS_N
    public static final String AN_SGT = "SGT";
    // Значение: Singularia tantum
    public static final String AV_SGT_SGT = "sgt";


    /**
     * Название атрибута: порядковость прилагательного
     * Класс: lex<br>
     * Область действия: AV_POS_A
     */
    public static final String AN_ORDIN = "ORDIN";
    /**
     * Значение: порядковое прилагательное (первый, девятнадцатый, 19-й)
     */
    public static final String AV_ORDIN_ORDIN = "ordin";

    /**
     * Название атрибута: тип имени собственного<br>
     * Класс: lex<br>
     * Область действия: AV_POS_N
     */
    public static final String AN_PNT = "PNT";
    /**
     * Атрибут: PNT<br>
     * Значение: фамилия
     */
    public static final String AV_PNT_FAM = "fam";
    /**
     * Атрибут: PNT<br>
     * Значение: личное имя
     */
    public static final String AV_PNT_FNAM = "fnam";
    /**
     * Атрибут: PNT<br>
     * Значение: отчество
     */
    public static final String AV_PNT_PTRN = "ptrn";

    /**
     * Название атрибута: припредложность местоимения<br>
     * Класс: infl<br>
     * Область действия: AV_POS_N && AV_PRN_PRN
     */
    public static final String AN_ADPREP = "ADPREP";
    /**
     * Атрибут: ADPRED<br>
     * Значение: форма припредложного местоимения с начальной буквой "н"
     */
    public static final String AV_ADPREP_ADPREP = "adprep";

    /**
     * <b>Название:</b> достоверность гипотезы<br>
     * <b>Класс:</b> serv<br>
     * <b>Область действия:</b> все слова
     */
    public static final String AN_RLB = "RLB";
    /**
     * Атрибут: достоверность гипотезы<br>
     * Значение: достоверна - проверена по словарю
     * Примечание: у достоверных гипотез этот атрибут обычно
     * отсутствует
     */
    public static final String AV_RLB_OK = "ok";
    /**
     * Атрибут: достоверность гипотезы<br>
     * Значение: недостоверна - степень достоверности 5 по
     * 10-ти балльной шкале
     */
    public static final String AV_RLB_5 = "5";

    /**
     * <b>Название:</b> лексема<br>
     * <b>Класс:</b> lex<br>
     * <b>Область действия:</b> все слова
     */
    public static final String AN_BASE = "base";

    /**
     * Название атрибута: гипокористичность<br>
     * Класс: lex<br>
     * Область действия: AV_POS_N && AV_PTN_fnam
     */
    public static final String AN_HPC = "HPC";
    /**
     * Атрибут: HPC<br>
     * Значение: гипокористическое (уменьшительно-ласкательное) имя
     */
    public static final String AV_HPC_HPC = "hpc";

    /**
     * Название атрибута: вводность<br>
     * Класс: lex<br>
     * Область действия: AV_POS_ADV
     */
    public static final String AN_PAR = "PAR";
    /**
     * Атрибут: PAR<br>
     * Значение: вводное слово
     */
    public static final String AV_PAR_PAR = "par";

    /**
     * Название атрибута: <br>
     * Класс: serv<br>
     * Область действия:
     */
    public static final String AN_AMBIG = "AMBIG";
    /**
     * Атрибут: <br>
     * Значение:
     */
    public static final String AV_AMBIG_AMBIG = "ambig";

    /**
     * <b>Название:</b> исходное слово<br>
     * <b>Класс:</b> serv<br>
     * <b>Область действия:</b> все слова
     */
    public static final String AN_SOURCE = "SOURCE";

    /**
     * <b>Название:</b> нормализованное исходное слово<br>
     * <b>Класс:</b> serv<br>
     * <b>Область действия:</b> все слова
     */
    public static final String AN_WORDFORM = "WORDFORM";

    /**
     * <b>Название атрибута:</b> идентификатор<br>
     * <b>Класс:</b> serv<br>
     * <b>Область действия:</b> все слова
     */
    public static final String AN_ID = "ID";

    public static boolean isMorph(String s) {
        return AT_MORPH.equals(s);
    }

    public static boolean hasAttrib(Properties prs, String aName, String aVal) {
        return aVal.equals(prs.getProperty(aName));
    }
}
