package tech.anapad.rpicm4experiments.util;

/**
 * {@link BitUtil} contains utility functions for bits.
 */
public class BitUtil {

    /**
     * Creates a bit mask with the given <code>msb</code> and <code>lsb</code>.
     *
     * @param msb the MSB (0 - 31) (inclusive)
     * @param lsb the LSB (0 - 31) (inclusive)
     *
     * @return the bit mask
     */
    private static int createMask(int msb, int lsb) {
        return (~0 << (msb + 1)) | ~(~0 << lsb);
    }

    /**
     * Sets <code>value</code> inside <code>toSet</code> at the bits defined by <code>msb</code> and <code>lsb</code>.
     *
     * @param toSet the value to set bits in
     * @param value the value to set inside <code>toSet</code>
     * @param msb   the MSB (0 - 31) (inclusive)
     * @param lsb   the LSB (0 - 31) (inclusive)
     *
     * @return the set value
     */
    public static int setBits(int toSet, int value, int msb, int lsb) {
        int mask = createMask(msb, lsb);
        toSet |= ~mask & (value << lsb);
        toSet &= mask | (value << lsb);
        return toSet;
    }

    /**
     * Sets <code>value</code> inside <code>toSet</code> at the <code>index</code> bit.
     *
     * @param toSet the value to set bits in
     * @param value the value to set inside <code>toSet</code>
     * @param index the index (0 - 31) (inclusive)
     *
     * @return the set value
     */
    public static int setBit(int toSet, int value, int index) {
        return setBits(toSet, value, index, index);
    }

    /**
     * Gets the value inside <code>toGet</code> at the bits defined by <code>msb</code> and <code>lsb</code>.
     *
     * @param toGet the value to get bits from
     * @param msb   the MSB (0 - 31) (inclusive)
     * @param lsb   the LSB (0 - 31) (inclusive)
     *
     * @return the value
     */
    public static int getBits(int toGet, int msb, int lsb) {
        int mask = createMask(msb, lsb);
        return (toGet & ~mask) >> lsb;
    }

    /**
     * Gets the value inside <code>toGet</code> at the <code>index</code> bit.
     *
     * @param toGet the value to get bits from
     * @param index the index (0 - 31) (inclusive)
     *
     * @return the value
     */
    public static int getBit(int toGet, int index) {
        return getBits(toGet, index, index);
    }
}
