package com.animaconnected.bluetooth.utils;

public class MathsUtils {

    /**
     * Calculates the modulo operator using floored division
     * <p/>
     * Let's call the return value r. It's defined by a = q*b + r, with q chosen as q = floor (a/b).
     * This gives the result the same sign as the divisor b.
     * <p/>
     * Here implemented using Java's built-in modulo operator.
     */
    public static int floorMod(final int a, final int b) {
        return ((a % b) + b) % b;
    }

    /**
     * Calculates the modulo operator using floored division
     * <p/>
     * Let's call the return value r. It's defined by a = q*b + r, with q chosen as q = floor (a/b).
     * This gives the result the same sign as the divisor b.
     * <p/>
     * Here implemented using Java's built-in modulo operator.
     */
    public static double floorMod(final double a, final double b) {
        return ((a % b) + b) % b;
    }
}
