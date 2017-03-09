/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru;

import treeton.core.*;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.scape.ParseException;
import treeton.core.util.BlockStack;
import treeton.core.util.FileMapper;
import treeton.core.util.LinkedTrns;
import treeton.core.util.sut;
import treeton.scape.ScapePhase;
import treeton.scape.ScapeResult;
import treeton.util.ReWalkerArray;
import treeton.util.Ts;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

public class NumbersProcessor extends Resource {
    protected ReWalkerArray rew;
    protected ScapePhase phase;
    protected StringBuffer sbuf = new StringBuffer();
    protected BlockStack stack = new BlockStack(5);
    protected BlackBoard localBoard = TreetonFactory.newBlackBoard(50, false);
    protected TrnType morphTp;
    protected boolean forGate;
    LinkedTrns.TrnsIterator linkedItr = LinkedTrns.newTrnsIterator(null);
    private String propertiesPath;
    private String propertiesFolder;
    private String rulesFiles;
    private StringBuffer tbuf = new StringBuffer();

    protected void readProperties(URL propertiesURL)
            throws NullPointerException, IOException {
        try {
            Properties p = new Properties();
            p.load(new FileInputStream(propertiesURL.getFile()));

            String path = propertiesURL.getFile();
            String host = propertiesURL.getHost();
            String morphPropPath = "";
            if (host != null && host.length() > 0) {
                morphPropPath += host + ":";
            }
            morphPropPath += path;

            this.propertiesFolder = sut.getFolder(morphPropPath);
            if (!this.propertiesFolder.endsWith("\\") &&
                    !this.propertiesFolder.endsWith("/")) {
                this.propertiesFolder += "/";
            }

            rulesFiles = p.getProperty("rulesFiles");
        } catch (NullPointerException e) {
            System.err.println(new StringBuffer(
                    "Can not read properties file for NumbersProcessor.\nFile: ").
                    append(propertiesPath).toString());
            throw e;
        } catch (IOException e) {
            System.err.println(new StringBuffer(
                    "Can not read properties file for NumbersProcessor.\nFile: ").
                    append(propertiesPath).toString());
            throw e;
        }
    }

    private Properties makeProperties(
            String digits, String pos,
            String cas, String anim) {
        Properties rslt = new Properties();
        rslt.setProperty("base", digits.toLowerCase());
        rslt.setProperty(Ts.AN_POS, pos);
        if (cas != null) {
            rslt.setProperty(Ts.AN_CAS, cas);
        }
        if (anim != null) {
            rslt.setProperty(Ts.AN_ANIM, anim);
        }
        return rslt;
    }

    private Properties makePropertiesA(String source,
                                       String digits, String cas, String nmb, String gend, String anim) {
        Properties rslt = makeProperties(digits, Ts.AV_POS_A, cas, anim);
        rslt.setProperty(Ts.AN_ORDIN, Ts.AV_ORDIN_ORDIN);
        if (nmb != null) {
            rslt.setProperty(Ts.AN_NMB, nmb);
        }
        if (gend != null) {
            rslt.setProperty(Ts.AN_GEND, gend);
        }
        return rslt;
    }

    private Properties makePropertiesNUM(String source,
                                         String digits, String cas, String anim, String numType) {
        Properties rslt = makeProperties(digits, Ts.AV_POS_NUM, cas, anim);
        if (numType != null) {
            rslt.setProperty(Ts.AN_NUMTYPE, numType);
            rslt.setProperty("INVAR", "invar");
        }
        return rslt;
    }

    private Properties makePropertiesNUM(String source,
                                         String digits, String cas, String anim, String numType,
                                         String numOrd) {
        Properties rslt = makePropertiesNUM(source, digits, cas, anim, numType);
        if (numOrd != null) {
            rslt.setProperty(Ts.AN_NUMORD, numOrd);
        }
        return rslt;
    }

    private ArrayList addToArray(ArrayList arr, Object elm) {
        ArrayList rslt = arr;
        if (rslt == null) {
            rslt = new ArrayList();
        }
        rslt.add(elm);
        return rslt;
    }

    protected ArrayList doProcNum(String str)
            throws IllegalArgumentException {
        ArrayList rslt = null;
        int digsLength;
        int lastDigit = -1;
        int prevDigit = -1;
        String digits;
        String flex;

        tbuf.setLength(0);
        char c;
        int i = 0;
        int n = str.length();
        while (i < n) {
            c = str.charAt(i);
            if (Character.isDigit(c)) {
                tbuf.append(c);
            } else {
                break;
            }
            i++;
        }
        digits = tbuf.toString();
        while (i < n) {
            c = str.charAt(i);
            if (c != '-') {
                break;
            }
            i++;
        }
        tbuf.setLength(0);
        while (i < n) {
            c = str.charAt(i);
            if (Character.isLetter(c)) {
                tbuf.append(c);
            } else {
                break;
            }
            i++;
        }
        flex = tbuf.toString().toLowerCase();

        if (digits.length() == 0) {
            throw new IllegalArgumentException();
        }
        digsLength = digits.length();
        lastDigit = Integer.parseInt(digits.substring(digsLength - 1));
        if (digsLength > 1) {
            prevDigit = Integer.parseInt(
                    digits.substring(digsLength - 2, digsLength - 1));
        }
        boolean last13 = (prevDigit == 1 && lastDigit == 3);

        if (flex.length() > 0) {
            if ("ое".equals(flex)) {
                // 1-ое, 2-ое, 104-ое
                // -ое (посл. цифра не 3 или две посл. цифры 13)
                if (lastDigit != 3 || last13) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                }
            } else if ("ые".equals(flex)) {
                // 4-ые, 10-ые, 20-ые, 80-ые
                // -ые (посл. цифра не 3 или две посл. цифры 13)
                if (lastDigit != 3 || last13) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_PL, null, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_PL, null, Ts.AV_ANIM_INAN));
                }
            } else if ("ье".equals(flex)) {
                // 3-ье, 23-ье
                // -ье (либо посл. цифра 3 и предпосл. цифра не 1, либо цифра 3)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                }
            } else if ("е".equals(flex)) {
                // 1-е, 2-е, 104-е
                // е	(посл. цифра не 0)
                // или
                // 10-е, 20-е, 80-е
                // -е (посл. цифра 0)
                if (lastDigit != 0) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                } else {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_PL, null, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_PL, null, Ts.AV_ANIM_INAN));
                }
            } else if ("ами".equals(flex)) {
                // 200-ами
                // -ами (посл. цифры 100, 200, 300, 400, 500, 600, 700, 800, 900)
                if (digsLength >= 3 && lastDigit == 0 &&
                        prevDigit == 0 &&
                        Integer.parseInt(digits.substring(digsLength - 3)) != 0) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_INST, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                }
            } else if ("ыми".equals(flex)) {
                // 1-ыми, 20-ыми
                // -ыми (посл. цифра не 3 или две посл. цифры 13)
                if (lastDigit != 3 || last13) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_PL, null, null));
                }
            } else if ("ими".equals(flex)) {
                // 3-ими, 23-ими
                // -ми (посл. цифра 3, предпосл. не 1)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_PL, null, null));
                }
            } else if ("ми".equals(flex)) {
                // 1-ми, 3-ми, 20-ми
                // -ми (посл. цифра не 7, не 8 или посл. цифры 17, 18)
                // или
                // 7-ми, 8-ми
                // -ми (либо посл. цифра 7 или 8 и предпосл. цифра не 1, либо одиночная цифра 7 или 8)
                if (lastDigit != 7 && lastDigit != 8 ||
                        digsLength >= 2 &&
                                (lastDigit == 7 || lastDigit == 8) &&
                                prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_PL, null, null));
                }
                if ((lastDigit == 7 || lastDigit == 8) &&
                        digsLength >= 2 &&
                        prevDigit != 1 ||
                        digsLength == 1 &&
                                (lastDigit == 7 || lastDigit == 8)) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_GEN, null, Ts.AV_NUMTYPE_LRG));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_DAT, null, Ts.AV_NUMTYPE_LRG));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_PRP, null, Ts.AV_NUMTYPE_LRG));
                }
            } else if ("ти".equals(flex)) {
                // 5-ти
                // -ти (посл. цифра 5, 6, 9, или предпосл. цифра 1, или посл. две цифры 10, 20, 30, 50, 60, 70, 80)
                if (lastDigit == 5 || lastDigit == 6 ||
                        lastDigit == 9 ||
                        digsLength >= 2 && prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_GEN, null, Ts.AV_NUMTYPE_LRG));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_DAT, null, Ts.AV_NUMTYPE_LRG));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_PRP, null, Ts.AV_NUMTYPE_LRG));
                } else if (digsLength >= 2 && lastDigit == 0 &&
                        (prevDigit == 2 || prevDigit == 3 ||
                                prevDigit == 5 || prevDigit == 6 ||
                                prevDigit == 7 || prevDigit == 8)) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_GEN, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_DAT, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_PRP, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                }
            } else if ("и".equals(flex)) {
                // 7-и
                // -и (посл. цифра 5, 6, 7, 8, 9, или предпосл. цифра 1, или посл. две цифры 10, 20, 30, 50, 60, 70, 80)
                if (lastDigit == 5 || lastDigit == 6 ||
                        lastDigit == 7 || lastDigit == 8 ||
                        lastDigit == 9 ||
                        digsLength >= 2 && prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_GEN, null, Ts.AV_NUMTYPE_LRG));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_DAT, null, Ts.AV_NUMTYPE_LRG));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_PRP, null, Ts.AV_NUMTYPE_LRG));
                } else if (digsLength >= 2 && lastDigit == 0 &&
                        (prevDigit == 2 || prevDigit == 3 ||
                                prevDigit == 5 || prevDigit == 6 ||
                                prevDigit == 7 || prevDigit == 8)) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_GEN, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_DAT, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_PRP, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                }

            } else if ("ей".equals(flex) ||
                    "ьей".equals(flex)) {
                // 3-ей, 3-ьей
                // -ьей, -ей (либо посл. цифра 3 и предпосл. не 1, либо одиночная цифра 3)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                }
            } else if ("ий".equals(flex)) {
                // 3-ий, 23-ий
                // ий	(либо посл. цифра 3 и предпосл. не 1, либо одиночная цифра 3)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                }
            } else if ("ой".equals(flex)) {
                // 2-ой, 6-ой, 7-ой, 8-ой
                // -ой (либо посл. цифра 2, 6, 7 или 8 и предпосл. цифра не 1, либо одиночная цифра 2, 6, 7 или 8)
                // или
                // 12-ой, 5-ой
                // -ой (посл. цифра 0, 1, 4, 5, 9 или предпосл. цифра 1)
                if (digsLength >= 2 &&
                        (lastDigit == 2 || lastDigit == 6 ||
                                lastDigit == 7 || lastDigit == 8) &&
                        prevDigit != 1 ||
                        digsLength == 1 &&
                                (lastDigit == 2 || lastDigit == 6 ||
                                        lastDigit == 7 || lastDigit == 8)) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_M, Ts.AV_ANIM_INAN));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                }
                if ((lastDigit == 0 || lastDigit == 1 ||
                        lastDigit == 4 || lastDigit == 5 ||
                        lastDigit == 9) ||
                        prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                }
            } else if ("ый".equals(flex)) {
                // 1-ый, 11-ый, 5-ый
                // -ый (посл. цифра 0, 1, 4, 5, 9 или предпосл. цифра 1)
                if ((lastDigit == 0 || lastDigit == 1 ||
                        lastDigit == 4 || lastDigit == 5 ||
                        lastDigit == 9) ||
                        prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_M, Ts.AV_ANIM_INAN));
                }
            } else if ("й".equals(flex)) {
                // 1-й, 2-й, 3-й
                // -й
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_M, Ts.AV_ANIM_INAN));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
            } else if ("ам".equals(flex)) {
                // 200-ам
                // -ам (посл. цифры 100, 200, 300, 400, 500, 600, 700, 800, 900)
                if (digsLength >= 3 && lastDigit == 0 &&
                        prevDigit == 0 &&
                        Integer.parseInt(digits.substring(digsLength - 3)) != 0) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_DAT, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                }
            } else if ("ьем".equals(flex)) {
                // 3-ьeм
                // -ьем (либо посл. цифра 3, предпосл. цифра не 1, либо одиночная цифра 3)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                }
            } else if ("ем".equals(flex)) {
                // 3-eм, 4-ем
                // -eм (либо посл. цифра 3 или 4, предпосл. цифра не 1, либо одиночная цифра 3 или 4)
                if ((lastDigit == 3 || lastDigit == 4) &&
                        prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_DAT, null, Ts.AV_NUMTYPE_SML));
                }
            } else if ("ьим".equals(flex) ||
                    "им".equals(flex)) {
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_DAT, Ts.AV_NMB_PL, null, null));
                }
            } else if ("ом".equals(flex)) {
                // 1-ом, 2-ом, 4-ом
                // -ом (посл. цифра не 3 или предпосл. цифра 1)
                if (lastDigit != 3 || prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                }
            } else if ("ум".equals(flex)) {
                // 2-ум
                // -ум (либо посл. цифра 2 и предпосл. цифра не 1, либо одиночная цифра 2)
                Properties props;
                if (lastDigit == 2 && (digsLength == 1 || prevDigit != 1)) {
                    rslt = addToArray(rslt, props = makePropertiesNUM(str, digits,
                            Ts.AV_CAS_DAT, null, Ts.AV_NUMTYPE_SML));
                    props.put("HASGEND", "hasgend");
                }
            } else if ("ым".equals(flex)) {
                // 1-ым, 2-ым, 4-ым
                // -ым (посл. цифра не 3 или предпосл. цифра 1)
                if (lastDigit != 3 || prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_DAT, Ts.AV_NMB_PL, null, null));
                }
            } else if ("м".equals(flex)) {
                // 1-м, 2-м, 3-м
                // -м
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_INST, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_PRP, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_DAT, Ts.AV_NMB_PL, null, null));
            } else if ("его".equals(flex) ||
                    "ьего".equals(flex)) {
                // 3-его, 3-ьего
                // -ьего, -его (либо посл. цифра 3 и предпосл. цифра не 1, либо одиночная цифра 3)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_M, Ts.AV_ANIM_ANIM));
                }
            } else if ("ого".equals(flex)) {
                // 1-ого, 2-ого
                // -ого (посл. цифра не 3 или предпосл. цифра 1)
                if (lastDigit != 3 || prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_M, Ts.AV_ANIM_ANIM));
                }
            } else if ("го".equals(flex)) {
                // 1-го, 2-го, 3-го
                // -го
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_GEN, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_M, Ts.AV_ANIM_ANIM));
            } else if ("ему".equals(flex) ||
                    "ьему".equals(flex)) {
                // 3-ему
                // -ьему, -ему (либо посл. цифра 3 и предпосл. не 1, либо одиночная цифра 3)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                }
            } else if ("ому".equals(flex)) {
                // 1-ому, 2-ому
                // -ому (посл. цифра не 3 или две посл. 13)
                if (lastDigit != 3 || last13) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
                }
            } else if ("му".equals(flex)) {
                // 1-му, 2-му, 3-му
                // -му
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_M, null));
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_DAT, Ts.AV_NMB_SG, Ts.AV_GEND_N, null));
            } else if ("ах".equals(flex)) {
                // 200-ах
                // -ах (посл. цифры 100, 200, 300, 400, 500, 600, 700, 800, 900)
                if (digsLength >= 3 && lastDigit == 0 &&
                        prevDigit == 0 &&
                        Integer.parseInt(digits.substring(digsLength - 3)) != 0) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_PRP, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                }
            } else if ("ех".equals(flex)) {
                // 3-ех, 4-ех
                // -ех (либо посл. цифра 3 или 4 и предпосл. не 1, либо одиночная цифра 3 или 4)
                if ((lastDigit == 3 || lastDigit == 4) &&
                        prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_GEN, null, Ts.AV_NUMTYPE_SML));
                    if (digsLength == 1) {
                        rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                                Ts.AV_CAS_ACC, Ts.AV_ANIM_ANIM, Ts.AV_NUMTYPE_SML));
                    }
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_PRP, null, Ts.AV_NUMTYPE_SML));
                }
            } else if ("ух".equals(flex)) {
                // 2-ух
                // -ух (посл. цифра 2 и предпосл. не 1, либо одиночная 2)
                if (lastDigit == 2 && prevDigit != 1) {
                    Properties props;
                    rslt = addToArray(rslt, props = makePropertiesNUM(str, digits,
                            Ts.AV_CAS_GEN, null, Ts.AV_NUMTYPE_SML));
                    props.put("HASGEND", "hasgend");
                    rslt = addToArray(rslt, props = makePropertiesNUM(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_ANIM_ANIM, Ts.AV_NUMTYPE_SML));
                    props.put("HASGEND", "hasgend");
                    rslt = addToArray(rslt, props = makePropertiesNUM(str, digits,
                            Ts.AV_CAS_PRP, null, Ts.AV_NUMTYPE_SML));
                    props.put("HASGEND", "hasgend");
                }
            } else if ("ых".equals(flex)) {
                // 1-ых, 5-ых, 80-ых
                // -ых (посл. цифра не 3 или предпосл. цифра 1)
                if (lastDigit != 3 || prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_PL, null, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_PL, null, Ts.AV_ANIM_ANIM));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_PL, null, null));
                }
            } else if ("х".equals(flex)) {
                // 2-х
                // -х (либо посл. цифра - 2, 3 или 4, предпосл. не 1, либо одиночная цифра - 2, 3 или 4)
                // или
                // 1-х, 5-х
                // -х (посл. цифра 0, 1, 5, 6, 7, 8, 9 или предпосл. 1)
                if ((lastDigit == 2 || lastDigit == 3 ||
                        lastDigit == 4) && prevDigit != 1) {
                    Properties props;
                    rslt = addToArray(rslt, props = makePropertiesNUM(str, digits,
                            Ts.AV_CAS_GEN, null, Ts.AV_NUMTYPE_SML));
                    if (lastDigit == 2) {
                        props.put("HASGEND", "hasgend");
                    }
                    rslt = addToArray(rslt, props = makePropertiesNUM(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_ANIM_ANIM, Ts.AV_NUMTYPE_SML));
                    if (lastDigit == 2) {
                        props.put("HASGEND", "hasgend");
                    }
                    rslt = addToArray(rslt, props = makePropertiesNUM(str, digits,
                            Ts.AV_CAS_PRP, null, Ts.AV_NUMTYPE_SML));
                    if (lastDigit == 2) {
                        props.put("HASGEND", "hasgend");
                    }
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_PL, null, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_PL, null, Ts.AV_ANIM_ANIM));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_PL, null, null));
                }
                if (lastDigit == 0 || lastDigit == 1 ||
                        lastDigit > 5 || prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_GEN, Ts.AV_NMB_PL, null, null));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_PL, null, Ts.AV_ANIM_ANIM));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_PRP, Ts.AV_NMB_PL, null, null));
                }
            } else if ("мь".equals(flex)) {
                // 7-мь
                // -мь (либо посл. цифра 7 или 8 и предпосл. цифра не 1, либо одиночная цифра 7 или 8)
                if ((lastDigit == 7 || lastDigit == 8) && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_NOM, null, Ts.AV_NUMTYPE_LRG));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_ACC, null, Ts.AV_NUMTYPE_LRG));
                }
            } else if ("ть".equals(flex)) {
                // 11-ть
                // -ть (посл. цифра 5, 6, 9, или предпосл. цифра 1, или посл. две цифры 20, 30)
                if (lastDigit == 5 || lastDigit == 6 || lastDigit == 9 ||
                        prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_NOM, null, Ts.AV_NUMTYPE_LRG));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_ACC, null, Ts.AV_NUMTYPE_LRG));
                } else if (lastDigit == 0 && (prevDigit == 2 || prevDigit == 3)) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_NOM, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_ACC, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                }
            } else if ("ую".equals(flex)) {
                // 1-ую
                // -ую (посл. цифра не 3 или предпосл. 1)
                if (lastDigit != 3 || prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                }
            } else if ("мью".equals(flex)) {
                // 7-мью
                // -мью (либо посл. цифра 7 или 8 и предпосл. цифра не 1, либо одиночная цифра 7 или 8)
                if ((lastDigit == 7 || lastDigit == 8) && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_INST, null, Ts.AV_NUMTYPE_LRG));
                }
            } else if ("тью".equals(flex)) {
                // 5-тью
                // -тью (посл. цифра 5, 6, 9, или предпосл. цифра 1, или последние две цифры 10, 20, 30, 50, 60, 70, 80)
                if ((lastDigit == 5 || lastDigit == 6 || lastDigit == 9) ||
                        prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_INST, null, Ts.AV_NUMTYPE_LRG));
                } else if (lastDigit == 0 &&
                        (prevDigit == 2 ||
                                prevDigit == 3 || prevDigit == 5 ||
                                prevDigit == 6 || prevDigit == 7 ||
                                prevDigit == 8)) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_INST, null, Ts.AV_NUMTYPE_LRG, Ts.AV_NUMORD_TENHUN));
                }
            } else if ("ью".equals(flex)) {
                // 3-ью
                // -ью (либо посл. цифра 3 и предпосл. не 1, либо одиночная цифра 3)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                }
            } else if ("ю".equals(flex)) {
                // 5-ю
                // -ю (посл. цифра 5, 6, 7, 8, 9, или предпосл. цифра 1, или посл. цифра 0 и предпосл. не 0)
                // или
                // 1-ю, 3-ю
                // -ю (либо посл. цифра 1, 2, 3 или 4 и предпосл. цифра не 1, либо одиночная цифра 1, 2, 3 или 4)

                if ((lastDigit == 5 || lastDigit == 6 || lastDigit == 7 ||
                        lastDigit == 8 || lastDigit == 9) ||
                        prevDigit == 1 ||
                        digsLength >= 2 && lastDigit == 0 && prevDigit != 0) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_INST, null, Ts.AV_NUMTYPE_LRG));
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                }
                if ((lastDigit == 1 || lastDigit == 2 ||
                        lastDigit == 3 || lastDigit == 4) &&
                        prevDigit != 1 ||
                        (lastDigit == 0 && prevDigit == 0)) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_ACC, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                }
            } else if ("ая".equals(flex)) {
                // 1-ая, 13-ая
                // -ая (посл. цифра не 3 или предпосл. цифра 1)
                if (lastDigit != 3 || prevDigit == 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                }

            } else if ("емя".equals(flex) ||
                    "мя".equals(flex)) {
                // 3-емя
                // -емя, -мя (либо посл. цифра 3, предпосл. цифра не 1, либо одиночная цифра 3)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_INST, null, Ts.AV_NUMTYPE_SML));
                }
            } else if ("умя".equals(flex)) {
                // 2-умя
                // -умя (либо посл. цифра 2 и предпосл. цифра не 1, либо одиночная цифра 2)
                if (lastDigit == 2 && prevDigit != 1) {
                    Properties props;
                    rslt = addToArray(rslt, props = makePropertiesNUM(str, digits,
                            Ts.AV_CAS_INST, null, Ts.AV_NUMTYPE_SML));
                    props.put("HASGEND", "hasgend");
                }
            } else if ("ьмя".equals(flex)) {
                // 4-ьмя
                // -ьмя (либо посл. цифра 4 и предпосл. цифра не 1, либо одиночная цифра 4)
                if (lastDigit == 4 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesNUM(str, digits,
                            Ts.AV_CAS_INST, null, Ts.AV_NUMTYPE_SML));
                }
            } else if ("мя".equals(flex)) {
                // 2-мя, 3-мя, 4-мя
                // -мя (либо посл. цифра 2, 3 или 4 и предпосл. цифра не 1, либо одиночная цифра 2, 3 или 4)
                if ((lastDigit == 2 || lastDigit == 3 || lastDigit == 4) &&
                        prevDigit != 1) {
                    Properties props;
                    rslt = addToArray(rslt, props = makePropertiesNUM(str, digits,
                            Ts.AV_CAS_INST, null, Ts.AV_NUMTYPE_SML));
                    if (lastDigit == 2) {
                        props.put("HASGEND", "hasgend");
                    }
                }
            } else if ("ья".equals(flex)) {
                // 3-ья, 23-ья
                // -ья (либо посл. цифра 3 и предпоследняя не 1, либо одиночная цифра 3)
                if (lastDigit == 3 && prevDigit != 1) {
                    rslt = addToArray(rslt, makePropertiesA(str, digits,
                            Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
                }
            } else if ("я".equals(flex)) {
                // 1-я, 2-я, 3-я, 13-я
                // -я
                rslt = addToArray(rslt, makePropertiesA(str, digits,
                        Ts.AV_CAS_NOM, Ts.AV_NMB_SG, Ts.AV_GEND_F, null));
            }
        } else {
        }
        return rslt;
    }

    protected String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        localBoard.clean();
        phase.reset(storage);

        ArrayList<Treenotation> buffer = new ArrayList<Treenotation>();

        Token rightEdge = null;

        while (phase.nextStartPoint(rightEdge) != null) {
            rightEdge = null;
            ScapeResult r;

            while ((r = phase.nextResult()) != null) {

                linkedItr.reset(r.getLastMatched());

                while (linkedItr.hasNext()) {
                    stack.push(linkedItr.nextTrn());
                }

                sbuf.setLength(0);

                while (!stack.isEmpty()) {
                    Treenotation trn = (Treenotation) stack.pop();
                    sbuf.append(trn.getText());
                }
                ArrayList<Properties> l;
                String source = sbuf.toString();
                if (r.size() > 1) {
                    l = doProcNum(source);
                    if (l == null) {
                        l = rew.getHyposAsProps(source);
                        for (Properties p : l) {
                            p.setProperty("base", source.toLowerCase());
                        }
                    } else {
                        ArrayList<Properties> l1 = rew.getHyposAsProps(source);
                        for (Properties p : l1) {
                            p.setProperty("base", source.toLowerCase());
                        }
                        l.addAll(l1);
                    }
                } else {
                    l = rew.getHyposAsProps(source);
                    for (Properties p : l) {
                        p.setProperty("base", source.toLowerCase());
                    }
                }
                if (l == null || l.size() == 0) {
                    continue;
                }

                Token end = r.getEndToken();

                if (rightEdge == null) {
                    rightEdge = end;
                    for (int i = 0; i < l.size(); i++) {
                        fillBlackBoard(source, l.get(i));
                        buffer.add(TreetonFactory.newTreenotation(r.getStartToken(), r.getEndToken(), morphTp, localBoard));
                    }
                } else {
                    int cmp = end.compareTo(rightEdge);
                    if (cmp > 0) {
                        buffer.clear();
                        for (int i = 0; i < l.size(); i++) {
                            fillBlackBoard(source, l.get(i));
                            buffer.add(TreetonFactory.newTreenotation(r.getStartToken(), r.getEndToken(), morphTp, localBoard));
                        }
                        rightEdge = end;
                    } else if (cmp == 0) {
                        for (int i = 0; i < l.size(); i++) {
                            fillBlackBoard(source, l.get(i));
                            buffer.add(TreetonFactory.newTreenotation(r.getStartToken(), r.getEndToken(), morphTp, localBoard));
                        }
                    }
                }
            }

            for (Treenotation aBuffer : buffer) {
                storage.addPostFactum(aBuffer);
            }
            buffer.clear();
        }
        storage.applyPostFactumTrns();
        return null;
    }

    protected void stop() {
    }

    protected void processTerminated() {
    }

    protected void updateProperties(Properties p, String source) {
        p.setProperty("ID", "-1");
        p.setProperty("DICTID", "-1");
        p.setProperty("ACCPL", "0");
        p.setProperty("WORDFORM", source);
        p.setProperty("NOTATION", "numerical");
        p.setProperty("TYPE", "Morph");
        p.setProperty("kind", "number");
    }

    protected void fillBlackBoard(String source, Properties p) {
        updateProperties(p, source);
        localBoard.fill(morphTp, p);
    }

    protected void init() throws ResourceInstantiationException {
        try {
            morphTp = getTrnContext().getType((String) getInitialParameters().get("Morph_type"));
            if (morphTp == null) {
                throw new ResourceInstantiationException("Unable to locate treenotation type for the morphological info");
            }

            URL propertiesURL;
            try {
                propertiesURL = new URL(getResContext().getFolder(), (String) getInitialParameters().get("propertiesURL"));
            } catch (MalformedURLException e) {
                throw new ResourceInstantiationException("Malformed url exception during NumbersProcessor instantiation", e);
            } catch (ContextException e) {
                throw new ResourceInstantiationException("ContextException exception during NumbersProcessor instantiation", e);
            }

            this.propertiesPath = propertiesURL.getPath();
            try {
                readProperties(propertiesURL);
            } catch (IOException e) {
                throw new ResourceInstantiationException("Wrong properties " + propertiesURL.getPath(), e);
            }

            rew = new ReWalkerArray();
            if (rulesFiles != null && rulesFiles.length() > 0) {
                rew.loadFromList(propertiesFolder, rulesFiles);
            }

            URL path;
            try {
                path = new URL(getResContext().getFolder(), (String) getInitialParameters().get("templatesPhase"));
                phase = new ScapePhase(getTrnContext().getTypes());
                char[] arr = FileMapper.map2memory(path.getPath());
                phase.readIn(arr, 0, arr.length - 1);
                phase.initialize();
            } catch (MalformedURLException e) {
                throw new ResourceInstantiationException("Malformed url exception during StarlingMorphApplier instantiation", e);
            } catch (ContextException e) {
                throw new ResourceInstantiationException("Problem with context", e);
            } catch (IOException e) {
                throw new ResourceInstantiationException("IOException when trying to read phase with templates", e);
            } catch (ParseException e) {
                throw new ResourceInstantiationException("Unable to parse phase with templates", e);
            }

        } catch (TreetonModelException e) {
            throw new ResourceInstantiationException("Error with model", e);
        }
    }

    protected void deInit() {
        rew = null;
        phase = null;
    }

}
