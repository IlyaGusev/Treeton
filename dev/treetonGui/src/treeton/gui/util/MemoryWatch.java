/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;

public class MemoryWatch implements Runnable {
    private static int maxMemBufferLength = 50;
    private static String[] units = {"b", "Kb", "Mb", "Gb"};
    Runtime rt;
    StringBuffer totalString;
    StringBuffer memBuffer = null;
    private MStatusBar sbar;
    private boolean canRun = false;

    public MemoryWatch(MStatusBar _sbar) {
        sbar = _sbar;
        rt = Runtime.getRuntime();
        memBuffer = new StringBuffer(maxMemBufferLength);
        canRun = true;
    }

    public static StringBuffer getMemString(long amount, StringBuffer buf) {
        int i = 0;
        double freeMemMain = (double) amount;
        if (amount > 800) {
            i++;
            freeMemMain = freeMemMain / 1024;
            if (freeMemMain > 800) {
                i++;
                freeMemMain = freeMemMain / 1024;
                if (freeMemMain > 800) {
                    i++;
                    freeMemMain = freeMemMain / 1024;
                }
            }
        }
        // "n" и "d" - округление до одного знака после запятой
        int n = (int) freeMemMain;
        int d = (int) ((freeMemMain - n) * 10 + 0.5);
        if (d == 10) {
            n++;
            d = 0;
        }

        // "ni" - округление целого
        int ni = n + ((d >= 5) ? 1 : 0);
        if (ni < 100) {
            buf.append(n).append('.').append(d);
        } else {
            buf.append(ni);
        }
        buf.append(units[i]);

        return buf;
    }

    public void run() {
        long totalMem;
        while (canRun) {
            try {
                totalMem = rt.totalMemory();
                memBuffer.setLength(0);
                getMemString(totalMem - rt.freeMemory(), memBuffer);
                memBuffer.append(" / ");
                getMemString(totalMem, memBuffer);
                SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                sbar.setMemory(memBuffer.toString());
                            }
                        }
                );
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
        }
    }

    public void start() {
    }

    public void destroy() {
    }

    public void denyRun() {
        canRun = false;
    }
}
