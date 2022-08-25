package tech.anapad.rpicm4experiments.jni;

import static tech.anapad.rpicm4experiments.util.BitUtil.getBit;
import static tech.anapad.rpicm4experiments.util.BitUtil.setBit;

/**
 * {@link JNIFunctions} is used to interface with the low-level C functions via JNI.
 */
public class JNIFunctions {

    static {
        // Load the .so shared library that should be placed in '/lib' and be named exactly "libJavaExperiments.so"
        System.loadLibrary("JavaExperiments");
    }

    /**
     * Starts the low-level I2C interface.
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public static native void i2cStart() throws Exception;

    /**
     * Stops the low-level I2C interface.
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public static native void i2cStop() throws Exception;

    /**
     * Writes a byte to a register of an I2C slave.
     *
     * @param slaveAddress          the slave address
     * @param registerAddress       the register address
     * @param registerData          the register data
     * @param is8BitRegisterAddress <code>true</code> for 8 bit register address, <code>false</code> for 16 bit
     *                              register address
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public static native void i2cWriteRegisterByte(short slaveAddress, short registerAddress, byte registerData,
            boolean is8BitRegisterAddress) throws Exception;

    /**
     * Writes an array of bytes to the registers of an I2C slave.
     *
     * @param slaveAddress          the slave address
     * @param registerAddress       the register address
     * @param registerData          the register data
     * @param is8BitRegisterAddress <code>true</code> for 8 bit register address, <code>false</code> for 16 bit
     *                              register address
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public static native void i2cWriteRegisterBytes(short slaveAddress, short registerAddress, byte[] registerData,
            boolean is8BitRegisterAddress) throws Exception;

    /**
     * Reads a byte from a register of an I2C slave.
     *
     * @param slaveAddress          the slave address
     * @param registerAddress       the register address
     * @param is8BitRegisterAddress <code>true</code> for 8 bit register address, <code>false</code> for 16 bit
     *                              register address
     *
     * @return the read byte
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public static native byte i2cReadRegisterByte(short slaveAddress, short registerAddress,
            boolean is8BitRegisterAddress) throws Exception;

    /**
     * Reads an array of bytes from the registers of an I2C slave.
     *
     * @param slaveAddress          the slave address
     * @param registerAddress       the register address
     * @param readSize              the number of bytes to read
     * @param is8BitRegisterAddress <code>true</code> for 8 bit register address, <code>false</code> for 16 bit
     *                              register address
     *
     * @return the read byte array
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public static native byte[] i2cReadRegisterBytes(short slaveAddress, short registerAddress, int readSize,
            boolean is8BitRegisterAddress) throws Exception;

    // TODO add documentation to below

    private static void i2cSetRegisterBit(short slaveAddress, short registerAddress, boolean is8BitRegisterAddress,
            int value, int index) throws Exception {
        byte registerByte = i2cReadRegisterByte(slaveAddress, registerAddress, is8BitRegisterAddress);
        registerByte = (byte) setBit(registerByte, value, index);
        i2cWriteRegisterByte(slaveAddress, registerAddress, registerByte, is8BitRegisterAddress);
    }

    public static void i2cRegisterBitSet(short slaveAddress, short registerAddress, boolean is8BitRegisterAddress,
            int index) throws Exception {
        i2cSetRegisterBit(slaveAddress, registerAddress, is8BitRegisterAddress, 1, index);
    }

    public static void i2cRegisterBitReset(short slaveAddress, short registerAddress, boolean is8BitRegisterAddress,
            int index) throws Exception {
        i2cSetRegisterBit(slaveAddress, registerAddress, is8BitRegisterAddress, 0, index);
    }

    public static boolean i2cRegisterBitGet(short slaveAddress, short registerAddress, boolean is8BitRegisterAddress,
            int index) throws Exception {
        return getBit(i2cReadRegisterByte(slaveAddress, registerAddress, is8BitRegisterAddress), index) == 1;
    }
}
