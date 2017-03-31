/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.fsm;

import treeton.core.util.EntryComparator;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;

public class MeterStatistics extends AbstractMap {
    class CellInfo {
        double rating;
        double overstressed;
        double unstressed;

        public CellInfo(double rating, double overstressed, double unstressed) {
            this.rating = rating;
            this.overstressed = overstressed;
            this.unstressed = unstressed;
        }

        @Override
        public String toString() {
            String os = df.format(overstressed);
            String us = df.format(unstressed);
            if ("0".equals(os) && "0".equals(us)) {
                return df.format(rating);
            } else {
                return df.format(rating)+"("+ os +","+ us +")";
            }

        }
    }

    class Info {
        Map<Integer,CellInfo> ratingsByNumberOfFoots = new HashMap<Integer, CellInfo>();

        public Info() {
        }

        public Info(Info value) {
            ratingsByNumberOfFoots.putAll(value.ratingsByNumberOfFoots);
        }

        public void merge(Info otherInfo, MergeMode mode) {
            for (Map.Entry<Integer, CellInfo> entry : ratingsByNumberOfFoots.entrySet()) {
                CellInfo d = otherInfo.ratingsByNumberOfFoots.get(entry.getKey());
                if (d != null) {
                    entry.setValue(merge(mode, d, entry.getValue()));
                } else {
                    entry.setValue(merge(mode, new CellInfo(0,0,0), entry.getValue()));
                }
            }
            for (Map.Entry<Integer, CellInfo> entry : otherInfo.ratingsByNumberOfFoots.entrySet()) {
                CellInfo d = ratingsByNumberOfFoots.get(entry.getKey());
                if (d == null) {
                    ratingsByNumberOfFoots.put(entry.getKey(),merge(mode, new CellInfo(0,0,0), entry.getValue()));
                }
            }
        }

        private CellInfo merge(MergeMode mode, CellInfo d, CellInfo v) {
            CellInfo value=new CellInfo(0,0,0);

            if (mode == MergeMode.AVERAGE) {
                value.rating = (v.rating + d.rating) / 2;
                value.overstressed = (v.overstressed + d.overstressed) / 2;
                value.unstressed = (v.unstressed + d.unstressed) / 2;
            } else if (mode == MergeMode.MAX) {
                throw new UnsupportedOperationException();
            }
            return value;
        }
    }

    private Map<Meter,Info> stats = new HashMap<Meter, Info>();

    public void add(Meter meter, int numberOfFoots, double value, double overstressed, double unstressed) {
        Info info = stats.get(meter);
        if (info == null) {
            info = new Info();
            stats.put(meter,info);
        }

        info.ratingsByNumberOfFoots.put(numberOfFoots,new CellInfo(value,overstressed,unstressed));
    }

    public enum MergeMode {
        AVERAGE,
        MAX
    }

    public void merge(MeterStatistics other, MergeMode mode) {
        for (Map.Entry<Meter, Info> entry : stats.entrySet()) {
            Info otherInfo = other.stats.get(entry.getKey());
            if (otherInfo != null) {
                entry.getValue().merge(otherInfo,mode);
            }
        }
        for (Map.Entry<Meter, Info> entry : other.stats.entrySet()) {
            Info info = stats.get(entry.getKey());
            if (info == null) {
                stats.put(entry.getKey(), new Info(entry.getValue()));
            }
        }
    }

    @Override
    public Set entrySet() {
        return stats.entrySet();
    }

    private int toStringFontSize=3;
    static DecimalFormat df = new DecimalFormat();

    static {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(dfs);
        df.setMaximumFractionDigits(2);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();

        TreeSet<Integer> allFoots = new TreeSet<Integer>();

        for (Info info : stats.values()) {
            allFoots.addAll(info.ratingsByNumberOfFoots.keySet());
        }

        buf.append("\n<table align=\"center\" border=\"1\" cellpadding=\"4\" cellspacing=\"1\">\n");
        buf.append("  <tr>\n");
        buf.append("    <td bgcolor=\"").append("rgb(180,180,180)").append("\">").
                append("<b><font size=\"").append(toStringFontSize).append("\">Метр\\Стопность</font></b></td>\n");
        for (Integer allFoot : allFoots) {
            buf.append("    <td bgcolor=\"").append("rgb(180,180,180)").append("\"><b><font size=\"").
                    append(toStringFontSize).append("\">").append(allFoot).append("</font></b></td>\n");
        }
        buf.append("  </tr>");

        List<Map.Entry<Meter, Info>> entries = new ArrayList<Entry<Meter, Info>>(stats.entrySet());
        //noinspection unchecked
        Collections.sort(entries, new EntryComparator());

        for (Map.Entry<Meter, Info> entry : entries) {
            buf.append("  <tr>\n");
            buf.append("    <td bgcolor=\"").append(getColor(entry.getKey())).append("\"><b><font size=\"").append(toStringFontSize).append("\">").
                    append(entry.getKey().getName()).append("</font></b></td>\n");
            for (Integer allFoot : allFoots) {
                CellInfo d = entry.getValue().ratingsByNumberOfFoots.get(allFoot);
                if (d == null) {
                    d = new CellInfo(0,0,0);
                }
                buf.append("    <td bgcolor=\"").append(getColor(entry.getKey())).append("\"><font size=\"").append(toStringFontSize).append("\">").
                        append(d).append("</font></td>\n");
            }
            buf.append("  </tr>");
        }
        buf.append("</table>\n");

        return buf.toString();
    }

    private String getColor(Meter meter) {
        int p = meter.getPriority();

        Color c = Color.WHITE;

        while ( p > 0 ) {
            c = c.darker();
            p--;
        }

        return "rgb("+c.getRed()+","+c.getGreen()+","+c.getBlue()+")";
    }

    public void setToStringFontSize(int toStringFontSize) {
        this.toStringFontSize = toStringFontSize;
    }
    
    public Map<Meter,Set<Integer>> findMetersAboveThreshold() {
        Map<Meter,Set<Integer>> result = new HashMap<Meter, Set<Integer>>();
        
        for (Entry<Meter, Info> entry : stats.entrySet()) {
            Meter m = entry.getKey();

            for (Entry<Integer, CellInfo> e : entry.getValue().ratingsByNumberOfFoots.entrySet()) {
                if (e.getValue().rating >= m.getThreshold()) {
                    Set<Integer> set = result.get(m);
                    if (set == null) {
                        set = new HashSet<Integer>();
                        result.put(m,set);
                    }
                    set.add(e.getKey());
                }
            }
        }

        return result;
    }

    public Map<Meter,Set<Integer>> findBestMeters() {
        Map<Meter,Set<Integer>> result = new HashMap<Meter, Set<Integer>>();

        double max = 0;

        for (Entry<Meter, Info> entry : stats.entrySet()) {
            for (Entry<Integer, CellInfo> e : entry.getValue().ratingsByNumberOfFoots.entrySet()) {
                if (max < e.getValue().rating) {
                    max = e.getValue().rating;
                }
            }
        }

        for (Entry<Meter, Info> entry : stats.entrySet()) {
            Meter m = entry.getKey();

            for (Entry<Integer, CellInfo> e : entry.getValue().ratingsByNumberOfFoots.entrySet()) {
                if (e.getValue().rating == max) {
                    Set<Integer> set = result.get(m);
                    if (set == null) {
                        set = new HashSet<Integer>();
                        result.put(m,set);
                    }
                    set.add(e.getKey());
                }
            }
        }

        return result;
    }

    public Double getUnstressed(Meter key, Integer integer) {
        CellInfo ci = getCellInfo(key, integer);
        if (ci == null)
            return null;

        return ci.unstressed;
    }

    public Double getOverstressed(Meter key, Integer integer) {
        CellInfo ci = getCellInfo(key, integer);
        if (ci == null)
            return null;

        return ci.overstressed;
    }

    private CellInfo getCellInfo(Meter key, Integer integer) {
        Info info = stats.get(key);

        if (info == null)
            return null;

        CellInfo ci = info.ratingsByNumberOfFoots.get(integer);

        if (ci == null)
            return null;
        return ci;
    }

}
