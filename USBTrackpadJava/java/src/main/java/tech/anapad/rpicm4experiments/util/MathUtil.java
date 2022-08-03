package tech.anapad.rpicm4experiments.util;

/**
 * {@link MathUtil} contains utility functions for math.
 */
public class MathUtil {

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
