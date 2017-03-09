/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core;

public class Fraction implements Comparable {
    public int numerator;
    public int denominator;

    public Fraction(int n, int d) throws IllegalArgumentException {
        numerator = n;
        if (d == 0)
            throw new IllegalArgumentException();
        denominator = d;
    }

    public Fraction(Fraction f) {
        numerator = f.numerator;
        denominator = f.denominator;
    }

    public Fraction(Fraction a, Fraction b) throws IllegalArgumentException {
        numerator = b.numerator * a.denominator - a.numerator * b.denominator;
        denominator = 2 * b.denominator * a.denominator;
    }

    public static void sum(Fraction result, Fraction a, int arg2N, int arg2D) {
        int arg1N, arg1D;
        arg1N = a.numerator;
        arg1D = a.denominator;

        result.numerator = arg1N * arg2D + arg2N * arg1D;
        result.denominator = arg1D * arg2D;
    }

    public static void sum(Fraction result, Fraction a, Fraction b) {
        int arg1N, arg1D, arg2N, arg2D;

        arg1N = a.numerator;
        arg1D = a.denominator;
        arg2N = b.numerator;
        arg2D = b.denominator;

        result.numerator = arg1N * arg2D + arg2N * arg1D;
        result.denominator = arg1D * arg2D;
    }

    public static void substract(Fraction result, int arg1N, int arg1D, Fraction b) {
        int arg2N, arg2D;
        arg2N = b.numerator;
        arg2D = b.denominator;

        result.numerator = arg1N * arg2D - arg1D * arg2N;
        result.denominator = arg2D * arg1D;
    }

    public static void substract(Fraction result, Fraction a, int arg2N, int arg2D) {
        int arg1N, arg1D;
        arg1N = a.numerator;
        arg1D = a.denominator;

        result.numerator = arg1N * arg2D - arg1D * arg2N;
        result.denominator = arg2D * arg1D;
    }

    public static void substract(Fraction result, Fraction a, Fraction b) {
        int arg1N, arg1D, arg2N, arg2D;

        arg1N = a.numerator;
        arg1D = a.denominator;
        arg2N = b.numerator;
        arg2D = b.denominator;

        result.numerator = arg1N * arg2D - arg1D * arg2N;
        result.denominator = arg2D * arg1D;
    }

    public static void divide(Fraction result, Fraction a, Fraction b) {
        int arg1N, arg1D, arg2N, arg2D;

        arg1N = a.numerator;
        arg1D = a.denominator;
        arg2N = b.numerator;
        arg2D = b.denominator;

        result.numerator = arg1N * arg2D;
        result.denominator = arg1D * arg2N;
    }

    public static void assign(Fraction result, int arg1N, int arg1D) {
        result.numerator = arg1N;
        result.denominator = arg1D;
    }

    public static void assign(Fraction result, Fraction a) {
        result.numerator = a.numerator;
        result.denominator = a.denominator;
    }

    public int compareTo(Fraction anotherFraction) {
        int ad = this.numerator * anotherFraction.denominator;
        int bc = this.denominator * anotherFraction.numerator;
        return (ad < bc ? -1 : (ad == bc ? 0 : 1));
    }

    public int compareTo(int n, int d) {
        int ad = this.numerator * d;
        int bc = this.denominator * n;
        return (ad < bc ? -1 : (ad == bc ? 0 : 1));
    }

    public int compareTo(Object o) {
        return compareTo((Fraction) o);
    }

    public boolean equals(Object o) {
        return this == o || this.compareTo(o) == 0;
    }

    public String toString() {
        return numerator + "/" + denominator;
    }

    public Fraction add(Fraction anotherFraction) {
        return new Fraction(numerator * anotherFraction.denominator + denominator * anotherFraction.numerator, denominator * anotherFraction.denominator);
    }

    public Fraction substract(Fraction anotherFraction) {
        return new Fraction(numerator * anotherFraction.denominator - denominator * anotherFraction.numerator, denominator * anotherFraction.denominator);
    }

    public Fraction divide(Fraction anotherFraction) {
        return new Fraction(numerator * anotherFraction.denominator, denominator * anotherFraction.numerator);
    }

    public int toInt() {
        return numerator / denominator;
    }

    public int toIntFromUp() {
        if (numerator % denominator != 0)
            return numerator / denominator + 1;
        else
            return numerator / denominator;
    }

    public double toDouble() {
        return (double) numerator / (double) denominator;
    }

}
