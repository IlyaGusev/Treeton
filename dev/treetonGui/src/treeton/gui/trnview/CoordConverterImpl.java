/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.trnview;

import treeton.core.Fraction;
import treeton.core.Token;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class CoordConverterImpl implements CoordConverter {
    private SortedMap<Fraction, Double> map = new TreeMap<Fraction, Double>();

    public CoordConverterImpl() {
//        map.put(new Fraction(0, 1), 0.0);
    }

    private static Fraction startFraction(Token tok) {
        return new Fraction(tok.getStartNumerator(), tok.getStartDenominator());
    }

    private static Fraction endFraction(Token tok) {
        return new Fraction(tok.getEndNumerator(), tok.getEndDenominator());
    }

    /**
     * Вычисление длины токена в координатах после масштабирования. Вычисленная длина не должна быть меньше длины токена
     * в координатах хранилища (символах), т.к. код отрисовки определяет токены, помещающиеся на экране, исходя из их
     * длины в координатах хранилища. Другими словами, коэффициент масштабирования для каждого токена должен быть >=1;
     * токены можно растягивать, но нельзя сжимать.
     *
     * @param tok            токен
     * @param maxLabelLength максимальная длина тринотаций лэйблов на данном токене
     *                       todo: (?если тринотация покрывает несколько токенов надо распределять длину на эти токены пропорционально их длине?)
     * @return длина токена в координатах после масштабирования; коэффициент масштабирования для каждого токена должен быть >=1
     */
    protected abstract double getLength(Token tok, int maxLabelLength);

    public void setLabelLength(Token trn, int maxLabelLength) {
        Double start = map.get(startFraction(trn));
        if (start == null) {
            map.clear();
            map.put(startFraction(trn), start = 0.0);
        }
        Double newEnd = start + getLength(trn, maxLabelLength);

        Fraction key = endFraction(trn);
        Double oldEnd = (Double) map.put(key, newEnd);
        if (oldEnd != null && !oldEnd.equals(newEnd)) {
            Iterator<Map.Entry<Fraction, Double>> it = map.tailMap(key).entrySet().iterator();
            assert it.hasNext();
            it.next();
            while (it.hasNext()) {
                Map.Entry<Fraction, Double> entry = it.next();
                entry.setValue(entry.getValue() + newEnd - oldEnd);
            }
        }
    }

    public double getSymbWidth(int i) {
        return storage2scaled(i + 1) - storage2scaled(i);
    }

    public double scaledDistToStart(double from, Token tok) {
        Double f = (Double) map.get(startFraction(tok));
        assert f != null;
        return f - storage2scaled(from);
    }

    public double scaledDistToEnd(double from, Token tok) {
        Double f = (Double) map.get(endFraction(tok));
        assert f != null;
        return f - storage2scaled(from);
    }

    public double storage2scaled(double pos) {
        assert map.firstKey().toDouble() <= pos && map.lastKey().toDouble() >= pos;

        Fraction floor = new Fraction((int) Math.floor(pos), 1);
        Map.Entry<Fraction, Double> right = null;

        for (Map.Entry<Fraction, Double> entry : map.tailMap(floor).entrySet()) {
            double d = entry.getKey().toDouble();
            if (d == pos)
                return entry.getValue();
            if (d < pos)
                continue;
            right = entry;
            break;
        }
        assert right != null;

        Fraction left = map.headMap(right.getKey()).lastKey();

        double stright = right.getKey().toDouble();
        double scright = right.getValue();
        double stleft = left.toDouble();
        double scleft = map.get(left);
        return scleft + (pos - stleft) * (scright - scleft) / (stright - stleft);
    }

    public double scaled2storage(double x) {
        Map.Entry<Fraction, Double> right = null;
        for (Map.Entry<Fraction, Double> entry : map.entrySet()) {
            if (entry.getValue() > x) {
                right = entry;
                break;
            }
        }
        if (right == null)
            return Double.POSITIVE_INFINITY;
        Fraction left = map.headMap(right.getKey()).lastKey();

        double stright = right.getKey().toDouble();
        double scright = right.getValue();
        double stleft = left.toDouble();
        double scleft = map.get(left);
        return stleft + (x - scleft) * (stright - stleft) / (scright - scleft);
    }

    public double scaledDist(double from, double to) {
        return storage2scaled(to) - storage2scaled(from);
    }

    public double increaseStoragePos(double storagePos, double scaledEps) {
        return scaled2storage(storage2scaled(storagePos) + scaledEps);
    }

    public void reset() {
        map.clear();
    }
}
